/* Copyright (c) 2005, University of Oslo, Norway
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 
 *  * Neither the name of the University of Oslo nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *      
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.vortikal.index.lucene;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.index.management.ManagementException;
import org.vortikal.index.management.Reindexer;
import org.vortikal.index.observation.FilterCriterion;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;

/**
 * Re-indexer implemented for Lucene indexes.
 * Currently only does full or partial (subtree) reindexing, recursively.
 * Dispatches to its own worker thread if property <code>asynchronous</code> is
 * <code>true</code>.
 *   
 * Will only allow one worker running at a time, whether it runs asynchronously 
 * or not.
 * 
 * @author oyviste

 * TODO: This might be turned into a Lucene-independent index operation, which
 *       will work ontop of our own Index interface, when it's finished (in
 *       other words, it won't be directly tied to Lucene (hopefully)).
 * TODO: Avoid having to configure a reindexer for each and every index.
 * TODO: More intelligent synchronization, by traversing repository tree and
 *       index tree in parallel, only reindexing the differences (incremental
 *       in-indexing).
 * TODO: Not suitable for large repository in full-reindexing mode without any
 *       sensible filter configured.
 *       Re-indexing a live repository from scratch can lead to 
 *       consistency problems, when going through the repository API. One 
 *       solution is to put repository in read-only mode during reindexing. 
 *       Another would be to reindex directly from database, fetching an ordered
 *       snapshot-tree of all resources, and traversing this. Changes occuring 
 *       in the mean time would be picked up after reindexing finishes and the 
 *       new resource changes are applied.
 *       
 * TODO: Support "hot-cold" reindexing, by re-indexing to a temporary location, and
 *       switching on-disk indexes when finished.
 *       
 * TODO: Perhaps create separate implementation that works directly on database
 *       backend, for efficiency and consitency.
 *       
 *       The configured Lucene index will be completely locked for writing 
 *       operations while reindexing is running.
 */
public class LuceneReindexer implements InitializingBean, Reindexer  {
    
    private static Log logger = LogFactory.getLog(LuceneReindexer.class);
    
    // Properties
    private Repository repository;
    private String token;
    
    /** If a filter is set, it will be used to skip filtered resources, otherwise
     * everything is extracted
     * (but not necessarily indexed, this depends on the extractor implementation, 
     * which always has the last say in whether a resource is indexable or not)
     */
    private FilterCriterion filter;
    private LuceneIndex index;
    
    /** If <code>true</code>, the reindexer will not descend into subtrees
     * (collections) that have been filtered out. Beware that if using
     * inclusive filtering, "/" must be explicitly included (not by a prefix filter), 
     * otherwise, it is skipped, and nothing will be reindexed. Generally, if this option is 
     * set, and filtering is specified in "what-resources-to-include"-manner, 
     * all resources above the included resources, must also explicitly be 
     * included (typically for the URI prefix filter). 
     * TODO: Using inclusive URI prefix filtering together with this option is
     *       confusing, but doable, using an AND-combination with a non-prefix-filter
     *       including all the parent collections of the "prefix-resource".
     *       Address this issue.
     *      
     */
    private boolean skipFilteredSubtrees = false;
    
    /**
     * If <code>true</code>, calls to start reindexing will immediately return, 
     * and a worker thread will be dispatched. Otherwise, the reindexing starts
     * in the caller thread.
     */
    private boolean asynchronous = false;

    private Thread workerThread = null;
    private Worker worker = null;
    
    public void afterPropertiesSet() throws BeanInitializationException {
        if (repository == null) {
            throw new BeanInitializationException("Property 'repository' not set.");
        }
        
        if (token == null) {
            throw new BeanInitializationException("Property 'token' not set.");
        }
        
        if (index == null) {
            throw new BeanInitializationException("Property 'index' not set.");
        }
        
    }
    
    /**
     * <code>Runnable</code> worker implementation. 
     *
     */
    private class Worker implements Runnable {
        String subtree;
        boolean alive = true;
        String currentWorkingTree;
        
        public Worker(String subtree) {
            this.subtree = subtree;
            this.currentWorkingTree = subtree;
        }
        
        public void run() {
            long start = System.currentTimeMillis();
            try {
                if (!index.lockAcquire()) {
                    logger.warn("Unable to lock index: '" + index.getIndexId() + "'");
                    this.alive = false;
                    return;
                }
                
                if (subtree.equals("/")) {
                    if (logger.isInfoEnabled()) {
                        logger.info("Clearing all contents of index '" + index.getIndexId() + "'");
                    }
                    index.createNewIndex();
                } else {
                    if (logger.isInfoEnabled()) {
                        logger.info("Deleting subtree '" + this.subtree + "'" +
                                     " from index '" + index.getIndexId() + "'");
                    }
                    index.deleteSubtree(subtree);
                }
                
                if (logger.isInfoEnabled()) {
                    logger.info("Starting reindexing from '" + subtree + "' " +
                                "for index '" + index.getIndexId() + "'");
                }
                
                // Start recursive re-indexing.
                indexResources(subtree);
                
                // Optimize and commit.
                index.optimize();
                index.commit();
            } catch (IOException io) {
                logger.warn("Got IOException while reindexing", io);
            } finally {
                index.lockRelease();
            }
            long end = System.currentTimeMillis();
            if (logger.isDebugEnabled()) {
                logger.debug("Reindexing finished (or stopped) on index '" + index.getIndexId() +
                        "', operation took " + (end-start) + " milliseconds.");
            }
        }

        /**
         * Recursively index resources under given URI.
         * @param uri the vortex resource uri on which to start the indexing.
         */
        void indexResources(String uri) throws IOException {
            if (! this.alive) {
                // Abort recursion; worker is dead.
                // TODO: perhaps add another abort-check in for-loop when
                //       iterating over children.
                return;
            }
            
            Resource resource = null;
            try {
                resource = repository.retrieve(token, uri, false);
            } catch (Exception e) {
                logger.warn("Exception when retrieving resource at '" + uri + "', " + 
                            "message: " + e.getMessage());
                return;
            }
            
            if (resource.isCollection()) {
                this.currentWorkingTree = uri;
                if (filter != null && filter.isFiltered(uri)) {
                    if (skipFilteredSubtrees) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Skipping filtered subtree '" + uri + "'");
                        }
                        return;
                    }
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Requesting indexing of resource '" + uri + "'");
                    }
                    index.addDocument(uri);
                }
                
                String[] children = resource.getChildURIs();
                for (int i = 0; i < children.length; i++) {
                    indexResources(children[i]);
                }
            } else {
                if (filter != null && filter.isFiltered(uri)) 
                    return;
                
                if (logger.isDebugEnabled()) {
                    logger.debug("Requesting indexing of resource '" + uri + "'");
                }
                index.addDocument(uri);
            }
        }
    }
    
    // TODO: implement a formal maintenance interface of some sort, instead..
    /* (non-Javadoc)
     * @see org.vortikal.index.management.Reindexer#start()
     */
    public synchronized void start() throws ManagementException {
        start("/");
    }
    
    /* (non-Javadoc)
     * @see org.vortikal.index.management.Reindexer#start(java.lang.String)
     */
    public synchronized void start(String subtreeURI) 
        throws ManagementException {
        if (workerThread != null && workerThread.isAlive()) {
            logger.warn("Worker is already running !");
            throw new ManagementException("Re-indexing is already running !");
        }
        
        worker = new Worker(subtreeURI);
        if (asynchronous) {
            if (logger.isInfoEnabled()) {
            logger.info("Starting asynchronous reindexing operation on index '" +
                        index.getIndexId() + "' from URI '" + subtreeURI + "'");
            }
            
            // Spawn thread for worker
            workerThread = new Thread(this.worker);
            workerThread.start();
        } else {
            if (logger.isInfoEnabled()) {
                logger.info("Reindexing on index '" +
                            index.getIndexId() + "' from URI '" + subtreeURI + "'");
            }
            
            // Start re-indexing in current thread.
            worker.run();
        }
    }
    
    /* (non-Javadoc)
     * @see org.vortikal.index.management.Reindexer#stop()
     */
    public synchronized void stop() throws ManagementException {
        if (workerThread != null && workerThread.isAlive()) {
            logger.debug("Signalling worker to stop ..");
            worker.alive = false;

            try {
                logger.debug("Waiting for worker thread to stop ..");
                workerThread.join();
            } catch (InterruptedException ie) {
                logger.warn("Interrupted while waiting for worker thread to stop !");
                throw new 
                    ManagementException("Interrupted while waiting for worker thread to stop !");
            }

            workerThread = null;
            worker = null;
        }
    }
    
    /* (non-Javadoc)
     * @see org.vortikal.index.management.Reindexer#isRunning()
     */
    public synchronized boolean isRunning() {
        return (workerThread != null && workerThread.isAlive()); 
    }
    
    /* (non-Javadoc)
     * @see org.vortikal.index.management.Reindexer#getWorkerThreadName()
     */
    public synchronized String getWorkerThreadName() {
        if (isRunning()) {
            return workerThread.getName();
        } else {
            return "";
        }
    }
    
    /* (non-Javadoc)
     * @see org.vortikal.index.management.Reindexer#getCurrentWorkingTree()
     */
    public synchronized String getCurrentWorkingTree() {
        if (isRunning()) {
            return this.worker.currentWorkingTree;
        } else return "";
    }
    
    // Setters
    public void setFilter(FilterCriterion filter) {
        this.filter = filter;
    }

    public void setIndex(LuceneIndex index) {
        this.index = index;
    }

    public void setSkipFilteredSubtrees(boolean skipFilteredSubtrees) {
        this.skipFilteredSubtrees = skipFilteredSubtrees;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setAsynchronous(boolean asynchronous) {
        this.asynchronous = asynchronous;
    }

    /* (non-Javadoc)
     * @see org.vortikal.index.management.Reindexer#isAsynchronous()
     */
    public boolean isAsynchronous() {
        return asynchronous;
    }

    /* (non-Javadoc)
     * @see org.vortikal.index.management.Reindexer#getFilter()
     */
    public FilterCriterion getFilter() {
        return filter;
    }

    /* (non-Javadoc)
     * @see org.vortikal.index.management.Reindexer#getIndexId()
     */
    public String getIndexId() {
        return this.index.getIndexId();
    }
    
    /* (non-Javadoc)
     * @see org.vortikal.index.management.Reindexer#isSkipFilteredSubtrees()
     */
    public boolean isSkipFilteredSubtrees() {
        return skipFilteredSubtrees;
    }
}

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
package org.vortikal.repositoryimpl.query;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.IndexSearcher;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;

import EDU.oswego.cs.dl.util.concurrent.FIFOSemaphore;

/**
 * TODO: better javadoc/description
 * 
 * New class for low-level index access.
 * 
 * Manages write & search access to a single Lucene index.
 * TODO: provide access to secondary index for re-indexing, etc.
 * TODO: provide method for switching in secondary index to primary
 *       after re-indexing.
 *       
 * Manages access to searcher for the index. The searcher instance
 * is reference-counted, and re-instantiated if it becomes outdated due
 * to index modifications. Outdated searcher instances that have 
 * not been released should be handled gracefully and eventually closed.
 * 
 * The implementation requires that a write-lock is acquired whenever writing
 * operations are done. When a thread has the write lock, all other threads that
 * need write access are blocked and queued. The write lock can be
 * used to temporarily stop anything from modifying the index.
 *
 * @author oyviste
 *
 */
public class LuceneIndex implements InitializingBean {
    
    private static Log logger = LogFactory.getLog(LuceneIndex.class);
    
    private static final int MAX_OUTDATED_SEARCH_INDEX_READERS = 10;
    
    private int commitCounter = 0;
    private boolean searchReaderOutdated = false;
    private int currentSearchIndexReaderRefCount = 0;
    private IndexReaderRefCountMap outdatedReaders = 
        new IndexReaderRefCountMap(MAX_OUTDATED_SEARCH_INDEX_READERS*2);
    
    private FSBackedLuceneIndex primaryFSIndex;
//    private FSBackedLuceneIndex secondaryFSIndex;
//    private VolatileLuceneIndex vIndex;
    
    private IndexReader searchIndexReader;
    private Object indexSearcherManagementLock = new Object();
    
    private String primaryIndexPath;
    private String secondaryIndexPath;
    private int optimizeInterval = 100;
    private int mergeFactor = 10;
    private int minMergeDocs = 100;
    private int maxMergeDocs = 10000;
    private boolean eraseExistingIndex = false;
    private boolean forceUnlock = false;
    
    /** This is our FIFO write lock on this index. Operations requiring write-access
     *  will need to acquire this before doing the operation. This includes all 
     *  operations using an IndexWriter and all operations using an IndexReader 
     *  for document deletion.
     */
    private FIFOSemaphore lock = new FIFOSemaphore(1);
    
    public void afterPropertiesSet() throws BeanInitializationException {
        
        if (this.primaryIndexPath == null) {
            throw new BeanInitializationException ("Property 'primaryIndexPath' not set.");
        }
        
        try {

            this.primaryFSIndex = new FSBackedLuceneIndex(this.primaryIndexPath, 
                                                          new KeywordAnalyzer(),
                                                          this.eraseExistingIndex,
                                                          this.forceUnlock);
            
            this.primaryFSIndex.setMaxMergeDocs(this.maxMergeDocs);
            this.primaryFSIndex.setMergeFactor(this.mergeFactor);
            this.primaryFSIndex.setMinMergeDocs(this.minMergeDocs);
            
            this.searchIndexReader = this.primaryFSIndex.getNewReadOnlyIndexReader();
            
        } catch (IOException io) {
            logger.error("Got IOException while initializing indexes: " + io.getMessage());
            throw new BeanInitializationException("Got IOException while initializing indexes.", io);
        }
    }
    
    /**
     * Removes and closes all outdated search index readers.
     */
    protected void cleanupOutdatedSearchIndexReaders() {
        synchronized(this.indexSearcherManagementLock) {
            for (Iterator i = this.outdatedReaders.keySet().iterator(); i.hasNext();) {
                IndexReader reader = (IndexReader)i.next();
                if (logger.isDebugEnabled()) {
                    logger.debug("Closing and removing an outdated search index reader with" 
                            + " ref-count = " + this.outdatedReaders.get(reader));
                }
                
                i.remove();
                
                try {
                    reader.close();
                } catch (IOException io) {
                    logger.warn("IOException while closing outdated search index reader.");
                }
            }
        }
    }
    
    /**
     * Get an <code>IndexSearcher</code> instance for this index.
     * The instance must be released with #releaseIndexSearcher(IndexSearcher)
     * after usage because of reference-counting.
     * 
     * XXX: locking might be too aggressive and provoke thread lock contention on
     *      SMP architectures.
     * 
     * @return
     * @throws IOException
     */
    protected IndexSearcher getIndexSearcher() throws IOException {
        synchronized (this.indexSearcherManagementLock) {
            if (this.searchReaderOutdated) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Current search index reader is outdated !");
                }
                
                if (this.currentSearchIndexReaderRefCount > 0) {
                    // Still in use, put it into outdated readers storage
                    // (schedule for closing either when too old, or when ref-count
                    // eventually reaches 0)
                    outdatedReaders.put(this.searchIndexReader, 
                            new Integer(this.currentSearchIndexReaderRefCount));
                    if (logger.isDebugEnabled()) {
                        logger.debug("Added outdated but still in-use search index reader to map of outdated readers.");
                        logger.debug("Current number of outdated, but still open readers is " 
                                + this.outdatedReaders.size());
                    }
                } else {
                    this.searchIndexReader.close();
                    if (logger.isDebugEnabled()) {
                        logger.debug("Closed outdated and un-used search index reader");
                    }
                }
                
                this.searchIndexReader = primaryFSIndex.getNewReadOnlyIndexReader();
                this.currentSearchIndexReaderRefCount = 0;

                // No longer out-dated
                this.searchReaderOutdated = false;
            } 
            
            // Bump ref-count
            ++this.currentSearchIndexReaderRefCount;
            
            if (logger.isDebugEnabled()) {
                logger.debug("Search index reader ref-count increased to " 
                        + this.currentSearchIndexReaderRefCount);
            }
            
            // Return new searcher on the reader
            return new IndexSearcher(this.searchIndexReader);
        }
    }
    
    /**
     * Release an <code>IndexSearcher</code> instance obtained with
     * #getIndexSearcher().
     * 
     * @param searcher
     * @throws IOException
     */
    protected void releaseIndexSearcher(IndexSearcher searcher)
        throws IOException {
        synchronized(this.indexSearcherManagementLock) {
            if (searcher == null) {
                return;
            }
            
            IndexReader reader = searcher.getIndexReader();
            
            if (reader == this.searchIndexReader) {
                // Decrease ref-count
                if (this.currentSearchIndexReaderRefCount > 0) {
                    --this.currentSearchIndexReaderRefCount;
                    
                    if (logger.isDebugEnabled()) {
                        logger.debug("Search index reader ref-count decreased to " 
                                + this.currentSearchIndexReaderRefCount);
                    }
                } else {
                    logger.warn("Ref-count for current search index reader below zero, something is wrong !");
                }
            } else {
                // Searcher released an outdated reader, decrease ref-count and close
                // if we can.
                
                Integer refCount = (Integer)this.outdatedReaders.get(reader);
                if (refCount == null) {
                    // We no longer have any pointer to this, ignore release-request.
                    return;
                }
                
                int c = refCount.intValue()-1;
                if (c == 0) {
                    // OK, last reference released, we close it.
                    this.outdatedReaders.remove(reader);
                    reader.close();
                } else {
                    // Still references left, we put it back with a decreased counter.
                    this.outdatedReaders.put(reader, new Integer(c));
                }
            }
        }
    }

    // CAN ONLY BE CALLED IF THREAD HAS ACQUIRED THE WRITE LOCK !!
    protected IndexReader getIndexReader() throws IOException {
        return this.primaryFSIndex.getIndexReader();
    }
    
    // CAN ONLY BE CALLED IF THREAD HAS ACQUIRED THE WRITE LOCK !!
    protected IndexWriter getIndexWriter() throws IOException {
        return this.primaryFSIndex.getIndexWriter();
    }
    
    // CAN ONLY BE CALLED IF THREAD HAS ACQUIRED THE WRITE LOCK !!
    protected void clear() throws IOException {
        this.primaryFSIndex.createNewIndex();
    }
    
    // CAN ONLY BE CALLED IF THREAD HAS ACQUIRED THE WRITE LOCK !!
    protected void reinitialize() throws IOException {
        this.primaryFSIndex.reinitializeIndex();
    }
    
    // CAN ONLY BE CALLED IF THREAD HAS ACQUIRED THE WRITE LOCK !!
    protected void optimize() throws IOException {
        this.primaryFSIndex.optimize();
    }
    
    // Commit changes done by reader/writer and optimize index if necessary.
    // This method checks if the index reader used for searching becomes
    // out-dated after the commit.
    // CAN ONLY BE CALLED IF THREAD HAS ACQUIRED THE WRITE LOCK !!
    protected void commit() throws IOException {
        // Optimize index, if we've reached 'optimizeInterval' number of commits.
        if (++commitCounter % optimizeInterval == 0) {
            primaryFSIndex.optimize();
        }
        
        primaryFSIndex.commit();
        checkForOutdatedSearcher();
    }

    private void checkForOutdatedSearcher() throws IOException {
        synchronized (this.indexSearcherManagementLock) {
            this.searchReaderOutdated = !this.searchIndexReader.isCurrent();
        }
    }
    
    // Explicit write locking. Must be acquired before doing any write
    // operations on index, using either the reader or the writer.
    protected boolean writeLockAcquire() {
        try {
            this.lock.acquire();
        } catch (InterruptedException ie) {
            return false;
        }
        
        if (logger.isDebugEnabled()) {
            logger.debug("Thread '" + Thread.currentThread().getName() + 
                         "' got index lock.");
        }
        
        return true;
    }
    
    protected boolean writeLockAttempt(long timeout) {
        try {
            return this.lock.attempt(timeout);
        } catch (InterruptedException ie) {
            return false;
        }
    }
    
    protected void writeLockRelease() {
        if (logger.isDebugEnabled()) {
            logger.debug("Thread '" + Thread.currentThread().getName() + 
                         "' released index lock.");
        }
        
        this.lock.release();
    }
    
    
   /**
     * LRU map of unclosed, but outdated index readers. When full, the oldest
     * is forcibly closed and nullified, no matter if its ref-count is still > 0.
     * The map contains readers as keys with an Integer ref-count as values.
     * 
     */
    private static class IndexReaderRefCountMap extends LinkedHashMap {
        
        public IndexReaderRefCountMap(int initialCapacity) {
            super(initialCapacity);
        }
        
        protected boolean removeEldestEntry(Map.Entry entry) {
            if (super.size() <= LuceneIndex.MAX_OUTDATED_SEARCH_INDEX_READERS) {
                return false;
            }
            
            IndexReader reader = (IndexReader)entry.getKey();
            Integer refCount = (Integer)entry.getValue();
            
            if (refCount.intValue() > 0) {
                LuceneIndex.logger.warn(
                        "Forcibly closing least recently used outdated search index reader, "
                        + "but ref-count is still " + refCount);
            }
            
            try {
                reader.close();
            } catch (IOException io) {
                LuceneIndex.logger.warn(
                        "IOException occured when closing outdated search index reader.");
            }
            
            return true;
        }
    }

    public boolean isEraseExistingIndex() {
        return eraseExistingIndex;
    }

    public void setEraseExistingIndex(boolean eraseExistingIndex) {
        this.eraseExistingIndex = eraseExistingIndex;
    }

    public boolean isForceUnlock() {
        return forceUnlock;
    }

    public void setForceUnlock(boolean forceUnlock) {
        this.forceUnlock = forceUnlock;
    }

    public int getMaxMergeDocs() {
        return maxMergeDocs;
    }

    public void setMaxMergeDocs(int maxMergeDocs) {
        this.maxMergeDocs = maxMergeDocs;
    }

    public int getMergeFactor() {
        return mergeFactor;
    }

    public void setMergeFactor(int mergeFactor) {
        this.mergeFactor = mergeFactor;
    }

    public int getMinMergeDocs() {
        return minMergeDocs;
    }

    public void setMinMergeDocs(int minMergeDocs) {
        this.minMergeDocs = minMergeDocs;
    }

    public int getOptimizeInterval() {
        return optimizeInterval;
    }

    public void setOptimizeInterval(int optimizeInterval) {
        this.optimizeInterval = optimizeInterval;
    }

    public String getPrimaryIndexPath() {
        return primaryIndexPath;
    }

    public void setPrimaryIndexPath(String primaryIndexPath) {
        this.primaryIndexPath = primaryIndexPath;
    }

    public String getSecondaryIndexPath() {
        return secondaryIndexPath;
    }

    public void setSecondaryIndexPath(String secondaryIndexPath) {
        this.secondaryIndexPath = secondaryIndexPath;
    }
    
}

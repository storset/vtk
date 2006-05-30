/* Copyright (c) 2004, University of Oslo, Norway
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
package org.vortikal.search.lucene;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.vortikal.repository.Repository;
import org.vortikal.repository.RepositoryException;
import org.vortikal.repository.Resource;
import org.vortikal.repository.event.ContentModificationEvent;
import org.vortikal.repository.event.RepositoryEvent;
import org.vortikal.repository.event.ResourceCreationEvent;
import org.vortikal.repository.event.ResourceDeletionEvent;
import org.vortikal.repository.event.ResourceModificationEvent;
import org.vortikal.search.SearchException;
import org.vortikal.security.AuthenticationException;
import org.vortikal.util.threads.Semaphore;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;






/**
 * TODO: asynchronous indexing (event queueing),
 * TODO: tuning (when to optimize(), mergeFactor size, etc.),
 * TODO: differential indexing based on MIME types (XML, mp3, HTML),
 * TODO: field namespaces,
 *
 * 
 * @version $Id$
 */
public class LuceneIndexer implements
                               InitializingBean,
                               DisposableBean,
                               ApplicationListener  {

    private Repository repository = null;
    private String token = null;
    private File index = null;    
    private Log logger = LogFactory.getLog(this.getClass());
    private String indexDirectory;

    /*
     * Asynchronous indexing: on an application event, add the event's
     * resource object to an (un)indexing queue, allowing the indexer
     * thread to generate a Lucene Document and write the index to
     * file in a serialized manner.
     */
    private boolean synchronousIndexing = false;
    private Semaphore semaphore = new Semaphore(0);
    private List indexQueue = new ArrayList();
    private IndexerThread indexerThread = null;


    public void setToken(String token) {
        this.token = token;
    }
    


    /**
     * @param indexDirectory The indexDirectory to set.
     */
    public void setIndexDirectory(String indexDirectory) {
        this.indexDirectory = indexDirectory;
    }


    /**
     * @param repository The repository to set.
     */
    public void setRepository(Repository repository) {
        this.repository = repository;
    }


    public void setSynchronousIndexing(boolean synchronousIndexing) {
        this.synchronousIndexing = synchronousIndexing;
    }
    


    public void afterPropertiesSet() {

        if (indexDirectory == null) {
            throw new BeanInitializationException(
                "No property `indexDirectory' set, can't continue.");
        }

        index = new File(indexDirectory.trim());
        if (!index.exists()) {
            try {
                index.mkdir();
            } catch (Exception e) {
                BeanInitializationException be = new BeanInitializationException(
                    "Creating lucene index directory: " +
                    index.getAbsolutePath() + " failed: " +
                    e.getMessage());
                be.fillInStackTrace();
                throw be;
            }
        }

        if (!index.isDirectory()) {
            throw new BeanInitializationException(
                "Property `indexDirectory' does not point to a directory");
        }

        if (token == null) {
            throw new BeanInitializationException(
                "Property `token' not set ");
        }

        if (repository == null) {
            throw new BeanInitializationException(
                "Property `repository' not set ");
        }

        this.indexerThread = new IndexerThread("indexer");
        this.indexerThread.start();
    }



    public void destroy() {
        this.indexerThread.kill();
    }
    

    
    public void onApplicationEvent(ApplicationEvent event) {

        if (! (event instanceof RepositoryEvent)) {
            return;
        }

        Repository rep = ((RepositoryEvent) event).getRepository();
        if (rep != this.repository) {
            return;
        }

        if (event instanceof ResourceCreationEvent) {
            indexResource(((ResourceCreationEvent) event).getResource());
        }
        if (event instanceof ResourceDeletionEvent) {
            unIndexResource(((ResourceDeletionEvent) event).getURI());
        }
        if (event instanceof ResourceModificationEvent) {
            indexResource(((ResourceModificationEvent) event).getResource());
        }
        if (event instanceof ContentModificationEvent) {
            indexResource(((ContentModificationEvent) event).getResource());
        }
    }
    


    public void indexResource(Resource resource) {
        if (this.synchronousIndexing) {
            synchronized(this) {
                indexResourceInternal(resource);
            }
            return;
        }

        synchronized(this.indexQueue) {
            this.indexQueue.add(new IndexRequest(resource));
            logger.debug("Index queue size: " + this.indexQueue.size());
            this.semaphore.up();
        }
    }


    public void unIndexResource(String uri) {
        if (this.synchronousIndexing) {
            synchronized(this) {
                unIndexResourceInternal(uri);
            }
            return;
        }

        synchronized(this.indexQueue) {
            this.indexQueue.add(new UnIndexRequest(uri));
            logger.debug("Index queue size: " + this.indexQueue.size());
            this.semaphore.up();
        }
    }
    
    
    private void unIndexResourceInternal(String uri) {
        try {
            int[] docs = findDocumentsToDelete(uri);
            deleteByIDs(docs);
        } catch (IOException e) {
            logger.warn("Un-indexing resource " + uri + " failed", e);
        }
    }
    


    private int[] findDocumentsToDelete(String uri) throws IOException {

        org.apache.lucene.search.Searcher searcher = null;
        
        try {

            // Construct a query to find documents to remove:

            org.apache.lucene.search.BooleanQuery query =
                new org.apache.lucene.search.BooleanQuery();
            
            query.add(new org.apache.lucene.search.TermQuery(
                          new org.apache.lucene.index.Term("uri", uri)),
                      BooleanClause.Occur.SHOULD);
            query.add(
                new org.apache.lucene.search.TermQuery(
                    new org.apache.lucene.index.Term(
                        "folders", getFolders(uri) + " " + uri)),
                    BooleanClause.Occur.SHOULD);
            query.add(
                new org.apache.lucene.search.WildcardQuery(
                    new org.apache.lucene.index.Term(
                        "folders", getFolders(uri) + " " + uri + " *")),
                    BooleanClause.Occur.SHOULD);

            searcher = 
                new org.apache.lucene.search.IndexSearcher(this.indexDirectory);
            org.apache.lucene.search.Hits hits = searcher.search(query);

            int[] deleteSet = new int[hits.length()];

            for (int i = 0; i < hits.length(); i++) {
                deleteSet[i] = hits.id(i);
            }
        
            searcher.close();

            return deleteSet;

        } finally {
            if (searcher != null) {
                searcher.close();
            }
        } 
    }
    
    

    private void deleteByIDs(int[] docs) throws IOException {
        IndexReader reader = null;
        try {
            reader = IndexReader.open(this.index);

            for (int i = 0; i < docs.length; i++) {
                logger.debug("Deleting index document: " + docs[i]);
                reader.deleteDocument(docs[i]);
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }
    


    /**
     * FIXME: should be able to use <code>reader.delete(term)</code>,
     * instead of having to construct a search to find documents to
     * delete.
     *
     * @param uri a <code>String</code> value
     */
    private void OLD_unIndexResourceInternal(String uri) {
        IndexReader reader = null;

        try {

            reader = IndexReader.open(this.index);
            Term t = new Term("folders", uri);
            int deleted = reader.deleteDocuments(t);
            logger.debug("Deleted from index: " + uri);
            reader.close();

        } catch (IOException e) {
            logger.warn("Unable to un-index resource " + uri, e);
        }
    }
    




    private void indexResourceInternal(Resource resource) {
        IndexReader reader = null;
        IndexWriter writer = null;

        long start = System.currentTimeMillis();

        try {

            /* Delete old document (if exists) */

            try {
                reader = IndexReader.open(this.index);
                logger.debug(
                    "Deleted " + 
                    reader.deleteDocuments(new Term("uri", resource.getURI())) +
                    " previously indexed document(s): for resource " + 
                    resource.getURI());
                reader.close();
            } catch (FileNotFoundException e) {
                // Ignore, this means that the index directory probably
                // does not exist
            }

            /* Create new document */

            try {
                Analyzer analyzer = new StandardAnalyzer();
                writer = new IndexWriter(
                    this.index, analyzer, false);
            } catch (java.io.FileNotFoundException e) {
                logger.debug("Index directory " +
                             this.index.getAbsolutePath() + 
                             " not found, trying to create it");
                writer = new IndexWriter(
                    this.index, new StandardAnalyzer(), true);
            }
            
            Document doc = createIndexDocument(resource);
            writer.addDocument(doc);
            logger.debug("Indexed " + resource.getURI());

        } catch (IOException e) {
            throw new SearchException(
                "Opening index writer failed", e);
            
        } catch (RepositoryException e) {
            logger.warn("Unable to index resource " + resource.getURI(), e);

        } catch (AuthenticationException e) {
            logger.warn("Unable to index resource " + resource.getURI(), e);
            
        } finally {
            try {
                if (writer != null) {
                    
                    writer.optimize();
                    writer.close();
                }
                
            } catch (IOException e) {
                throw new SearchException(
                    "Closing index writer failed: " + e.getMessage());
            }
            logger.debug("Indexing resource " + resource.getURI() + " took " +
                         (System.currentTimeMillis() - start) + " ms");
        }        
    }



    private Document createIndexDocument(Resource resource) {

        /* Create an index for the resource */
        Document doc = new Document();

        ResourceAnalyzer analyzer = new StandardResourceAnalyzer();
        analyzer.processResource(resource, this.repository, this.token, doc);

        // TODO: run additional analyzers here, based on content-type, etc.

        return doc;
    }



    static String getFolders(String path) {
        if (path.indexOf("/") < 0) {
            return null;
        }
        
        StringBuffer folders = new StringBuffer();
        folders.append("/");

        int pos = 0;
        while (true) {
            int n = path.indexOf("/", pos + 1);
            if (n < 0) {
                break;
            }
            pos = n;
            folders.append(" ");
            folders.append(path.substring(0, pos));
        }
        return folders.toString();
    }




    private class IndexRequest {
        private Resource resource;

        public IndexRequest(Resource resource) {
            this.resource = resource;
        }
        
        public Resource getResource() {
            return this.resource;
        }        
    }
    


    private class UnIndexRequest {
        private String uri;

        public UnIndexRequest(String uri) {
            this.uri = uri;
        }
        
        public String getURI() {
            return this.uri;
        }
    }



    private class IndexerThread extends Thread {

        private boolean alive = true;

        public IndexerThread(String name) {
            super(name);
        }
        

        public void kill() {
            alive = false;
            this.interrupt();
        }
        

        public void run() {

            while (alive) {
                try {

                    logger.debug(
                        "Got indexing request from queue: " + semaphore.down());
                    
                    if (!alive) {
                        break;
                    }

                    Object o = null;
                    synchronized(indexQueue) {
                        o = indexQueue.remove(0);
                    }
                    
                    if (o instanceof IndexRequest) {
                        indexResourceInternal(
                            ((IndexRequest) o).getResource());
                    } else {
                        unIndexResourceInternal(
                            ((UnIndexRequest) o).getURI());
                    }

                } catch (Throwable t) {
                    logger.warn("Error: ", t);
                }
            }
            logger.info("Exiting");
        }
    }
    
}

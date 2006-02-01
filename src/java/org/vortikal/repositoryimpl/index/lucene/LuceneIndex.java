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
package org.vortikal.repositoryimpl.index.lucene;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.TermQuery;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repositoryimpl.index.Extractor;
import org.vortikal.repositoryimpl.index.ExtractorException;
import org.vortikal.repositoryimpl.index.Index;
import org.vortikal.repositoryimpl.index.IndexConstants;
import org.vortikal.repositoryimpl.index.util.IndexResourceIdHelper;

import EDU.oswego.cs.dl.util.concurrent.FIFOSemaphore;

/**
 * Class for low-level Lucene index access. 
 * Will eventually implement a generic index interface. Refactoring has been
 * done to make the transition easier. Most methods are declared protected for
 * this reason.
 * 
 * <p>
 * The class is itself thread-safe, but Lucene doesn't allow multiple
 * threads to concurrently modify an index. For this reason a lock is provided
 * which _must_ be acquired by any object that does writing operations on the
 * index, by calling {@link #lockAcquire()}. Failing to do so will result in
 * <code>IOException</code>s, but Lucene's internal locking should prevent
 * actual corruption. But forgetting to lock is like begging for consistency
 * problems further down the road. The lock must always be released _after_ the
 * last writing operation is finished (typically after a call to {@link #commit()}).
 * The lock can be released by calling {@link #lockRelease()}.
 * Failing to release the lock will prevent all other lock obedient threads from
 * doing their work, as they will block in their call to {@link #lockAcquire()}
 * (but will still not prevent rogue lock-ignorant threads from messing things
 * up).
 *  
 * <p>
 * Objects requiring read only access should _only_ be calling
 * {@link AbstractLuceneIndex#getReadOnlyIndexReader()} or
 * {@link AbstractLuceneIndex#getIndexSearcher()}. The instances returned from
 * these methods can be safely closed by the caller (they _should_ be closed, to
 * prevent resource leakage). However, in the case of getReadOnlyIndexReader(), 
 * the returned IndexReader instance should never be used for deletion, 
 * only lookups/searching. 
 * 
 * TODO: Should not be a subclass of FSBackedLuceneIndex, but instead use it as 
 * a component (well, perhaps).
 */
public class LuceneIndex extends FSBackedLuceneIndex implements
    InitializingBean, BeanNameAware, Index {
    
    private Log logger = LogFactory.getLog(this.getClass());
    
    private Extractor extractor;
    private IndexResourceIdHelper indexResourceIdHelper;
    private Class extractorBeanClass;
    private int optimizeInterval = 500;
    private int commitCounter = 0;
    private String beanName;
    
    /** This is our FIFO write lock on this index. Operations requiring write-access
     *  will need to acquire this before doing the operation. This includes all 
     *  operations using an IndexWriter and all operations using an IndexReader 
     *  for document deletion.
     */
    private FIFOSemaphore lock = new FIFOSemaphore(1);
    
    public void afterPropertiesSet() throws BeanInitializationException {
        super.afterPropertiesSet();
        
        if (this.extractor == null ) {
            throw new BeanInitializationException("Property 'extractor' not set.");
        } else if (this.indexResourceIdHelper == null) {
            throw new BeanInitializationException("Property 'indexResourceIdHelper' not set.");
        }
        
        this.extractorBeanClass = extractor.getExtractedClass();
        if (this.extractorBeanClass == null) {
            throw new BeanInitializationException("Bean class supplied by extractor " + extractor
                    + " is null");
        }
    }
    
    /**
     * Add repository resource at URI to index, using this index' <code>Extractor</code>.
     * NOTE: Existing index document(s) at the same URI is _NOT_ automatically 
     *       deleted by this method. This allows several additions/updates to be batched
     *       together using a single <code>IndexWriter</code> instance, without
     *       needing a (conflicting) deleting <code>IndexReader</code> in between
     *       (which is not very efficient, taking into account the overhead involved in
     *       opening/closing multiple IndexWriter instances).
     *       
     * @param uri the URI of the repository resource which should be added to index.
     * @return <code>true</code> if the document was successfully extracted and added, 
     *         <code>false</code> otherwise.
     */
    protected boolean addDocument(String uri) throws IOException {
        return addDocument(getIndexWriter(), uri);
    }
    
    /**
     * @see #addDocument(String)
     */
    protected boolean addDocument(IndexWriter writer, String uri) throws IOException {
        Object indexBean = null;
        try {
            indexBean = this.extractor.extract(uri);
        } catch (ExtractorException ee) {
            logger.warn("Unable to extract resource '" + uri + "', " + ee.getMessage());
        }
        
        if (indexBean == null) {
            logger.warn("Extractor returned null, not adding document at '" + uri + "'");
            return false;
        }
        
        String parentIds = this.indexResourceIdHelper.getResourceParentCollectionIds(uri);
        if (parentIds == null) {
            logger.warn("Unable to get parent resource IDs for resource '" + "'" + 
                        ", will not add to index.");
            return false;
        }
        
        Document doc = IndexDocumentUtil.createDocument(indexBean, this.extractorBeanClass, 
                                                         uri, parentIds);
        
        if (doc == null) {
            logger.warn("Unable to create Lucene document from extracted index bean of class '" + 
                        this.extractorBeanClass.getName() + "' for resource '" + uri + "'");
            return false;
        }
        
        writer.addDocument(doc);
        return true;
    }

    /**
     * TODO: JavaDoc
     */
    protected boolean updateDocument(String uri) throws IOException {
        deleteDocument(uri);
        return addDocument(uri);
    }
    
    /**
     * Delete a single document from index.
     * @param uri The URI (document identifier) of the document to delete.
     * @return <code>true</code> iff a document was actually deleted, <code>false</code>
     *         otherwise (if it didn't exist in the index). 
     */
    protected boolean deleteDocument(String uri) throws IOException {
        return deleteDocument(getIndexReader(), uri);
    }
    
    /**
     * @see #deleteDocument(String)
     */
    protected boolean deleteDocument(IndexReader reader, String uri) throws IOException {
        if (reader.delete(new Term(IndexConstants.URI_FIELD, uri)) > 0)
            return true;
        
        return false;
    }

    /**
     * Delete a subtree of documents from index based on the collection's resource id.
     * This can be acquired using an <code>IndexResourceIdHelper</code> instance.
     * Using this method is much more efficient than to delete by URI.
     *  
     * @param resourceId The ID of the collection resource to delete.
     * @param uri the URI of the collection resource to delete.
     * @return the number of documents that was deleted.
     * @see org.vortikal.repositoryimpl.index.util.IndexResourceIdHelper
     */
    protected int deleteSubtree(String resourceId, String uri) throws IOException {
        return deleteSubtree(getIndexReader(), resourceId, uri);
    }
    
    /**
     * @see #deleteSubtree(String, String)
     */
    protected int deleteSubtree(IndexReader reader, String resourceId, String uri)
        throws IOException {
        
        // Delete subtree
        int deleted = reader.delete(new Term(IndexConstants.PARENT_IDS_FIELD, resourceId));
        
        // Delete collection, in case this index also stores collections as documents.
        deleted += reader.delete(new Term(IndexConstants.URI_FIELD, uri));    

        return deleted;
    }
    
    /**
     * Delete a subtree of documents from index based on the URI only.
     * Tries to get id using index resource id helper. If not found, then deletes
     * using a prefix query.
     * 
     * @param reader <code>IndexReader</code> to use.
     * @param uri The URI of the subtree/collection.
     * @return the number of documents deleted from the index.
     */
    protected int deleteSubtree(String uri) throws IOException {
        IndexReader reader = getIndexReader();
        return deleteSubtree(reader, uri);
    }
    
    /**
     * @see #deleteSubtree(String)
     */
    protected int deleteSubtree(IndexReader reader, String uri) throws IOException {
        String id = this.indexResourceIdHelper.getResourceId(uri);
        if (id == null) {
            return deleteSubtreeWithoutId(reader, uri);
        } else {
            return deleteSubtree(reader, id, uri);
        }
    }
    
    /**
     * Delete a subtree without using a resource id. Uses a prefix-query instead.
     * Avoid using this one, if possible.
     * @param reader
     * @param uri
     * @return
     * @throws IOException
     */
    protected int deleteSubtreeWithoutId(IndexReader reader, String uri) throws IOException {
        IndexSearcher searcher = new IndexSearcher(reader);

        // We don't have the resource id, so we'll need to perform a prefix query, to
        // find the documents we want to delete.
        PrefixQuery pq = new PrefixQuery(new Term(IndexConstants.URI_FIELD, uri));
        Hits hits = searcher.search(pq);
        int nHits = hits.length();
        for (int i=0; i<nHits; i++) {
            reader.delete(hits.id(i));
        }
        searcher.close();
        
        return nHits;
    }
    
    /**
     * Get a document from the index based in its URI.
     * TODO: JavaDoc
     * @param uri
     * @return
     * @throws IOException
     */
    protected Object getDocument(String uri) throws IOException {
        IndexSearcher searcher = getIndexSearcher();
        Hits hits = searcher.search(new TermQuery(new Term(IndexConstants.URI_FIELD, uri)));
        if (hits.length() > 0) {
            Document doc = hits.doc(0);
            String className = doc.get(IndexConstants.CLASS_FIELD);
            if (className == null) {
                logger.warn("No CLASS field found for index document at '" + uri + "'");
                
                // Try with the class assigned to this index' extractor implementation.
                className = this.extractorBeanClass.getName();
            }
            
            Class beanClass = null;
            try {
                beanClass = Class.forName(className);
            } catch (ClassNotFoundException cnf) {
                logger.warn("Unable to instantiate object of class " + className + 
                            " for index document at '" + uri + "'", cnf);
            }
            
            Object indexBean = IndexDocumentUtil.createIndexBean(doc, beanClass);
            searcher.close();
            return indexBean;
        } else {
            return null;
        }
    }
    
    // Locking
    protected boolean lockAcquire() {
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
    
    protected boolean lockAttempt(long timeout) {
        try {
            return this.lock.attempt(timeout);
        } catch (InterruptedException ie) {
            return false;
        }
    }
    
    protected void lockRelease() {
        if (logger.isDebugEnabled()) {
            logger.debug("Thread '" + Thread.currentThread().getName() + 
                         "' released index lock.");
        }
        
        this.lock.release();
    }
    
    // Override parent's commit() to allow for optimization at a certain interval.
    protected synchronized void commit() throws IOException {
        // Optimize index, if we've reached 'optimizeInterval' number of commits.
        if (++commitCounter % optimizeInterval == 0) {
            super.optimize();
        }
        
        super.commit();
    }
    
    public Class getIndexBeanClass() {
        return this.extractorBeanClass;
    }
    
    public String getIndexId() {
        return this.beanName;
    }
    
    public void setExtractor(Extractor extractor) {
        this.extractor = extractor;
    }
    
    public void setIndexResourceIdHelper(IndexResourceIdHelper indexResourceIdHelper) {
        this.indexResourceIdHelper = indexResourceIdHelper;
    }
    
    public void setOptimizeInterval(int optimizeInterval) {
        this.optimizeInterval = optimizeInterval;
    }

    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }
    
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("[ ").append(getClass().getName());
        buffer.append(", id='").append(getIndexId()).append("' ]");
        return buffer.toString();
    }
}

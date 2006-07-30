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
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import EDU.oswego.cs.dl.util.concurrent.FIFOSemaphore;

/**
 * Class for low-level index access.
 * 
 * Manages write & search access to a single Lucene index.

 * Manages access to shared searcher(s) for the index. The searcher instance
 * is reference-counted, and re-instantiated if it becomes outdated due
 * to index modifications. Outdated searcher instances that have 
 * not been released should be handled gracefully and eventually closed.
 * 
 * The implementation requires that a write-lock is acquired whenever writing
 * operations are done. When a thread has the write lock, all other threads that
 * try to acquire the lock are <em>blocked and queued</em>. The write lock can be
 * used to prevent other threads from modifying the index (it assures
 * mutually exclusive write access to index between threads).
 *
 * @author oyviste
 */
public class LuceneIndex implements InitializingBean, DisposableBean {
    
    private static Log logger = LogFactory.getLog(LuceneIndex.class);

    private static final int MAX_OUTDATED_SEARCH_READERS = 10;

    private boolean currentReadOnlyReaderOutdated = false;
    private int currentReadOnlyReaderRefCount = 0;
    private IndexReaderRefCountMap outdatedReadOnlyReaders = 
                    new IndexReaderRefCountMap(MAX_OUTDATED_SEARCH_READERS + 2);
    private IndexReader currentReadOnlyReader;
    private Object readOnlyReaderManagementLock = new Object();
    
    private FSBackedLuceneIndex fsIndex;
    
    private String indexPath;
    private int optimizeInterval = 100;
    private int commitCounter = 0;
    private int mergeFactor = 10;
    private int maxBufferedDocs = 100;
    private int maxMergeDocs = 10000;
    private int maxLockAcquireTimeOnShutdown = 30; // 30 seconds max to wait for lock when shutting down
    private boolean eraseExistingIndex = false;
    private boolean forceUnlock = false;
    
    /** This is our FIFO write lock on this index. Operations requiring write-access
     *  will need to acquire this before doing the operation. This includes all 
     *  operations using an IndexWriter and all operations using an IndexReader 
     *  for document deletion.
     */
    private FIFOSemaphore lock = new FIFOSemaphore(1);
    
    public void afterPropertiesSet() throws BeanInitializationException {
        
        if (this.indexPath == null) {
            throw new BeanInitializationException ("Property 'indexPath' not set.");
        }
        
        try {

            this.fsIndex = new FSBackedLuceneIndex(this.indexPath, 
                                                   new KeywordAnalyzer(),
                                                   this.eraseExistingIndex,
                                                   this.forceUnlock);
            
            this.fsIndex.setMaxMergeDocs(this.maxMergeDocs);
            this.fsIndex.setMergeFactor(this.mergeFactor);
            this.fsIndex.setMaxBufferedDocs(this.maxBufferedDocs);
            
            this.currentReadOnlyReader = this.fsIndex.getNewReadOnlyIndexReader();
            
        } catch (IOException io) {
            logger.error("Got IOException while initializing indexes: " + io.getMessage());
            throw new BeanInitializationException("Got IOException while initializing indexes.", io);
        }
    }
    
    /**
     * Removes and closes all outdated read-only index readers 
     * regardless of reference count.
     */
    protected void cleanupOutdatedReadOnlyReaders() {
        synchronized(this.readOnlyReaderManagementLock) {
            for (Iterator i = this.outdatedReadOnlyReaders.entrySet().iterator(); i.hasNext();) {
                Map.Entry entry = (Map.Entry)i.next();
                
                IndexReader reader = (IndexReader) entry.getKey();
                Integer refCount = (Integer)entry.getValue();
                
                if (logger.isDebugEnabled()) {
                    logger.debug("Closing and removing an outdated index reader with" 
                            + " ref-count = " + refCount);
                }
                
                i.remove();
                
                try {
                    reader.close();
                } catch (IOException io) {
                    logger.warn("IOException while closing outdated index reader.");
                }
            }
        }
    }
    
    protected void releaseIndexSearcher(IndexSearcher searcher) 
        throws IOException {
        if (searcher == null) {
            return;
        }
        
        releaseReadOnlyIndexReader(searcher.getIndexReader());
    }
    
    protected IndexSearcher getIndexSearcher() 
        throws IOException {
        
        return new IndexSearcher(getReadOnlyIndexReader());
    }

    /**
     * Get a <em>read-only</em> <code>IndexReader</code> instance for this index.
     * The instance must be released with #releaseIndexReader(IndexReader)
     * after usage because of reference-counting.
     * 
     * XXX: Locking might be too aggressive and provoke thread lock contention on
     *      SMP architectures.
     * 
     * @return
     * @throws IOException
     */
    protected IndexReader getReadOnlyIndexReader() throws IOException {
        synchronized (this.readOnlyReaderManagementLock) {
            if (this.currentReadOnlyReaderOutdated) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Current index reader is outdated !");
                }
                
                if (this.currentReadOnlyReaderRefCount > 0) {
                    // Still in use, put it into outdated readers storage
                    // (schedule for closing either when too old, or when ref-count
                    // eventually reaches 0)
                    this.outdatedReadOnlyReaders.put(this.currentReadOnlyReader, 
                            new Integer(this.currentReadOnlyReaderRefCount));
                    if (logger.isDebugEnabled()) {
                        logger.debug("Added outdated but still in-use index reader to map of outdated readers.");
                        logger.debug("Current number of outdated, but still open readers is " 
                                + this.outdatedReadOnlyReaders.size());
                    }
                } else {
                    
                    this.currentReadOnlyReader.close();
                    
                    if (logger.isDebugEnabled()) {
                        logger.debug("Closed outdated and un-used search index reader");
                    }
                }
                
                this.currentReadOnlyReader = this.fsIndex.getNewReadOnlyIndexReader();
                this.currentReadOnlyReaderRefCount = 0;

                // No longer out-dated
                this.currentReadOnlyReaderOutdated = false;
            } 
            
            // Bump ref-count
            ++this.currentReadOnlyReaderRefCount;
            
            if (logger.isDebugEnabled()) {
                logger.debug("Index reader ref-count increased to " 
                        + this.currentReadOnlyReaderRefCount);
            }
            
            // Return new searcher on the reader
            return this.currentReadOnlyReader;
        }
    }
    
    
    /**
     * Release a read-only <code>IndexReader</code> instance obtained with
     * #getReadOnlyIndexReader().
     * 
     * @param searcher
     * @throws IOException
     */
    protected void releaseReadOnlyIndexReader(IndexReader reader)
        throws IOException {
        if (reader == null) {
            return;
        }
        
        synchronized(this.readOnlyReaderManagementLock) {
            
            if (reader == this.currentReadOnlyReader) {
                // Decrease ref-count
                if (this.currentReadOnlyReaderRefCount > 0) {
                    --this.currentReadOnlyReaderRefCount;
                    
                    if (logger.isDebugEnabled()) {
                        logger.debug("Index reader ref-count decreased to " 
                                + this.currentReadOnlyReaderRefCount);
                    }
                } else {
                    logger.warn("Ref-count for current index reader went below zero.");
                }
            } else {
                // Thread released an outdated reader, decrease ref-count and close
                // if we can.
                
                Integer refCount = (Integer)this.outdatedReadOnlyReaders.get(reader);
                if (refCount == null) {
                    // We no longer have any pointer to this, ignore release-request.
                    return;
                }
                
                int c = refCount.intValue() - 1;
                if (c == 0) {
                    // OK, last reference released, we close it.
                    this.outdatedReadOnlyReaders.remove(reader);
                    reader.close();
                } else {
                    // Still references left, we put it back with a decreased counter.
                    this.outdatedReadOnlyReaders.put(reader, new Integer(c));
                }
            }
        }
    }

    // CAN ONLY BE CALLED IF THREAD HAS ACQUIRED THE WRITE LOCK !!
    protected IndexReader getIndexReader() throws IOException {
        return this.fsIndex.getIndexReader();
    }
    
    // CAN ONLY BE CALLED IF THREAD HAS ACQUIRED THE WRITE LOCK !!
    protected IndexWriter getIndexWriter() throws IOException {
        return this.fsIndex.getIndexWriter();
    }
    
    // CAN ONLY BE CALLED IF THREAD HAS ACQUIRED THE WRITE LOCK !!
    protected void clearContents() throws IOException {
        
        synchronized (this.readOnlyReaderManagementLock) {
            cleanupOutdatedReadOnlyReaders();
            this.currentReadOnlyReader.close();
            this.currentReadOnlyReaderRefCount = 0;

            this.fsIndex.createNewIndex();
            
            this.currentReadOnlyReader = fsIndex.getNewReadOnlyIndexReader();
            this.currentReadOnlyReaderOutdated = false;
        }
        
    }
    
    // CAN ONLY BE CALLED IF THREAD HAS ACQUIRED THE WRITE LOCK !!
    protected void reinitialize() throws IOException {

        synchronized (this.readOnlyReaderManagementLock) {
            cleanupOutdatedReadOnlyReaders();
            this.currentReadOnlyReader.close();
            this.currentReadOnlyReaderRefCount = 0;

            this.fsIndex.reinitialize();
            
            this.currentReadOnlyReader = fsIndex.getNewReadOnlyIndexReader();
            this.currentReadOnlyReaderOutdated = false;
        }
        
    }
    
    //  CAN ONLY BE CALLED IF THREAD HAS ACQUIRED THE WRITE LOCK !!
    protected void close() throws IOException {
        // XXX: needs review, also see TODO in {@link AbstractLuceneIndex#close()}
        synchronized (this.readOnlyReaderManagementLock) {
            cleanupOutdatedReadOnlyReaders();
            this.currentReadOnlyReader.close();
            this.currentReadOnlyReaderRefCount = 0;

            this.fsIndex.close();
            
            this.currentReadOnlyReaderOutdated = true;
        }
    }
    
    // CAN ONLY BE CALLED IF THREAD HAS ACQUIRED THE WRITE LOCK !!
    protected void optimize() throws IOException {
        this.fsIndex.getIndexWriter().optimize();
    }
    
    // Commit changes done by reader/writer and optimize index if necessary.
    // This method checks if the index reader used for searching becomes
    // out-dated after the commit.
    // CAN ONLY BE CALLED IF THREAD HAS ACQUIRED THE WRITE LOCK !!
    protected void commit() throws IOException {
        // Optimize index, if we've reached 'optimizeInterval' number of commits.
        if (++this.commitCounter % this.optimizeInterval == 0) {
            logger.info("Reached " + this.commitCounter 
                                        + " commits, auto-optimizing index ..");
            this.fsIndex.getIndexWriter().optimize();
            logger.info("Auto-optimization completed.");
        }
        
        this.fsIndex.commit();

        synchronized (this.readOnlyReaderManagementLock) {
            if (logger.isDebugEnabled()) {
                if (this.currentReadOnlyReader.isCurrent()) {
                    logger.debug("Read-only index reader is current");
                } else {
                    logger.debug("Read-only index reader is outdated");
                }
            }
            
            this.currentReadOnlyReaderOutdated = ! this.currentReadOnlyReader.isCurrent();
            
        }        
    }
    
    
    
    /* (non-Javadoc)
     * @see org.springframework.beans.factory.DisposableBean#destroy()
     */
    public void destroy() throws Exception {
       logger.info("Graceful index shutdown, waiting for write lock ..");
       if (writeLockAttempt(this.maxLockAcquireTimeOnShutdown * 1000)) {
           logger.info("Got write lock, closing down.");
           this.fsIndex.close();
       } else {
           logger.warn("Failed to acquire the write lock within "
              + " the time limit of " + this.maxLockAcquireTimeOnShutdown 
              + " seconds, index might be corrupted.");
       }
    }

    /**
     * Explicit write locking. Must be acquired before doing any write
     * operations on index, using either the reader or the writer.
     */
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

    /**
     * Attempt write lock with timeout 
     */
    protected boolean writeLockAttempt(long timeout) {
        try {
            boolean acquired = this.lock.attempt(timeout);
            
            if (logger.isDebugEnabled()) {
                if (acquired) {
                    logger.debug("Thread '" + Thread.currentThread().getName() + 
                    "' got index lock.");
                } else {
                    logger.debug("Thread '" + Thread.currentThread().getName() + 
                      "' failed to acquire lock after waiting for " + timeout + " ms");
                    
                }
            }
            
            return acquired;
        } catch (InterruptedException ie) {
            return false;
        }
    }

    /**
     * Write lock release.
     */
    protected void writeLockRelease() {
        
        this.lock.release();
        
        if (logger.isDebugEnabled()) {
            logger.debug("Thread '" + Thread.currentThread().getName() + 
                         "' released index lock.");
        }
        
    }
    
    
   /**
     * LRU map of unclosed, but outdated index readers. When full, the oldest
     * is forcibly closed and nullified, no matter if its ref-count is still > 0.
     * The map contains readers as keys with an Integer ref-count as values.
     * 
     */
    private static class IndexReaderRefCountMap extends LinkedHashMap {
        
        private static final long serialVersionUID = -8554031873156146523L;

        public IndexReaderRefCountMap(int initialCapacity) {
            super(initialCapacity);
        }
        
        protected boolean removeEldestEntry(Map.Entry entry) {
            if (super.size() <= LuceneIndex.MAX_OUTDATED_SEARCH_READERS) {
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
        return this.eraseExistingIndex;
    }

    public void setEraseExistingIndex(boolean eraseExistingIndex) {
        this.eraseExistingIndex = eraseExistingIndex;
    }

    public boolean isForceUnlock() {
        return this.forceUnlock;
    }

    public void setForceUnlock(boolean forceUnlock) {
        this.forceUnlock = forceUnlock;
    }

    public int getMaxMergeDocs() {
        return this.maxMergeDocs;
    }

    public void setMaxMergeDocs(int maxMergeDocs) {
        this.maxMergeDocs = maxMergeDocs;
    }

    public int getMergeFactor() {
        return this.mergeFactor;
    }

    public void setMergeFactor(int mergeFactor) {
        this.mergeFactor = mergeFactor;
    }

    public int getMaxBufferedDocs() {
        return this.maxBufferedDocs;
    }

    public void setMaxBufferedDocs(int maxBufferedDocs) {
        this.maxBufferedDocs = maxBufferedDocs;
    }

    public int getOptimizeInterval() {
        return this.optimizeInterval;
    }

    public void setOptimizeInterval(int optimizeInterval) {
        this.optimizeInterval = optimizeInterval;
    }

    public String getIndexPath() {
        return this.indexPath;
    }

    public void setIndexPath(String indexPath) {
        this.indexPath = indexPath;
    }

    public void setMaxLockAcquireTimeOnShutdown(int maxLockAcquireTimeOnShutdown) {
        this.maxLockAcquireTimeOnShutdown = maxLockAcquireTimeOnShutdown;
    }
    
}

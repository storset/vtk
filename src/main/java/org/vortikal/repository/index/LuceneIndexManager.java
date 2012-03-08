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
package org.vortikal.repository.index;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.LogMergePolicy;
import org.apache.lucene.search.IndexSearcher;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.util.threads.Mutex;

/**
 * Manages access to a single file-system based Lucene index instance.
 * 
 * Contains mutex-locking functionality.
 *
 * TODO: JavaDoc must be updated
 * TODO: Design a more elegant locking API
 * 
 * @author oyviste
 */
public class LuceneIndexManager implements InitializingBean, DisposableBean {
    
    private final Log logger = LogFactory.getLog(LuceneIndexManager.class);

    private FSBackedLuceneIndex fsIndex;
    
    private String storageRootPath;
    private String storageId;
    private int optimizeInterval = 25;
    private int commitCounter = 0;
    private int mergeFactor = LogMergePolicy.DEFAULT_MERGE_FACTOR;
    private int maxMergeDocs = LogMergePolicy.DEFAULT_MAX_MERGE_DOCS;

    private int maxReadOnlyReaders = 1;
    private int maxAgingReadOnlyReaders = 0;
    private int agingReadOnlyReaderThreshold = 30;
    private IndexReaderWarmup irw;

    private int maxLockAcquireTimeOnShutdown = 30; // 30 seconds max to wait for lock when shutting down
    private boolean forceUnlock = false;
    
    /** If <code>true</code>, the underlying index will be explicitly closed 
     * after initialization is complete. Can be used for index instances you 
     * don't want to have open before it is explicitly needed.
     * Such an instance must be re-initialized before it can be 
     * used (by calling {@link #reinitialize()}). 
     **/
    private boolean closeAfterInitialization = false;
    
    /* Internal mutex write lock backing the public locking functions of this class. */
    /* The lock disregards any thread-ownership, and it need not be released by
     * the same thread that acquired it. The only thing it stores is the locking state itself.
     */
    private final Mutex lock = new Mutex();
    
    @Override
    public void afterPropertiesSet() throws BeanInitializationException {
        try {
            // Initialization of physical storage directory
            File storageDirectory = initializeStorageDirectory(this.storageRootPath, 
                                                               this.storageId);
            
            // Initialization of file-system backed Lucene index
            this.fsIndex = new FSBackedLuceneIndex(storageDirectory,
                                                   new KeywordAnalyzer(),
                                                   this.forceUnlock);
            
            this.fsIndex.setMaxMergeDocs(this.maxMergeDocs);
            this.fsIndex.setMergeFactor(this.mergeFactor);
            this.fsIndex.setMaxReadOnlyReaders(this.maxReadOnlyReaders);
            this.fsIndex.setMaxAgingReadOnlyReaders(this.maxAgingReadOnlyReaders);
            this.fsIndex.setAgingReadOnlyReaderThreshold(this.agingReadOnlyReaderThreshold);
            this.fsIndex.setIndexReaderWarmup(this.irw);
            this.fsIndex.reinitialize();
            
            logger.info("Initialization of index '" + this.getStorageId() + "' complete.");
            
            if (this.closeAfterInitialization) {
                logger.info("Closing instance after initialization.");
                this.fsIndex.close();
            }
            
        } catch (IOException io) {
            logger.error("Got IOException while initializing index: " + io.getMessage());
            throw new BeanInitializationException("Got IOException while initializing index", io);
        }
    }
    
    private File initializeStorageDirectory(String storageRootPath, String storageId)
        throws IOException {
        
        File storageDirectory = new File(storageRootPath, storageId);
        
        if (storageDirectory.isDirectory()) {
            if (! storageDirectory.canWrite()) {
                throw new IOException("Resolved storage directory '"
                        + storageDirectory.getAbsolutePath() 
                        + "' is not writable");
            }
        } else if (storageDirectory.isFile()) {
            throw new IOException("Resolved storage directory '" 
                    + storageDirectory.getAbsolutePath()
                    + "' is a file");
        } else {
            // Directory does not exist, we need to create it.
            if (!storageDirectory.mkdir()) {
                throw new IOException("Failed to create resolved storage directory '"
                        + storageDirectory.getAbsolutePath() 
                        + "'");
            }
        }
        
        return storageDirectory;
    }

    public IndexSearcher getIndexSearcher(int maxDirtyAge) throws IOException {
        return new IndexSearcher(getReadOnlyIndexReader(maxDirtyAge));
    }

    public IndexSearcher getIndexSearcher() throws IOException {
        return new IndexSearcher(getReadOnlyIndexReader());
    }

    public void releaseIndexSearcher(IndexSearcher searcher) throws IOException {
        if (searcher != null) releaseReadOnlyIndexReader(searcher.getIndexReader());
    }
    
    public IndexReader getReadOnlyIndexReader() throws IOException {
        return this.fsIndex.getReadOnlyIndexReader();
    }

    public IndexReader getReadOnlyIndexReader(int maxDirtyAge) throws IOException {
        return this.fsIndex.getReadOnlyIndexReader(maxDirtyAge);
    }
    
    public void releaseReadOnlyIndexReader(IndexReader readOnlyReader) throws IOException {
        if (readOnlyReader != null) {
            this.fsIndex.releaseReadOnlyIndexReader(readOnlyReader);
        }
    }

    // CAN ONLY BE CALLED IF THREAD HAS ACQUIRED THE WRITE LOCK !!
    public IndexReader getIndexReader() throws IOException {
        return this.fsIndex.getIndexReader();
    }
    
    // CAN ONLY BE CALLED IF THREAD HAS ACQUIRED THE WRITE LOCK !!
    public IndexWriter getIndexWriter() throws IOException {
        return this.fsIndex.getIndexWriter();
    }
    
    // CAN ONLY BE CALLED IF THREAD HAS ACQUIRED THE WRITE LOCK !!
    public void clearContents() throws IOException {
        this.fsIndex.createNewIndex();
    }
    
    // CAN ONLY BE CALLED IF THREAD HAS ACQUIRED THE WRITE LOCK !!
    public void reinitialize() throws IOException {
        this.fsIndex.reinitialize();
    }
    
    //  CAN ONLY BE CALLED IF THREAD HAS ACQUIRED THE WRITE LOCK !!
    public void close() throws IOException {
        this.fsIndex.close();
    }
    
    public boolean isClosed() {
        return this.fsIndex.isClosed();
    }
    
    // CAN ONLY BE CALLED IF THREAD HAS ACQUIRED THE WRITE LOCK !!
    public void optimize() throws IOException {
        this.fsIndex.getIndexWriter().optimize();
        this.fsIndex.commit(); // Mark read-only reader dirty (force refresh)
    }
    
    // Commit changes done by reader/writer and optimize index if necessary.
    // CAN ONLY BE CALLED IF THREAD HAS ACQUIRED THE WRITE LOCK !!
    public void commit() throws IOException {
        // Optimize index, if we've reached 'optimizeInterval' number of commits.
        if (++this.commitCounter % this.optimizeInterval == 0) {
            logger.info("Reached " + this.commitCounter
                                        + " commits, auto-optimizing index ..");
            this.fsIndex.getIndexWriter().optimize();
            logger.info("Auto-optimization completed.");
        }
        
        this.fsIndex.commit();
    }
    
    // CAN ONLY BE CALLED IF THREAD HAS ACQUIRED THE WRITE LOCK !!
    public void corruptionTest() throws IOException {
        this.fsIndex.corruptionTest();
    }
    
    /* (non-Javadoc)
     * @see org.springframework.beans.factory.DisposableBean#destroy()
     */
    public void destroy() throws Exception {
       logger.info("Graceful index shutdown, waiting for write lock on index '"
               + this.storageId + "' ..");
       if (writeLockAttempt(this.maxLockAcquireTimeOnShutdown * 1000)) {
           logger.info("Got write lock on index '" + this.storageId
                   + "', closing down.");
           this.fsIndex.close();
       } else {
           logger.warn("Failed to acquire the write lock on index '"
              + this.storageId + "' within "
              + " the time limit of " + this.maxLockAcquireTimeOnShutdown 
              + " seconds, index might be corrupted.");
           
           // XXX: maybe force closing anyway, here.
       }
    }

    /**
     * Explicit write locking. Must be acquired before doing any write
     * operations on index, using either the reader or the writer.
     */
    public boolean writeLockAcquire() {
        if (this.lock.lock()) {
            if (logger.isDebugEnabled()) {
                logger.debug("Thread '" + Thread.currentThread().getName()
                        + "' got write lock on index '"
                        + this.storageId + "'.");
            }
            return true;
        }

        return false;
    }

    /**
     * Attempt write lock with timeout 
     */
    public boolean writeLockAttempt(long timeout) {
        if (this.lock.tryLock(timeout, TimeUnit.MILLISECONDS)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Thread '" + Thread.currentThread().getName()
                        + "' got write lock on index '" + this.storageId + "'.");
            }
            return true;

        } else if (logger.isDebugEnabled()) {
            logger.debug("Thread '" + Thread.currentThread().getName()
                    + "' failed to acquire write lock on index '"
                    + this.storageId
                    + "' after waiting for " + timeout + " ms");
        }

        return false;
    }

    /**
     * Write lock release.
     */
    public void writeLockRelease() {
        this.lock.unlock();
        if (logger.isDebugEnabled()) {
            logger.debug("Thread '" + Thread.currentThread().getName() +
                         "' unlocked write lock on index '"
                    + this.storageId + "'.");
        }
    }
    
    public void setForceUnlock(boolean forceUnlock) {
        this.forceUnlock = forceUnlock;
    }

    public void setMaxMergeDocs(int maxMergeDocs) {
        this.maxMergeDocs = maxMergeDocs;
    }

    public void setMergeFactor(int mergeFactor) {
        this.mergeFactor = mergeFactor;
    }

    public void setOptimizeInterval(int optimizeInterval) {
        this.optimizeInterval = optimizeInterval;
    }

    public void setMaxLockAcquireTimeOnShutdown(int maxLockAcquireTimeOnShutdown) {
        this.maxLockAcquireTimeOnShutdown = maxLockAcquireTimeOnShutdown;
    }

    public String getStorageId() {
        return this.storageId;
    }

    @Required
    public void setStorageId(String storageId) {
        this.storageId = storageId;
    }

    public String getStorageRootPath() {
        return this.storageRootPath;
    }

    @Required
    public void setStorageRootPath(String storageRootPath) {
        this.storageRootPath = storageRootPath;
    }

    public void setCloseAfterInitialization(boolean closeAfterInitialization) {
        this.closeAfterInitialization = closeAfterInitialization;
    }

    public void setMaxReadOnlyReaders(int maxReadOnlyReaders) {
        this.maxReadOnlyReaders = maxReadOnlyReaders;
    }

    public void setMaxAgingReadOnlyReaders(int maxAgingReadOnlyReaders) {
        this.maxAgingReadOnlyReaders = maxAgingReadOnlyReaders;
    }

    public void setAgingReadOnlyReaderThreshold(int agingReadOnlyReaderThreshold) {
        this.agingReadOnlyReaderThreshold = agingReadOnlyReaderThreshold;
    }

    public void setIndexReaderWarmup(IndexReaderWarmup irw) {
        this.irw = irw;
    }

}

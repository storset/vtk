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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;


/**
 * Some common Lucene index functionality. This class does not handle locking, 
 * but returns IndexWriter/IndexReader instances in a mutually exclusive fashion, 
 * automatically closing one or the other.
 * 
 * TODO: JavaDoc
 * @author oyviste
 */
public abstract class AbstractLuceneIndex implements InitializingBean {
    
    private static Log logger = LogFactory.getLog(AbstractLuceneIndex.class.getName());
    
    /* Lucene tunables */
    private int mergeFactor = 10;
    private int minMergeDocs = 100;
    private int maxMergeDocs = 10000;
    
    /** 
     * Can be set to <code>true</code> to erase _all_ contents in index <code>Directory</code>, 
     * upon first time initialization. Will delete all files and empty subdirectories, 
     * regardless if they belong to Lucene or not. This is potentially dangerous if the
     * underlying directory implementation is file system based, and should be used
     * with caution.
     * Warning: Read description once more.
     */
    private boolean eraseExistingIndex = false;
    
    /** Specifies if any existing index should be forcibly unlocked, if it was
     *  locked at init-time.  */
    private boolean forceUnlock = false;
    
    /** <code>IndexWriter</code> instance. */
    private IndexWriter writer;
    
    /** <code>IndexReader</code> instance. */
    private IndexReader reader;
    
    /** Lucene <code>Directory</code> implementation. */
    private Directory directory;
    
    /** Lucene <code>Analyzer</code> implementation used. */
    private Analyzer analyzer;
    
    public void afterPropertiesSet() 
        throws BeanInitializationException {
        if (this.analyzer == null) {
            this.analyzer = new KeywordAnalyzer(); // Default (and simplest) Analyzer
        } 
        
        try {
            directory = createDirectory(this.eraseExistingIndex);
            if (directory == null) {
                throw new BeanInitializationException("Directory was null");
            }
            initializeIndex(this.directory);
        } catch (IOException io) {
            logger.warn("Got IOException while initializing index: " + io.getMessage());
            throw new BeanInitializationException("Got IOException while " +
                                                  "initializing index: ", io);
        }
    }
    
    protected synchronized IndexWriter getIndexWriter() throws IOException {
        // Check if we are already providing a reader, close it if so.
        if (reader != null) {
            reader.close();
            reader = null;
        }
        
        // Create a new writer if necessary.
        if (writer == null) {
            writer = new IndexWriter(directory, analyzer, false);
            writer.setMaxBufferedDocs(this.minMergeDocs);
            writer.setMaxMergeDocs(this.maxMergeDocs);
            writer.setMergeFactor(this.mergeFactor);
        }
        
        return writer;
    }
    
    protected synchronized IndexReader getIndexReader() throws IOException {
        if (writer != null) {
            writer.close();
            writer = null;
        }
        
        if (reader == null) {
            reader = IndexReader.open(directory);
        }
        
        return reader;
    }

    protected IndexReader getReadOnlyIndexReader() throws IOException {
        return IndexReader.open(this.directory);
    }
    
    protected IndexSearcher getIndexSearcher() throws IOException {
        return new IndexSearcher(this.directory);
    }
    
    protected synchronized void commit() throws IOException {
        if (! IndexReader.isLocked(this.directory)) {
            // No lock on index means there are no pending changes.
            return;
        }

        if (reader != null) {
            reader.close();
            reader = null;
        }
        
        if (writer != null) {
            writer.close();
            writer = null;
        }
    }

    /**
     * Re-initializes index directory.
     * @throws IOException
     */
    protected synchronized void reinitializeIndex() throws IOException {
        if (reader != null) {
            reader.close();
            reader = null;
        }
        
        if (writer != null) {
            writer.close();
            writer = null;
        }
        
        if (directory != null) {
            directory.close();
            // Don't null directory here. This closes a small race between this
            // method and getReadOnlyIndexReader(), getIndexSearcher(). If we don't
            // null here, they might get the old closed instance, and might throw an IOException, 
            // but that's much better than a null-pointer exception.
            // The read-only/searcher methods are not synchronized for performance reasons.
        }
        
        this.directory = createDirectory(false);
        initializeIndex(this.directory);
    }

    /**
     * Clear existing index contents, and create a new one.
     * @throws IOException
     */
    protected synchronized void createNewIndex() throws IOException {
        if (reader != null) {
            reader.close();
            reader = null;
        }
        
        if (writer != null) {
            writer.close();
            writer = null;
        }
        

        if (directory != null) {
            if (IndexReader.isLocked(directory)) {
                IndexReader.unlock(directory);
            }
            directory.close();
            // Don't null directory here, for the same reason as explained above.
        }
        
        this.directory = createDirectory(true);
        writer = new IndexWriter(this.directory, this.analyzer, true);
        writer.close();
        writer = null;
    }

    protected synchronized void optimize() throws IOException {
        getIndexWriter().optimize();
    }
    
    /** Initialize Lucene index */
    private void initializeIndex(Directory directory) throws IOException {
        // Check index lock, no matter if a valid index exists in directory
        // or not. The index locks are stored in /tmp, and as such, not
        // dependent upon index directory contents.
        // If contents of an index has been cleared manually, but locks still
        // remain in /tmp, we are in trouble if we don't clear them here,
        // and try to create a new index.
        checkIndexLock(directory);
        
        // Check status on index, create new if necessary
        if (! IndexReader.indexExists(directory)) {
            new IndexWriter(directory, analyzer, true).close();
            logger.debug("New index created.");
        } 
    }
    
    /** Check index lock, force-unlock if requested. */
    private void checkIndexLock(Directory directory) throws IOException {
        if (IndexReader.isLocked(directory)) {
            // See if we should try to force-unlock it
            if (this.forceUnlock) {
                logger.warn("Index was locked, forcibly releasing lock.");
                IndexReader.unlock(directory);
            } else {
                logger.warn("Index was locked and 'forceUnlock' is set to false.");
            }
        }
    }

    /**
     * Must be implemented by subclasses to provide a directory implementation.
     */
    protected abstract Directory createDirectory(boolean eraseContents) 
        throws IOException;

    protected Directory getDirectory() {
        return this.directory;
    }
    
    protected Analyzer getAnalyzer() {
        return this.analyzer;
    }
    
    protected void setAnalyzer(Analyzer analyzer) {
        this.analyzer = analyzer;
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

    public int getMaxMergeDocs() {
        return maxMergeDocs;
    }

    public void setMaxMergeDocs(int maxMergeDocs) {
        this.maxMergeDocs = maxMergeDocs;
    }

    public boolean isForceUnlock() {
        return forceUnlock;
    }

    public void setForceUnlock(boolean forceUnlock) {
        this.forceUnlock = forceUnlock;
    }
    
    public boolean isEraseExistingIndex() {
        return this.eraseExistingIndex;
    }
    
    public void setEraseExistingIndex(boolean eraseExistingIndex) {
        this.eraseExistingIndex = eraseExistingIndex;
    }
}

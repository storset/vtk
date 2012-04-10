/* Copyright (c) 2005–2012, University of Oslo, Norway
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

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.LogByteSizeMergePolicy;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Version;

/**
 * Some common Lucene index
 * functionality. This class does not handle locking explicitly, but returns
 * IndexWriter/IndexReader instances in a mutually exclusive fashion,
 * automatically closing one or the other. It is, by itself, synchronized, and
 * thus should be thread safe.
 * 
 * TODO consider taking advantage of near realtime reader.
 * @see org.apache.lucene.index.IndexWriter#getReader(boolean) 
 * 
 * @author oyviste
 */
public abstract class AbstractLuceneIndex {

    private final Log logger = LogFactory.getLog(AbstractLuceneIndex.class);

    /**
     * Whether to tune IndexWriter settings for batch indexing. Default
     * is <code>false</code>, which means use mostly Lucene defaults and
     * optimize for incremental updates. Batch-indexing mode should not be
     * used for indexes which are searched directly.
     */
    private boolean batchIndexingMode = false;

    /**
     * Specifies if any existing index should be forcibly unlocked, if it was
     * locked at init-time.
     **/
    private boolean forceUnlock = false;

    /** Main <code>IndexWriter</code> instance. */
    private IndexWriter writer = null;

    /** Main <code>IndexReader</code> instance. */
    private IndexReader reader = null;

    /**
     * Main <code>Directory</code> implementation. Index is considered closed if
     * this is null.
     */
    private Directory directory = null;

    /** Default Lucene <code>Analyzer</code> implementation used. */
    private Analyzer analyzer = null;

    // Aging/possibly dirty read-only index reader instances
    private int maxAgingReadOnlyReaders = 0;
    private int agingReadOnlyReaderThreshold = 30;
    private ReadOnlyIndexReaderPool agingReadOnlyReaderPool;

    // Normal always update-to-date read only index reader instances
    private int maxReadOnlyReaders = 1;
    private ReadOnlyIndexReaderPool readOnlyReaderPool;

    // Reader warmup delegate
    private IndexReaderWarmup irw;

    /**
     * Constructor with some sensible defaults.
     *
     * Note that #reinitialize must be called before index can be used.
     */
    public AbstractLuceneIndex() {
        this(new KeywordAnalyzer(), false);
    }

    /**
     * Constructor with selectable analyzer implementation and some parameters
     * controlling initialization.
     * 
     * Note that #reinitialize() must be called before index can be used.
     *
     * @param analyzer
     * @param forceUnlock
     */
    public AbstractLuceneIndex(Analyzer analyzer, boolean forceUnlock) {
        this.analyzer = analyzer;
        this.forceUnlock = forceUnlock;
    }

    /**
     * This method must be implemented by subclasses to provide a
     * {@link org.apache.lucene.store.Directory} implementation.
     */
    protected abstract Directory createDirectory() throws IOException;

    /**
     * Re-initialize/open index.
     *
     * This method must be called to open the underlying index, even at
     * first time initialization !
     *
     * @throws IOException
     */
    protected final synchronized void reinitialize() throws IOException {
        if (this.reader != null) {
            this.reader.close();
            this.reader = null;
        }

        if (this.writer != null) {
            this.writer.close();
            this.writer = null;
        }

        closeReadOnlyReaderPools();

        if (this.directory != null) {
            this.directory.close();
            this.directory = null;
        }

        this.directory = createDirectory();
        if (this.directory == null) {
            throw new IOException("Directory provided by subclass was null");
        }

        initializeIndexDirectory(this.directory, false);

        initializeReadOnlyReaderPools();

        logger.info("Opened and initialized index at directory '" + this.directory + "'");
    }

    protected final synchronized IndexWriter getIndexWriter() throws IOException {
        if (this.directory == null) {
            throw new IOException("Index is closed");
        }

        // Check if we are already providing a reader, close it if so.
        if (this.reader != null) {
            this.reader.close();
            this.reader = null;
        }

        // Create a new writer if necessary.
        if (this.writer == null) {
            this.writer = new IndexWriter(this.directory, newIndexWriterConfig());
        }

        return this.writer;
    }
    
    private IndexWriterConfig newIndexWriterConfig() {
        IndexWriterConfig cfg = new IndexWriterConfig(Version.LUCENE_35, this.analyzer);
        cfg.setMaxThreadStates(1);
        // TODO switch to newer TieredMergePolicy when ready, but keep old behaviour for now.
        LogByteSizeMergePolicy mp = new LogByteSizeMergePolicy();
        mp.setMergeFactor(this.batchIndexingMode ? 20 : 4);
        cfg.setMergePolicy(mp);
        cfg.setRAMBufferSizeMB(this.batchIndexingMode ? 32.0 : IndexWriterConfig.DEFAULT_RAM_BUFFER_SIZE_MB);
        return cfg;
    }

    protected final synchronized IndexReader getIndexReader() throws IOException {
        if (this.directory == null) {
            throw new IOException("Index is closed");
        }

        if (this.writer != null) {
            this.writer.close();
            this.writer = null;
        }

        if (this.reader == null) {
            // This reader is typically not used for queries, so
            // decrease RAM usage by setting TermInfosIndexDivisor to 4.
            this.reader = IndexReader.open(this.directory, null, false, 4);
        }

        return this.reader;
    }

    // Method is intentionally *not* synchronized
    protected final IndexReader getReadOnlyIndexReader() throws IOException {
        if (isClosed()) throw new IOException("Index is closed.");

        return this.readOnlyReaderPool.getReader(false);
    }

    // Method is intentionally *not* synchronized
    protected final IndexReader getReadOnlyIndexReader(int maxDirtyAge)
        throws IOException {

        if (isClosed()) throw new IOException("Index is closed.");

        if (this.agingReadOnlyReaderPool != null
                && maxDirtyAge >= this.agingReadOnlyReaderThreshold) {

            // Use aging readers and thread stickyness
            return this.agingReadOnlyReaderPool.getReader(true);
        }

        return this.readOnlyReaderPool.getReader(false);
    }

    // Method is intentionally *not* synchronized
    protected final void releaseReadOnlyIndexReader(IndexReader reader)
            throws IOException {
        // Decrease ref-count (it will be closed if refCount hits zero)
        reader.decRef();
    }

    private void closeReadOnlyReaderPools() throws IOException {
        if (this.readOnlyReaderPool != null) {
            this.readOnlyReaderPool.close();
            this.readOnlyReaderPool = null;
        }
        if (this.agingReadOnlyReaderPool != null) {
            this.agingReadOnlyReaderPool.close();
            this.agingReadOnlyReaderPool = null;
        }
    }

    private void initializeReadOnlyReaderPools() throws IOException {
        if (isClosed()) {
            // This shouldn't happen, but still ..
            throw new IOException("Unable to initialize read-only reader pools: index is closed.");
        }

        this.readOnlyReaderPool = new ReadOnlyIndexReaderPool(this.directory,
                                                           this.maxReadOnlyReaders);
        this.readOnlyReaderPool.setIndexReaderWarmup(this.irw);

        if (this.maxAgingReadOnlyReaders > 0) {
            this.agingReadOnlyReaderPool = new ReadOnlyIndexReaderPool(this.directory,
                    this.maxAgingReadOnlyReaders, this.agingReadOnlyReaderThreshold);
            this.agingReadOnlyReaderPool.setIndexReaderWarmup(this.irw);
        } else {
            this.agingReadOnlyReaderPool = null;
        }
    }

    /**
     * Commits any changes, but does not close directory.
     * 
     * @throws IOException
     */
    protected final synchronized void commit() throws IOException {
        if (this.directory == null) {
            throw new IOException("Index is closed");
        }

        if (this.reader != null) {
            this.reader.close();
            this.reader = null;
        }

        if (this.writer != null) {
            this.writer.close();
            this.writer = null;
        }

        try {
            // Initiate instant refresh of read-only reader pool with no allowed dirty-time.
            // Catch any IOException, since we don't want to disturb index update thread
            // with potential pooling problems.
            this.readOnlyReaderPool.refreshPoolNow();
        } catch (IOException io) {
            this.logger.warn("IOException while calling ReadOnlyIndexReaderPool#refreshPoolNow()", io);
        }
    }

    /**
     * Close down the managed Lucene index.
     * 
     * This will also gracefully close all active read-only readers.
     * 
     **/
    protected final synchronized void close() throws IOException {
        if (this.reader != null) {
            this.reader.close();
            this.reader = null;
        }

        if (this.writer != null) {
            this.writer.close();
            this.writer = null;
        }

        closeReadOnlyReaderPools();

        if (this.directory != null) {
            this.directory.close();
            this.directory = null;
        }

        logger.info("Index closed for business.");
    }

    protected final synchronized boolean isClosed() {
        return (this.directory == null);
    }


    /**
     * Clear existing index directory contents, and create a new one. This
     * method will automaticallly re-initialize and re-open index.
     * 
     * @throws IOException
     */
    protected final synchronized void createNewIndex() throws IOException {

        if (this.reader != null) {
            this.reader.close();
            this.reader = null;
        }

        if (this.writer != null) {
            this.writer.close();
            this.writer = null;
        }

        closeReadOnlyReaderPools();

        if (this.directory != null) {
            this.directory.close();
            this.directory = null;
        }

        this.directory = createDirectory();
        initializeIndexDirectory(this.directory, true);

        initializeReadOnlyReaderPools();

        logger.info("Created new index at directory '" + this.directory + "'");
    }

    /** Initialize Lucene index */
    private void initializeIndexDirectory(Directory directory, boolean createNew)
            throws IOException {

        if (!IndexReader.indexExists(directory) || createNew) {
            IndexWriterConfig cfg = newIndexWriterConfig();
            cfg.setOpenMode(OpenMode.CREATE);
            new IndexWriter(directory, cfg).close();
            logger.info("Empty new index created in directory '" + directory + "'");
        } else {
            checkIndexLock(directory);
        }
    }

    /** Check index filesystem-lock, force-unlock if requested. */
    private void checkIndexLock(Directory directory) throws IOException {
        if (IndexWriter.isLocked(directory)) {
            // See if we should try to force-unlock it
            if (this.forceUnlock) {
                logger.warn("Index directory is locked, forcibly releasing lock.");
                IndexWriter.unlock(directory);
            } else {
                throw new IOException("Index directory '" + directory
                        + "' is locked and 'forceUnlock' is set to false.");
            }
        }
    }

    public void setMaxReadOnlyReaders(int maxReadOnlyReaders) {
        if (maxReadOnlyReaders < 1) {
            throw new IllegalArgumentException("maxReadOnlyReaders must be >= 1");
        }
        this.maxReadOnlyReaders = maxReadOnlyReaders;
    }

    public void setMaxAgingReadOnlyReaders(int maxAgingReadOnlyReaders) {
        if (maxAgingReadOnlyReaders < 0) {
            throw new IllegalArgumentException("maxAgingReadOnlyReaders must be >= 0");
        }
        this.maxAgingReadOnlyReaders = maxAgingReadOnlyReaders;
    }

    /**
     * When requestion a read-only reader, at what max dirty age should we
     * return an aging (and possibly dirty) reader instead of a refreshed one.
     * The value is in seconds.
     *
     * @param agingReadOnlyReaderThreshold the agingReadOnlyReaderThreshold to set
     */
    public void setAgingReadOnlyReaderThreshold(int agingReadOnlyReaderThreshold) {
        if (maxAgingReadOnlyReaders < 0) {
            throw new IllegalArgumentException("agingReadOnlyReaderThreshold must be >= 0");
        }
        this.agingReadOnlyReaderThreshold = agingReadOnlyReaderThreshold;
    }

    public void setIndexReaderWarmup(IndexReaderWarmup irw) {
        this.irw = irw;
    }
    
    public void setBatchIndexingMode(boolean batchIndexingMode) {
        this.batchIndexingMode = batchIndexingMode;
    }

}

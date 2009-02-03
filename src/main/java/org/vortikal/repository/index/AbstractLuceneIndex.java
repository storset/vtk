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

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.store.Directory;

/**
 * TODO: This JavaDoc description is outdated. Some common Lucene index
 * functionality. This class does not handle locking explicitly, but returns
 * IndexWriter/IndexReader instances in a mutually exclusive fashion,
 * automatically closing one or the other. It is, by itself, synchronized, and
 * thus should be thread safe.
 * 
 * @author oyviste
 */
public abstract class AbstractLuceneIndex {

    private final Log logger = LogFactory.getLog(AbstractLuceneIndex.class);

    /* Lucene tunables */
    private int mergeFactor = 10;

    private int maxBufferedDocs = 100;

    private int maxMergeDocs = 10000;

    /**
     * Specifies if any existing index should be forcibly unlocked, if it was
     * locked at init-time.
     **/
    private boolean forceUnlock = false;

    /** Erase existing index upon first initialization */
    private boolean eraseExistingIndex = false;

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

    /**
     * Maximum number of outdated but in-use read-only index readers to keep
     * before forcefully closing them.
     */
    // private static final int MAX_DIRTY_READONLY_READERS = 10;
    // private LinkedList<ReadOnlyIndexReader> dirtyReadOnlyReaders = new
    // LinkedList<ReadOnlyIndexReader>();
    /** Shared read-only index reader instance */
    private IndexReader roReader = null;

    /**
     * Constructor with some sensible defaults.
     * 
     */
    public AbstractLuceneIndex() {
        this(new KeywordAnalyzer(), false, false);
    }

    /**
     * Constructor with selectable analyzer implementation and some parameters
     * controlling initialization.
     * 
     * @param analyzer
     * @param eraseExistingIndex
     * @param forceUnlock
     */
    public AbstractLuceneIndex(Analyzer analyzer, boolean eraseExistingIndex,
            boolean forceUnlock) {
        this.analyzer = analyzer;
        this.forceUnlock = forceUnlock;
        this.eraseExistingIndex = eraseExistingIndex;
    }

    /**
     * Should be called <em>once</em> before any of the other methods.
     * 
     * @throws IOException
     */
    protected void initialize() throws IOException {
        this.directory = createDirectory();
        if (this.directory == null) {
            throw new IOException("Directory provided by subclass was null");
        }

        initializeIndexDirectory(this.directory, this.eraseExistingIndex);
    }

    /**
     * This method must be implemented by subclasses to provide a
     * {@link org.apache.lucene.store.Directory} implementation.
     */
    protected abstract Directory createDirectory() throws IOException;

    protected synchronized IndexWriter getIndexWriter() throws IOException {
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
            this.writer = new IndexWriter(this.directory, this.analyzer, false,
                    MaxFieldLength.LIMITED);
            this.writer.setMaxBufferedDocs(this.maxBufferedDocs);
            this.writer.setMaxMergeDocs(this.maxMergeDocs);
            this.writer.setMergeFactor(this.mergeFactor);
        }

        return this.writer;
    }

    protected synchronized IndexReader getIndexReader() throws IOException {
        if (this.directory == null) {
            throw new IOException("Index is closed");
        }

        if (this.writer != null) {
            this.writer.close();
            this.writer = null;
        }

        if (this.reader == null) {
            this.reader = IndexReader.open(this.directory);
        }

        return this.reader;
    }

    /**
     * XXX: Not entirely sure we can ignore closing of old read-only readers ..
     * Need to find out. Otherwise we risk leaking system resources.
     * 
     * XXX: We might need to continue with simple dirty-state tracking
     *      here, since we do not want huge parts of index to become unavailable
     *      during large incremental updates (because deletes are done first).
     * 
     * @return
     * @throws IOException
     */
    protected synchronized IndexReader getReadOnlyIndexReader()
            throws IOException {
        if (this.directory == null) {
            throw new IOException("Index is closed");
        }

        if (this.roReader == null) {
            logger.debug("Instantiating new read-only index reader");

            // No read-only reader has been instantiated yet, create new.
            this.roReader = IndexReader.open(this.directory, true);
        } else {
            if (!this.roReader.isCurrent()) {
                logger
                        .debug("Read-only index reader is outdated, re-opening ..");
                this.roReader = this.roReader.reopen();
            } else {
                logger.debug("Read-only index reader up-to-date");
            }
        }

        return this.roReader;
    }

    @Deprecated
    protected synchronized void releaseReadOnlyIndexReader(
            IndexReader readOnlyReader) throws IOException {

        logger.debug("releaseReadOnlyIndexReader");

        // if (readOnlyReader == null)
        // return;
        //
        // if (!(readOnlyReader instanceof ReadOnlyIndexReader)) {
        // throw new IllegalArgumentException(
        // "Only instances obtained with "
        // + " getReadOnlyIndexReader() should be released with this method");
        // }
        //
        // ((ReadOnlyIndexReader) readOnlyReader).decreaseReferenceCount();
        //
        // if (LOG.isDebugEnabled() && this.roReader != null) {
        // LOG
        // .debug("releaseReadOnlyIndexReader(): current read-only reader ref count is "
        // + this.roReader.getReferenceCount());
        // }

    }

    /**
     * Commits any changes, but does not close directory.
     * 
     * @throws IOException
     */
    protected synchronized void commit() throws IOException {
        if (this.directory == null) {
            throw new IOException("Index is closed");
        }

        // if (!IndexWriter.isLocked(this.directory)) {
        // return; // If there is no lock on index, there are no pending
        // // changes.
        // }

        if (this.reader != null) {
            this.reader.close();
            this.reader = null;
        }

        if (this.writer != null) {
            this.writer.close();
            this.writer = null;
        }
    }

    /**
     * Close down the managed Lucene index.
     * 
     * This will also forcefully close all active read-only readers.
     * 
     **/
    protected synchronized void close() throws IOException {
        if (this.reader != null) {
            this.reader.close();
            this.reader = null;
        }

        if (this.writer != null) {
            this.writer.close();
            this.writer = null;
        }

        if (this.directory != null) {
            this.directory.close();
            this.directory = null;
        }

        if (this.roReader != null) {
            // if (this.roReader.getReferenceCount() > 0) {
            // LOG
            // .warn("Closing current read-only index reader, but reference count is still "
            // + this.roReader.getReferenceCount());
            // }
            this.roReader.close();
            this.roReader = null;
        }

        // Also clean up any outdated read-only readers
        // cleanupDirtyReadOnlyIndexReaders();

        logger.info("Index closed");
    }

    protected synchronized boolean isClosed() {
        return (this.directory == null);
    }

    /**
     * Re-initialize index resources.
     * 
     * @throws IOException
     */
    protected synchronized void reinitialize() throws IOException {
        if (this.reader != null) {
            this.reader.close();
            this.reader = null;
        }

        if (this.writer != null) {
            this.writer.close();
            this.writer = null;
        }

        if (this.directory != null) {
            this.directory.close();
            this.directory = null;
        }

        this.directory = createDirectory();
        initializeIndexDirectory(this.directory, false);

        // Re-initialize read-only reader
        this.roReader = IndexReader.open(this.directory, true);

        logger.info("Re-initialized index at directory '" + this.directory
                + "'");
    }

    /**
     * Clear existing index directory contents, and create a new one. This
     * method will automaticallly re-initialize and re-open index.
     * 
     * Current read-only reader instance is only flagged dirty. This is to
     * prevent on-going searches from being interrupted.
     * 
     * @throws IOException
     */
    protected synchronized void createNewIndex() throws IOException {

        if (this.reader != null) {
            this.reader.close();
            this.reader = null;
        }

        if (this.writer != null) {
            this.writer.close();
            this.writer = null;
        }

        if (this.directory != null) {
            this.directory.close();
            this.directory = null;
        }

        this.directory = createDirectory();
        initializeIndexDirectory(this.directory, true);

        // Re-initialize read-only reader
        this.roReader = IndexReader.open(this.directory, true);

        logger.info("Created new index at directory '" + this.directory + "'");
    }

    /**
     * Force close all outdated read-only index readers in
     * close-on-last-reference state
     */
    // private void cleanupDirtyReadOnlyIndexReaders() {
    // for (Iterator<ReadOnlyIndexReader> i = this.dirtyReadOnlyReaders
    // .iterator(); i.hasNext();) {
    // ReadOnlyIndexReader readOnlyReader = i.next();
    // if (readOnlyReader.getReferenceCount() > 0) {
    // LOG
    // .warn("Forcefully closing old read-only index reader with ref count "
    // + readOnlyReader.getReferenceCount());
    // try {
    // readOnlyReader.close();
    // } catch (IOException io) {
    // LOG
    // .warn("IOException while closing outdated read-only index reader: "
    // + io.getMessage());
    // }
    // }
    //
    // i.remove();
    // }
    // }
    /** Initialize Lucene index */
    private void initializeIndexDirectory(Directory directory, boolean createNew)
            throws IOException {

        if (createNew || !IndexReader.indexExists(directory)) {
            IndexWriter writer = new IndexWriter(directory, this.analyzer,
                    true, MaxFieldLength.LIMITED);
            writer.setUseCompoundFile(true);
            writer.close();
            logger.info("Empty new index created in directory '" + directory
                    + "'");
        } else {
            checkIndexLock(directory);
        }
    }

    /** Check index filesystem-lock, force-unlock if requested. */
    private void checkIndexLock(Directory directory) throws IOException {
        if (IndexWriter.isLocked(directory)) {
            // See if we should try to force-unlock it
            if (this.forceUnlock) {
                logger
                        .warn("Index directory is locked, forcibly releasing lock.");
                IndexWriter.unlock(directory);
            } else {
                throw new IOException("Index directory '" + directory
                        + "' is locked and 'forceUnlock' is set to false.");
            }
        }
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

    public int getMaxMergeDocs() {
        return this.maxMergeDocs;
    }

    public void setMaxMergeDocs(int maxMergeDocs) {
        this.maxMergeDocs = maxMergeDocs;
    }

}

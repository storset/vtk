/* Copyright (c) 2010, University of Oslo, Norway
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.Directory;

/**
 * Pooling of Lucene read-only IndexReader instances with support
 * for aging (that is allowing dirty readers for some time to increase performance)
 * and asynchronous refresh and warmup. It uses round-robin or optionally thread-sticky
 * hand-out of readers if pool size > 1.
 *
 * Refresh of <code>IndexReader</code> instances is done asynchronously in background
 * threads to avoid blocking callers. While refreshing, <code>IndexReader</code>
 * instances can be optionally warmed up by 
 * (warmup details delegated to a configured <code>IndexReaderWarmup</code> instance).
 *
 * The pool is thread safe and does not require external synchronization. It allows
 * asynchronous and parallel opening/refresh of readers while trying to keep
 * readers in sync on index version for best possible consistency, concurrency and
 * response time.
 */
final class ReadOnlyIndexReaderPool {

    private final Log logger = LogFactory.getLog(ReadOnlyIndexReaderPool.class);

    public static final int DEFAULT_MAX_DIRTY_AGE = 0;

    private final PoolItem[] items;
    private final long maxDirtyAge;
    private Directory directory;
    private long poolRefreshTimestamp;
    private final ExecutorService refreshExecutorService;
    private IndexReaderWarmup irw;

    /**
     * Construct a new pool with a maximum allowed dirty-age for the managed
     * <code>IndexReader</code> instances.
     *
     * @param directory   The {@link Directory} which holds the index data.
     *                    The pool is only valid as long as the directory remains open.
     *                    If it's closed, then any pools using it must be discarded.
     * @param size        Maximum number of IndexReader instances to keep. The will
     *                    be distributed in a round-robin fashion to improve concurrency on
     *                    queries upon a single index directory.
     * @param maxDirtyAge Maximum number of seconds to keep an outdated IndexReader open
     *                    before doing refresh. Theoretically, it can go longer than this
     *                    before refresh, but chances of that happening is probably rather slim.
     *                    If zero or negative, then no aging of dirty readers are allowed at all,
     *                    and a refresh will always be done for dirty readers.
     */
    public ReadOnlyIndexReaderPool(Directory directory, int size, int maxDirtyAge) {
        if (size <= 0) {
            throw new IllegalArgumentException("size must be > 0");
        }

        this.directory = directory;
        this.maxDirtyAge = maxDirtyAge*1000;
        this.items = new PoolItem[size];
        for (int i=0; i<this.items.length; i++) {
            this.items[i] = new PoolItem();
        }
        this.poolRefreshTimestamp = System.currentTimeMillis();
        this.refreshExecutorService = Executors.newFixedThreadPool(size);
    }

    /**
     * Construct a new pool with no aging of dirty readers allowed.
     *
     * @param directory   The {@link Directory} which holds the index data.
     * @param size        Maximum number of IndexReader instances to keep.
     */
    public ReadOnlyIndexReaderPool(Directory directory, int size) {
        this(directory, size, DEFAULT_MAX_DIRTY_AGE);
    }

    /**
     * Get a reader from the pool, selected in round-robin fashion.
     *
     * @see #getReader(boolean)
     * @return
     * @throws IOException
     */
    public IndexReader getReader() throws IOException {
        return getReader(false);
    }


    /**
     * Get a reader from the pool. Caller should decrease ref-count of IndexReader
     * when it is no longer in use, or return the IndexReader through method 
     * {@link #returnReader(org.apache.lucene.index.IndexReader)}.
     *
     * What instance will be returned is determined
     * either by which thread is requesting a reader or round-robin rotation.
     *
     * @param selectByThread If <cod>true</code>, then reader returned will be based
     *                 on thread identity. This can be used to make sure
     *                 the same thread always gets the same reader through-out
     *                 its processeing, to improve consistency.
     */
    public IndexReader getReader(boolean selectByThread) throws IOException {

        this.logger.debug("borrow request, selectByThread=" + selectByThread);

        final PoolItem item;
        synchronized (this) {
            if (this.directory == null) throw new IOException("This ReadOnlyIndexReaderPool instance is closed");
            item = getPoolItem(selectByThread);
        }

        synchronized(item) {
            try {
                if (item.reader == null) {
                    this.logger.debug("Reader was null, creating new");
                    item.reader = IndexReader.open(this.directory, true);
                    item.version = item.reader.getVersion();
                } else if (!item.reader.isCurrent()) {
                    this.logger.debug("Reader not current");
                    if (item.refreshHint) {
                        this.logger.debug("Reader refresh hint set, submitting background refresh task.");
                        refreshAsynchronously(item);
                    }
                } else {
                    this.logger.debug("Setting reader refresh hint to false, since reader is current");
                    item.refreshHint = false;
                }
            } catch (IOException io) {
                item.reader = null; // Something went wrong, throw away the reader.
                this.logger.warn("IOException when attempting to open a new reader on directory", io);
                throw io;
            }

            this.logger.debug("Returning reader instance");
            item.reader.incRef();
            return item.reader;
        }
    }

    /**
     * Release a reader. Client code can use this method, if it's not
     * IndexReader ref-counting aware. Client code should never try
     * to directly close IndexReader instances borrowed from this pool.
     *
     * @param reader
     * @throws IOException
     */
    public void returnReader(IndexReader reader) throws IOException {
        reader.decRef();
    }

    /**
     * Send signal to immediately refresh pool, unconditionally. Client code
     * is not obligated to call this, but can do so to ensure freshness of
     * readers right after index has been modified.
     */
    public void refreshPoolNow() throws IOException {
        // NOTE: could have used thread calling this method to drive
        // warm-up of readers, instead of dedicated background threads.
        // The thread that will typically call this method is the scheduled indexupdater thread.
        synchronized(this) {
            if (this.directory == null) return;
        }

        for (int i = 0; i < this.items.length; i++) {
            final PoolItem item = this.items[i];
            synchronized (item) {
                // Obey contract for calling #refreshAsynchronously(PoolItem)
                if (item.reader != null && !item.reader.isCurrent()) {
                    refreshAsynchronously(item);
                }
            }
        }
    }

    /**
     * Close all index readers and shuts down pool. Does not close provided
     * <code>Directory</code>, which should be controlled externally.
     *
     * @throws IOException
     */
    public void close() throws IOException {
        synchronized (this) {
            if (this.directory == null)
                throw new IOException("This ReadOnlyIndexReaderPool instance is already closed or currently closing down.");
            this.directory = null;
        }

        // Shut down refresh executor service and wait for any background refreshes to complete.
        this.logger.debug("Shutting down refresh executor service pool ..");
        this.refreshExecutorService.shutdown();
        try {
            this.logger.debug("Awaiting refresh executor service pool termination..");
            this.refreshExecutorService.awaitTermination(15, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            this.logger.warn("Interrupted while shutting down background refresh thread pool service");
        }

        for (int i = 0; i < this.items.length; i++) {
            PoolItem item = this.items[i];
            synchronized (item) {
                if (item.reader != null) {
                    try {
                        item.reader.decRef();
                        item.reader = null;
                    } catch (IOException io) {
                        item.reader = null;
                        throw io;
                    }
                }
            }
        }
    }

    // Before calling this method, make sure:
    // 1. It should only be called from item-synchronized context
    // 2. It must never be called when item reader is current.
    private void refreshAsynchronously(final PoolItem item) {
        if (item.refreshInProgress) {
            this.logger.debug("Item reader refresh already in progress");
            return; // Refresh already in progress
        }

        if (item.reader == null) {
            throw new IllegalStateException("Can only refresh an existing reader");
        }

        // Note, could check reader.isCurrent here and throw IllegalStateException
        // if it's current, but avoid it, since that call always goes to disk
        // and is already done by all methods that call this method.

        item.refreshInProgress = true;
        this.refreshExecutorService.execute(new ReaderWarmupAndReplaceTask(item));
    }

    // A background refresh task.
    private final class ReaderWarmupAndReplaceTask implements Runnable {

        final PoolItem item;
        final IndexReader oldReader;

        // Constructor should only be called from item-synchronized context
        ReaderWarmupAndReplaceTask(final PoolItem item) {
            this.item = item;
            this.oldReader = item.reader;
        }

        @Override
        public void run() {
            IndexReader reader = null;
            try {
                ReadOnlyIndexReaderPool.this.logger.debug("RWAPT: Re-opening oldReader");
//                reader = this.oldReader.reopen();
                reader = IndexReader.openIfChanged(this.oldReader, true);

                if (reader == null) {
                    // This should never happen. Warm-up and replace task should only be started
                    // when the item's reader is actually dirty, never otherwise. Log a warning
                    // and deploy parachute (return).

                    // [the IndexReader.reopen method can return the same instance back, if
                    // there is no need to do re-open (that's documented and correct).]
                    ReadOnlyIndexReaderPool.this.logger.warn(
                            "RWAPT: Was going to do background-refresh, but reader was already current. Fix code please.");
                    return; // Don't proceed, or we end up closing a current reader.
                }

                if (ReadOnlyIndexReaderPool.this.irw != null) {
                    ReadOnlyIndexReaderPool.this.logger.debug("RWAPT: Executing new reader warmup");
                    ReadOnlyIndexReaderPool.this.irw.warmup(reader);
                } else {
                    ReadOnlyIndexReaderPool.this.logger.warn("No IndexReaderWarmup configured, skipping warmup.");
                }

                ReadOnlyIndexReaderPool.this.logger.debug("RWAPT: Replacing reader NOW");
                synchronized (this.item) {
                    this.item.reader = reader;
                    this.item.version = reader.getVersion();
                }

                this.oldReader.decRef();
            } catch (IOException io) {
                // Here be dragons (unstested code path).
                ReadOnlyIndexReaderPool.this.logger.warn("IOException while refreshing reader", io);
                try {
                    // Try to shut down the old one, if a new reader was actually re-opened
                    if (reader != null) {
                        // Could theoretically throw AlreadyClosedException, but we don't care.
                        ReadOnlyIndexReaderPool.this.logger.warn("Cleanup: Closing down new reader");
                        reader.decRef();
                    } else {
                        ReadOnlyIndexReaderPool.this.logger.warn("Failed to re-open new reader");
                    }
                    ReadOnlyIndexReaderPool.this.logger.warn("Cleanup: Closing down old reader");
                    this.oldReader.decRef();
                } catch (IOException io2) {
                    ReadOnlyIndexReaderPool.this.logger.warn("Cleanup: IOException while closing down reader", io2);
                }
                synchronized (this.item) {
                    this.item.reader = null; // Throw away any current reader to force re-creation.
                }
            } finally {
                // Make sure we flag properly that refresh task is over.
                synchronized (this.item) {
                    this.item.refreshInProgress = false; 
                }
            }
        }
    }

    private static final class PoolItem {
        IndexReader reader;
        volatile long version; // Index version for current item reader. Modified atomically, but read without locking.

        // Status flags
        boolean refreshInProgress = false;
        volatile boolean refreshHint = false; // Item refresh hint. Modified and read concurrently without locking.
    }

    private int counter = 0;
    private PoolItem getPoolItem(boolean selectByThread) {
        if (setPoolRefreshHint()) {
            // Set refresh hint on all pool items
            for (int i = 0; i < this.items.length; i++) {
                this.items[i].refreshHint = true; // Intentionally unsynchronized modification of item refresh hint
            }
            this.poolRefreshTimestamp = System.currentTimeMillis(); // Update pool refresh hint timestamp
        }

        // Select pool item
        int i;
        if (selectByThread) {
            int key = Thread.currentThread().hashCode();
            i = (key & 0x7FFFFFFF) % this.items.length;
        } else {
            i = (this.counter++ & 0x7FFFFFFF) % this.items.length;
        }

        return this.items[i];
    }

    private boolean setPoolRefreshHint() {
        if (this.maxDirtyAge <= 0) {
            this.logger.debug("setPoolRefreshInt(): refresh required because maxDirtyAge <= 0");
            return true;
        }

        // Determine if pool items should get refresh hint set
        // based on if hint was last set more than maxDirtyAge milliseconds ago, or
        // if no dirty age is acceptable.
        // This is done on a global basis to keep pooled IndexReader instances in sync on version.
        // That's important for consistency if there are more than one reader instances in the pool.
        if (System.currentTimeMillis() - this.poolRefreshTimestamp > this.maxDirtyAge) {
            this.logger.debug("setPoolRefreshHint(): refresh required because of pool age");
            return true;
        } else {
            // Inside potential dirty time window. If there are version differences
            // between readers in pool, we set refresh hint on all to sync them up.
            // This helps prevent version mismatches between readers when:
            // 1. One reader has been refreshed early in dirty window
            // 2. Another one is still waiting to get refreshed, and does so later in the window.
            // 3. The index gets modified between events 1 og 2.
            // Preventing version mismatches that stick until next time-based refresh is important
            // because it can often result in bouncing results based on what
            // version of reader is handed out.
            long v = this.items[0].version;
            for (int i=1; i<this.items.length; i++) {
                if (this.items[i].version != v) {
                    this.logger.debug("setPoolRefreshHint(): refresh required because of version mismatches in pool");
                    return true;
                }
            }
        }

        if (logger.isDebugEnabled()) {
            this.logger.debug("setPoolRefreshHint(): no refresh required yet, age = "
                    + (System.currentTimeMillis() - this.poolRefreshTimestamp));
        }

        return false;
    }

    public void setIndexReaderWarmup(IndexReaderWarmup irw) {
        this.irw = irw;
    }

}

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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.Directory;

/**
 * Pooling of Lucene read-only IndexReader instances with support
 * for aging (that is allowing dirty readers for some time to increase performance).
 * It uses round-robin or optionally thread-sticky hand-out of readers if pool size > 1.
 *
 * The pool is thread safe and does not require external synchronization. It allows
 * parallel opening/refresh of readers while trying to keep readers in sync on index version
 * for best possible consistency and concurrency.
 */
final class ReadOnlyIndexReaderPool {

    private final Log logger = LogFactory.getLog(ReadOnlyIndexReaderPool.class);

    public static final int DEFAULT_MAX_DIRTY_AGE = 0;

    private final Directory directory;
    private final PoolItem[] items;
    private final long maxDirtyAge;
    private long poolRefreshTimestamp;

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

        this.logger.debug("borrow request, byThread=" + selectByThread);

        final PoolItem item = getPoolItem(selectByThread);

        // Synchronize only per item to allow parallel opening or refresh of
        // IndexReader instances.
        synchronized(item) {
            try {
                if (item.reader == null) {
                    this.logger.debug("reader was null, creating new");
                    item.reader = IndexReader.open(this.directory, true);
                    item.version = item.reader.getVersion();
                    item.refresh = false;
                } else if (!item.reader.isCurrent()) {
                    this.logger.debug("reader not current");
                    if (item.refresh) {
                        this.logger.debug("reader refresh hint set, re-opening.");
                        // Pool refresh is over age limit or no dirty time acceptable, time to refresh.
                        IndexReader oldReader = item.reader;
                        item.reader = item.reader.reopen();
                        item.version = item.reader.getVersion();
                        item.refresh = false;
                        oldReader.decRef();
                    }
                } else {
                    this.logger.debug("setting reader refresh hint to false, since reader is current");
                    item.refresh = false;
                }
            } catch (IOException io) {
                item.reader = null; // Something went wrong, throw away the reader.
                this.logger.warn("Got an IOException when attempting to open/refresh or close a reader", io);
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
     * to close IndexReader instances gotting from this pool.
     *
     * @param reader
     * @throws IOException
     */
    public void returnReader(IndexReader reader) throws IOException {
        reader.decRef();
    }

    /**
     * Close all index readers in pool.
     *
     * New instances will be automatically re-created on-demand for sub-sequent calls
     * to {@link #getReader(boolean) }, so to avoid spawning new readers, you'll
     * have to make sure not to call {@link #getReader(boolean)} after this method
     * has been called.
     *
     * Should be called before provided <code>Directory</code> is closed.
     *
     * @throws IOException
     */
    public void closeAll() throws IOException {
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

    private static final class PoolItem {
        IndexReader reader;
        volatile boolean refresh = true; // Item refresh hint. Modified and read concurrently without locking.
        volatile long version;           // Index version for current item reader. Modified atomically, but read without locking.
    }

    private int counter = 0;
    private synchronized PoolItem getPoolItem(boolean selectByThread) {
        if (setPoolRefreshHint()) {
            // Set refresh hint on all pool items
            for (int i = 0; i < this.items.length; i++) {
                this.items[i].refresh = true; // Intentionally unsynchronized modification of item refresh hint
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

}

/* Copyright (c) 2004, 2005, 2006, 2007, University of Oslo, Norway
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
package org.vortikal.repository.store;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.vortikal.repository.Path;


/**
 * Manager for locks on cache items (URIs).
 */
public class LockManager {
    private int maxIterations = 40;
    private long iterationWaitTimeout = 2000;

    /* All access to this map must be synchronized on the map object itself */
    private final Map<Path, Lock> locks = new HashMap<Path, Lock>();

    private Log logger = LogFactory.getLog(LockManager.class);

    /**
     * Aquires lock on a single URI. Blocks until lock is
     * obtained, or throws an exception (leaving no lock) if it could not
     * be obtained.
     *
     * @param uri the URI to lock
     * @throws RuntimeException if URI could not be locked.
     */
    public List<Path> lock(Path uri) {
        return lockInternal(new Path[]{uri});
    }

    /**
     * Aquires locks for a list of URIs. Blocks until all locks are
     * obtained, or throws an exception (leaving no locks) if not all
     * the locks could be obtained.
     *
     * @param uris the list of URIs to lock
     * @throws RuntimeException if not all of the requested locks
     * could be obtained
     */
    public List<Path> lock(List<Path> uris) {

        // Do a shallow copy of URI list because we need to sort it
        // (not nice to directly modify input list).
        return lockInternal(uris.toArray(new Path[uris.size()]));
    }
    
    /**
     * Releases locks on a list of URIs. Wakes up threads waiting on
     * the locks.
     *
     * @param uris the URIs to unlock.
     */
    public void unlock(List<Path> uris) {
        for (Path uri: uris) {
            getLock(uri).release();
        }
    }
    
    private List<Path> lockInternal(Path[] uris) {
        
        Arrays.sort(uris); // Always try to lock a set of URIs in the same order to reduce chance of deadlocking.
        
        List<Path> claimedLocks = new ArrayList<Path>(uris.length);
        for (Path uri: uris) {
            Lock lock = null;
            boolean haveLock = false;
            int iterations = 0;

            while (!haveLock) {
                lock = getLock(uri);

                /* The lock will only be valid if this thread enters
                 * the synchronized method claim() as the first
                 * thread. Otherwise the (now invalid) lock object
                 * serves only as a synchronization point, in which
                 * threads can wait their turn, and then try to
                 * acquire a new lock (on the next iteration in this
                 * loop). This process is repeated at most
                 * this.lmaxIterations times.
                 */

                if (this.logger.isDebugEnabled()) {
                    this.logger.debug("claiming " + uri);
                }

                if (lock.claim(this.iterationWaitTimeout)) {
                    if (this.logger.isDebugEnabled()) {
                        this.logger.debug("suceeded: locking " + uri
                                     + ", iterations = " + iterations);
                    }
                    haveLock = true;
                    claimedLocks.add(uri);
                } else {
                    if (this.logger.isDebugEnabled()) {
                        this.logger.debug("failed: locking " + uri
                                     + ", iterations = " + iterations);
                    }
                    if (iterations > this.maxIterations) {
                        if (this.logger.isDebugEnabled()) {
                            this.logger.debug("giving up after " +
                                         this.maxIterations + " iterations");
                        }
                        try {
                            throw new RuntimeException(
                                "Thread " + Thread.currentThread().getName() +
                                " giving up locking " + uri +
                                " after " + iterations + " iterations");
                        } finally {
                            unlock(claimedLocks);
                        }
                    }
                }
                iterations++;
            }
        }

        return Collections.unmodifiableList(claimedLocks);
    }
    



    /**
     * Gets a lock instance. The instance maps one to one to resource
     * URIs. This method itself never blocks for a long time.
     *
     * @param uri the URI for which to get the lock
     * @return the lock object corresponding to the URI
     */
    private Lock getLock(Path uri) {
        Lock lock = null;
        synchronized(this.locks) {
            lock = this.locks.get(uri);
            if (lock == null) {
                lock = new Lock(uri);
                this.locks.put(uri, lock);
            }
        }

        return lock;
    }

    /**
     * Removes a lock from the lock table.
     * This method itself never blocks for a long time.
     * 
     * @param uri a <code>String</code> value
     */
    private void disposeLock(Path uri) {
        synchronized (this.locks) {
            this.locks.remove(uri);
        }
    }

    /**
     * A lock on a single URI in the namespace.
     */
    private class Lock {

        private Path uri;
        private Thread owner = null;

        public Lock(Path uri) {
            this.uri = uri;
        }

        /**
         * Claims a lock. If a thread is the first to enter this
         * method, it will return immediately, and this lock object
         * will be valid for that thread. Otherwise, threads will wait
         * in the wait set for this object, either until a timeout
         * occurs, or another thread calls
         * <code>LockManager.disposeLock()</code> on this lock, which
         * will wake up all threads waiting for it. These threads will
         * then have to try to claim a new lock again, which involves
         * getting a new lock object using
         * <code>LockManager.getLock()</code>.
         * 
         * @return <code>true</code> iff the lock was successfully claimed, 
         *         <code>false</code> otherwise.
         *
         * @param timeout an <code>int</code> value
         */
        public synchronized boolean claim(long timeout) {

            // Support re-entrant locking (same thread already owner == OK, or take ownership)
            if (this.owner == null) {

                // We successfully claimed the lock. This lock is only
                // valid for this thread.
                this.owner = Thread.currentThread();
                
                return true;
            } else {

                // Some other thread already owns this lock, and we
                // have to wait until notify() or timeout. (This lock
                // object is now invalid and cannot be reused).
                try {

                    wait(timeout);

                } catch (InterruptedException e) {
                    LockManager.this.logger.warn(e);
                }
                
                return false;
            }
        }
        
        /**
         * Releases a lock. Only threads that are the owner of a lock
         * may release it. Any threads waiting for this lock will be
         * awakened. These threads have to obtain a new lock object
         * (via <code>LockManager.getLock()</code>, and compete for it
         * by calling <code>Lock.claim()</code>.
         */
        public synchronized void release() {
 
            if (LockManager.this.logger.isDebugEnabled()) {
                LockManager.this.logger.debug("releasing " + this.uri);
            }

            if (this.owner == null) {
                throw new IllegalStateException(
                    "Thread " + Thread.currentThread().getName() +
                    " tried to release a lock on " + this.uri +
                    " without an owner");

            }

            if (Thread.currentThread() != this.owner) {
                throw new IllegalStateException(
                    "Thread " + Thread.currentThread().getName() +
                    " trying to release a lock on " + this.uri +
                    " owned by thread " + this.owner.getName());
            }

            LockManager.this.disposeLock(this.uri);
            notifyAll();
        }

    }
}

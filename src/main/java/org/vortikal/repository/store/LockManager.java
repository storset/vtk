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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.vortikal.repository.Path;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentReaderHashMap;


/**
 * Manager for locks on cache items (URIs).
 */
public class LockManager {
    private int maxIterations = 15;
    private long iterationWaitTimeout = 5000; // 5 seconds

    @SuppressWarnings("unchecked")
    private Map locks = new ConcurrentReaderHashMap();
    private Log logger = LogFactory.getLog(LockManager.class);



    public List<Path> lock(Path[] uris) {
        List<Path> list = java.util.Arrays.asList(uris);
        return lock(list);
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

        Collections.sort(uris);

        List<Path> claimedLocks = new ArrayList<Path>();
        for (Path uri: uris) {
            Lock lock = null;
            boolean haveLock = false;
            int iterations = 0;

            while (!haveLock) {
                lock = getLock(uri);

                if (this.logger.isDebugEnabled()) {
                    this.logger.debug("claiming " + uri);
                }

                lock.claim(this.iterationWaitTimeout);

                /* The lock will only be valid if this thread enters
                 * the synchronized method claim() as the first
                 * thread. Otherwise the (now invalid) lock object
                 * serves only as a synchronization point, in which
                 * threads can wait their turn, and then try to
                 * acquire a new lock (on the next iteration in this
                 * loop). This process is repeated at most
                 * this.lmaxIterations times.
                 */

                if (lock.isValid()) {                    
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
     * Releases locks on a list of URIs. Wakes up threads waiting on
     * the locks.
     *
     * @param uris the URIs to unlock.
     */
    public void unlock(List<Path> uris) {

        for (Path uri: uris) {
            Lock lock = getLock(uri);
            lock.release();
        }
    }


    /**
     * Gets a lock instance. The instance maps one to one to resource
     * URIs.
     *
     * @param uri the URI for which to get the lock
     * @return the lock object corresponding to the URI
     */
    @SuppressWarnings("unchecked")
    private synchronized Lock getLock(Path uri) {

        if (!this.locks.containsKey(uri)) {
            Lock lock = new Lock(uri);
            this.locks.put(uri, lock);
        }
        return (Lock) this.locks.get(uri);
    }

    /**
     * Removes a lock from the lock table.
     * 
     * @param uri a <code>String</code> value
     */
    private synchronized void disposeLock(Path uri) {
        if (this.locks.containsKey(uri)) {
            this.locks.remove(uri);
        }
    }

    /**
     * Simple lock implementation.
     *
     */
    private class Lock {

        private Path uri;        

        private Thread owner = null;

        public Lock(Path uri) {
            this.uri = uri;
        }


        /**
         * Decides if this lock is valid (i.e. if the current thread
         * is the owner of the lock).
         *
         * @return a <code>boolean</code>
         */
        public boolean isValid() {
            return Thread.currentThread() == this.owner;
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

                throw new RuntimeException(
                    "Thread " + Thread.currentThread().getName() +
                    " tried to release a lock on " + this.uri +
                    " without an owner");

            }

            if (Thread.currentThread() != this.owner) {

                throw new RuntimeException(
                    "Thread " + Thread.currentThread().getName() +
                    " trying to release a lock on " + this.uri +
                    " owned by thread " + this.owner.getName());
            }

            disposeLock(this.uri);
            notifyAll();
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
         * @param timeout an <code>int</code> value
         */
        public synchronized void claim(long timeout) {

            if (this.owner == null) {

                // We successfully claimed the lock. This lock is only
                // valid for this thread.
                this.owner = Thread.currentThread();

            } else {

                // Some other thread already owns this lock, and we
                // have to wait until notify() or timeout. (This lock
                // object is now invalid and cannot be reused).
                try {

                    wait(timeout);

                } catch (InterruptedException e) {
                    LockManager.this.logger.warn(e);
                }
            }
        }
    }
}

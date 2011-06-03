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
package org.vortikal.repository;

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
 * 
 * There are two main "synchronization domains" in this code.
 * 1. Synchronization on individual lock objects, over which multiple threads contend for exclusive access to paths.

 * 2. Synchronization on the map holding any locks currently in use and management
 *    of lock disposal. This synchronization is exclusively handled by the code
 *    managing the lock map (getLock(Path), returnLock(Lock) and unlockInternal(Path)).
 *
 */
public class PathLockManager {
    private int maxIterations = 80;
    private long iterationWaitTimeout = 1000;  // 80 * 1000 = 80 seconds of maximum wait time for a single URI lock

    /* All access to this map must be synchronized on the map object itself */
    private final Map<Path, Lock> locks = new HashMap<Path, Lock>();

    private Log logger = LogFactory.getLog(PathLockManager.class);

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
     * the locks could be obtained. Duplicate paths in list are ignored, and
     * if  locking is successful, the returned list of locked paths will not
     * contain any duplicates.
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
     * Releases locks on a list of URIs. Wakes up any threads waiting on
     * the locks.
     *
     * @param uris the URIs to unlock.
     */
    public void unlock(List<Path> uris) {
        for (Path uri: uris) {
            unlockInternal(uri);
        }
    }
    
    private List<Path> lockInternal(Path[] uris) {
        
        // Always try to lock a set of URIs in the same order to reduce chance of deadlocking.
        Arrays.sort(uris);
        
        List<Path> claimedLocks = new ArrayList<Path>(uris.length);
        Path previous = null;
        for (Path uri: uris) {
            if (uri.equals(previous)) {
                // Don't try to claim same lock twice, since the locks
                // are not re-entrant even for owner. Since list is sorted
                // it is enough to compare with previous to detect any duplicates.
                continue;
            }
            
            boolean haveLock = false;
            int iteration = 1;
            Lock lock = getLock(uri);   // Request lock object
            
            while (!haveLock) {
                // Try at most this.maxIterations times to claim the lock
                if (this.logger.isDebugEnabled()) {
                    this.logger.debug("Claiming " + uri);
                }

                if (lock.claim(this.iterationWaitTimeout)) {
                    if (this.logger.isDebugEnabled()) {
                        this.logger.debug("suceeded: locking " + uri
                                     + ", iteration = " + iteration);
                    }
                    haveLock = true;
                    claimedLocks.add(uri);
                    previous = uri;
                } else {
                    if (this.logger.isDebugEnabled()) {
                        this.logger.debug("failed: locking " + uri
                                     + ", iteration = " + iteration);
                    }
                    if (iteration >= this.maxIterations) {
                        if (this.logger.isDebugEnabled()) {
                            this.logger.debug("giving up after " +
                                         this.maxIterations + " iterations");
                        }
                        try {
                            throw new RuntimeException(
                                "Thread " + Thread.currentThread().getName() +
                                " giving up locking " + uri +
                                " after " + iteration + " iterations");
                        } finally {
                            // Clean up, we failed.
                            // Return current lock, so it may be disposed of.
                            returnLock(lock);

                            // Release any locks we managed to claim as well.
                            unlock(claimedLocks);     
                        }
                    }
                }
                
                iteration++;
            }
        }

        return Collections.unmodifiableList(claimedLocks);
    }
    
    private void unlockInternal(Path uri) {
        Lock lock = null;
        synchronized (this.locks) {
            lock = this.locks.get(uri);
            if (lock == null) {
                throw new IllegalStateException("Thread "
                        + Thread.currentThread().getName()
                        + " tried to release lock on path '"
                        + uri + "', but there is currently no registered lock for that path.");
            }
            if (logger.isDebugEnabled()) {
                logger.debug("releasing " + uri);
            }

            returnLock(lock);
        }

        lock.release(); // Allow other threads waiting for this lock to proceed
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
            ++lock.useCount;
        }

        return lock;
    }

    /**
     * Returns a lock object (not the same as releasing the lock) so it may be
     * disposed of if no longer in use.
     * @param lock The lock object to return.
     */
    private void returnLock(Lock lock) {
        synchronized (this.locks) {
            if (--lock.useCount <= 0) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Use count for lock on path '" 
                            + lock.uri 
                            + "' reached "
                            + lock.useCount + ", disposing it.");
                }
                this.locks.remove(lock.uri);
            }
        }
    }
    
    /**
     * A lock on a single URI in the namespace.
     */
    private class Lock {

        private final Path uri;
        private Thread owner = null;
        int useCount = 0;

        Lock(Path uri) {
            this.uri = uri;
        }
        
        /**
         * Claims a lock. If a thread is the first to enter this
         * method, it will return <code>true</code> immediately, and this lock object
         * will be valid for that thread. Otherwise, threads will wait
         * in the wait set for this object, with a timeout.
         * 
         * @return <code>true</code> iff the lock was successfully claimed, 
         *         <code>false</code> otherwise.
         *
         * @param timeout an <code>int</code> value
         */
        synchronized boolean claim(long timeout) {

            if (this.owner == null) {

                // We successfully claimed the lock. This lock is only
                // valid for this thread.
                this.owner = Thread.currentThread();
                
                return true;
            } else {
                // Some other thread already owns this lock, and we
                // have to wait until notify() or timeout, then try to re-claim it.
                try {

                    wait(timeout);

                } catch (InterruptedException e) {
                    logger.warn(e);
                }
                
                return false;
            }
        }
        
        /**
         * Releases a lock. Only threads that are the owner of a lock
         * may release it. Another thread contending/waiting for the lock will be
         * awakened.
         */
        synchronized void release() {
 
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

            // Free for grabs
            this.owner = null;
            // Wake up one waiting thread and let it acquire the lock object.
            notify(); 
        }
        
        @Override
        public String toString() {
            return "Lock [path = " + this.uri 
                    + ", current owner = " + this.owner
                    + ", usecount = " + this.useCount + "]";
        }

    }
}

/* Copyright (c) 2004, University of Oslo, Norway
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
package org.vortikal.repositoryimpl.dao;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentReaderHashMap;


/**
 * Manager for locks on cache items (URIs).
 */
public class LockManager {
    private int maxIterations = 10;
    private long iterationWaitTimeout = 6000; // 6 seconds

    private Map locks = new ConcurrentReaderHashMap();
    private Log logger = LogFactory.getLog(LockManager.class);
    private StringComparator comparator = new StringComparator();

    /**
     * Aquires a lock for a URI. Blocks until lock has been obtained.
     *
     * @param uri the URI to lock
     */
    public void lock(String uri) {
        lock(new String[] { uri });
    }

    /**
     * Aquires locks for a list of URIs. Blocks until all locks are
     * obtained.
     *
     * @param uris the <code>List</code> of URIs (Strings) to lock
     */
    public void lock(List uris) {
        String[] list = new String[uris.size()];

        int index = 0;

        for (Iterator i = uris.iterator(); i.hasNext();) {
            String uri = (String) i.next();

            list[index++] = uri;
        }

        lock(list);
    }

    /**
     * Aquires locks for a list of URIs. Blocks until all locks are
     * obtained.
     *
     * @param uris the list of URIs to lock
     */
    public void lock(String[] uris) {
        Arrays.sort(uris, comparator);

        for (int i = 0; i < uris.length; i++) {
            Lock lock = null;
            boolean haveLock = false;
            int iterations = 0;

            while (!haveLock) {
                iterations++;
                lock = getLock(uris[i]);

                if (logger.isDebugEnabled()) {
                    logger.debug("claiming " + uris[i]);
                }

                lock.claim(this.iterationWaitTimeout);

                /* The lock will only be valid if this thread enters
                 * the synchronized method claim() as the first
                 * thread. Otherwise the (now invalid) lock object
                 * serves only as a synchronization point, in which
                 * threads can wait their turn, and then try to
                 * acquire a new lock (on the next iteration in this
                 * loop). This process is repeaded at most
                 * this.lmaxIterations times.  */

                if (lock.isValid()) {                    
                    if (logger.isDebugEnabled()) {
                        logger.debug("suceeded: locking " + uris[i]
                                     + ", iterations = " + iterations);
                    }
                    haveLock = true;
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug("failed: locking " + uris[i]
                                     + ", iterations = " + iterations);
                    }
                    if (iterations > maxIterations) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("giving up after " +
                                         maxIterations + " iterations");
                        }
                        throw new RuntimeException(
                            "Thread " + Thread.currentThread().getName() +
                            " giving up locking " + uris[i] +
                            " after " + iterations + " iterations");
                    }
                }
            }
        }
    }


    /**
     * Releases the lock on a URI. Wakes up the first thread waiting
     * to obtain the lock. The current thread must be the owner of the
     * lock.
     *
     * @param uri the URI to unlock
     */
    public void unlock(String uri) {
        unlock(new String[] { uri });
    }


    /**
     * Releases locks on a list of URIs. Wakes up threads waiting on
     * the locks.
     *
     * @param uris the URIs to unlock.
     */
    public void unlock(List uris) {
        String[] list = new String[uris.size()];

        int index = 0;

        for (Iterator i = uris.iterator(); i.hasNext();) {
            String uri = (String) i.next();

            list[index++] = uri;
        }

        unlock(list);
    }

    /**
     * Releases locks on a list of URIs. Wakes up threads waiting on
     * the locks.
     *
     * @param uris the URIs to unlock.
     */
    public void unlock(String[] uris) {
        Arrays.sort(uris, comparator);

        for (int i = 0; i < uris.length; i++) {
            Lock lock = getLock(uris[i]);

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
    private synchronized Lock getLock(String uri) {
        Integer hashCode = new Integer(uri.hashCode());

        if (!locks.containsKey(hashCode)) {
            Lock lock = new Lock(uri);

            locks.put(hashCode, lock);
        }

        return (Lock) locks.get(hashCode);
    }

    /**
     * Removes a lock from the lock table.
     * 
     * @param uri a <code>String</code> value
     */
    private synchronized void disposeLock(String uri) {
        Integer hashCode = new Integer(uri.hashCode());

        if (locks.containsKey(hashCode)) {
            locks.remove(hashCode);
        }
    }

    /**
     * Simple mutex implementation.
     *
     */
    private class Lock {

        private String uri;        

        private Thread owner = null;

        public Lock(String uri) {
            this.uri = uri;
        }


        /**
         * Decides if this lock is valid (i.e. if the current thread
         * is the owner of the lock).
         *
         * @return a <code>boolean</code>
         */
        public boolean isValid() {
            return Thread.currentThread() == owner;
        }


        /**
         * Releases a lock. Only threads that are the owner of a lock
         * may release it. Any threads waiting for this lock will be
         * awakened. These threads have to obtain a new lock object
         * (via <code>LockManager.getLock()</code>, and compete for it
         * by calling <code>Lock.claim()</code>.
         */
        public synchronized void release() {
 
            if (logger.isDebugEnabled()) {
                logger.debug("releasing " + uri);
            }

            if (owner == null) {

                throw new RuntimeException(
                    "Thread " + Thread.currentThread().getName() +
                    " tried to release a lock on " + uri +
                    " without an owner");

            }

            if (Thread.currentThread() != owner) {

                throw new RuntimeException(
                    "Thread " + Thread.currentThread().getName() +
                    " trying to release a lock on " + uri +
                    " owned by thread " + owner.getName());
            }

            disposeLock(uri);
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

            if (owner == null) {

                // We successfully claimed the lock. This lock is only
                // valid for this thread.
                owner = Thread.currentThread();

            } else {

                // Some other thread already owns this lock, and we
                // have to wait until notify() or timeout. (This lock
                // object is now invalid and cannot be reused).
                try {

                    wait(timeout);

                } catch (InterruptedException e) {
                    logger.warn(e);
                }
            }
        }
    }

    private class StringComparator implements Comparator {
        public int compare(Object o1, Object o2) {
            if (!((o1 instanceof String) && (o2 instanceof String))) {
                throw new IllegalArgumentException("Need two Strings");
            }

            String a = (String) o1;
            String b = (String) o2;

            return a.compareTo(b);
        }
    }
}

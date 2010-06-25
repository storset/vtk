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

package org.vortikal.util.threads;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * Simple mutex type lock with no thread ownership.
 */
public class Mutex {
    private static final class SyncPrimitive extends AbstractQueuedSynchronizer {
        @Override
        public boolean tryAcquire(int state) {
            return compareAndSetState(0, 1);
        }
        
        @Override
        public boolean tryRelease(int state) {
            setState(0);
            return true;
        }
    }

    private final SyncPrimitive sync = new SyncPrimitive();

    /**
     * Locks the lock. Blocks thread if lock was already locked. Fails to
     * lock if thread is interrupted while blocked in wait state.
     */
    public boolean lock() {
        try {
            this.sync.acquireInterruptibly(1);
            return true;
        } catch (InterruptedException ie) {
            return false;
        }
    }

    /**
     * Tries to lock. Can block for a maximum duration given as timeout, if
     * lock is not available.
     * 
     * @param timeout
     * @param unit
     * @return
     */
    public boolean tryLock(long timeout, TimeUnit unit) {
        try {
            return this.sync.tryAcquireNanos(1, unit.toNanos(timeout));
        } catch (InterruptedException ie) {
            return false;
        }
    }

    /**
     * Unlocks lock, never blocks.
     */
    public void unlock() {
        this.sync.release(0);
    }

}

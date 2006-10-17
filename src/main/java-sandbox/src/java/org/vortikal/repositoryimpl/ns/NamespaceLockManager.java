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
package org.vortikal.repositoryimpl.ns;


/**
 * A manager for synchronizing access to the URI namespace. Namespace
 * access is controlled using read (shared) and write (exclusive)
 * locks.
 *
 * <p>A lock on a path can be thought of conceptually as a list of
 * lock nodes, leading to the leaf node. A read lock on a path implies
 * read locks on every node along that path, while a write lock
 * consists of read locks along the path, except for the leaf node,
 * which is a write lock.
 *
 * <p>For example, the operation <code>readLock(/a/b/c)</code> would
 * lead to this conceptual lock tree:
 *
 * <pre>
 * / (r)
 *    a (r)
 *       b (r)
 *          c (r)
 * </pre>
 *
 * Setting a write lock: <code>writeLock(/a/b/c)</code> would produce
 * this tree:
 * <pre>
 * / (r)
 *    a (r)
 *       b (r)
 *          c (w)
 * </pre>
 *
 * Because read locks are shared, setting a read lock on a path means
 * that other threads can also set read locks along that path, and
 * beyond. On the other hand, when a write lock is set on a path it
 * means that other threads may set read locks along that path, up to
 * the leaf node (which is a write lock). Reaching this node, other
 * threads must wait (block) until the thread holding the write lock
 * releases it. Similarly, a thread attempting to set a write lock may
 * obtain read locks along the path, but if the leaf node contanins a
 * read lock held by another thread, it must also wait until the read
 * lock is released.
 *
 * <p>Notes:
 * <ul>
 * <li>Once a lock has been granted, there is no limit to how long it
 * may be kept.  it is therefore important that client code always
 * releases the locks it obtains.
 * </ul>
 * 
 */
public interface NamespaceLockManager {


    /**
     * Obtains a read lock on a path.
     *
     * @param uri the path to lock. Must start with a <code>/</code>
     * character.
     * @return a lock token, or <code>null</code> if the operation
     * failed for some reason.
     */
    public NamespaceLock readLock(String uri);
    

    public NamespaceLock readNamespaceLock(String uri);
    


    /**
     * Obtains a write lock on a path.
     *
     * @param uri the path to lock. Must start with a <code>/</code>
     * character.
     * @return a lock token, or <code>null</code> if the operation
     * failed for some reason.
     */
    public NamespaceLock writeLock(String uri);



    /**
     * Obtains a write lock on a path.
     *
     * @param uri the path to lock. Must start with a <code>/</code>
     * character.
     * @return a lock token, or <code>null</code> if the operation
     * failed for some reason.
     */
    public NamespaceLock writeNamespaceLock(String uri);


    /**
     * Releases a read lock on a path.
     *
     * @param lock a lock token previously obtained through
     * {@link #readLock} or {@link #readLockDeep}.
     */
    public void readUnlock(NamespaceLock lock);

    public void readNamespaceUnlock(NamespaceLock lock);


    /**
     * Releases a write lock on a path.
     *
     * @param lock a lock token previously obtained through
     * {@link #writeLock}.
     */
    public void writeUnlock(NamespaceLock lock);


    public void writeNamespaceUnlock(NamespaceLock lock);

}

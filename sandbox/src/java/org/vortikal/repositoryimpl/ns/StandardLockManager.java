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

import EDU.oswego.cs.dl.util.concurrent.ReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.ReentrantWriterPreferenceReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.Sync;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.vortikal.util.web.URLUtil;


/**
 * Default implementation of the namespace lock manager. A writer
 * preference scheme is used when obtaining locks, and deadlocks are
 * prevented using timeouts.
 *
 * <p>Configurable JavaBean properties:
 * <ul>
 *   <li><code>timeoutMillis</code> - the number of milliseconds to
 *   use as timeout value when otbaining locks
 * </ul>
 */
public class StandardLockManager implements NamespaceLockManager {

    private Log logger = LogFactory.getLog(this.getClass());

    private long timeoutMillis = 30000;

    private AccessTree accessTree = new AccessTree(StandardLockNode.class);
    
    public void setTimeoutMillis(long timeoutMillis) {
        if (timeoutMillis <= 0) {
            throw new IllegalArgumentException(
                "The timeout value must be positive number");
        }
        this.timeoutMillis = timeoutMillis;
    }
    

    public void setAccessTree(AccessTree accessTree) {
        this.accessTree = accessTree;
    }
    

    public NamespaceLock readLock(String uri) {
        if (uri == null || uri.trim().length() == 0 || !uri.startsWith("/")) {
            throw new IllegalArgumentException("Invalid URI: " + uri);
        }
        String[] path = URLUtil.splitUri(uri);
        AccessNode accessPath = this.accessTree.getAccess(path);
        boolean succeeded = readLockInternal(path);
        if (!succeeded) {
            this.accessTree.releaseAccess(accessPath);
            return null;
        }
        return new NamespaceLockImpl(uri, accessPath);
    }
    

    public void readUnlock(NamespaceLock lock) {
        if (!(lock instanceof NamespaceLockImpl)) {
            throw new IllegalArgumentException(
                "Lock " + lock + " not an instance of "
                + NamespaceLockImpl.class.getName());
        }
        AccessNode accessPath = ((NamespaceLockImpl) lock).getLock();
        AccessNode node = accessPath;
        
        LockNode lockNode = (LockNode) node.getAccessObject();
        lockNode.releaseRead();

        node = node.getParent();

        while (node != null) {
            lockNode = (LockNode) node.getAccessObject();
            lockNode.releaseReadBelow();
            node = node.getParent();
        }
        this.accessTree.releaseAccess(accessPath);
    }
    

    public NamespaceLock readNamespaceLock(String uri) {
        if (uri == null || uri.trim().length() == 0 || !uri.startsWith("/")) {
            throw new IllegalArgumentException("Invalid URI: " + uri);
        }
        String[] path = URLUtil.splitUri(uri);
        AccessNode accessPath = this.accessTree.getAccess(path);
        boolean succeeded = readNamespaceLockInternal(path);
        if (!succeeded) {
            this.accessTree.releaseAccess(accessPath);
            return null;
        }
        return new NamespaceLockImpl(uri, accessPath);
    }
    

    public void readNamespaceUnlock(NamespaceLock lock) {
        if (!(lock instanceof NamespaceLockImpl)) {
            throw new IllegalArgumentException(
                "Lock " + lock + " not an instance of "
                + NamespaceLockImpl.class.getName());
        }
        AccessNode accessPath = ((NamespaceLockImpl) lock).getLock();
        AccessNode node = accessPath;
        LockNode lockNode = (LockNode) node.getAccessObject();
        lockNode.releaseReadNamespace();
        node = node.getParent();

        while (node != null) {
            lockNode = (LockNode) node.getAccessObject();
            lockNode.releaseReadBelow();
            node = node.getParent();
        }
        this.accessTree.releaseAccess(accessPath);
    }
    

    public NamespaceLock writeLock(String uri) {
        if (uri == null || uri.trim().length() == 0 || !uri.startsWith("/")) {
            throw new IllegalArgumentException("Invalid URI: " + uri);
        }
        String[] path = URLUtil.splitUri(uri);
        AccessNode accessPath = this.accessTree.getAccess(path);
        boolean succeeded = writeLockInternal(path);
        if (!succeeded) {
            this.accessTree.releaseAccess(accessPath);
            return null;
        }
        return new NamespaceLockImpl(uri, accessPath);
    }
    

    public void writeUnlock(NamespaceLock lock) {
        if (!(lock instanceof NamespaceLockImpl)) {
            throw new IllegalArgumentException(
                "Lock " + lock + " not an instance of "
                + NamespaceLockImpl.class.getName());
        }

        AccessNode accessPath = ((NamespaceLockImpl) lock).getLock();
        AccessNode node = accessPath;
        LockNode lockNode = (LockNode) node.getAccessObject();
        lockNode.releaseWrite();
        node = node.getParent();
        while (node != null) {
            lockNode = (LockNode) node.getAccessObject();
            lockNode.releaseWriteBelow();
            node = node.getParent();
        }
        this.accessTree.releaseAccess(accessPath);
    }

    public NamespaceLock writeNamespaceLock(String uri) {
        if (uri == null || uri.trim().length() == 0 || !uri.startsWith("/")) {
            throw new IllegalArgumentException("Invalid URI: " + uri);
        }
        String[] path = URLUtil.splitUri(uri);
        AccessNode accessPath = this.accessTree.getAccess(path);
        boolean succeeded = writeNamespaceLockInternal(path);
        if (!succeeded) {
            this.accessTree.releaseAccess(accessPath);
            return null;
        }
        return new NamespaceLockImpl(uri, accessPath);
    }
    



    public void writeNamespaceUnlock(NamespaceLock lock) {
        if (!(lock instanceof NamespaceLockImpl)) {
            throw new IllegalArgumentException(
                "Lock " + lock + " not an instance of "
                + NamespaceLockImpl.class.getName());
        }
        AccessNode accessPath = ((NamespaceLockImpl) lock).getLock();
        AccessNode node = accessPath;
        LockNode lockNode = (LockNode) node.getAccessObject();
        lockNode.releaseWriteNamespace();
        node = node.getParent();
        while (node != null) {
            lockNode = (LockNode) node.getAccessObject();
            lockNode.releaseWriteBelow();
            node = node.getParent();
        }
        this.accessTree.releaseAccess(accessPath);

    }


    public AccessTree getTree() {
        return this.accessTree;
    }
    
    public void reset() {
        this.accessTree = new AccessTree(StandardLockNode.class);
    }
    

    
    private boolean readLockInternal(String[] path) {
        long timeLeft = this.timeoutMillis;
        AccessNode node = this.accessTree.getRootNode();
        List tickets = new ArrayList();

        try {

            for (int i = 1; i <= path.length; i++) {
                
                LockNode lock = (LockNode) node.getAccessObject();
                long before = System.currentTimeMillis();

                if (i < path.length) {
                    if (logger.isTraceEnabled())
                        logger.trace("Waiting for read-below lock on " + node);
                    tickets.add(0, lock.readBelow(timeLeft));
                } else {
                    if (logger.isTraceEnabled())
                        logger.trace("Waiting for read lock on " + node);
                    tickets.add(0, lock.read(timeLeft));
                }
                timeLeft -= (System.currentTimeMillis() - before);

                if (logger.isTraceEnabled()) logger.trace("Obtained lock on " + node);

                if (i < path.length) node = node.getChild(path[i]);
            }
            return true;
        } catch (Exception e) {
            logger.warn("Failed to lock node " + node, e);
            for (Iterator i = tickets.iterator(); i.hasNext();) {
                Ticket ticket = (Ticket) i.next();
                ticket.getLockNode().release(ticket);
            }
            return false;
        }
    }
    

    private boolean readNamespaceLockInternal(String[] path) {
        long timeLeft = this.timeoutMillis;
        AccessNode node = this.accessTree.getRootNode();
        List tickets = new ArrayList();

        try {

            for (int i = 1; i <= path.length; i++) {
                
                LockNode lock = (LockNode) node.getAccessObject();
                long before = System.currentTimeMillis();
                if (i < path.length) {
                    if (logger.isTraceEnabled())
                        logger.trace("Waiting for read-below lock on " + node);
                    tickets.add(0, lock.readBelow(timeLeft));
                } else {
                    if (logger.isTraceEnabled())
                        logger.trace("Waiting for read-namespace lock on " + node);
                    tickets.add(0, lock.readNamespace(timeLeft));
                }

                
                timeLeft -= (System.currentTimeMillis() - before);

                if (logger.isTraceEnabled()) logger.trace("Obtained lock on " + node);

                if (i < path.length) node = node.getChild(path[i]);
            }
            return true;
        } catch (Exception e) {
            logger.warn("Failed to lock node " + node, e);
            for (Iterator i = tickets.iterator(); i.hasNext();) {
                Ticket ticket = (Ticket) i.next();
                ticket.getLockNode().release(ticket);
            }
            return false;
        }
    }
    



    private boolean writeLockInternal(String[] path) {
        long timeLeft = this.timeoutMillis;
        AccessNode node = this.accessTree.getRootNode();
        List tickets = new ArrayList();

        try {

            for (int i = 1; i <= path.length; i++) {
                
                LockNode lock = (LockNode) node.getAccessObject();
                long before = System.currentTimeMillis();

                if (i < path.length) {
                    if (logger.isTraceEnabled())
                        logger.trace("Waiting for write-below lock on " + node);
                    tickets.add(0, lock.writeBelow(timeLeft));
                } else {
                    if (logger.isTraceEnabled())
                        logger.trace("Waiting for write lock on " + node);
                    tickets.add(0, lock.write(timeLeft));
                }

                timeLeft -= (System.currentTimeMillis() - before);

                if (logger.isTraceEnabled()) logger.trace("Obtained lock on " + node);

                if (i < path.length) node = node.getChild(path[i]);
            }
            return true;
        } catch (Exception e) {
            logger.warn("Failed to lock node " + node, e);
            for (Iterator i = tickets.iterator(); i.hasNext();) {
                Ticket ticket = (Ticket) i.next();
                logger.info("Cleaning up: " + ticket);
                ticket.getLockNode().release(ticket);
            }
            return false;
        }
    }
    

    private boolean writeNamespaceLockInternal(String[] path) {
        long timeLeft = this.timeoutMillis;
        AccessNode node = this.accessTree.getRootNode();
        List tickets = new ArrayList();

        try {

            for (int i = 1; i <= path.length; i++) {
                
                LockNode lock = (LockNode) node.getAccessObject();
                long before = System.currentTimeMillis();

                if (i < path.length) {
                    if (logger.isTraceEnabled())
                        logger.trace("Waiting for write-below lock on " + node);
                    tickets.add(0, lock.writeBelow(timeLeft));
                } else {
                    if (logger.isTraceEnabled())
                        logger.trace("Waiting for write-namespace lock on " + node);
                    tickets.add(0, lock.writeNamespace(timeLeft));
                }

                timeLeft -= (System.currentTimeMillis() - before);

                if (logger.isTraceEnabled()) logger.trace("Obtained lock on " + node);

                if (i < path.length) node = node.getChild(path[i]);
            }
            return true;
        } catch (Exception e) {
            logger.warn("Failed to lock node " + node, e);
            for (Iterator i = tickets.iterator(); i.hasNext();) {
                Ticket ticket = (Ticket) i.next();
                ticket.getLockNode().release(ticket);
            }
            return false;
        }

    }
    

    private class NamespaceLockImpl implements NamespaceLock {
        private String uri;
        private AccessNode lock;

        public NamespaceLockImpl(String uri, AccessNode lock) {
            this.uri = uri;
            this.lock = lock;
        }

        public String getURI() {
            return this.uri;
        }
        
        public AccessNode getLock() {
            return this.lock;
        }

        public String toString() {
            return this.uri;
        }
    }
    

}

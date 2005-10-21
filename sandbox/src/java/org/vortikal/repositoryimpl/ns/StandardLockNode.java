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

import java.util.HashMap;
import java.util.Map;


/**
 * LockNode implementation that preserves integrity by disallowing
 * dirty-reads. Reentrant locking is permitted.
 */
public class StandardLockNode implements LockNode {

    private Map readers = new HashMap();
    private Map writers = new HashMap();
    private Map namespaceReaders = new HashMap();
    private Map namespaceWriters = new HashMap();
    private Map readersBelow = new HashMap();
    private Map writersBelow = new HashMap();


    public synchronized Ticket read(long timeout)
        throws InterruptedException, TimeoutException {

        long timeLeft = timeout;
        Thread t = Thread.currentThread();
        boolean proceed = false;

        while (!proceed) {
            
            long before = System.currentTimeMillis();
            Object writersEntry = this.writers.get(t);
            Object namespaceWritersEntry = this.namespaceWriters.get(t);

            if ((this.writers.size() == 0 && this.namespaceWriters.size() == 0)
                || writersEntry != null || namespaceWritersEntry != null ) {
                proceed = true;
            } else {
                wait(timeLeft);
            }
            timeLeft -= (System.currentTimeMillis() - before);
            if (timeLeft <= 0) {
                notify();
                throw new TimeoutException();
            }
        }

        Integer entry = (Integer) this.readers.get(t);
        if (entry == null) {
            entry = new Integer(1);
        } else {
            entry = new Integer(entry.intValue() + 1);
        }
        this.readers.put(t, entry);

        notify();
        return new TicketImpl(this, TicketImpl.READ);
    }


    public synchronized void releaseRead() {
        Thread t = Thread.currentThread();
        Integer entry = (Integer) this.readers.get(t);
        if (entry == null) {
            throw new IllegalStateException(
                "Trying to release read lock not owned by thread: " + t);
        } 
        if (entry.intValue() == 1) {
            this.readers.remove(t);
        } else {
            entry = new Integer(entry.intValue() - 1);
            this.readers.put(t, entry);
        }
        notify();
    }
        

    public synchronized Ticket readBelow(long timeout)
        throws InterruptedException, TimeoutException {

        long timeLeft = timeout;
        Thread t = Thread.currentThread();
        boolean proceed = false;

        while (!proceed) {
            
            long before = System.currentTimeMillis();
            Object writersEntry = this.writers.get(t);
            Object namespaceWritersEntry = this.namespaceWriters.get(t);

            if ((this.writers.size() == 0 && this.namespaceWriters.size() == 0)
                || writersEntry != null || namespaceWritersEntry != null ) {
                proceed = true;
            } else {
                wait(timeLeft);
            }
            timeLeft -= (System.currentTimeMillis() - before);
            if (timeLeft <= 0) {
                notify();
                throw new TimeoutException();
            }
        }

        Integer entry = (Integer) this.readersBelow.get(t);
        if (entry == null) {
            entry = new Integer(1);
        } else {
            entry = new Integer(entry.intValue() + 1);
        }
        this.readersBelow.put(t, entry);

        notify();
        return new TicketImpl(this, TicketImpl.READ_BELOW);
    }


    public synchronized void releaseReadBelow() {
        Thread t = Thread.currentThread();
        Integer entry = (Integer) this.readersBelow.get(t);
        if (entry == null) {
            throw new IllegalStateException(
                "Trying to release read-below lock not owned by thread: " + t);
        } 
        if (entry.intValue() == 1) {
            this.readersBelow.remove(t);
        } else {
            entry = new Integer(entry.intValue() - 1);
            this.readersBelow.put(t, entry);
        }
        notify();
    }
        

    public synchronized Ticket write(long timeout)
        throws InterruptedException, TimeoutException {

        long timeLeft = timeout;
        Thread t = Thread.currentThread();
        boolean proceed = false;

        while (!proceed) {
            
            long before = System.currentTimeMillis();
            Object readersEntry = this.readers.get(t);
            Object namespaceReadersEntry = this.namespaceReaders.get(t);
            Object writersEntry = this.writers.get(t);
            Object namespaceWritersEntry = this.namespaceWriters.get(t);

            if ((this.readers.size() == 0 && this.namespaceReaders.size() == 0
                 && this.writers.size() == 0 && this.namespaceWriters.size() == 0)
                || readersEntry != null || namespaceReadersEntry != null
                || writersEntry != null || namespaceWritersEntry != null) {

                proceed = true;

            } else {

                wait(timeLeft);
            }
            timeLeft -= (System.currentTimeMillis() - before);
            if (timeLeft <= 0) {
                notify();
                throw new TimeoutException();
            }
        }

        Integer entry = (Integer) this.writers.get(t);
        if (entry == null) {
            entry = new Integer(1);
        } else {
            entry = new Integer(entry.intValue() + 1);
        }
        this.writers.put(t, entry);

        notify();
        return new TicketImpl(this, TicketImpl.WRITE);
    }


    public synchronized void releaseWrite() {
        Thread t = Thread.currentThread();
        Integer entry = (Integer) this.writers.get(t);
        if (entry == null) {
            throw new IllegalStateException(
                "Trying to release write lock not owned by thread: " + t);
        } 
        if (entry.intValue() == 1) {
            this.writers.remove(t);
        } else {
            entry = new Integer(entry.intValue() - 1);
            this.writers.put(t, entry);
        }
        notify();
    }


    public synchronized Ticket writeBelow(long timeout)
        throws InterruptedException, TimeoutException {

        long timeLeft = timeout;
        Thread t = Thread.currentThread();
        boolean proceed = false;

        while (!proceed) {
            
            long before = System.currentTimeMillis();
            Object readersEntry = this.readers.get(t);
            Object namespaceReadersEntry = this.namespaceReaders.get(t);
            Object writersEntry = this.writers.get(t);
            Object namespaceWritersEntry = this.namespaceWriters.get(t);

            if ((this.readers.size() == 0 && this.namespaceReaders.size() == 0
                 && this.writers.size() == 0 && this.namespaceWriters.size() == 0)
                || readersEntry != null || namespaceReadersEntry != null
                || writersEntry != null || namespaceWritersEntry != null) {

                proceed = true;

            } else {

                wait(timeLeft);
            }
            timeLeft -= (System.currentTimeMillis() - before);
            if (timeLeft <= 0) {
                notify();
                throw new TimeoutException();
            }
        }

        Integer entry = (Integer) this.writersBelow.get(t);
        if (entry == null) {
            entry = new Integer(1);
        } else {
            entry = new Integer(entry.intValue() + 1);
        }
        this.writersBelow.put(t, entry);

        notify();
        return new TicketImpl(this, TicketImpl.WRITE_BELOW);
    }

        
    public synchronized void releaseWriteBelow() {
        Thread t = Thread.currentThread();
        Integer entry = (Integer) this.writersBelow.get(t);
        if (entry == null) {
            throw new IllegalStateException(
                "Trying to release write-below lock not owned by thread: " + t);
        } 
        if (entry.intValue() == 1) {
            this.writersBelow.remove(t);
        } else {
            entry = new Integer(entry.intValue() - 1);
            this.writersBelow.put(t, entry);
        }
        notify();
    }


    public synchronized Ticket readNamespace(long timeout)
        throws InterruptedException, TimeoutException {

        long timeLeft = timeout;
        Thread t = Thread.currentThread();
        boolean proceed = false;

        while (!proceed) {
            
            long before = System.currentTimeMillis();
            Object writersEntry = this.writers.get(t);
            Object writersBelowEntry = this.writersBelow.get(t);
            Object namespaceWritersEntry = this.namespaceWriters.get(t);

            if ((this.writers.size() == 0 && this.writersBelow.size() == 0
                 && this.namespaceWriters.size() == 0)
                || writersEntry != null || writersBelowEntry != null
                || namespaceWritersEntry != null) {

                proceed = true;

            } else {

                wait(timeLeft);
            }
            timeLeft -= (System.currentTimeMillis() - before);
            if (timeLeft <= 0) {
                notify();
                throw new TimeoutException();
            }
        }

        Integer entry = (Integer) this.namespaceReaders.get(t);
        if (entry == null) {
            entry = new Integer(1);
        } else {
            entry = new Integer(entry.intValue() + 1);
        }
        this.namespaceReaders.put(t, entry);

        notify();
        return new TicketImpl(this, TicketImpl.READ_NS);
    }
    

    public synchronized void releaseReadNamespace() {
        Thread t = Thread.currentThread();
        Integer entry = (Integer) this.namespaceReaders.get(t);
        if (entry == null) {
            throw new IllegalStateException(
                "Trying to release read-namespace lock not owned by thread: " + t);
        } 
        if (entry.intValue() == 1) {
            this.namespaceReaders.remove(t);
        } else {
            entry = new Integer(entry.intValue() - 1);
            this.namespaceReaders.put(t, entry);
        }
        notify();
    }


    public synchronized Ticket writeNamespace(long timeout)
        throws InterruptedException, TimeoutException {

        long timeLeft = timeout;
        Thread t = Thread.currentThread();
        boolean proceed = false;

        while (!proceed) {
            
            long before = System.currentTimeMillis();
            Object readersBelowEntry = this.readersBelow.get(t);
            Object namespaceReadersEntry = this.namespaceReaders.get(t);
            Object writersEntry = this.writers.get(t);
            Object writersBelowEntry = this.writersBelow.get(t);
            Object namespaceWritersEntry = this.namespaceWriters.get(t);

            if ((this.readersBelow.size() == 0 && this.namespaceReaders.size() == 0
                 && this.writers.size() == 0 && this.namespaceWriters.size() == 0
                 && this.writersBelow.size() == 0)
                || readersBelowEntry != null || namespaceReadersEntry != null
                || writersEntry != null || writersBelowEntry != null
                || namespaceWritersEntry != null) {

                proceed = true;

            } else {

                wait(timeLeft);
            }
            timeLeft -= (System.currentTimeMillis() - before);
            if (timeLeft <= 0) {
                notify();
                throw new TimeoutException();
            }
        }

        Integer entry = (Integer) this.namespaceWriters.get(t);
        if (entry == null) {
            entry = new Integer(1);
        } else {
            entry = new Integer(entry.intValue() + 1);
        }
        this.namespaceWriters.put(t, entry);

        notify();
        return new TicketImpl(this, TicketImpl.WRITE_NS);
    }
    

    public synchronized void releaseWriteNamespace() {
        Thread t = Thread.currentThread();
        Integer entry = (Integer) this.namespaceWriters.get(t);
        if (entry == null) {
            throw new IllegalStateException(
                "Trying to release write-namespace lock not owned by thread: " + t);
        } 
        if (entry.intValue() == 1) {
            this.namespaceWriters.remove(t);
        } else {
            entry = new Integer(entry.intValue() - 1);
            this.namespaceWriters.put(t, entry);
        }
        notify();
    }


    public synchronized void release(Ticket t) {
        TicketImpl ticket = (TicketImpl) t;

        switch (ticket.operation) {
            case TicketImpl.READ:
                this.releaseRead();
                break;
            case TicketImpl.WRITE:
                this.releaseWrite();
                break;
            case TicketImpl.READ_NS:
                this.releaseReadNamespace();
                break;
            case TicketImpl.WRITE_NS:
                this.releaseWriteNamespace();
                break;
            case TicketImpl.READ_BELOW:
                this.releaseReadBelow();
                break;
            case TicketImpl.WRITE_BELOW:
                this.releaseWriteBelow();
                break;
            default:
                throw new IllegalArgumentException("Invalid ticket: " + ticket);
        }
        notify();
    }
    
    

    public synchronized String toString() {
        StringBuffer sb = new StringBuffer();
        
        if (this.readers.size() > 0) sb.append(this.readers.size()).append("r");

        if (this.writers.size() > 0) {
            if (sb.length() > 0) sb.append(",");
            sb.append(this.writers.size()).append("w");
        }
        if (this.namespaceReaders.size() > 0) {
            if (sb.length() > 0) sb.append(",");
            sb.append(this.namespaceReaders.size()).append("nsr");
        }
        if (this.namespaceWriters.size() > 0) {
            if (sb.length() > 0) sb.append(",");
            sb.append(this.namespaceWriters.size()).append("nsw");
        }
        if (this.readersBelow.size() > 0) {
            if (sb.length() > 0) sb.append(",");
            sb.append(this.readersBelow.size()).append("rb");
        }
        if (this.writersBelow.size() > 0) {
            if (sb.length() > 0) sb.append(",");
            sb.append(this.writersBelow.size()).append("wb");
        }
        
        return sb.toString();
    }
    
}

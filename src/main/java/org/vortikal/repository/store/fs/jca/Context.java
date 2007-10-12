/* Copyright (c) 2007, University of Oslo, Norway
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
package org.vortikal.repository.store.fs.jca;


import java.io.IOException;
import java.util.Stack;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

public class Context implements XAResource {
        
    private int transactionTimeout = 20000;
    

    private Stack<FileOperation> operations = new Stack<FileOperation>();
    private Stack<FileMapper> mappers = new Stack<FileMapper>();
        
    public FileMapper getCurrentFileMapper() {
        if (this.mappers.isEmpty()) {
            return null;
        }
        return this.mappers.peek();
    }
        
    public void pushOperation(FileOperation operation, FileMapper mapper) {
        this.operations.push(operation);
        this.mappers.push(mapper);
    }

    public void start(Xid xid, int flags) throws XAException {
// System.out.println("__start: Xid: " + xid);
    }
    

    public int prepare(Xid xid) throws XAException {
// System.out.println("__prepare: Xid: " + xid);
        return XAResource.XA_OK;
    }
    

    public void commit(Xid xid, boolean onePhase) throws XAException {
// System.out.println("__commit:  Xid: " + xid);
        System.out.println("__commit");
        while (!this.operations.isEmpty()) {
            FileOperation operation = this.operations.pop();
            try {
                System.out.println("__persisting: " + operation);
                operation.persist();
            } catch (IOException e) {
                throw new XAException("Unable to persist operation: " + operation
                                      + ": " + e.getMessage());
            }
        }
    }
        
    public void end(Xid xid, int flags) throws XAException {
System.out.println("__end:  Xid: " + xid);
        
    }
    
    public Xid[] recover(int flag) throws XAException {
        return new Xid[0];
    }
    

    public boolean isSameRM(XAResource xares) throws XAException {
        boolean same = xares instanceof Context;
//System.out.println("__same_rm:  XAResource: " + xares + ": " + same);

        return same;
    }
    

    public void forget(Xid xid) throws XAException {
System.out.println("__forget: Xid: " + xid);
    }
    


    public void rollback(Xid xid) throws XAException {
// System.out.println("__rollback: Xid: " + xid);
        while (!this.operations.isEmpty()) {
            FileOperation operation = this.operations.pop();
            try {
System.out.println("__rollback: " + operation);
                operation.forget();
            } catch (IOException e) {
                throw new XAException("Unable to roll back operation: "
                                      + operation + ": " + e.getMessage());
            }
        }
    }

    public int getTransactionTimeout() throws XAException {
        return this.transactionTimeout;
    }

    public boolean setTransactionTimeout(int seconds) throws XAException {
        this.transactionTimeout = seconds;
        return true;
    }
    
    
}

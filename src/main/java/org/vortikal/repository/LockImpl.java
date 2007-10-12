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
package org.vortikal.repository;

import java.util.Date;

import org.vortikal.security.Principal;


public class LockImpl implements Lock {

    private static final long serialVersionUID = 3546639889186633783L;
    
    private Principal principal;
    
    private String ownerInfo;
    private String depth;
    private Date timeout;
    private String lockToken;
    
    public LockImpl(String lockToken, Principal principal, String ownerInfo, String depth,
        Date timeout) {
        this.lockToken = lockToken;
        this.principal = principal;

        this.timeout = timeout;
        this.ownerInfo = ownerInfo;
        this.depth = depth;
    }

    public String getOwnerInfo() {
        return this.ownerInfo;
    }

    public String getDepth() {
        return this.depth;
    }

    public String getLockToken() {
        return this.lockToken;
    }

    public Date getTimeout() {
        return this.timeout;
    }

    public Principal getPrincipal() {
        return this.principal;
    }
    
    public void setPrincipal(Principal principal) {
        this.principal = principal;
    }
    
//    public Object clone() throws CloneNotSupportedException {
//        return super.clone();
//    }
//
    public Object clone() {
        LockImpl lock = new LockImpl(this.lockToken, this.principal, this.ownerInfo, this.depth,
            (Date) this.timeout.clone());
        return lock;
    }
    
    public String toString() {
        StringBuffer sb = new StringBuffer(this.getClass().getName());
        sb.append("[");
        sb.append(", depth =").append(this.depth);
        sb.append(", principal = ").append(this.principal);
        sb.append(", ownerInfo = ").append(this.ownerInfo);
        sb.append(", timeout = ").append(this.timeout);
        sb.append(", token = ").append(this.lockToken);
        sb.append("]");
        return sb.toString();
    }

}

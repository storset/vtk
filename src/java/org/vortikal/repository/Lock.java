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


/**
 * This class encapsulates meta information about a lock.
 */
public class Lock implements java.io.Serializable, Cloneable {
    // Supported lock types:
    public final static String LOCKTYPE_EXCLUSIVE_WRITE = "EXCLUSIVE_WRITE";
    private String type;
    private String depth;
    private String user;
    private String ownerInfo;
    private java.util.Date timeout;
    private String token;

    public Lock(String type, String depth, String user, String ownerInfo,
        java.util.Date timeout, String token) {
        this.type = type;
        this.depth = depth;
        this.user = user;
        this.ownerInfo = ownerInfo;
        this.timeout = timeout;
        this.token = token;
    }

    public String getLockType() {
        return type;
    }

    public String getDepth() {
        return depth;
    }

    public String getUser() {
        return user;
    }

    public String getOwnerInfo() {
        return ownerInfo;
    }

    public java.util.Date getTimeout() {
        return timeout;
    }

    public String getLockToken() {
        return token;
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}

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
package org.vortikal.repositoryimpl;

import org.vortikal.security.AuthenticationException;
import org.vortikal.security.Principal;
import org.vortikal.security.PrincipalManager;

import org.doomdark.uuid.UUIDGenerator;

import java.util.Date;


public class Lock implements Cloneable {
    public final static long DEFAULT_TIMEOUT = 30 * 60 * 1000; // 30 minutes
    public final static long MAX_TIMEOUT = DEFAULT_TIMEOUT;
    public final static String LOCKTYPE_EXCLUSIVE_WRITE = "EXCLUSIVE_WRITE";
    private String user = null;
    private String ownerInfo = null;
    private String depth = null;
    private Date timeout = new Date(System.currentTimeMillis() +
            DEFAULT_TIMEOUT);
    private String lockType = LOCKTYPE_EXCLUSIVE_WRITE;
    private String lockToken;

    public Lock(Principal principal, String ownerInfo, String depth,
        Date timeout) throws AuthenticationException {
        if (principal == null) {
            throw new AuthenticationException();
        }

        this.user = principal.getQualifiedName();
        this.ownerInfo = ownerInfo;
        this.depth = depth;

        this.timeout = timeout;

        if (timeout.getTime() > (System.currentTimeMillis() + MAX_TIMEOUT)) {
            this.timeout = new Date(System.currentTimeMillis() +
                    DEFAULT_TIMEOUT);
        }

        lockToken = "opaquelocktoken:" +
            UUIDGenerator.getInstance().generateRandomBasedUUID().toString();
    }

    public Lock(String lockToken, String user, String ownerInfo, String depth,
        Date timeout) {
        this.lockToken = lockToken;
        this.user = user;

        this.timeout = timeout;
        this.ownerInfo = ownerInfo;
        this.depth = depth;
    }

    public String getOwnerInfo() {
        return ownerInfo;
    }

    public String getUser() {
        return user;
    }

    public String getDepth() {
        return depth;
    }

    public String getLockType() {
        return lockType;
    }

    public String getLockToken() {
        return lockToken;
    }

    public Date getTimeout() {
        return timeout;
    }

    public org.vortikal.repository.Lock getLockDTO(PrincipalManager principalManager) {
        return new org.vortikal.repository.Lock(
            getLockType(), getDepth(), principalManager.getPrincipal(getUser()),
            getOwnerInfo(), getTimeout(), getLockToken());
    }

    public void authorize(Principal principal, String privilege,
        RoleManager roleManager)
        throws AuthenticationException, 
            org.vortikal.repository.ResourceLockedException {
        if (!org.vortikal.repository.PrivilegeDefinition.WRITE.equals(
                    privilege)) {
            return;
        }

        if (principal == null) {
            throw new AuthenticationException();
        }

        if (!user.equals(principal.getQualifiedName())) {
            throw new org.vortikal.repository.ResourceLockedException();
        }
    }

    public Object clone() {
        return new Lock(lockToken, user, ownerInfo, depth,
            (Date) timeout.clone());
    }
}

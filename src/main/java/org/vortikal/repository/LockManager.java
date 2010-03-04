/* Copyright (c) 2006, 2007, University of Oslo, Norway
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


import java.io.IOException;
import java.util.Date;
import java.util.UUID;

import org.vortikal.repository.store.DataAccessor;
import org.vortikal.security.AuthenticationException;
import org.vortikal.security.Principal;


public class LockManager {

    public final static long LOCK_DEFAULT_TIMEOUT = 30 * 60 * 1000; // 30 minutes
    public final static long LOCK_MAX_TIMEOUT = LOCK_DEFAULT_TIMEOUT;

    private long lockDefaultTimeout = LOCK_DEFAULT_TIMEOUT;
    private long lockMaxTimeout = LOCK_DEFAULT_TIMEOUT;
    
    private DataAccessor dao;

    public void lockAuthorize(Resource resource, Principal principal,
            boolean deep) throws ResourceLockedException, IOException,
            AuthenticationException {

        lockAuthorize(resource.getLock(), principal);

        if (resource.isCollection() && deep) {
            Path[] uris = this.dao.discoverLocks(resource.getURI());

            for (int i = 0; i < uris.length; i++) {
                Resource ancestor = this.dao.load(uris[i]);
                lockAuthorize(ancestor.getLock(), principal);
            }
        }
    }

    public String lockResource(ResourceImpl resource, Principal principal,
            String ownerInfo, Repository.Depth depth, int desiredTimeoutSeconds,
            boolean refresh) throws AuthenticationException,
            AuthorizationException, ResourceLockedException, IOException {

        if (!refresh) {
            resource.setLock(null);
        }

        if (resource.getLock() == null) {
            String lockToken = "opaquelocktoken:"
                + UUID.randomUUID().toString();

            Date timeout = new Date(System.currentTimeMillis()
                    + this.lockDefaultTimeout);

            if ((desiredTimeoutSeconds * 1000) > this.lockMaxTimeout) {
                timeout = new Date(System.currentTimeMillis()
                        + this.lockDefaultTimeout);
            }

            LockImpl lock = new LockImpl(lockToken, principal, ownerInfo,
                    depth, timeout);
            resource.setLock(lock);
        } else {
            resource.setLock(new LockImpl(resource.getLock().getLockToken(),
                    principal, ownerInfo, depth, new Date(System
                            .currentTimeMillis()
                            + (desiredTimeoutSeconds * 1000))));
        }
        this.dao.store(resource);
        return resource.getLock().getLockToken();
    }

    private void lockAuthorize(Lock lock, Principal principal)
            throws ResourceLockedException, AuthenticationException {

        if (lock == null) {
            return;
        }

        if (principal == null) {
            throw new AuthenticationException();
        }

        if (!lock.getPrincipal().equals(principal)) {
            throw new ResourceLockedException();
        }
    }



    
    public void setLockMaxTimeout(long lockMaxTimeout) {
        this.lockMaxTimeout = lockMaxTimeout;
    }

    public void setLockDefaultTimeout(long lockDefaultTimeout) {
        this.lockDefaultTimeout = lockDefaultTimeout;
    }

    public void setDao(DataAccessor dao) {
        this.dao = dao;
    }

}

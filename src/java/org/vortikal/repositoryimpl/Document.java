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

import org.vortikal.repository.AuthorizationException;
import org.vortikal.repository.IllegalOperationException;
import org.vortikal.repository.PrivilegeDefinition;
import org.vortikal.repository.ResourceLockedException;
import org.vortikal.repositoryimpl.dao.DataAccessor;
import org.vortikal.security.AuthenticationException;
import org.vortikal.security.Principal;
import org.vortikal.security.PrincipalManager;
import org.vortikal.security.PrincipalStore;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.Date;


public class Document extends Resource implements Cloneable {
    protected String contentLanguage = "";

    public Document(String uri, String owner, String contentModifiedBy,
        String propertiesModifiedBy, ACL acl, boolean inheritedACL, Lock lock,
        DataAccessor dao, PrincipalStore principalStore) {
        super(uri, owner, contentModifiedBy, propertiesModifiedBy, acl,
            inheritedACL, lock, dao, principalStore);
    }

    public InputStream getInputStream(Principal principal, String privilege,
        RoleManager roleManager)
        throws AuthenticationException, AuthorizationException, IOException, 
            ResourceLockedException {
        authorize(principal, privilege, roleManager);
        lockAuthorize(principal, privilege, roleManager);

        return dao.getInputStream(this);
    }

    public OutputStream getOutputStream(Principal principal,
        RoleManager roleManager)
        throws AuthenticationException, AuthorizationException, IOException, 
            ResourceLockedException {
        authorize(principal, PrivilegeDefinition.WRITE, roleManager);

        if (lock != null) {
            lockAuthorize(principal, PrivilegeDefinition.WRITE, roleManager);
        }

        setContentLastModified(new Date());
        setContentModifiedBy(principal.getQualifiedName());
        dao.store(this);

        return dao.getOutputStream(this);
    }

    public Object clone() throws CloneNotSupportedException {
        ACL acl = (this.acl == null) ? null : (ACL) this.acl.clone();
        Lock lock = (this.lock == null) ? null : (Lock) this.lock.clone();

        return new Document(uri, owner, contentModifiedBy,
            propertiesModifiedBy, acl, inheritedACL, lock, dao, principalStore);
    }

    /**
     * Gets a resource's content language.
     *
     * @return the language (if it has one, <code>null</code>
     * otherwise)
     */
    public String getContentLanguage() {
        return contentLanguage;
    }

    /**
     * Sets this resource's content language.
     *
     * @param contentLanguage the language to use
     */
    public void setContentLanguage(String contentLanguage) {
        this.contentLanguage = contentLanguage;
    }

    public Resource retrieve(Principal principal, String path,
        RoleManager roleManager)
        throws AuthenticationException, AuthorizationException, IOException {
        if (path.equals(uri)) {
            acl.authorize(principal, PrivilegeDefinition.READ, dao, roleManager);

            return this;
        }

        return null;
    }

    public void store(Principal principal,
        org.vortikal.repository.Resource dto, RoleManager roleManager)
        throws AuthenticationException, AuthorizationException, 
            ResourceLockedException, IllegalOperationException, IOException {
        authorize(principal, PrivilegeDefinition.WRITE, roleManager);

        if (lock != null) {
            lockAuthorize(principal, PrivilegeDefinition.WRITE, roleManager);
        }

        if (!dto.getOverrideLiveProperties()) {
            setPropertiesLastModified(new Date());
        } else {
            setPropertiesLastModified(dto.getPropertiesLastModified());

            if (logger.isDebugEnabled()) {
                logger.debug(
                    "Setting propertiesLastModified to supplied date: " +
                    dto.getPropertiesLastModified());
            }
        }

        if (dto.getOverrideLiveProperties()) {
            setCreationTime(dto.getCreationTime());

            if (logger.isDebugEnabled()) {
                logger.debug("Setting creation time to supplied date: " +
                    dto.getCreationTime());
            }
        }

        setContentType(dto.getContentType());

        setCharacterEncoding(null);

        if ((contentType != null) && contentType.startsWith("text/") &&
                (dto.getCharacterEncoding() != null)) {
            try {
                /* Force checking of encoding */
                new String(new byte[0], dto.getCharacterEncoding());

                setCharacterEncoding(dto.getCharacterEncoding());
            } catch (java.io.UnsupportedEncodingException e) {
                // FIXME: Ignore unsupported character encodings?
            }
        }

        setDisplayName(dto.getDisplayName());

        if (!this.owner.equals(dto.getOwner().getQualifiedName())) {
            /* Attempt to delegate ownership, only the owner of
             * a resource may do it (or root ...) */
            setOwner(principal, dto, dto.getOwner().getQualifiedName(), roleManager);
        }

        setPropertiesModifiedBy(principal.getQualifiedName());
        setProperties(dto.getProperties());

        /* Specific for documents */
        setContentLanguage(dto.getContentLanguage());

        dao.store(this);
    }

    public org.vortikal.repository.Resource getResourceDTO(
        Principal principal, PrincipalManager principalManager,
        RoleManager roleManager) throws IOException {

        org.vortikal.repository.Resource dto =
            super.getResourceDTO(principal, principalManager, roleManager);

        dto.setContentLength(dao.getContentLength(this));
        dto.setContentLanguage(getContentLanguage());

        return dto;
    }
}

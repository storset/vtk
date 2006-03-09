/* Copyright (c) 2006, University of Oslo, Norway
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


import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.doomdark.uuid.UUIDGenerator;
import org.vortikal.repository.AclException;
import org.vortikal.repository.AuthorizationException;
import org.vortikal.repository.IllegalOperationException;
import org.vortikal.repository.PrivilegeDefinition;
import org.vortikal.repository.Property;
import org.vortikal.repository.ResourceLockedException;
import org.vortikal.repository.ResourceNotFoundException;
import org.vortikal.repositoryimpl.dao.DataAccessor;
import org.vortikal.security.AuthenticationException;
import org.vortikal.security.Principal;
import org.vortikal.security.PrincipalManager;
import org.vortikal.security.roles.RoleManager;
import org.vortikal.util.repository.ContentTypeHelper;
import org.vortikal.util.repository.LocaleHelper;
import org.vortikal.util.repository.MimeHelper;
import org.vortikal.util.repository.URIUtil;


public class ResourceManager {

    public final static long LOCK_DEFAULT_TIMEOUT = 30 * 60 * 1000; // 30 minutes
    public final static long LOCK_MAX_TIMEOUT = LOCK_DEFAULT_TIMEOUT;

    private long lockDefaultTimeout = LOCK_DEFAULT_TIMEOUT;
    private long lockMaxTimeout = LOCK_DEFAULT_TIMEOUT;
    
    private PrincipalManager principalManager;
    private PermissionsManager permissionsManager;
    private RoleManager roleManager;
    private DataAccessor dao;

    private void lockAuthorize(Resource resource, Principal principal) 
        throws ResourceLockedException, AuthenticationException {

        if (resource.getLock() == null) {
            return;
        }

        if (principal == null) {
            throw new AuthenticationException();
        }

        if (!resource.getLock().getUser().equals(principal.getQualifiedName())) {
            throw new ResourceLockedException();
        }
    }
    
    public void lockAuthorize(Resource resource, Principal principal, boolean deep) 
        throws ResourceLockedException, IOException, AuthenticationException {

        lockAuthorize(resource, principal);

        if (resource.isCollection() && deep) {
            String[] uris = this.dao.discoverLocks(resource);

            for (int i = 0; i < uris.length;  i++) {
                Resource ancestor = this.dao.load(uris[i]);
                lockAuthorize(ancestor, principal);
            }
        }
    }


    public String lockResource(Resource resource, Principal principal,
                               String ownerInfo, String depth,
                               int desiredTimeoutSeconds, boolean refresh)
        throws AuthenticationException, AuthorizationException, 
            ResourceLockedException, IOException {

        if (!refresh) {
            resource.setLock(null);
        }

        if (resource.getLock() == null) {
            String lockToken = "opaquelocktoken:" +
                UUIDGenerator.getInstance().generateRandomBasedUUID().toString();

            Date timeout = new Date(System.currentTimeMillis() + lockDefaultTimeout);

            if ((desiredTimeoutSeconds * 1000) > lockMaxTimeout) {
                timeout = new Date(System.currentTimeMillis() + lockDefaultTimeout);
            }

            LockImpl lock = new LockImpl(lockToken,principal.getQualifiedName(), ownerInfo, depth, timeout);
            resource.setLock(lock);
        } else {
            resource.setLock(
                new LockImpl(resource.getLock().getLockToken(), principal.getQualifiedName(),
                         ownerInfo, depth, 
                         new Date(System.currentTimeMillis() + (desiredTimeoutSeconds * 1000))));
        }
        this.dao.store(resource);
        return resource.getLock().getLockToken();
    }

    /**
     * Creates a resource.
     */
    public org.vortikal.repository.Resource create(Resource parent, Principal principal,
                           boolean collection, String path)
        throws IllegalOperationException, AuthenticationException, 
            AuthorizationException, AclException, IOException {
        this.permissionsManager.authorize(parent, principal, PrivilegeDefinition.WRITE);

        Resource r = new Resource(
            path,
            principal.getQualifiedName(),
            principal.getQualifiedName(),
            principal.getQualifiedName(),
            new ACL(),
            true,
            collection);

        if (collection) {
            r.setContentType("application/x-vortex-collection");
        } else {
            r.setContentType(MimeHelper.map(r.getName()));
        }

        
        Date now = new Date();
        r.setCreationTime(now);
        r.setContentLastModified(now);
        r.setPropertiesLastModified(now);


        this.dao.store(r);
        r = this.dao.load(path);

        addChildURI(parent, r.getURI());

        // Update timestamps:
        parent.setContentLastModified(now);
        parent.setPropertiesLastModified(now);

        // Update principal info:
        parent.setContentModifiedBy(principal.getQualifiedName());
        parent.setPropertiesModifiedBy(principal.getQualifiedName());

        this.dao.store(parent);

        return getResourceDTO(r, principal);
    }



    /**
     * Adds a URI to the child URI list.
     *
     * @param childURI a <code>String</code> value
     */
    private void addChildURI(Resource parent, String childURI) {
        synchronized (parent) {
            List l = new ArrayList();

            l.addAll(Arrays.asList(parent.getChildURIs()));
            l.add(childURI);

            String[] newChildren = (String[]) l.toArray(new String[l.size()]);

            parent.setChildURIs(newChildren);
        }
    }



    
    
    public void copy(Principal principal, Resource resource, String destUri,
                     boolean preserveACLs, boolean preserveOwner)
        throws IllegalOperationException, AuthenticationException, 
            AuthorizationException, IOException, ResourceLockedException, 
            AclException {
        
        this.dao.copy(resource, destUri, preserveACLs, preserveOwner,
                      principal.getQualifiedName());
    }
    


    public void storeProperties(Resource resource, Principal principal,
                                org.vortikal.repository.Resource dto)
        throws AuthenticationException, AuthorizationException, 
            ResourceLockedException, IllegalOperationException, IOException {

        if (!resource.getOwner().equals(dto.getOwner().getQualifiedName())) {
            /* Attempt to take ownership, only the owner of a parent
             * resource may do that, so do it in a secure manner: */
            setResourceOwner(resource, principal, dto.getOwner());
        }

        if (dto.getOverrideLiveProperties()) {
            resource.setPropertiesLastModified(dto.getPropertiesLastModified());
            resource.setContentLastModified(dto.getContentLastModified());
            resource.setCreationTime(dto.getCreationTime());

        } else {
            resource.setPropertiesLastModified(new Date());
            resource.setPropertiesModifiedBy(principal.getQualifiedName());
        }
        
        if (!resource.isCollection()) {

            resource.setContentType(dto.getContentType());
            resource.setCharacterEncoding(null);

            resource.setContentLocale(null);
            if (dto.getContentLocale() != null)
                resource.setContentLocale(dto.getContentLocale().toString());

            if ((resource.getContentType() != null)
                && ContentTypeHelper.isTextContentType(resource.getContentType()) &&
                (dto.getCharacterEncoding() != null)) {
                try {
                    /* Force checking of encoding */
                    new String(new byte[0], dto.getCharacterEncoding());

                    resource.setCharacterEncoding(dto.getCharacterEncoding());
                } catch (java.io.UnsupportedEncodingException e) {
                    // FIXME: Ignore unsupported character encodings?
                }
            }

        }

        resource.setDisplayName(dto.getDisplayName());
        resource.setProperties(Arrays.asList(dto.getProperties()));
        
        this.dao.store(resource);
    }
    


    private void setResourceOwner(Resource resource, Principal principal, Principal newOwner)
        throws AuthorizationException, IllegalOperationException {
        if (newOwner == null) {
            throw new IllegalOperationException(
                "Unable to delete owner of resource" + resource +
                ": All resources must have an owner.");
        }

        /*
         * Only principals of the ROOT role or owners are allowed to
         * set owner:
         */
        if (!(this.roleManager.hasRole(principal.getQualifiedName(), RoleManager.ROOT) ||
              principal.getQualifiedName().equals(resource.getOwner()))) {
            throw new AuthorizationException(
                "Principal " + principal.getQualifiedName()
                + " is not allowed to set owner of "
                + "resource " + resource.getURI());
        }

        if (!principalManager.validatePrincipal(newOwner)) {
            throw new IllegalOperationException(
                "Unable to set owner of resource " + resource.getURI()
                + ": invalid owner: '" + newOwner + "'");
        }

        resource.setOwner(newOwner.getQualifiedName());
    }


    public void storeACL(Resource resource, Principal principal,
                         org.vortikal.repository.Ace[] aceList)
        throws AuthorizationException, AuthenticationException, 
            IllegalOperationException, IOException, AclException {

        ACL acl = permissionsManager.buildACL(aceList);
        resource.setACL(acl);

        /* If the first ACE has set inheritance, we know that the
         * whole ACL has valid inheritance (ACL.validateACL() ensures
         * this), so we can go ahead and set it here: */
        boolean inheritedACL = aceList[0].getInheritedFrom() != null;

        if (!"/".equals(resource.getURI()) && inheritedACL) {
            /* When the ACL is inherited, make our ACL a copy of our
             * parent's ACL, since the supplied one may contain other
             * ACEs than the one we now inherit from. */
            try {
                ACL parentACL = (ACL) this.dao.load(URIUtil.getParentURI(resource.getURI())).getACL().clone();

                resource.setACL(parentACL);
            } catch (CloneNotSupportedException e) {
            }
        }

        try {
            resource.setInheritedACL(inheritedACL);
            resource.setDirtyACL(true);

            this.dao.store(resource);
        } catch (Exception e) {
            
            throw new IOException(e.getMessage());
        } finally {
            resource.setDirtyACL(false);
        }
    }

    private org.vortikal.repository.Lock getActiveLock(LockImpl lock) {
        if (lock != null) {
            // Is cloning neccessary and/or working?
            LockImpl clone = (LockImpl)lock.clone();
            clone.setPrincipal(principalManager.getPrincipal(clone.getUser()));
            return clone;
        }
        
        return null;
    }


    
    public org.vortikal.repository.Resource getResourceDTO(
            Resource resource, Principal principal) throws IOException {

        org.vortikal.repository.Resource dto = new org.vortikal.repository.Resource();

        dto.setURI(resource.getURI());
        dto.setCreationTime(resource.getCreationTime());
        dto.setContentLastModified(resource.getContentLastModified());
        dto.setPropertiesLastModified(resource.getPropertiesLastModified());
        dto.setContentType(resource.getContentType());
        dto.setCharacterEncoding(resource.getCharacterEncoding());
        dto.setDisplayName(resource.getDisplayName());
        dto.setName(resource.getName());

        dto.setActiveLock(getActiveLock(resource.getLock()));

        dto.setOwner(principalManager.getPrincipal(resource.owner));
        dto.setContentModifiedBy(principalManager.getPrincipal(resource
                .getContentModifiedBy()));
        dto.setPropertiesModifiedBy(principalManager.getPrincipal(resource
                .getPropertiesModifiedBy()));

        dto.setProperties((Property[])resource.getProperties().toArray(new Property[resource.getProperties().size()]));

        String inheritedFrom = null;
        if (resource.isInheritedACL()) {
            inheritedFrom = URIUtil.getParentURI(resource.getURI());
        }
        dto.setACL(permissionsManager.convertToACEArray(resource.getACL(), inheritedFrom));
        
        // Adding parent props: 
        String parentURI = URIUtil.getParentURI(resource.getURI());
        if (parentURI != null) {
            inheritedFrom = null;
            Resource parent = this.dao.load(parentURI);
            // FIXME: if loading the parent fails, the resource itself
            // has been removed
            if (parent == null) {
                throw new ResourceNotFoundException(
                    "Resource " + resource.getURI() + " has no parent (possibly removed)");
            }

            if (resource.isInheritedACL()) {
                inheritedFrom = URIUtil.getParentURI(parent.getURI());
            }
            dto.setParentOwner(principalManager.getPrincipal(parent.getOwner()));
            dto.setParentACL(permissionsManager.convertToACEArray(parent.getACL(),inheritedFrom));
        }
        
        if (resource.isCollection()) {
            dto.setChildURIs(resource.getChildURIs());
        } else {
            dto.setContentLength(dao.getContentLength(resource));
            dto.setContentLocale(LocaleHelper.getLocale(resource.getContentLocale()));
        }

        return dto;
    }

    public void setDao(DataAccessor dao) {
        this.dao = dao;
    }

    public void setPrincipalManager(PrincipalManager principalManager) {
        this.principalManager = principalManager;
    }

    public void setRoleManager(RoleManager roleManager) {
        this.roleManager = roleManager;
    }

    public void setPermissionsManager(PermissionsManager permissionsManager) {
        this.permissionsManager = permissionsManager;
    }

    public void setLockMaxTimeout(long lockMaxTimeout) {
        this.lockMaxTimeout = lockMaxTimeout;
    }

    public void setLockDefaultTimeout(long lockDefaultTimeout) {
        this.lockDefaultTimeout = lockDefaultTimeout;
    }

}

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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.doomdark.uuid.UUIDGenerator;
import org.vortikal.repository.AclException;
import org.vortikal.repository.AuthorizationException;
import org.vortikal.repository.IllegalOperationException;
import org.vortikal.repository.Property;
import org.vortikal.repository.Resource;
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

    private PropertyManagerImpl propertyManager;
    
    private void lockAuthorize(ResourceImpl resource, Principal principal) 
        throws ResourceLockedException, AuthenticationException {

        if (resource.getLock() == null) {
            return;
        }

        if (principal == null) {
            throw new AuthenticationException();
        }

        if (!resource.getLock().getPrincipal().equals(principal)) {
            throw new ResourceLockedException();
        }
    }
    
    public void lockAuthorize(ResourceImpl resource, Principal principal, boolean deep) 
        throws ResourceLockedException, IOException, AuthenticationException {

        lockAuthorize(resource, principal);

        if (resource.isCollection() && deep) {
            String[] uris = this.dao.discoverLocks(resource);

            for (int i = 0; i < uris.length;  i++) {
                ResourceImpl ancestor = this.dao.load(uris[i]);
                lockAuthorize(ancestor, principal);
            }
        }
    }


    public String lockResource(ResourceImpl resource, Principal principal,
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

            LockImpl lock = new LockImpl(lockToken,principal, ownerInfo, depth, timeout);
            resource.setLock(lock);
        } else {
            resource.setLock(
                new LockImpl(resource.getLock().getLockToken(), principal,
                         ownerInfo, depth, 
                         new Date(System.currentTimeMillis() + (desiredTimeoutSeconds * 1000))));
        }
        this.dao.store(resource);
        return resource.getLock().getLockToken();
    }

    /**
     * Creates a resource.
     */
    public org.vortikal.repository.Resource create(ResourceImpl parent, Principal principal,
                           boolean collection, String path)
        throws IllegalOperationException, AuthenticationException, 
            AuthorizationException, AclException, IOException {

        ResourceImpl resource = null;
        
        try {
            propertyManager.create(principal, path, collection);
        } catch (Exception e) {
            // TODO: handle exception
        }
        
        this.dao.store(resource);
        resource = this.dao.load(path);

        addChildURI(parent, resource.getURI());
        propertyManager.collectionContentModified(parent, principal);
        
        this.dao.store(parent);

        return getResourceClone(resource);
    }



    /**
     * Adds a URI to the child URI list.
     *
     * @param childURI a <code>String</code> value
     */
    private void addChildURI(ResourceImpl parent, String childURI) {
        synchronized (parent) {
            List l = new ArrayList();

            l.addAll(Arrays.asList(parent.getChildURIs()));
            l.add(childURI);

            String[] newChildren = (String[]) l.toArray(new String[l.size()]);

            parent.setChildURIs(newChildren);
        }
    }



    
    
    public void copy(Principal principal, ResourceImpl resource, String destUri,
                     boolean preserveACLs, boolean preserveOwner)
        throws IllegalOperationException, AuthenticationException, 
            AuthorizationException, IOException, ResourceLockedException, 
            AclException {
        
        this.dao.copy(resource, destUri, preserveACLs, preserveOwner,
                      principal.getQualifiedName());
    }
    


    public void storeProperties(ResourceImpl resource, Principal principal,
                                org.vortikal.repository.Resource dto)
        throws AuthenticationException, AuthorizationException, 
            ResourceLockedException, IllegalOperationException, IOException {
        this.propertyManager.storeProperties(resource, principal, dto);
        this.dao.store(resource);
    }
    
    public void collectionContentModification(ResourceImpl resource, Principal principal) throws IOException {
        this.propertyManager.collectionContentModified(resource, principal);
        this.dao.store(resource);
    }

    public void resourceContentModification(ResourceImpl resource, 
            Principal principal, InputStream inputStream) throws IOException {

        this.propertyManager.resourceContentModification(resource, principal, inputStream);

        this.dao.store(resource);
    }
    
    public void storeACL(ResourceImpl resource, Principal principal,
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
    
    public Resource getResourceClone(
            ResourceImpl resource) throws IOException {

        ResourceImpl dto = (ResourceImpl)resource.clone();
        
        dto.setLock((LockImpl)resource.getLock().clone());

        String inheritedFrom = null;
        if (resource.isInheritedACL()) {
            inheritedFrom = URIUtil.getParentURI(resource.getURI());
        }
        dto.setACL(permissionsManager.convertToACEArray(resource.getACL(), inheritedFrom));
        
        // Adding parent props: 
        String parentURI = URIUtil.getParentURI(resource.getURI());
        if (parentURI != null) {
            inheritedFrom = null;
            ResourceImpl parent = this.dao.load(parentURI);
            // FIXME: if loading the parent fails, the resource itself
            // has been removed
            if (parent == null) {
                throw new ResourceNotFoundException(
                    "Resource " + resource.getURI() + " has no parent (possibly removed)");
            }

            if (resource.isInheritedACL()) {
                inheritedFrom = URIUtil.getParentURI(parent.getURI());
            }
            dto.setParentOwner(parent.getOwner());
            dto.setParentACL(permissionsManager.convertToACEArray(parent.getACL(),inheritedFrom));
        }
        
        if (resource.isCollection()) {
            dto.setChildURIs(resource.getChildURIs());
        } else {
            dto.setContentLength(dao.getContentLength(resource));
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

    /**
     * @param propertyManager The propertyManager to set.
     */
    public void setPropertyManager(PropertyManagerImpl propertyManager) {
        this.propertyManager = propertyManager;
    }

}

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

import java.io.IOException;
import java.io.InputStream;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.vortikal.repository.Acl;
import org.vortikal.repository.AuthorizationException;
import org.vortikal.repository.FailedDependencyException;
import org.vortikal.repository.IllegalOperationException;
import org.vortikal.repository.PrivilegeDefinition;
import org.vortikal.repository.ReadOnlyException;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.ResourceLockedException;
import org.vortikal.repository.ResourceNotFoundException;
import org.vortikal.repository.ResourceOverwriteException;
import org.vortikal.repository.event.ACLModificationEvent;
import org.vortikal.repository.event.ContentModificationEvent;
import org.vortikal.repository.event.ResourceCreationEvent;
import org.vortikal.repository.event.ResourceDeletionEvent;
import org.vortikal.repository.event.ResourceModificationEvent;
import org.vortikal.repositoryimpl.dao.DataAccessor;
import org.vortikal.security.AuthenticationException;
import org.vortikal.security.Principal;
import org.vortikal.security.TokenManager;
import org.vortikal.security.roles.RoleManager;
import org.vortikal.util.repository.URIUtil;



/**
 * A (still non-transactional) implementation of the
 * <code>org.vortikal.repository.Repository</code> interface.
 * 
 * XXX: Cloning and equals must be checked for all domain objects!
 * XXX: implement locking of depth 'infinity'
 * XXX: namespace locking/concurrency
 * XXX: Evaluate exception practice, handling and propagation
 * XXX: transcation demarcation
 * XXX: split dao into multiple daos
 * XXX: externalize caching
 * XXX: duplication of owner and inherited between resource and acl.
 */
public class RepositoryImpl implements Repository, ApplicationContextAware,
        InitializingBean {

    private ApplicationContext context;

    private DataAccessor dao;
    private RoleManager roleManager;
    private TokenManager tokenManager;
    private ResourceManager resourceManager;
    private PropertyManagerImpl propertyManager;
    private PermissionsManager permissionsManager;
    private AclValidator aclValidator;
    private URIValidator uriValidator = new URIValidator();
    
    private String id;
    private boolean readOnly = false;

    public boolean isReadOnly() {
        return readOnly;
    }
    
    public void setReadOnly(String token, boolean readOnly) {

        Principal principal = tokenManager.getPrincipal(token);

        if (!roleManager.hasRole(principal.getQualifiedName(), RoleManager.ROOT)) {
            throw new AuthorizationException();
        }

        this.readOnly = readOnly;
    }

    public boolean exists(String token, String uri) throws IOException {
        if (!uriValidator.validateURI(uri)) return false;

        ResourceImpl r = dao.load(uri);

        if (r == null) return false;

        return true;
    }

    public org.vortikal.repository.Resource retrieve(String token, String uri,
        boolean forProcessing)
        throws ResourceNotFoundException, AuthorizationException, 
            AuthenticationException, IOException {
        Principal principal = tokenManager.getPrincipal(token);

        if (!uriValidator.validateURI(uri)) throw new ResourceNotFoundException(uri);

        ResourceImpl r = dao.load(uri);

        if (r == null) throw new ResourceNotFoundException(uri);

        try {
            /* authorize for the right privilege */
            String privilege = (forProcessing)
                ? PrivilegeDefinition.READ_PROCESSED
                : PrivilegeDefinition.READ;

            this.permissionsManager.authorize(r, principal, privilege);
            ResourceImpl clone = (ResourceImpl)r.clone();
            
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new IOException("An internal error occurred: unable to " +
                "clone() resource: " + r);
        }
    }

    public org.vortikal.repository.Resource[] listChildren(String token,
            String uri, boolean forProcessing)
            throws ResourceNotFoundException, AuthorizationException,
            AuthenticationException, IOException {
        Principal principal = tokenManager.getPrincipal(token);

        if (!uriValidator.validateURI(uri)) throw new ResourceNotFoundException(uri);

        ResourceImpl r = dao.load(uri);

        if (r == null) {
            throw new ResourceNotFoundException(uri);
        }

        // FIXME: magic return value (throw IllegalOperationException ?)
        //        Need to watch out for client code that depends on this behaviour ..
        if (!r.isCollection()) {
            return null;
        }

        /* authorize for the right privilege: */
        String privilege = (forProcessing) ? PrivilegeDefinition.READ_PROCESSED
                : PrivilegeDefinition.READ;

        this.permissionsManager.authorize(r, principal, privilege);

        ResourceImpl[] list = this.dao.loadChildren(r);
        Resource[] children = new Resource[list.length];

        for (int i = 0; i < list.length; i++) {
            try {
                children[i] = (ResourceImpl)list[i].clone();

            } catch (CloneNotSupportedException e) {
                throw new IOException("An internal error occurred: unable to " +
                    "clone() resource: " + list[i]);
            }

        }

        return children;
    }

    public Resource createDocument(String token,
            String uri) throws IllegalOperationException,
            AuthorizationException, AuthenticationException,
            ResourceLockedException, ReadOnlyException, IOException {
        return create(token, uri, false);
    }

    public Resource createCollection(String token, String uri) 
            throws IllegalOperationException, 
            AuthorizationException, AuthenticationException, 
            ResourceLockedException, ReadOnlyException, IOException {

        return create(token, uri, true);
    }

    private Resource create(String token, String uri, boolean collection)
    throws AuthorizationException, AuthenticationException, 
    IllegalOperationException, ResourceLockedException, 
    ReadOnlyException, IOException {

        Principal principal = tokenManager.getPrincipal(token);

        if (readOnly(principal)) throw new ReadOnlyException();

        if (!uriValidator.validateURI(uri)) throw new ResourceNotFoundException(uri);

        ResourceImpl resource = dao.load(uri);

        if (resource != null) { 
            throw new IllegalOperationException("Resource already exists");
        }

        String parentURI = URIUtil.getParentURI(uri);

        ResourceImpl parent = dao.load(parentURI);

        if ((parent == null) || !parent.isCollection()) {
            throw new IllegalOperationException("Either parent doesn't exist " +
                    "or parent is document");
        }

        ResourceImpl newResource = null;

        try {
            this.permissionsManager.authorize(parent, principal, 
                    PrivilegeDefinition.WRITE);
            this.resourceManager.lockAuthorize(parent, principal, false);
//            newResource = 
//                this.resourceManager.create(parent, principal, collection, uri);
            newResource = propertyManager.create(principal, uri, collection);
            newResource.setACL((Acl)parent.getAcl().clone());
            newResource.setInheritedACL(true);
            this.dao.store(newResource);
            newResource = this.dao.load(uri);

            parent.addChildURI(uri);
            propertyManager.collectionContentModification(parent, principal);
            
            this.dao.store(parent);

            newResource = (ResourceImpl)newResource.clone();
        } catch (CloneNotSupportedException e) {
            throw new IOException("An internal error occurred: unable to " +
                "clone() resource: " + uri);
        }

        context.publishEvent(new ResourceCreationEvent(this, newResource));

        return newResource;
    }    

    public void copy(String token, String srcUri, String destUri, String depth,
        boolean overwrite, boolean preserveACL)
        throws IllegalOperationException, AuthorizationException, 
            AuthenticationException, FailedDependencyException, 
            ResourceOverwriteException, ResourceLockedException, 
            ResourceNotFoundException, ReadOnlyException, IOException {
        Principal principal = tokenManager.getPrincipal(token);

        if (readOnly(principal)) {
            throw new ReadOnlyException();
        }

        if (!uriValidator.validateURI(srcUri)) {
            throw new ResourceNotFoundException(srcUri);
        }

        if (!uriValidator.validateURI(destUri)) {
            throw new IllegalOperationException("Invalid URI: '" + destUri + "'");
        }
        
        uriValidator.validateCopyURIs(srcUri, destUri);
        
        ResourceImpl src = dao.load(srcUri);

        if (src == null) {
            throw new ResourceNotFoundException(srcUri);
        } else if (src.isCollection()) {
            authorizeRecursively(src, principal, PrivilegeDefinition.READ);
        } else {
            this.permissionsManager.authorize(src, principal, PrivilegeDefinition.READ);
        }

        ResourceImpl dest = dao.load(destUri);

        if (dest != null) {
            if (!overwrite)
                throw new ResourceOverwriteException();
            
            this.resourceManager.lockAuthorize(dest, principal, true);
        }

        String destParentUri = URIUtil.getParentURI(destUri);

        ResourceImpl destParent = dao.load(destParentUri);

        if ((destParent == null) || !destParent.isCollection()) {
            throw new IllegalOperationException("destination is either a document or does not exist");
        }

        this.permissionsManager.authorize(destParent, principal,
                                       PrivilegeDefinition.WRITE);

        this.resourceManager.lockAuthorize(destParent, principal, false);

        try {
            if (dest != null) {
                this.dao.delete(dest);
            }
            this.dao.copy(src, destUri, preserveACL, false, principal.getQualifiedName());

            dest = (ResourceImpl)dao.load(destUri).clone();

            ResourceCreationEvent event = new ResourceCreationEvent(this, dest);

            context.publishEvent(event);
        } catch (AclException e) {
            throw new IllegalOperationException(e.getMessage());
        } catch (CloneNotSupportedException e) {
            throw new IOException("An internal error occurred: unable to " +
                "clone() resource: " + destUri);
        }
    }

    public void move(String token, String srcUri, String destUri,
        boolean overwrite)
        throws IllegalOperationException, AuthorizationException, 
            AuthenticationException, FailedDependencyException, 
            ResourceOverwriteException, ResourceLockedException, 
            ResourceNotFoundException, ReadOnlyException, IOException {
        Principal principal = tokenManager.getPrincipal(token);

        if (readOnly(principal)) {
            throw new ReadOnlyException();
        }

        if (!uriValidator.validateURI(srcUri)) {
            throw new ResourceNotFoundException(srcUri);
        }

        if (!uriValidator.validateURI(destUri)) {
            throw new IllegalOperationException("Invalid URI: '" + destUri +
                "'");
        }

        uriValidator.validateCopyURIs(srcUri, destUri);
        
        // Loading and checking source resource
        ResourceImpl src = dao.load(srcUri);

        if (src == null) {
            throw new ResourceNotFoundException(srcUri);
        }

        this.resourceManager.lockAuthorize(src, principal, true);

        if (src.isCollection()) {
            authorizeRecursively(src, principal, PrivilegeDefinition.READ);
        } else {
            this.permissionsManager.authorize(src, principal, PrivilegeDefinition.READ);
        }

        // Loading and checking srcParent
        String srcParentURI = URIUtil.getParentURI(srcUri);

        ResourceImpl srcParent = dao.load(srcParentURI);

        this.permissionsManager.authorize(srcParent, principal, PrivilegeDefinition.WRITE);
        this.resourceManager.lockAuthorize(srcParent, principal, false);

        // Checking dest
        ResourceImpl dest = dao.load(destUri);

        if (dest != null) {
            if (!overwrite) {
                throw new ResourceOverwriteException();
            }
            
            this.resourceManager.lockAuthorize(dest, principal, true);
        }

        // checking destParent 
        String destParentUri = URIUtil.getParentURI(destUri);

        ResourceImpl destParent = dao.load(destParentUri);

        if ((destParent == null) || !destParent.isCollection()) {
            throw new IllegalOperationException("Invalid destination resource");
        }

        this.permissionsManager.authorize(destParent, principal, PrivilegeDefinition.WRITE);
        
        // Performing move operation
        try {
            if (dest != null) {
                this.dao.delete(dest);
                context.publishEvent(new ResourceDeletionEvent(this, dest.getURI(), dest.getID(), 
                        dest.isCollection()));
            }
            
            this.dao.copy(src, destUri, true, true, principal.getQualifiedName());
            this.dao.delete(src);
            context.publishEvent(new ResourceDeletionEvent(this, srcUri,
                    src.getID(), src.isCollection()));

            dest = (ResourceImpl) dao.load(destUri).clone();

            context.publishEvent(new ResourceCreationEvent(this, dest));
        } catch (AclException e) {
            throw new IllegalOperationException(e.getMessage());
        } catch (CloneNotSupportedException e) {
            throw new IOException("An internal error occurred: unable to " +
                "clone() resource: " + destUri);
        }
    }

    public void delete(String token, String uri)
        throws IllegalOperationException, AuthorizationException, 
            AuthenticationException, ResourceNotFoundException, 
            ResourceLockedException, FailedDependencyException, 
            ReadOnlyException, IOException {
        Principal principal = tokenManager.getPrincipal(token);

        if (!uriValidator.validateURI(uri)) {
            throw new ResourceNotFoundException(uri);
        }

        if ("/".equals(uri)) {
            throw new IllegalOperationException(
                    "Cannot delete the root resource ('/')");
        }

        ResourceImpl r = dao.load(uri);

        if (r == null) {
            throw new ResourceNotFoundException(uri);
        }

        this.resourceManager.lockAuthorize(r, principal, true);

        String parent = URIUtil.getParentURI(uri);

        ResourceImpl parentCollection = dao.load(parent);

        this.permissionsManager.authorize(parentCollection, principal, 
                                          PrivilegeDefinition.WRITE);
        this.resourceManager.lockAuthorize(parentCollection, principal, false);

        if (readOnly(principal)) {
            throw new ReadOnlyException();
        }

        this.dao.delete(r);

        this.resourceManager.collectionContentModification(parentCollection, 
                                                           principal);

        ResourceDeletionEvent event = new ResourceDeletionEvent(this, uri, r.getID(), 
                                                                r.isCollection());

        context.publishEvent(event);
    }


    public String lock(String token, String uri, String lockType,
            String ownerInfo, String depth, int requestedTimeoutSeconds, String lockToken)
        throws ResourceNotFoundException, AuthorizationException, 
            AuthenticationException, FailedDependencyException, 
            ResourceLockedException, IllegalOperationException, 
            ReadOnlyException, IOException {
        Principal principal = tokenManager.getPrincipal(token);

        if (!uriValidator.validateURI(uri)) {
            throw new ResourceNotFoundException(uri);
        }

        if ("0".equals(depth) || "infinity".equals(depth)) {
            depth = "0";
        } else {
            throw new IllegalOperationException("Invalid depth parameter: "
                                                + depth);
        }

        if (readOnly(principal)) {
            throw new ReadOnlyException();
        }

        ResourceImpl r = dao.load(uri);

        if (r == null) {
            throw new ResourceNotFoundException(uri);
        }

        if (lockToken != null) {
            if (r.getLock() == null) {
                throw new IllegalOperationException(
                    "Invalid lock refresh request: lock token '" + lockToken
                    + "' does not exists on resource " + r.getURI());
            }
            if (!r.getLock().getLockToken().equals(lockToken)) {
                throw new IllegalOperationException(
                    "Invalid lock refresh request: lock token '" + lockToken
                    + "' does not match existing lock token on resource " + r.getURI());
            }
        }

        this.permissionsManager.authorize(r, principal, PrivilegeDefinition.WRITE);
        this.resourceManager.lockAuthorize(r, principal, false);

        String newLockToken = this.resourceManager.lockResource(r, principal, ownerInfo, depth,
                requestedTimeoutSeconds, (lockToken != null));

        return newLockToken;
    }

    public void unlock(String token, String uri, String lockToken)
        throws ResourceNotFoundException, AuthorizationException, 
            AuthenticationException, ResourceLockedException, ReadOnlyException, 
            IOException {
        Principal principal = tokenManager.getPrincipal(token);

        if (readOnly(principal)) {
            throw new ReadOnlyException();
        }

        if (!uriValidator.validateURI(uri)) {
            throw new ResourceNotFoundException(uri);
        }

        ResourceImpl r = dao.load(uri);

        if (r == null) {
            throw new ResourceNotFoundException(uri);
        }

        this.permissionsManager.authorize(r, principal, PrivilegeDefinition.WRITE);
        // Root role has permission to remove all locks
        if (!this.roleManager.hasRole(principal.getQualifiedName(), RoleManager.ROOT)) {
            this.resourceManager.lockAuthorize(r, principal, false);
        }

        if (r.getLock() != null) {
            r.setLock(null);
            this.dao.store(r);
        }
    }

    public Resource store(String token, Resource resource)
        throws ResourceNotFoundException, AuthorizationException, 
            ResourceLockedException, AuthenticationException, 
            IllegalOperationException, ReadOnlyException, IOException {
        Principal principal = tokenManager.getPrincipal(token);

        if (resource == null) {
            throw new IllegalOperationException("Can't store nothing.");
        }

        String uri = resource.getURI();

        if (readOnly(principal)) {
            throw new ReadOnlyException();
        }

        if (!uriValidator.validateURI(uri)) {
            throw new IllegalOperationException("Invalid URI: " + uri);
        }

        ResourceImpl original = dao.load(uri);

        if (original == null) {
            throw new ResourceNotFoundException(uri);
        }

        try {
            this.permissionsManager.authorize(original, principal, PrivilegeDefinition.WRITE);
            this.resourceManager.lockAuthorize(original, principal, false);
            
            ResourceImpl originalClone = (ResourceImpl) original.clone();

            ResourceImpl newResource = 
                this.propertyManager.storeProperties(original, principal, resource);
            this.dao.store(newResource);

            newResource = (ResourceImpl)this.dao.load(uri).clone();

            ResourceModificationEvent event = new ResourceModificationEvent(
                this, newResource, originalClone);

            context.publishEvent(event);

            return newResource;
        } catch (CloneNotSupportedException e) {
            throw new IOException("An internal error occurred: unable to " +
                "clone() resource: " + original);
        }

    }

    public InputStream getInputStream(String token, String uri,
        boolean forProcessing)
        throws ResourceNotFoundException, AuthorizationException, 
            AuthenticationException, ResourceLockedException, IOException {
        Principal principal = tokenManager.getPrincipal(token);

        if (!uriValidator.validateURI(uri)) {
            throw new ResourceNotFoundException(uri);
        }

        ResourceImpl r = dao.load(uri);

        if (r == null) {
            throw new ResourceNotFoundException(uri);
        }

        if (r.isCollection()) {
            throw new IllegalOperationException("resource is collection");
        }

        /* authorize for the right privilege */
        String privilege = (forProcessing)
            ? PrivilegeDefinition.READ_PROCESSED : PrivilegeDefinition.READ;

        this.permissionsManager.authorize(r, principal, privilege);

        InputStream inStream = this.dao.getInputStream(r);

        return inStream;
    }


    /**
     * Requests that an InputStream be written to a resource.
     */
    public void storeContent(String token, String uri, InputStream byteStream)
        throws AuthorizationException, AuthenticationException, 
            ResourceNotFoundException, ResourceLockedException, 
            IllegalOperationException, ReadOnlyException, IOException {
        Principal principal = tokenManager.getPrincipal(token);

        if (!uriValidator.validateURI(uri)) {
            throw new ResourceNotFoundException(uri);
        }

        ResourceImpl r = dao.load(uri);

        if (r == null) {
            throw new ResourceNotFoundException(uri);
        }

        if (r.isCollection()) {
            throw new IllegalOperationException("resource is collection");
        }

        if (readOnly(principal)) {
            throw new ReadOnlyException();
        }

        try {
            this.permissionsManager.authorize(r, principal, PrivilegeDefinition.WRITE);
            this.resourceManager.lockAuthorize(r, principal, false);
        
            ResourceImpl original = (ResourceImpl) r.clone();

            this.dao.storeContent(r, byteStream);
            
            this.resourceManager.resourceContentModification(r, principal, this.dao.getInputStream(r));

            ContentModificationEvent event = new ContentModificationEvent(
                this, (Resource) r.clone(), (Resource) original.clone());

            context.publishEvent(event);
        } catch (CloneNotSupportedException e) {
            throw new IOException("An internal error occurred: unable to " +
                "clone() resource: " + r);
        }
    }


    public Acl getACL(String token, String uri)
        throws ResourceNotFoundException, AuthorizationException, 
            AuthenticationException, IOException {
        Principal principal = tokenManager.getPrincipal(token);

        if (!uriValidator.validateURI(uri)) {
            throw new ResourceNotFoundException(uri);
        }

        ResourceImpl r = dao.load(uri);

        if (r == null) {
            throw new ResourceNotFoundException(uri);
        }

        try {
            this.permissionsManager.authorize(r, principal, PrivilegeDefinition.READ);

            Acl acl = (Acl)r.getAcl().clone();

            return acl;
        } catch (CloneNotSupportedException e) {
            throw new IOException(e.getMessage());
        }
    }

    public void storeACL(String token, String uri, Acl acl)
        throws ResourceNotFoundException, AuthorizationException, 
            AuthenticationException, IllegalOperationException, AclException, 
            ReadOnlyException, IOException {
        Principal principal = tokenManager.getPrincipal(token);

        if (readOnly(principal)) {
            throw new ReadOnlyException();
        }

        if (!uriValidator.validateURI(uri)) {
            throw new ResourceNotFoundException(uri);
        }

        ResourceImpl r = dao.load(uri);

        if (r == null) {
            throw new ResourceNotFoundException(uri);
        }

        if ("/".equals(r.getURI()) && acl.isInherited()) {
            throw new IllegalOperationException("Can't make root acl inherited.");
        }

        try {
            Resource originalResource = (Resource)r.clone();

            this.permissionsManager.authorize(r, principal, PrivilegeDefinition.WRITE_ACL);
            this.resourceManager.lockAuthorize(r, principal, false);
            aclValidator.validateACL(acl);


//             this.resourceManager.storeACL(r, principal, acl);

            Acl newAcl = null;
            if (acl.isInherited()) {
                /* When the ACL is inherited, make our ACL a copy of the
                 * parent's ACL, since the supplied one may contain other
                 * ACEs than the one we now inherit from. */
                newAcl = (Acl) this.dao.load(
                    URIUtil.getParentURI(r.getURI())).getAcl().clone();
                newAcl.setInherited(true);
            } else {
                newAcl = (Acl)acl.clone();
            }
        
            r.setACL(newAcl);
            r.setInheritedACL(newAcl.isInherited());

            try {
                r.setDirtyACL(true);
                this.dao.store(r);
            } finally {
                r.setDirtyACL(false);
            }


            ACLModificationEvent event = new ACLModificationEvent(
                this, (Resource)r.clone(),
                originalResource, acl, originalResource.getAcl());

            context.publishEvent(event);
        } catch (CloneNotSupportedException e) {
            throw new IOException(e.getMessage());
        }
    }

    /* Internal (not Repository API) methods: */
    public DataAccessor getDataAccessor() {
        return this.dao;
    }

    private void authorizeRecursively(ResourceImpl resource, Principal principal,
            String privilege) throws IOException, AuthenticationException,
            AuthorizationException {

        permissionsManager.authorize(resource, principal, privilege);
        if (resource.isCollection()) {
            String[] uris = this.dao.discoverACLs(resource);
            for (int i = 0; i < uris.length; i++) {
                ResourceImpl ancestor = this.dao.load(uris[i]);
                permissionsManager.authorize(ancestor, principal, privilege);
            }
        }
    }

    private boolean readOnly(Principal principal) {
        return this.readOnly && !roleManager.hasRole(
                principal.getQualifiedName(), RoleManager.ROOT);
    }
    
    public void setTokenManager(TokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }


    public void setDao(DataAccessor dao) {
        this.dao = dao;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public void setRoleManager(RoleManager roleManager) {
        this.roleManager = roleManager;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setResourceManager(ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }
    
    public void setPermissionsManager(PermissionsManager permissionsManager) {
        this.permissionsManager = permissionsManager;
    }

    public void setAclValidator(AclValidator aclValidator) {
        this.aclValidator = aclValidator;
    }

    public void setApplicationContext(ApplicationContext context) {
        this.context = context;
    }

    public void afterPropertiesSet() {
        if (this.dao == null) {
            throw new BeanInitializationException(
                "Bean property 'dao' must be set");
        }
        if (this.roleManager == null) {
            throw new BeanInitializationException(
                "Bean property 'roleManager' must be set");
        }
        if (this.aclValidator == null) {
            throw new BeanInitializationException(
                "Bean property 'aclValidator' must be set");
        }
        if (this.tokenManager == null) {
            throw new BeanInitializationException(
                "Bean property 'tokenManager' must be set");
        }
        if (this.id == null) {
            throw new BeanInitializationException(
                "Bean property 'id' must be set");
        }
        if (this.resourceManager == null) {
            throw new BeanInitializationException(
            "Bean property 'resourceManager' must be set");
        }
        if (this.propertyManager == null) {
            throw new BeanInitializationException(
            "Bean property 'propertyManager' must be set");
        }
    }

    /**
     * @param propertyManager The propertyManager to set.
     */
    public void setPropertyManager(PropertyManagerImpl propertyManager) {
        this.propertyManager = propertyManager;
    }

}

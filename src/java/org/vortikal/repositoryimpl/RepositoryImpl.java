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
import java.util.Date;

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
import org.vortikal.repository.RepositoryOperations;
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
 */
public class RepositoryImpl implements Repository, ApplicationContextAware,
        InitializingBean {

    private ApplicationContext context;

    private DataAccessor dao;
    private RoleManager roleManager;
    private TokenManager tokenManager;
    private ResourceManager resourceManager;
    private PermissionsManager permissionsManager;
    private ACLValidator aclValidator;
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
        Principal principal = tokenManager.getPrincipal(token);

        String operation = RepositoryOperations.EXISTS;
        
        if (!uriValidator.validateURI(uri)) {
            OperationLog.info(operation, "(" + uri + "): false",
                    token, principal);
            return false;
        }

        ResourceImpl r = dao.load(uri);

        if (r == null) {
            OperationLog.info(operation, "(" + uri + "): false",
                    token, principal);

            return false;
        }

        OperationLog.info(operation, "(" + uri + "): true", token,
                principal);

        return true;
    }

    public org.vortikal.repository.Resource retrieve(String token, String uri,
        boolean forProcessing)
        throws ResourceNotFoundException, AuthorizationException, 
            AuthenticationException, IOException {
        Principal principal = tokenManager.getPrincipal(token);

        String operation = RepositoryOperations.RETRIEVE;
        
        if (!uriValidator.validateURI(uri)) {
            OperationLog.failure(operation, "(" + uri + ")", "resource not found",
                token, principal);

            throw new ResourceNotFoundException(uri);
        }

        ResourceImpl r = dao.load(uri);

        if (r == null) {
            OperationLog.failure(operation, "(" + uri + ")", "resource not found",
                token, principal);
            throw new ResourceNotFoundException(uri);
        }

        try {
            /* authorize for the right privilege */
            String privilege = (forProcessing)
                ? PrivilegeDefinition.CUSTOM_PRIVILEGE_READ_PROCESSED
                : PrivilegeDefinition.READ;

            this.permissionsManager.authorize(r, principal, privilege);

            OperationLog.success(operation, "(" + uri + ")", token, principal);

            return resourceManager.getResourceClone(r);

        } catch (AuthorizationException e) {
            OperationLog.failure(operation, "(" + uri + ")", "not authorized",
                token, principal);
            throw e;
        } catch (AuthenticationException e) {
            OperationLog.failure(operation, "(" + uri + ")", "not authenticated",
                token, principal);
            throw e;
        }
    }

    public org.vortikal.repository.Resource[] listChildren(String token,
            String uri, boolean forProcessing)
            throws ResourceNotFoundException, AuthorizationException,
            AuthenticationException, IOException {
        Principal principal = tokenManager.getPrincipal(token);

        String operation = RepositoryOperations.LIST_CHILDREN;
        
        if (!uriValidator.validateURI(uri)) {
            OperationLog.failure(operation, "(" + uri + ")",
                    "resource not found", token, principal);
            throw new ResourceNotFoundException(uri);
        }

        ResourceImpl r = dao.load(uri);

        if (r == null) {
            OperationLog.failure(operation, "(" + uri + ")",
                    "resource not found", token, principal);
            throw new ResourceNotFoundException(uri);
        }

        if (!r.isCollection()) {
            OperationLog.failure(operation, "(" + uri + ")",
                    "uri is document", token, principal);

            return null;
        }

        /* authorize for the right privilege: */
        String privilege = (forProcessing) ? PrivilegeDefinition.CUSTOM_PRIVILEGE_READ_PROCESSED
                : PrivilegeDefinition.READ;

        this.permissionsManager.authorize(r, principal, privilege);

        ResourceImpl[] list = this.dao.loadChildren(r);
        org.vortikal.repository.Resource[] children = new org.vortikal.repository.Resource[list.length];

        for (int i = 0; i < list.length; i++) {
            children[i] = resourceManager.getResourceClone(list[i]);
        }

        OperationLog.success(operation, "(" + uri + ")",
                token, principal);

        return children;
    }

    public org.vortikal.repository.Resource createDocument(String token,
            String uri) throws IllegalOperationException,
            AuthorizationException, AuthenticationException,
            ResourceLockedException, ReadOnlyException, IOException {
        return create(token, uri, false);
    }

    public org.vortikal.repository.Resource createCollection(String token, 
            String uri) throws IllegalOperationException, 
            AuthorizationException, AuthenticationException, 
            ResourceLockedException, ReadOnlyException, IOException {

        return create(token, uri, true);
    }

    private org.vortikal.repository.Resource create(String token, String uri, boolean collection)
    throws AuthorizationException, AuthenticationException, 
    IllegalOperationException, ResourceLockedException, 
    ReadOnlyException, IOException {

        String operation = (collection) ? RepositoryOperations.CREATE_COLLECTION : RepositoryOperations.CREATE;
        
        Principal principal = tokenManager.getPrincipal(token);

        if (readOnly(principal)) {
            OperationLog.failure(operation, "(" + uri + ")",
                    "read-only", token, principal);
            throw new ReadOnlyException();
        }

        if (!uriValidator.validateURI(uri)) {
            OperationLog.failure(operation, "(" + uri + ")",
                    "resource not found.", token, principal);
            throw new ResourceNotFoundException(uri);
        }

        ResourceImpl resource = dao.load(uri);

        if (resource != null) {
            OperationLog.failure(operation, "(" + uri + ")",
                    "illegal operation: Resource already exists", token,
                    principal);
            throw new IllegalOperationException("Resource already exists");
        }

        String parentURI = URIUtil.getParentURI(uri);

        ResourceImpl parent = dao.load(parentURI);

        if ((parent == null) || !parent.isCollection()) {
            OperationLog.failure(operation, "(" + uri + ")",
                    "illegal operation: either parent doesn't exist " +
                    "or parent is document", token, principal);
            throw new IllegalOperationException("Either parent doesn't exist " +
                    "or parent is document");
        }

        try {
            this.permissionsManager.authorize(parent, principal, 
                    PrivilegeDefinition.WRITE);
            this.resourceManager.lockAuthorize(parent, principal, false);
        } catch (ResourceLockedException e) {
            OperationLog.failure(operation, "(" + uri + ")",
                    "the parent resource was locked", token, principal);
            throw e;
        } catch (AuthorizationException e) {
            OperationLog.failure(operation, "(" + uri + ")",
                    "unauthorized", token, principal);
            throw e;
        }

        org.vortikal.repository.Resource dto = 
            this.resourceManager.create(parent, principal, collection, uri);
        
        OperationLog.success(operation, "(" + uri + ")", token,
                principal);

        context.publishEvent(new ResourceCreationEvent(this, dto));

        return dto;
    }    

    public void copy(String token, String srcUri, String destUri, String depth,
        boolean overwrite, boolean preserveACL)
        throws IllegalOperationException, AuthorizationException, 
            AuthenticationException, FailedDependencyException, 
            ResourceOverwriteException, ResourceLockedException, 
            ResourceNotFoundException, ReadOnlyException, IOException {
        Principal principal = tokenManager.getPrincipal(token);

        String operation = RepositoryOperations.COPY;
        
        if (readOnly(principal)) {
            OperationLog.failure(operation, "(" + srcUri + ", " + destUri + ")",
                "read-only", token, principal);
            throw new ReadOnlyException();
        }

        if (!uriValidator.validateURI(srcUri)) {
            OperationLog.failure(operation, "(" + srcUri + ", " + destUri + ")",
                "source not found", token, principal);
            throw new ResourceNotFoundException(srcUri);
        }

        if (!uriValidator.validateURI(destUri)) {
            OperationLog.failure(operation, "(" + srcUri + ", " + destUri + ")",
                "invalid URI: '" + destUri + "'", token, principal);
            throw new IllegalOperationException("Invalid URI: '" + destUri +
                "'");
        }

        try {
            uriValidator.validateCopyURIs(srcUri, destUri);
        } catch (IllegalOperationException e) {
            OperationLog.failure(operation, "(" + srcUri + ", " + destUri + ")",
                    e.getMessage(), token, principal);
            throw e;
        }
        
        ResourceImpl src = dao.load(srcUri);

        if (src == null) {
            OperationLog.failure(operation, "(" + srcUri + ", " + destUri + ")",
                "source not found", token, principal);
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
            OperationLog.failure(operation, "(" + srcUri + ", " + destUri + ")",
                "destination is either a document or does not exist", token,
                principal);
            throw new IllegalOperationException();
        }

        this.permissionsManager.authorize(destParent, principal,
                                       PrivilegeDefinition.WRITE);

        this.resourceManager.lockAuthorize(destParent, principal, false);

        try {
            if (dest != null) {
                this.dao.delete(dest);
            }
            this.resourceManager.copy(principal, src, destUri, preserveACL, false);

            OperationLog.success(operation, "(" + srcUri + ", " + destUri + ")",
                token, principal);

            org.vortikal.repository.Resource dto = 
                resourceManager.getResourceClone(dao.load(destUri));

            ResourceCreationEvent event = new ResourceCreationEvent(this, dto);

            context.publishEvent(event);
        } catch (AclException e) {
            OperationLog.failure(operation, "(" + srcUri + ", " + destUri + ")",
                "tried to set an illegal ACL", token, principal);
            throw new IllegalOperationException(e.getMessage());
        }
    }

    // XXX: Missing a lot of operation logging!
    public void move(String token, String srcUri, String destUri,
        boolean overwrite)
        throws IllegalOperationException, AuthorizationException, 
            AuthenticationException, FailedDependencyException, 
            ResourceOverwriteException, ResourceLockedException, 
            ResourceNotFoundException, ReadOnlyException, IOException {
        Principal principal = tokenManager.getPrincipal(token);

        String operation = RepositoryOperations.MOVE;
        
        if (readOnly(principal)) {
            OperationLog.failure(operation, "(" + srcUri + ", " + destUri + ")",
                "read-only", token, principal);
            throw new ReadOnlyException();
        }

        if (!uriValidator.validateURI(srcUri)) {
            OperationLog.failure(operation, "(" + srcUri + ", " + destUri + ")",
                "source not found", token, principal);
            throw new ResourceNotFoundException(srcUri);
        }

        if (!uriValidator.validateURI(destUri)) {
            OperationLog.failure(operation, "(" + srcUri + ", " + destUri + ")",
                "invalid URI: '" + destUri + "'", token, principal);
            throw new IllegalOperationException("Invalid URI: '" + destUri +
                "'");
        }

        try {
            uriValidator.validateCopyURIs(srcUri, destUri);
        } catch (IllegalOperationException e) {
            OperationLog.failure(operation, "(" + srcUri + ", " + destUri + ")",
                    e.getMessage(), token, principal);
            throw e;
        }
        
        // Loading and checking source resource
        ResourceImpl src = dao.load(srcUri);

        if (src == null) {
            OperationLog.failure(operation, "(" + srcUri + ", " + destUri + ")",
                "source not found", token, principal);
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
                OperationLog.failure(operation, "(" + srcUri + ", " + destUri + ")",
                        "destination already existed, no overwrite", token, principal);
                throw new ResourceOverwriteException();
            }
            
            this.resourceManager.lockAuthorize(dest, principal, true);
        } 

        // checking destParent 
        String destParentUri = URIUtil.getParentURI(destUri);

        ResourceImpl destParent = dao.load(destParentUri);

        if ((destParent == null) || !destParent.isCollection()) {
            OperationLog.failure(operation, "(" + srcUri + ", " + destUri + ")",
                "destination collection either a document or does not exist",
                token, principal);
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
            this.resourceManager.copy(principal, src, destUri, true, true);
            this.dao.delete(src);
            context.publishEvent(new ResourceDeletionEvent(this, srcUri,
                    src.getID(), src.isCollection()));

            OperationLog.success(operation, "(" + srcUri + ", " + destUri + ")",
                token, principal);

            ResourceImpl r = dao.load(destUri);
            org.vortikal.repository.Resource dto = 
                resourceManager.getResourceClone(r);

            context.publishEvent(new ResourceCreationEvent(this, dto));
        } catch (AclException e) {
            OperationLog.failure(operation, "(" + srcUri + ", " + destUri + ")",
                "tried to set an illegal ACL", token, principal);

            throw new IllegalOperationException(e.getMessage());
        }
    }

    public void delete(String token, String uri)
        throws IllegalOperationException, AuthorizationException, 
            AuthenticationException, ResourceNotFoundException, 
            ResourceLockedException, FailedDependencyException, 
            ReadOnlyException, IOException {
        Principal principal = tokenManager.getPrincipal(token);

        String operation = RepositoryOperations.DELETE;
        
        if (!uriValidator.validateURI(uri)) {
            OperationLog.failure(operation, "(" + uri + ")", "resource not found",
                token, principal);
            throw new ResourceNotFoundException(uri);
        }

        if ("/".equals(uri)) {
            OperationLog.failure(operation, "(" + uri + ")",
                "cannot delete the root resource ('/')", token, principal);
            throw new IllegalOperationException(
                "Cannot delete the root resource ('/')");
        }

        ResourceImpl r = dao.load(uri);

        if (r == null) {
            OperationLog.failure(operation, "(" + uri + ")", "resource not found",
                token, principal);
            throw new ResourceNotFoundException(uri);
        }

        this.resourceManager.lockAuthorize(r, principal, true);

        String parent = URIUtil.getParentURI(uri);

        ResourceImpl parentCollection = dao.load(parent);

        this.permissionsManager.authorize(parentCollection, principal, PrivilegeDefinition.WRITE);
        this.resourceManager.lockAuthorize(parentCollection, principal, false);

        if (readOnly(principal)) {
            OperationLog.failure(operation, "(" + uri + ")", "read-only", token,
                principal);
            throw new ReadOnlyException();
        }

        this.dao.delete(r);

        this.resourceManager.collectionContentModification(parentCollection, principal);

        OperationLog.success(operation, "(" + uri + ")", token, principal);

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

        String operation = RepositoryOperations.LOCK;
        
        if (!uriValidator.validateURI(uri)) {
            OperationLog.failure(operation, "(" + uri + ")", "resource not found",
                token, principal);
            throw new ResourceNotFoundException(uri);
        }

        if ("0".equals(depth) || "infinity".equals(depth)) {
            // FIXME: implement locking of depth 'infinity'
            depth = "0";
        } else {
            throw new IllegalOperationException("Invalid depth parameter: "
                                                + depth);
        }

        if (readOnly(principal)) {
            OperationLog.failure(operation, "(" + uri + ")", "read-only", token,
                principal);
            throw new ReadOnlyException();
        }

        ResourceImpl r = dao.load(uri);

        if (r == null) {
            OperationLog.failure(operation, "(" + uri + ")", "resource not found",
                token, principal);
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


        try {
            this.permissionsManager.authorize(r, principal, PrivilegeDefinition.WRITE);
            this.resourceManager.lockAuthorize(r, principal, false);
        } catch (AuthorizationException e) {
            OperationLog.failure(operation, "(" + uri + ")", "permission denied",
                token, principal);
            throw e;
        } catch (AuthenticationException e) {
            OperationLog.failure(operation, "(" + uri + ")", "not authenticated",
                token, principal);
            throw e;
        } catch (ResourceLockedException e) {
            OperationLog.failure(operation, "(" + uri + ")", "already locked", token,
                principal);
            throw e;
        }

        String newLockToken = this.resourceManager.lockResource(r, principal, ownerInfo, depth,
                requestedTimeoutSeconds, (lockToken != null));

        OperationLog.success(operation, "(" + uri + ")", token, principal);

        return newLockToken;
    }

    public void unlock(String token, String uri, String lockToken)
        throws ResourceNotFoundException, AuthorizationException, 
            AuthenticationException, ResourceLockedException, ReadOnlyException, 
            IOException {
        Principal principal = tokenManager.getPrincipal(token);

        String operation = RepositoryOperations.UNLOCK;
        
        if (readOnly(principal)) {
            OperationLog.failure(operation, "(" + uri + ")", "read-only", token,
                principal);
            throw new ReadOnlyException();
        }

        if (!uriValidator.validateURI(uri)) {
            OperationLog.failure(operation, "(" + uri + ")", "resource not found",
                token, principal);
            throw new ResourceNotFoundException(uri);
        }

        ResourceImpl r = dao.load(uri);

        if (r == null) {
            OperationLog.failure(operation, "(" + uri + ")", "resource not found",
                token, principal);
            throw new ResourceNotFoundException(uri);
        }

        try {
            this.permissionsManager.authorize(r, principal, PrivilegeDefinition.WRITE);
            // Root role has permission to remove all locks
            if (!this.roleManager.hasRole(principal.getQualifiedName(), RoleManager.ROOT)) {
                this.resourceManager.lockAuthorize(r, principal, false);
            }
        } catch (AuthorizationException e) {
            OperationLog.failure(operation, "(" + uri + ")", "permission denied",
                token, principal);
            throw e;
        } catch (AuthenticationException e) {
            OperationLog.failure(operation, "(" + uri + ")", "not authenticated",
                token, principal);
            throw e;
        } catch (ResourceLockedException e) {
            OperationLog.failure(operation, "(" + uri + ")", "resource locked",
                token, principal);
            throw e;
        }

        if (r.getLock() != null) {
            r.setLock(null);
            this.dao.store(r);
        }

        OperationLog.success(operation, "(" + uri + ")", token,
                principal);
    }

    public void store(String token, org.vortikal.repository.Resource resource)
        throws ResourceNotFoundException, AuthorizationException, 
            ResourceLockedException, AuthenticationException, 
            IllegalOperationException, ReadOnlyException, IOException {
        Principal principal = tokenManager.getPrincipal(token);

        String operation = RepositoryOperations.STORE;
        
        if (resource == null) {
            // FIXME: should throw java.lang.IllegalArgumentException
            OperationLog.failure(operation, "(null)", "resource null", token,
                principal);
            throw new IllegalOperationException("I cannot store nothing.");
        }

        String uri = resource.getURI();

        if (!uriValidator.validateURI(uri)) {
            OperationLog.failure(operation, "(" + uri + ")", "invalid uri", token,
                principal);
            throw new IllegalOperationException("Invalid URI: " + uri);
        }

        ResourceImpl r = dao.load(uri);

        if (r == null) {
            OperationLog.failure(operation, "(" + uri + ")", "invalid uri", token,
                principal);
            throw new ResourceNotFoundException(uri);
        }

        if (readOnly(principal)) {
            OperationLog.failure(operation, "(" + uri + ")", "read-only", token,
                principal);
            throw new ReadOnlyException();
        }

        try {
            // Fix me: log authexceptions...
            this.permissionsManager.authorize(r, principal, PrivilegeDefinition.WRITE);
            this.resourceManager.lockAuthorize(r, principal, false);
            
            ResourceImpl original = (ResourceImpl) r.clone();

            this.resourceManager.storeProperties(r, principal, resource);
            OperationLog.success(operation, "(" + uri + ")", token, principal);

            ResourceModificationEvent event = new ResourceModificationEvent(
                this, resourceManager.getResourceClone(r),
                resourceManager.getResourceClone(original));

            context.publishEvent(event);
        } catch (ResourceLockedException e) {
            OperationLog.failure(operation, "(" + uri + ")", "resource locked",
                token, principal);
            throw e;
        } catch (CloneNotSupportedException e) {
            throw new IOException("An internal error occurred: unable to " +
                "clone() resource: " + r);
        }
    }

    public InputStream getInputStream(String token, String uri,
        boolean forProcessing)
        throws ResourceNotFoundException, AuthorizationException, 
            AuthenticationException, ResourceLockedException, IOException {
        Principal principal = tokenManager.getPrincipal(token);

        String operation = RepositoryOperations.GET_INPUTSTREAM;
        
        if (!uriValidator.validateURI(uri)) {
            OperationLog.failure(operation, "(" + uri + ")",
                "resource not found", token, principal);
            throw new ResourceNotFoundException(uri);
        }

        ResourceImpl r = dao.load(uri);

        if (r == null) {
            OperationLog.failure(operation, "(" + uri + ")",
                "resource not found", token, principal);
            throw new ResourceNotFoundException(uri);
        }

        if (r.isCollection()) {
            OperationLog.failure(operation, "(" + uri + ")",
                "resource is collection", token, principal);
            throw new IOException("resource is collection");
        }

        /* authorize for the right privilege */
        String privilege = (forProcessing)
            ? PrivilegeDefinition.CUSTOM_PRIVILEGE_READ_PROCESSED : PrivilegeDefinition.READ;

        this.permissionsManager.authorize(r, principal, privilege);

        InputStream inStream = this.dao.getInputStream(r);

        OperationLog.success(operation, "(" + uri + ")", token, principal);

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

        String operation = RepositoryOperations.STORE_CONTENT;
        
        if (!uriValidator.validateURI(uri)) {
            OperationLog.failure(operation, "(" + uri + ")",
                "resource not found", token, principal);
            throw new ResourceNotFoundException(uri);
        }

        ResourceImpl r = dao.load(uri);

        if (r == null) {
            OperationLog.failure(operation, "(" + uri + ")",
                "resource not found", token, principal);
            throw new ResourceNotFoundException(uri);
        }

        if (r.isCollection()) {
            OperationLog.failure(operation, "(" + uri + ")",
                "resource is collection", token, principal);
            throw new IOException("Collections have no output stream");
        }

        if (readOnly(principal)) {
            OperationLog.failure(operation, "(" + uri + ")", "read-only",
                token, principal);
            throw new ReadOnlyException();
        }

        try {
            this.permissionsManager.authorize(r, principal, PrivilegeDefinition.WRITE);
            this.resourceManager.lockAuthorize(r, principal, false);
        
            ResourceImpl original = (ResourceImpl) r.clone();

            this.dao.storeContent(r, byteStream);
            
            this.resourceManager.resourceContentModification(r, principal, this.dao.getInputStream(r));

            OperationLog.success(operation, "(" + uri + ")", token, principal);

            ContentModificationEvent event = new ContentModificationEvent(
                this, resourceManager.getResourceClone(r),
                resourceManager.getResourceClone(original));

            context.publishEvent(event);
        } catch (ResourceLockedException e) {
            OperationLog.failure(operation, "(" + uri + ")",
                "resource locked", token, principal);
            throw e;
        } catch (AuthorizationException e) {
            OperationLog.failure(operation, "(" + uri + ")",
                "not authorized", token, principal);
            throw e;
        } catch (CloneNotSupportedException e) {
            throw new IOException("An internal error occurred: unable to " +
                "clone() resource: " + r);
        }
    }


    public Acl getACL(String token, String uri)
        throws ResourceNotFoundException, AuthorizationException, 
            AuthenticationException, IOException {
        Principal principal = tokenManager.getPrincipal(token);

        String operation = RepositoryOperations.GET_ACL;
        
        if (!uriValidator.validateURI(uri)) {
            OperationLog.failure(operation, "(" + uri + ")", "resource not found",
                token, principal);

            throw new ResourceNotFoundException(uri);
        }

        ResourceImpl r = dao.load(uri);

        if (r == null) {
            OperationLog.failure(operation, "(" + uri + ")", "resource not found",
                token, principal);
            throw new ResourceNotFoundException(uri);
        }

        try {
            this.permissionsManager.authorize(r, principal, PrivilegeDefinition.READ);

            String inheritedFrom = null;
            if (r.isInheritedACL()) {
                inheritedFrom = URIUtil.getParentURI(r.getURI());
            }
            
            Acl acl = permissionsManager.toAceList(r.getACL(), inheritedFrom);

            OperationLog.success(operation, "(" + uri + ")", token, principal);

            return acl;
        } catch (AuthorizationException e) {
            OperationLog.failure(operation, "(" + uri + ")", "permission denied",
                token, principal);
            throw e;
        } catch (AuthenticationException e) {
            OperationLog.failure(operation, "(" + uri + ")", "not authenticated",
                token, principal);
            throw e;
        }
    }

    public void storeACL(String token, String uri, Acl acl)
        throws ResourceNotFoundException, AuthorizationException, 
            AuthenticationException, IllegalOperationException, AclException, 
            ReadOnlyException, IOException {
        Principal principal = tokenManager.getPrincipal(token);

        String operation = RepositoryOperations.STORE_ACL;
        
        if (readOnly(principal)) {
            OperationLog.failure(operation, "(" + uri + ")", "read-only",
                token, principal);
            throw new ReadOnlyException();
        }

        if (!uriValidator.validateURI(uri)) {
            OperationLog.failure(operation, "(" + uri + ")", "resource not found",
                token, principal);
            throw new ResourceNotFoundException(uri);
        }

        ResourceImpl r = dao.load(uri);

        if (r == null) {
            OperationLog.failure(operation, "(" + uri + ")", "resource not found",
                token, principal);
            throw new ResourceNotFoundException(uri);
        }

        if ("/".equals(r.getURI()) && acl.isInherited()) {
            OperationLog.failure(operation, "(" + uri + ")", "Can't make root acl inherited.",
                    token, principal);
            throw new IllegalOperationException("Can't make root acl inherited.");
        }

        try {
            org.vortikal.repository.Resource originalResource = 
                resourceManager.getResourceClone(r);
            String inheritedFrom = null;
            if (r.isInheritedACL()) {
                inheritedFrom = URIUtil.getParentURI(r.getURI());
            }
            Acl originalACL = permissionsManager.toAceList(r.getACL(), inheritedFrom);

            this.permissionsManager.authorize(r, principal, PrivilegeDefinition.WRITE_ACL);
            this.resourceManager.lockAuthorize(r, principal, false);
            aclValidator.validateACL(acl);
            this.resourceManager.storeACL(r, principal, acl);
            OperationLog.success(operation, "(" + uri + ")", token, principal);

            ACLModificationEvent event = new ACLModificationEvent(
                this, resourceManager.getResourceClone(r),
                originalResource, acl, originalACL);

            context.publishEvent(event);
        } catch (AuthorizationException e) {
            OperationLog.failure(operation, "(" + uri + ")", "permission denied",
                token, principal);
            throw e;
        } catch (AuthenticationException e) {
            OperationLog.failure(operation, "(" + uri + ")", "not authenticated",
                token, principal);
            throw e;
        } catch (IllegalOperationException e) {
            OperationLog.failure(operation, "(" + uri + ")", "illegal operation",
                token, principal);
            throw e;
        } catch (AclException e) {
            OperationLog.failure(operation, "(" + uri + ")",
                "illegal ACL operation", token, principal);
            throw e;
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

    public void setAclValidator(ACLValidator aclValidator) {
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
    }

}
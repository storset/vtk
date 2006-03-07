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
import java.io.OutputStream;
import java.util.Date;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.vortikal.repository.Ace;
import org.vortikal.repository.AclException;
import org.vortikal.repository.AuthorizationException;
import org.vortikal.repository.FailedDependencyException;
import org.vortikal.repository.IllegalOperationException;
import org.vortikal.repository.PrivilegeDefinition;
import org.vortikal.repository.ReadOnlyException;
import org.vortikal.repository.Repository;
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

    private boolean readOnly = false;
    private ApplicationContext context = null;

    private DataAccessor dao;
    private RoleManager roleManager = null;
    private TokenManager tokenManager;
    private ResourceManager resourceManager;
    private PermissionsManager permissionsManager;
    private ACLValidator aclValidator;
    private URIValidator uriValidator = new URIValidator();
    
    private String id;

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

    public boolean exists(String token, String uri)
            throws AuthorizationException, AuthenticationException, IOException {
        Principal principal = tokenManager.getPrincipal(token);

        if (!uriValidator.validateURI(uri)) {
            OperationLog.info(OperationLog.EXISTS, "(" + uri + "): false",
                    token, principal);

            return false;
        }

        Resource r = dao.load(uri);

        if (r == null) {
            OperationLog.info(OperationLog.EXISTS, "(" + uri + "): false",
                    token, principal);

            return false;
        }

        OperationLog.info(OperationLog.EXISTS, "(" + uri + "): true", token,
                principal);

        return true;
    }

    public org.vortikal.repository.Resource retrieve(String token, String uri,
        boolean forProcessing)
        throws ResourceNotFoundException, AuthorizationException, 
            AuthenticationException, IOException {
        Principal principal = tokenManager.getPrincipal(token);

        if (!uriValidator.validateURI(uri)) {
            OperationLog.failure(OperationLog.RETRIEVE, "(" + uri + ")", "resource not found",
                token, principal);

            throw new ResourceNotFoundException(uri);
        }

        Resource r = dao.load(uri);

        if (r == null) {
            OperationLog.failure(OperationLog.RETRIEVE, "(" + uri + ")", "resource not found",
                token, principal);
            throw new ResourceNotFoundException(uri);
        }

        try {
            /* authorize for the right privilege */
            String privilege = (forProcessing)
                ? PrivilegeDefinition.CUSTOM_PRIVILEGE_READ_PROCESSED
                : PrivilegeDefinition.READ;

            this.permissionsManager.authorize(r, principal, privilege);

            OperationLog.success(OperationLog.RETRIEVE, "(" + uri + ")", token, principal);

            return resourceManager.getResourceDTO(r, principal);

        } catch (AuthorizationException e) {
            OperationLog.failure(OperationLog.RETRIEVE, "(" + uri + ")", "not authorized",
                token, principal);
            throw e;
        } catch (AuthenticationException e) {
            OperationLog.failure(OperationLog.RETRIEVE, "(" + uri + ")", "not authenticated",
                token, principal);
            throw e;
        }
    }

    public org.vortikal.repository.Resource[] listChildren(String token,
            String uri, boolean forProcessing)
            throws ResourceNotFoundException, AuthorizationException,
            AuthenticationException, IOException {
        Principal principal = tokenManager.getPrincipal(token);

        if (!uriValidator.validateURI(uri)) {
            OperationLog.failure(OperationLog.LIST_CHILDREN, "(" + uri + ")",
                    "resource not found", token, principal);
            throw new ResourceNotFoundException(uri);
        }

        Resource r = dao.load(uri);

        if (r == null) {
            OperationLog.failure(OperationLog.LIST_CHILDREN, "(" + uri + ")",
                    "resource not found", token, principal);
            throw new ResourceNotFoundException(uri);
        }

        if (!r.isCollection()) {
            OperationLog.failure(OperationLog.LIST_CHILDREN, "(" + uri + ")",
                    "uri is document", token, principal);

            return null;
        }

        /* authorize for the right privilege: */
        String privilege = (forProcessing) ? PrivilegeDefinition.CUSTOM_PRIVILEGE_READ_PROCESSED
                : PrivilegeDefinition.READ;

        this.permissionsManager.authorize(r, principal, privilege);
        this.resourceManager.lockAuthorize(r, principal);

        Resource[] list = this.dao.loadChildren(r);
        org.vortikal.repository.Resource[] children = new org.vortikal.repository.Resource[list.length];

        for (int i = 0; i < list.length; i++) {
            children[i] = resourceManager.getResourceDTO(list[i], principal);
        }

        OperationLog.success(OperationLog.LIST_CHILDREN, "(" + uri + ")",
                token, principal);

        return children;
    }

    public org.vortikal.repository.Resource createDocument(String token,
            String uri) throws IllegalOperationException,
            AuthorizationException, AuthenticationException,
            ResourceLockedException, ReadOnlyException, IOException {
        Principal principal = tokenManager.getPrincipal(token);

        if (!uriValidator.validateURI(uri)) {
            OperationLog.failure(OperationLog.CREATE, "(" + uri + ")",
                    "resource not found.", token, principal);
            throw new ResourceNotFoundException(uri);
        }

        if ("/".equals(uri)) {
            OperationLog.failure(OperationLog.CREATE, "(" + uri + ")",
                    "illegal operation. Cannot create the root resource ('/')",
                    token, principal);
            throw new IllegalOperationException(
                    "Cannot create the root resource ('/')");
        }

        Resource r = dao.load(uri);

        if (r != null) {
            OperationLog.failure(OperationLog.CREATE, "(" + uri + ")",
                    "illegal operation: Resource already exists", token,
                    principal);
            throw new IllegalOperationException("Resource already exists");
        }

        String parentURI = URIUtil.getParentURI(uri);

        r = dao.load(parentURI);

        if ((r == null) || !r.isCollection()) {
            OperationLog.failure(OperationLog.CREATE, "(" + uri + ")",
                    "illegal operation: either parent doesn't exist " +
                    "or parent is document", token, principal);
            throw new IllegalOperationException("Either parent doesn't exist " +
                    "or parent is document");
        }

        try {
            this.permissionsManager.authorize(r, principal,
                    PrivilegeDefinition.WRITE);
            this.resourceManager.lockAuthorize(r, principal);
        } catch (ResourceLockedException e) {
            OperationLog.failure(OperationLog.CREATE, "(" + uri + ")",
                    "resource was locked", token, principal);
            throw e;
        } catch (AuthorizationException e) {
            OperationLog.failure(OperationLog.CREATE, "(" + uri + ")",
                    "unauthorized", token, principal);
            throw e;
        }

        if (readOnly(principal)) {
            OperationLog.failure(OperationLog.CREATE, "(" + uri + ")",
                    "read-only", token, principal);
            throw new ReadOnlyException();
        }

        Resource doc = this.resourceManager.create(r, principal, false,
                principal.getQualifiedName(), uri, null, true);
        OperationLog.success(OperationLog.CREATE, "(" + uri + ")", token,
                principal);

        org.vortikal.repository.Resource dto = resourceManager.getResourceDTO(
                doc, principal);

        context.publishEvent(new ResourceCreationEvent(this, dto));

        return dto;
    }

    public org.vortikal.repository.Resource createCollection(String token,
        String uri)
        throws AuthorizationException, AuthenticationException, 
            IllegalOperationException, ResourceLockedException, 
            ReadOnlyException, IOException {
        Principal principal = tokenManager.getPrincipal(token);

        if (!uriValidator.validateURI(uri)) {
            OperationLog.failure(OperationLog.CREATE_COLLECTION, "(" + uri + ")",
                "illegal operation: " + "Invalid URI: '" + uri + "'", token,
                principal);
            throw new IllegalOperationException("Invalid URI: '" + uri + "'");
        }

        if ("/".equals(uri)) {
            OperationLog.failure(OperationLog.CREATE_COLLECTION, "(" + uri + ")",
                "illegal operation: " +
                "Cannot create the root resource ('/')", token, principal);

            throw new IllegalOperationException(
                "Cannot create the root resource ('/')");
        }

        String path = (uri.charAt(uri.length() - 1) == '/')
            ? uri.substring(0, uri.length() - 1) : uri;

        /* try to load the resource to see if it exists: */
        Resource r = dao.load(path);

        if (r != null) {
            // Resource already exists
            OperationLog.failure(OperationLog.CREATE_COLLECTION, "(" + uri + ")",
                "illegal operation: " + "resource already exists", token,
                principal);
            throw new IllegalOperationException();
        }

        /* try to load the parent: */
        String parentURI = uri.substring(0, uri.lastIndexOf("/") + 1);

        if (parentURI.endsWith("/") && !parentURI.equals("/")) {
            parentURI = parentURI.substring(0, parentURI.length() - 1);
        }

        r = dao.load(parentURI);

        if (r == null || !r.isCollection()) {
            /* Parent does not exist or is a document */
            OperationLog.failure(OperationLog.CREATE_COLLECTION, "(" + uri + ")",
                "illegal operation: " +
                "either parent is null or parent is document", token, principal);
            throw new IllegalOperationException();
        }

        try {
            this.permissionsManager.authorize(r, principal, PrivilegeDefinition.WRITE);
            this.resourceManager.lockAuthorize(r, principal);

            if (readOnly(principal)) {
                OperationLog.failure(OperationLog.CREATE_COLLECTION, "(" + uri + ")",
                    "read-only", token, principal);
                throw new ReadOnlyException();
            }

            Resource newCollection = this.resourceManager.create(
                r, principal, true, principal.getQualifiedName(), path, new ACL(), true);

            OperationLog.success(OperationLog.CREATE_COLLECTION, "(" + uri + ")", token,
                principal);

            org.vortikal.repository.Resource dto = 
                resourceManager.getResourceDTO(newCollection, principal);

            context.publishEvent(new ResourceCreationEvent(this, dto));

            return dto;
        } catch (ResourceLockedException e) {
            OperationLog.failure(OperationLog.CREATE_COLLECTION, "(" + uri + ")",
                "resource was locked", token, principal);
            throw e;
        } catch (AclException e) {
            OperationLog.failure(OperationLog.CREATE_COLLECTION, "(" + uri + ")",
                "tried to set an illegal ACL: " + e.getMessage(), token, principal);
            throw new IllegalOperationException("tried to set an illegal ACL: "
                    + e.getMessage());
        }
    }

    public void copy(String token, String srcUri, String destUri, String depth,
        boolean overwrite, boolean preserveACL)
        throws IllegalOperationException, AuthorizationException, 
            AuthenticationException, FailedDependencyException, 
            ResourceOverwriteException, ResourceLockedException, 
            ResourceNotFoundException, ReadOnlyException, IOException {
        Principal principal = tokenManager.getPrincipal(token);

        if (!uriValidator.validateURI(srcUri)) {
            OperationLog.failure(OperationLog.COPY, "(" + srcUri + ", " + destUri + ")",
                "source not found", token, principal);
            throw new ResourceNotFoundException(srcUri);
        }

        if (!uriValidator.validateURI(destUri)) {
            OperationLog.failure(OperationLog.COPY, "(" + srcUri + ", " + destUri + ")",
                "invalid URI: '" + destUri + "'", token, principal);
            throw new IllegalOperationException("Invalid URI: '" + destUri +
                "'");
        }

        Resource src = dao.load(srcUri);

        if (src == null) {
            OperationLog.failure(OperationLog.COPY, "(" + srcUri + ", " + destUri + ")",
                "source not found", token, principal);
            throw new ResourceNotFoundException(srcUri);
        } else if (src.isCollection()) {
            authorizeRecursively(src, principal, PrivilegeDefinition.READ);
        } else {
            this.permissionsManager.authorize(src, principal, PrivilegeDefinition.READ);
        }

        String destPath = (destUri.charAt(destUri.length() - 1) == '/')
            ? destUri.substring(0, destUri.length() - 1) : destUri;

        if (destPath.equals(srcUri)) {
            OperationLog.failure(OperationLog.COPY, "(" + srcUri + ", " + destUri + ")",
                "trying to copy a resoure to itself", token, principal);
            throw new IllegalOperationException(
                "Cannot copy a resource to itself");
        }

        if (destPath.startsWith(srcUri)) {
            if (destPath.substring(srcUri.length(), destPath.length())
                            .startsWith("/")) {
                OperationLog.failure(OperationLog.COPY, "(" + srcUri + ", " + destUri + ")",
                    "trying to copy a resoure into itself", token, principal);
                throw new IllegalOperationException(
                    "Cannot copy a resource into itself");
            }
        }

        Resource dest = dao.load(destPath);

        if ((dest != null) && !overwrite) {
            throw new ResourceOverwriteException();
        }

        String destParent = destPath;

        if (destParent.lastIndexOf("/") == 0) {
            destParent = "/";
        } else {
            destParent = destPath.substring(0, destPath.lastIndexOf("/"));
        }

        Resource destCollection = dao.load(destParent);

        if ((destCollection == null) || !destCollection.isCollection()) {
            OperationLog.failure(OperationLog.COPY, "(" + srcUri + ", " + destUri + ")",
                "destination is either a document or does not exist", token,
                principal);
            throw new IllegalOperationException();
        }

        this.permissionsManager.authorize(destCollection, principal,
                                       PrivilegeDefinition.WRITE);

        this.resourceManager.lockAuthorize(destCollection, principal);

        if (readOnly(principal)) {
            OperationLog.failure(OperationLog.COPY, "(" + srcUri + ", " + destUri + ")",
                "read-only", token, principal);
            throw new ReadOnlyException();
        }

        if (dest != null) {
            if (dest.isCollection()) {
                this.resourceManager.lockAuthorizeRecursively(dest, principal);
            } else {
                this.resourceManager.lockAuthorize(dest, principal);
            }

            this.dao.delete(dest);
        }

        try {
            this.resourceManager.copy(
                principal, src, destPath, preserveACL, false);

            OperationLog.success(OperationLog.COPY, "(" + srcUri + ", " + destUri + ")",
                token, principal);

            org.vortikal.repository.Resource dto = 
                resourceManager.getResourceDTO(dao.load(destPath), principal);

            ResourceCreationEvent event = new ResourceCreationEvent(this, dto);

            context.publishEvent(event);
        } catch (AclException e) {
            OperationLog.failure(OperationLog.COPY, "(" + srcUri + ", " + destUri + ")",
                "tried to set an illegal ACL", token, principal);
            throw new IllegalOperationException(e.getMessage());
        }
    }

    public void move(String token, String srcUri, String destUri,
        boolean overwrite)
        throws IllegalOperationException, AuthorizationException, 
            AuthenticationException, FailedDependencyException, 
            ResourceOverwriteException, ResourceLockedException, 
            ResourceNotFoundException, ReadOnlyException, IOException {
        Principal principal = tokenManager.getPrincipal(token);

        if (!uriValidator.validateURI(srcUri)) {
            OperationLog.failure(OperationLog.MOVE, "(" + srcUri + ", " + destUri + ")",
                "source not found", token, principal);
            throw new ResourceNotFoundException(srcUri);
        }

        if (!uriValidator.validateURI(destUri)) {
            OperationLog.failure(OperationLog.MOVE, "(" + srcUri + ", " + destUri + ")",
                "invalid URI: '" + destUri + "'", token, principal);
            throw new IllegalOperationException("Invalid URI: '" + destUri +
                "'");
        }

        if ("/".equals(srcUri)) {
            OperationLog.failure(OperationLog.MOVE, "(" + srcUri + ", " + destUri + ")",
                "cannot move the root resource ('/')", token, principal);
            throw new IllegalOperationException(
                "Cannot move the root resource ('/')");
        }

        Resource src = dao.load(srcUri);

        if (src == null) {
            OperationLog.failure(OperationLog.MOVE, "(" + srcUri + ", " + destUri + ")",
                "source not found", token, principal);
            throw new ResourceNotFoundException(srcUri);
        }

        if (src.isCollection()) {
            this.resourceManager.lockAuthorizeRecursively(src, principal);
            authorizeRecursively(src, principal, PrivilegeDefinition.READ);
        } else {
            this.resourceManager.lockAuthorize(src, principal);
            this.permissionsManager.authorize(src, principal, PrivilegeDefinition.READ);
        }

        String srcParent = srcUri;

        if (srcParent.endsWith("/")) {
            srcParent = srcParent.substring(0, srcParent.length() - 1);
        }

        if (srcParent.lastIndexOf("/") == 0) {
            srcParent = "/";
        } else {
            srcParent = srcParent.substring(0, srcParent.lastIndexOf("/"));
        }

        Resource srcCollection = dao.load(srcParent);

        this.permissionsManager.authorize(srcCollection, principal, PrivilegeDefinition.WRITE);
        this.resourceManager.lockAuthorize(srcCollection, principal);

        String destPath = destUri;

        if (destPath.endsWith("/")) {
            destPath = destPath.substring(0, destPath.length() - 1);
        }

        if (destPath.equals(srcUri)) {
            OperationLog.failure(OperationLog.MOVE, "(" + srcUri + ", " + destUri + ")",
                "trying to move a resoure to itself", token, principal);
            throw new IllegalOperationException(
                "Cannot move a resource to itself");
        }

        if (destPath.startsWith(srcUri)) {
            if (destPath.substring(srcUri.length(), destPath.length())
                            .startsWith("/")) {
                OperationLog.failure(OperationLog.MOVE, "(" + srcUri + ", " + destUri + ")",
                    "trying to move a resoure into itself", token, principal);
                throw new IllegalOperationException(
                    "Cannot move a resource into itself");
            }
        }

        Resource dest = dao.load(destPath);

        if ((dest != null) && !overwrite) {
            OperationLog.failure(OperationLog.MOVE, "(" + srcUri + ", " + destUri + ")",
                "destination already existed, no overwrite", token, principal);
            throw new ResourceOverwriteException();
        } else if (dest != null) {
            if (dest.isCollection()) {
                this.resourceManager.lockAuthorizeRecursively(dest, principal);
            } else {
                    this.resourceManager.lockAuthorize(dest, principal);
            }
        }

        String destParent = destPath;

        if (destParent.lastIndexOf("/") == 0) {
            destParent = "/";
        } else {
            destParent = destPath.substring(0, destPath.lastIndexOf("/"));
        }

        Resource destCollection = dao.load(destParent);

        if ((destCollection == null) || !destCollection.isCollection()) {
            OperationLog.failure(OperationLog.MOVE, "(" + srcUri + ", " + destUri + ")",
                "destination collection either a document or does not exist",
                token, principal);
            throw new IllegalOperationException("Invalid destination resource");
        }

        this.permissionsManager.authorize(destCollection, principal, PrivilegeDefinition.WRITE);

        if (dest != null) {
            this.dao.delete(dest);
            context.publishEvent(new ResourceDeletionEvent(this, dest.getURI(), dest.getID(), 
                    dest.isCollection()));

        }

        try {
            if (readOnly(principal)) {
                OperationLog.failure(OperationLog.MOVE, "(" + srcUri + ", " + destUri + ")",
                    "read-only", token, principal);
                throw new ReadOnlyException();
            }

            this.resourceManager.copy(principal, src, destPath, true, true);

            this.dao.delete(src);

            OperationLog.success(OperationLog.MOVE, "(" + srcUri + ", " + destUri + ")",
                token, principal);

            Resource r = dao.load(destUri);
            ResourceDeletionEvent deletionEvent = new ResourceDeletionEvent(this, srcUri,
                   src.getID(), src.isCollection());

            context.publishEvent(deletionEvent);

            org.vortikal.repository.Resource dto = 
                resourceManager.getResourceDTO(r, principal);

            ResourceCreationEvent creationEvent = new ResourceCreationEvent(
                this, dto);

            context.publishEvent(creationEvent);
        } catch (AclException e) {
            OperationLog.failure(OperationLog.MOVE, "(" + srcUri + ", " + destUri + ")",
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

        if (!uriValidator.validateURI(uri)) {
            OperationLog.failure(OperationLog.DELETE, "(" + uri + ")", "resource not found",
                token, principal);
            throw new ResourceNotFoundException(uri);
        }

        if ("/".equals(uri)) {
            OperationLog.failure(OperationLog.DELETE, "(" + uri + ")",
                "cannot delete the root resource ('/')", token, principal);
            throw new IllegalOperationException(
                "Cannot delete the root resource ('/')");
        }

        Resource r = dao.load(uri);

        if (r == null) {
            OperationLog.failure(OperationLog.DELETE, "(" + uri + ")", "resource not found",
                token, principal);
            throw new ResourceNotFoundException(uri);
        }

        if (r.isCollection()) {
            this.resourceManager.lockAuthorizeRecursively(r, principal);
        } else {
            this.resourceManager.lockAuthorize(r, principal);
        }

        String parent = URIUtil.getParentURI(uri);

        Resource parentCollection = dao.load(parent);

        this.permissionsManager.authorize(parentCollection, principal, PrivilegeDefinition.WRITE);
        this.resourceManager.lockAuthorize(parentCollection, principal);

        if (readOnly(principal)) {
            OperationLog.failure(OperationLog.DELETE, "(" + uri + ")", "read-only", token,
                principal);
            throw new ReadOnlyException();
        }

        this.dao.delete(r);

        this.resourceManager.storeProperties(parentCollection, principal,
                               resourceManager.getResourceDTO(parentCollection, principal));

        OperationLog.success(OperationLog.DELETE, "(" + uri + ")", token, principal);

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
            OperationLog.failure(OperationLog.LOCK, "(" + uri + ")", "resource not found",
                token, principal);
            throw new ResourceNotFoundException(uri);
        }

        Resource r = dao.load(uri);

        if (r == null) {
            OperationLog.failure(OperationLog.LOCK, "(" + uri + ")", "resource not found",
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
            OperationLog.failure(OperationLog.LOCK, "(" + uri + ")", "read-only", token,
                principal);
            throw new ReadOnlyException();
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
            this.resourceManager.lockAuthorize(r, principal);

            String newLockToken = this.resourceManager.lockResource(r, principal, ownerInfo, depth,
                                      requestedTimeoutSeconds, (lockToken != null));

            OperationLog.success(OperationLog.LOCK, "(" + uri + ")", token, principal);

            return newLockToken;
        } catch (AuthorizationException e) {
            OperationLog.failure(OperationLog.LOCK, "(" + uri + ")", "permission denied",
                token, principal);
            throw e;
        } catch (AuthenticationException e) {
            OperationLog.failure(OperationLog.LOCK, "(" + uri + ")", "not authenticated",
                token, principal);
            throw e;
        } catch (ResourceLockedException e) {
            OperationLog.failure(OperationLog.LOCK, "(" + uri + ")", "already locked", token,
                principal);
            throw e;
        }
    }

    public void unlock(String token, String uri, String lockToken)
        throws ResourceNotFoundException, AuthorizationException, 
            AuthenticationException, ResourceLockedException, ReadOnlyException, 
            IOException {
        Principal principal = tokenManager.getPrincipal(token);

        if (!uriValidator.validateURI(uri)) {
            OperationLog.failure(OperationLog.UNLOCK, "(" + uri + ")", "resource not found",
                token, principal);
            throw new ResourceNotFoundException(uri);
        }

        Resource r = dao.load(uri);

        if (r == null) {
            OperationLog.failure(OperationLog.UNLOCK, "(" + uri + ")", "resource not found",
                token, principal);
            throw new ResourceNotFoundException(uri);
        }

        if (readOnly(principal)) {
            OperationLog.failure(OperationLog.UNLOCK, "(" + uri + ")", "read-only", token,
                principal);
            throw new ReadOnlyException();
        }

        try {
            this.permissionsManager.authorize(r, principal, PrivilegeDefinition.WRITE);
            // Root role has permission to remove all locks
            if (!this.roleManager.hasRole(principal.getQualifiedName(), RoleManager.ROOT)) {
                this.resourceManager.lockAuthorize(r, principal);
            }
            
            if (r.getLock() != null) {
                r.setLock(null);
                this.dao.store(r);
            }
            OperationLog.success(OperationLog.UNLOCK, "(" + uri + ")", token, principal);
        } catch (AuthorizationException e) {
            OperationLog.failure(OperationLog.UNLOCK, "(" + uri + ")", "permission denied",
                token, principal);
            throw e;
        } catch (AuthenticationException e) {
            OperationLog.failure(OperationLog.UNLOCK, "(" + uri + ")", "not authenticated",
                token, principal);
            throw e;
        } catch (ResourceLockedException e) {
            OperationLog.failure(OperationLog.UNLOCK, "(" + uri + ")", "resource locked",
                token, principal);
            throw e;
        }
    }

    public void store(String token, org.vortikal.repository.Resource resource)
        throws ResourceNotFoundException, AuthorizationException, 
            ResourceLockedException, AuthenticationException, 
            IllegalOperationException, ReadOnlyException, IOException {
        Principal principal = tokenManager.getPrincipal(token);

        if (resource == null) {
            // FIXME: should throw java.lang.IllegalArgumentException
            OperationLog.failure(OperationLog.STORE, "(null)", "resource null", token,
                principal);
            throw new IllegalOperationException("I cannot store nothing.");
        }

        String uri = resource.getURI();

        if (!uriValidator.validateURI(uri)) {
            OperationLog.failure(OperationLog.STORE, "(" + uri + ")", "invalid uri", token,
                principal);
            throw new IllegalOperationException("Invalid URI: " + uri);
        }

        Resource r = dao.load(uri);

        if (r == null) {
            OperationLog.failure(OperationLog.STORE, "(" + uri + ")", "invalid uri", token,
                principal);
            throw new ResourceNotFoundException(uri);
        }

        if (readOnly(principal)) {
            OperationLog.failure(OperationLog.STORE, "(" + uri + ")", "read-only", token,
                principal);
            throw new ReadOnlyException();
        }

        try {
            // Fix me: log authexceptions...
            this.permissionsManager.authorize(r, principal, PrivilegeDefinition.WRITE);
            this.resourceManager.lockAuthorize(r, principal);
            
            Resource original = (Resource) r.clone();

            this.resourceManager.storeProperties(r, principal, resource);
            OperationLog.success(OperationLog.STORE, "(" + uri + ")", token, principal);

            ResourceModificationEvent event = new ResourceModificationEvent(
                this, resourceManager.getResourceDTO(r, principal),
                resourceManager.getResourceDTO(original, principal));

            context.publishEvent(event);
        } catch (ResourceLockedException e) {
            OperationLog.failure(OperationLog.STORE, "(" + uri + ")", "resource locked",
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

        if (!uriValidator.validateURI(uri)) {
            OperationLog.failure(OperationLog.GET_INPUTSTREAM, "(" + uri + ")",
                "resource not found", token, principal);
            throw new ResourceNotFoundException(uri);
        }

        Resource r = dao.load(uri);

        if (r == null) {
            OperationLog.failure(OperationLog.GET_INPUTSTREAM, "(" + uri + ")",
                "resource not found", token, principal);
            throw new ResourceNotFoundException(uri);
        }

        if (r.isCollection()) {
            OperationLog.failure(OperationLog.GET_INPUTSTREAM, "(" + uri + ")",
                "resource is collection", token, principal);
            throw new IOException("resource is collection");
        }

        /* authorize for the right privilege */
        String privilege = (forProcessing)
            ? PrivilegeDefinition.CUSTOM_PRIVILEGE_READ_PROCESSED : PrivilegeDefinition.READ;

        this.permissionsManager.authorize(r, principal, privilege);

        InputStream inStream = this.dao.getInputStream(r);

        OperationLog.success(OperationLog.GET_INPUTSTREAM, "(" + uri + ")", token, principal);

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
            OperationLog.failure(OperationLog.STORE_CONTENT, "(" + uri + ")",
                "resource not found", token, principal);
            throw new ResourceNotFoundException(uri);
        }

        Resource r = dao.load(uri);

        if (r == null) {
            OperationLog.failure(OperationLog.STORE_CONTENT, "(" + uri + ")",
                "resource not found", token, principal);
            throw new ResourceNotFoundException(uri);
        }

        if (r.isCollection()) {
            OperationLog.failure(OperationLog.STORE_CONTENT, "(" + uri + ")",
                "resource is collection", token, principal);
            throw new IOException("Collections have no output stream");
        }

        if (readOnly(principal)) {
            OperationLog.failure(OperationLog.STORE_CONTENT, "(" + uri + ")", "read-only",
                token, principal);
            throw new ReadOnlyException();
        }


        OutputStream stream = null;

        try {
            this.permissionsManager.authorize(r, principal, PrivilegeDefinition.WRITE);
            this.resourceManager.lockAuthorize(r, principal);
        
            Resource original = (Resource) r.clone();

            stream = this.dao.getOutputStream(r);

            /* Write the input data to the resource: */
            byte[] buffer = new byte[1000];
            int n = 0;

            while ((n = byteStream.read(buffer, 0, 1000)) > 0) {
                stream.write(buffer, 0, n);
            }

            stream.flush();
            stream.close();
            byteStream.close();

            // Update timestamps:
            r.setContentLastModified(new Date());
            r.setContentModifiedBy(principal.getQualifiedName());
            this.dao.store(r);

            OperationLog.success(OperationLog.STORE_CONTENT, "(" + uri + ")", token, principal);

            ContentModificationEvent event = new ContentModificationEvent(
                this, resourceManager.getResourceDTO(r, principal),
                resourceManager.getResourceDTO(original, principal));

            context.publishEvent(event);
        } catch (ResourceLockedException e) {
            OperationLog.failure(OperationLog.STORE_CONTENT, "(" + uri + ")",
                "resource locked", token, principal);
            throw e;
        } catch (AuthorizationException e) {
            OperationLog.failure(OperationLog.STORE_CONTENT, "(" + uri + ")",
                "not authorized", token, principal);
            throw e;
        } catch (CloneNotSupportedException e) {
            throw new IOException("An internal error occurred: unable to " +
                "clone() resource: " + r);
        }
    }


    public org.vortikal.repository.Ace[] getACL(String token, String uri)
        throws ResourceNotFoundException, AuthorizationException, 
            AuthenticationException, IOException {
        Principal principal = tokenManager.getPrincipal(token);

        if (!uriValidator.validateURI(uri)) {
            OperationLog.failure(OperationLog.GET_ACL, "(" + uri + ")", "resource not found",
                token, principal);

            throw new ResourceNotFoundException(uri);
        }

        Resource r = dao.load(uri);

        if (r == null) {
            OperationLog.failure(OperationLog.GET_ACL, "(" + uri + ")", "resource not found",
                token, principal);
            throw new ResourceNotFoundException(uri);
        }

        try {
            this.permissionsManager.authorize(r, principal, PrivilegeDefinition.READ);

            String inheritedFrom = null;
            if (r.isInheritedACL()) {
                inheritedFrom = URIUtil.getParentURI(r.getURI());
            }
            
            org.vortikal.repository.Ace[] dtos = permissionsManager.toAceList(r.getACL(), inheritedFrom);

            OperationLog.success(OperationLog.GET_ACL, "(" + uri + ")", token, principal);

            return dtos;
        } catch (AuthorizationException e) {
            OperationLog.failure(OperationLog.GET_ACL, "(" + uri + ")", "permission denied",
                token, principal);
            throw e;
        } catch (AuthenticationException e) {
            OperationLog.failure(OperationLog.GET_ACL, "(" + uri + ")", "not authenticated",
                token, principal);
            throw e;
        }
    }

    public void storeACL(String token, String uri, Ace[] acl)
        throws ResourceNotFoundException, AuthorizationException, 
            AuthenticationException, IllegalOperationException, AclException, 
            ReadOnlyException, IOException {
        Principal principal = tokenManager.getPrincipal(token);

        if (!uriValidator.validateURI(uri)) {
            OperationLog.failure(OperationLog.STORE_ACL, "(" + uri + ")", "resource not found",
                token, principal);
            throw new ResourceNotFoundException(uri);
        }

        Resource r = dao.load(uri);

        if (r == null) {
            OperationLog.failure(OperationLog.STORE_ACL, "(" + uri + ")", "resource not found",
                token, principal);
            throw new ResourceNotFoundException(uri);
        }

        if (readOnly(principal)) {
            OperationLog.failure(OperationLog.STORE_ACL, "(" + uri + ")", "read-only",
                token, principal);
            throw new ReadOnlyException();
        }

        try {
            org.vortikal.repository.Resource originalResource = 
                resourceManager.getResourceDTO(r, principal);
            String inheritedFrom = null;
            if (r.isInheritedACL()) {
                inheritedFrom = URIUtil.getParentURI(r.getURI());
            }
            Ace[] originalACL = permissionsManager.toAceList(r.getACL(), inheritedFrom);

            this.permissionsManager.authorize(r, principal, PrivilegeDefinition.WRITE_ACL);
            this.resourceManager.lockAuthorize(r, principal);
            aclValidator.validateACL(acl);
            this.resourceManager.storeACL(r, principal, acl);
            OperationLog.success(OperationLog.STORE_ACL, "(" + uri + ")", token, principal);

            ACLModificationEvent event = new ACLModificationEvent(
                this, resourceManager.getResourceDTO(r,principal),
                originalResource, acl, originalACL);

            context.publishEvent(event);
        } catch (AuthorizationException e) {
            OperationLog.failure(OperationLog.STORE_ACL, "(" + uri + ")", "permission denied",
                token, principal);
            throw e;
        } catch (AuthenticationException e) {
            OperationLog.failure(OperationLog.STORE_ACL, "(" + uri + ")", "not authenticated",
                token, principal);
            throw e;
        } catch (IllegalOperationException e) {
            OperationLog.failure(OperationLog.STORE_ACL, "(" + uri + ")", "illegal operation",
                token, principal);
            throw e;
        } catch (AclException e) {
            OperationLog.failure(OperationLog.STORE_ACL, "(" + uri + ")",
                "illegal ACL operation", token, principal);
            throw e;
        }
    }

    /* Internal (not Repository API) methods: */
    public DataAccessor getDataAccessor() {
        return this.dao;
    }

    private void authorizeRecursively(Resource resource, Principal principal,
            String privilege) throws IOException, AuthenticationException,
            AuthorizationException {

        permissionsManager.authorize(resource, principal, privilege);
        if (resource.isCollection()) {
            String[] uris = this.dao.discoverACLs(resource);
            for (int i = 0; i < uris.length; i++) {
                Resource ancestor = this.dao.load(uris[i]);
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
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

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.vortikal.repository.Ace;
import org.vortikal.repository.AclException;
import org.vortikal.repository.AuthorizationException;
import org.vortikal.repository.FailedDependencyException;
import org.vortikal.repository.IllegalOperationException;
import org.vortikal.repository.Privilege;
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
import org.vortikal.security.InvalidPrincipalException;
import org.vortikal.security.Principal;
import org.vortikal.security.PrincipalManager;
import org.vortikal.security.TokenManager;
import org.vortikal.security.roles.RoleManager;
import org.vortikal.util.repository.URIUtil;



/**
 * A (still non-transactional) implementation of the
 * <code>org.vortikal.repository.Repository</code> interface.
 */
public class RepositoryImpl implements Repository, ApplicationContextAware,
                                       InitializingBean {

    public static final int MAX_URI_LENGTH = 1500;
    private boolean readOnly = false;
    private ApplicationContext context = null;

    private DataAccessor dao;
    private RoleManager roleManager = null;
    private PrincipalManager principalManager;
    private TokenManager tokenManager;
    private ResourceManager resourceManager;
    
    private String id;

    /* The CMS API implementation */
    public org.vortikal.repository.Configuration getConfiguration() {
        return new Configuration(this.readOnly);
    }

    public void setConfiguration(String token,
        org.vortikal.repository.Configuration c) {

        if (c == null) {
            throw new IllegalArgumentException("Configuration is null");
        }

        Principal principal = tokenManager.getPrincipal(token);

        if (!roleManager.hasRole(principal.getQualifiedName(), RoleManager.ROOT)) {
            throw new AuthorizationException();
        }

        this.readOnly = c.isReadOnly();
    }

    public org.vortikal.repository.Resource retrieve(String token, String uri,
        boolean forProcessing)
        throws ResourceNotFoundException, AuthorizationException, 
            AuthenticationException, IOException {
        Principal principal = tokenManager.getPrincipal(token);

        if (!validateURI(uri)) {
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
                ? org.vortikal.repository.Resource.CUSTOM_PRIVILEGE_READ_PROCESSED
                : PrivilegeDefinition.READ;

            this.resourceManager.authorize(r, principal, privilege);
            this.resourceManager.lockAuthorize(r, principal, privilege);

            OperationLog.success(OperationLog.RETRIEVE, "(" + uri + ")", token, principal);

            return resourceManager.getResourceDTO(r, principal);
        } catch (ResourceLockedException e) {
            OperationLog.failure(OperationLog.RETRIEVE, "(" + uri + ")", "resource locked",
                token, principal);
            throw new AuthorizationException();
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

    public org.vortikal.repository.Resource createDocument(String token,
        String uri)
        throws IllegalOperationException, AuthorizationException, 
            AuthenticationException, ResourceLockedException, ReadOnlyException, 
            IOException {
        Principal principal = tokenManager.getPrincipal(token);

        if (!validateURI(uri)) {
            OperationLog.failure(OperationLog.CREATE, "(" + uri + ")", "resource not found.",
                token, principal);
            throw new ResourceNotFoundException(uri);
        }

        if ("/".equals(uri)) {
            OperationLog.failure(OperationLog.CREATE, "(" + uri + ")",
                "illegal operation. " +
                "Cannot create the root resource ('/')", token, principal);
            throw new IllegalOperationException(
                "Cannot create the root resource ('/')");
        }

        Resource r = dao.load(uri);

        if (r != null) {
            OperationLog.failure(OperationLog.CREATE, "(" + uri + ")",
                "illegal operation. " + "Resource already existed", token,
                principal);
            throw new IllegalOperationException();
        }

        String parentURI = uri.substring(0, uri.lastIndexOf("/") + 1);

        if (parentURI.endsWith("/") && !parentURI.equals("/")) {
            parentURI = parentURI.substring(0, parentURI.length() - 1);
        }

        r = dao.load(parentURI);

        if ((r == null) || r instanceof Document) {
            OperationLog.failure(OperationLog.CREATE, "(" + uri + ")",
                "illegal operation: " +
                "either parent is null or parent is document", token, principal);
            throw new IllegalOperationException();
        }

        try {
            this.resourceManager.authorize(r, principal, PrivilegeDefinition.WRITE);
            this.resourceManager.lockAuthorize(r, principal, PrivilegeDefinition.WRITE);
        } catch (ResourceLockedException e) {
            OperationLog.failure(OperationLog.CREATE, "(" + uri + ")", "resource was locked",
                token, principal);
            throw e;
        } catch (AuthorizationException e) {
            OperationLog.failure(OperationLog.CREATE, "(" + uri + ")", "unauthorized", token,
                principal);
            throw e;
        }

        try {
            if (this.readOnly &&
                    !roleManager.hasRole(principal.getQualifiedName(), RoleManager.ROOT)) {
                OperationLog.failure(OperationLog.CREATE, "(" + uri + ")", "read-only", token,
                    principal);
                throw new ReadOnlyException();
            }

            Document doc = (Document) this.resourceManager.create((Collection) r, principal, uri);
            OperationLog.success(OperationLog.CREATE, "(" + uri + ")", token, principal);

            org.vortikal.repository.Resource dto = resourceManager.getResourceDTO(doc,principal);

            context.publishEvent(
                new ResourceCreationEvent(
                    this, dto));

            return dto;
        } catch (AclException e) {
            OperationLog.failure(OperationLog.CREATE, "(" + uri + ")",
                "tried to set an illegal ACL", token, principal);
            throw new IllegalOperationException(e.getMessage());
        }
    }

    public org.vortikal.repository.Resource createCollection(String token,
        String uri)
        throws AuthorizationException, AuthenticationException, 
            IllegalOperationException, ResourceLockedException, 
            ReadOnlyException, IOException {
        Principal principal = tokenManager.getPrincipal(token);

        if (!validateURI(uri)) {
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

        if ((r == null) || r instanceof Document) {
            /* Parent does not exist or is a document */
            OperationLog.failure(OperationLog.CREATE_COLLECTION, "(" + uri + ")",
                "illegal operation: " +
                "either parent is null or parent is document", token, principal);
            throw new IllegalOperationException();
        }

        try {
            this.resourceManager.authorize(r, principal, PrivilegeDefinition.WRITE);
            this.resourceManager.lockAuthorize(r, principal, PrivilegeDefinition.WRITE);

            if (this.readOnly &&
                    !roleManager.hasRole(principal.getQualifiedName(), RoleManager.ROOT)) {
                OperationLog.failure(OperationLog.CREATE_COLLECTION, "(" + uri + ")",
                    "read-only", token, principal);
                throw new ReadOnlyException();
            }

            Resource newCollection = this.resourceManager.createCollection(
                (Collection) r, principal, path);

            OperationLog.success(OperationLog.CREATE_COLLECTION, "(" + uri + ")", token,
                principal);

            org.vortikal.repository.Resource dto = resourceManager.getResourceDTO(newCollection, principal);

            context.publishEvent(
                new ResourceCreationEvent(this, dto));

            return dto;
        } catch (ResourceLockedException e) {
            OperationLog.failure(OperationLog.CREATE_COLLECTION, "(" + uri + ")",
                "resource was locked", token, principal);
            throw e;
        } catch (AclException e) {
            OperationLog.failure(OperationLog.CREATE_COLLECTION, "(" + uri + ")",
                "tried to set an illegal ACL", token, principal);
            throw new IllegalOperationException(e.getMessage());
        }
    }

    public void copy(String token, String srcUri, String destUri, String depth,
        boolean overwrite, boolean preserveACL)
        throws IllegalOperationException, AuthorizationException, 
            AuthenticationException, FailedDependencyException, 
            ResourceOverwriteException, ResourceLockedException, 
            ResourceNotFoundException, ReadOnlyException, IOException {
        Principal principal = tokenManager.getPrincipal(token);

        if (!validateURI(srcUri)) {
            OperationLog.failure(OperationLog.COPY, "(" + srcUri + ", " + destUri + ")",
                "source not found", token, principal);
            throw new ResourceNotFoundException(srcUri);
        }

        if (!validateURI(destUri)) {
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
        } else if (src instanceof Collection) {
            this.resourceManager.authorizeRecursively(src, principal,
                PrivilegeDefinition.READ);
        } else {
            this.resourceManager.authorize(src, principal, PrivilegeDefinition.READ);
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

        if ((destCollection == null) || destCollection instanceof Document) {
            OperationLog.failure(OperationLog.COPY, "(" + srcUri + ", " + destUri + ")",
                "destination is either a document or does not exist", token,
                principal);
            throw new IllegalOperationException();
        }

        this.resourceManager.authorize(destCollection, principal,
                                       PrivilegeDefinition.WRITE);

        this.resourceManager.lockAuthorize(destCollection, principal, PrivilegeDefinition.WRITE);

        if (this.readOnly &&
                !roleManager.hasRole(principal.getQualifiedName(), RoleManager.ROOT)) {
            OperationLog.failure(OperationLog.COPY, "(" + srcUri + ", " + destUri + ")",
                "read-only", token, principal);
            throw new ReadOnlyException();
        }

        if (dest != null) {
            if (dest instanceof Collection) {
                this.resourceManager.lockAuthorizeRecursively((Collection) dest, principal,
                                                              PrivilegeDefinition.WRITE);
            } else {
                this.resourceManager.lockAuthorize(dest, principal, PrivilegeDefinition.WRITE);
            }

            this.resourceManager.delete(dest, principal);
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

        if (!validateURI(srcUri)) {
            OperationLog.failure(OperationLog.MOVE, "(" + srcUri + ", " + destUri + ")",
                "source not found", token, principal);
            throw new ResourceNotFoundException(srcUri);
        }

        if (!validateURI(destUri)) {
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

        if (src instanceof Collection) {
            this.resourceManager.lockAuthorizeRecursively((Collection) src, principal,
                                                          PrivilegeDefinition.WRITE);
        } else {
            this.resourceManager.lockAuthorize(src, principal, PrivilegeDefinition.WRITE);
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

        this.resourceManager.authorize(srcCollection, principal, PrivilegeDefinition.WRITE);
        this.resourceManager.lockAuthorize(srcCollection, principal, PrivilegeDefinition.WRITE);

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
            if (dest instanceof Collection) {
                this.resourceManager.lockAuthorizeRecursively((Collection) dest, principal,
                                                               PrivilegeDefinition.WRITE);
            } else {
                    this.resourceManager.lockAuthorize(dest, principal, PrivilegeDefinition.WRITE);
            }
        }

        String destParent = destPath;

        if (destParent.lastIndexOf("/") == 0) {
            destParent = "/";
        } else {
            destParent = destPath.substring(0, destPath.lastIndexOf("/"));
        }

        Resource destCollection = dao.load(destParent);

        if ((destCollection == null) || destCollection instanceof Document) {
            OperationLog.failure(OperationLog.MOVE, "(" + srcUri + ", " + destUri + ")",
                "destination collection either a document or does not exist",
                token, principal);
            throw new IllegalOperationException("Invalid destination resource");
        }

        this.resourceManager.authorize(destCollection, principal, PrivilegeDefinition.WRITE);

        if (dest != null) {
            this.resourceManager.delete(dest, principal);
            context.publishEvent(new ResourceDeletionEvent(this, dest.getURI(), dest.getID(), 
                    dest instanceof Collection));

        }

        try {
            if (this.readOnly &&
                    !roleManager.hasRole(principal.getQualifiedName(), RoleManager.ROOT)) {
                OperationLog.failure(OperationLog.MOVE, "(" + srcUri + ", " + destUri + ")",
                    "read-only", token, principal);
                throw new ReadOnlyException();
            }

            this.resourceManager.copy(principal, src, destPath, true, true);

            this.resourceManager.delete(src, principal);

            OperationLog.success(OperationLog.MOVE, "(" + srcUri + ", " + destUri + ")",
                token, principal);

            Resource r = dao.load(destUri);
            ResourceDeletionEvent deletionEvent = new ResourceDeletionEvent(this, srcUri,
                   src.getID(), src instanceof Collection);

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

        if (!validateURI(uri)) {
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

        if (r instanceof Collection) {
            this.resourceManager.lockAuthorizeRecursively((Collection) r, principal,
                                                          PrivilegeDefinition.WRITE);
        } else {
            this.resourceManager.lockAuthorize(r, principal, PrivilegeDefinition.WRITE);
        }

        String parent = new String(uri);

        if (parent.endsWith("/")) {
            parent = parent.substring(0, parent.length() - 1);
        }

        if (parent.lastIndexOf("/") == 0) {
            parent = "/";
        } else {
            parent = uri.substring(0, uri.lastIndexOf("/"));
        }

        Resource parentCollection = dao.load(parent);

        this.resourceManager.authorize(parentCollection, principal, PrivilegeDefinition.WRITE);
        this.resourceManager.lockAuthorize(parentCollection, principal, PrivilegeDefinition.WRITE);

        if (this.readOnly &&
                !roleManager.hasRole(principal.getQualifiedName(), RoleManager.ROOT)) {
            OperationLog.failure(OperationLog.DELETE, "(" + uri + ")", "read-only", token,
                principal);
            throw new ReadOnlyException();
        }

        this.resourceManager.delete(r, principal);

        this.resourceManager.storeProperties(parentCollection, principal,
                               resourceManager.getResourceDTO(parentCollection, principal));

        OperationLog.success(OperationLog.DELETE, "(" + uri + ")", token, principal);

        ResourceDeletionEvent event = new ResourceDeletionEvent(this, uri, r.getID(), 
                                                                r instanceof Collection);

        context.publishEvent(event);
    }

    public boolean exists(String token, String uri)
        throws AuthorizationException, AuthenticationException, IOException {
        Principal principal = tokenManager.getPrincipal(token);

        if (!validateURI(uri)) {
            OperationLog.info(OperationLog.EXISTS, "(" + uri + "): false", token, principal);

            return false;
        }

        Resource r = dao.load(uri);

        if (r == null) {
            OperationLog.info(OperationLog.EXISTS, "(" + uri + "): false", token, principal);

            return false;
        }

        OperationLog.info(OperationLog.EXISTS, "(" + uri + "): true", token, principal);

        return true;
    }

    public String lock(String token, String uri, String lockType,
            String ownerInfo, String depth, int requestedTimeoutSeconds, String lockToken)
        throws ResourceNotFoundException, AuthorizationException, 
            AuthenticationException, FailedDependencyException, 
            ResourceLockedException, IllegalOperationException, 
            ReadOnlyException, IOException {
        Principal principal = tokenManager.getPrincipal(token);

        if (!validateURI(uri)) {
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

        if (this.readOnly &&
                !roleManager.hasRole(principal.getQualifiedName(), RoleManager.ROOT)) {
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
            this.resourceManager.lockAuthorize(r, principal, PrivilegeDefinition.WRITE);

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

        if (!validateURI(uri)) {
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

        if (this.readOnly &&
                !roleManager.hasRole(principal.getQualifiedName(), RoleManager.ROOT)) {
            OperationLog.failure(OperationLog.UNLOCK, "(" + uri + ")", "read-only", token,
                principal);
            throw new ReadOnlyException();
        }

        try {
            this.resourceManager.unlockResource(r, principal, lockToken);
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

    public org.vortikal.repository.Resource[] listChildren(String token,
        String uri, boolean forProcessing)
        throws ResourceNotFoundException, AuthorizationException, 
            AuthenticationException, IOException {
        Principal principal = tokenManager.getPrincipal(token);

        if (!validateURI(uri)) {
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

        if (r instanceof Document) {
            OperationLog.failure(OperationLog.LIST_CHILDREN, "(" + uri + ")",
                "uri is document", token, principal);

            return null;
        }

        try {
            /* authorize for the right privilege: */
            String privilege = (forProcessing)
                ? org.vortikal.repository.Resource.CUSTOM_PRIVILEGE_READ_PROCESSED
                : PrivilegeDefinition.READ;

            this.resourceManager.authorize(r, principal, privilege);
            this.resourceManager.lockAuthorize(r, principal, privilege);

            Resource[] list = this.dao.loadChildren(((Collection) r));
            org.vortikal.repository.Resource[] children = new org.vortikal.repository.Resource[list.length];

            for (int i = 0; i < list.length; i++) {
                children[i] = resourceManager.getResourceDTO(list[i], principal);
            }

            OperationLog.success(OperationLog.LIST_CHILDREN, "(" + uri + ")", token, principal);

            return children;
        } catch (ResourceLockedException e) {
            OperationLog.failure(OperationLog.LIST_CHILDREN, "(" + uri + ")",
                "permission denied", token, principal);
            throw new AuthorizationException();
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

        if (!validateURI(uri)) {
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

        if (this.readOnly &&
                !roleManager.hasRole(principal.getQualifiedName(), RoleManager.ROOT)) {
            OperationLog.failure(OperationLog.STORE, "(" + uri + ")", "read-only", token,
                principal);
            throw new ReadOnlyException();
        }

        try {
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

        if (!validateURI(uri)) {
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

        if (r instanceof Collection) {
            OperationLog.failure(OperationLog.GET_INPUTSTREAM, "(" + uri + ")",
                "resource is collection", token, principal);
            throw new IOException("resource is collection");
        }

        /* authorize for the right privilege */
        String privilege = (forProcessing)
            ? org.vortikal.repository.Resource.CUSTOM_PRIVILEGE_READ_PROCESSED : PrivilegeDefinition.READ;

        InputStream inStream = this.resourceManager.getResourceInputStream(
            ((Document) r), principal, privilege);

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

        if (!validateURI(uri)) {
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

        if (r instanceof Collection) {
            OperationLog.failure(OperationLog.STORE_CONTENT, "(" + uri + ")",
                "resource is collection", token, principal);
            throw new IOException("Collections have no output stream");
        }

        if (this.readOnly &&
                !roleManager.hasRole(principal.getQualifiedName(), RoleManager.ROOT)) {
            OperationLog.failure(OperationLog.STORE_CONTENT, "(" + uri + ")", "read-only",
                token, principal);
            throw new ReadOnlyException();
        }

        OutputStream stream = null;

        try {
            Resource original = (Resource) r.clone();

            stream = this.resourceManager.getResourceOutputStream(((Document) r), principal);

            /* Write the input data to the resource: */
            byte[] buffer = new byte[1000];
            int n = 0;

            while ((n = byteStream.read(buffer, 0, 1000)) > 0) {
                stream.write(buffer, 0, n);
            }

            stream.flush();
            stream.close();
            byteStream.close();

            OperationLog.success(OperationLog.STORE_CONTENT, "(" + uri + ")", token, principal);

            ContentModificationEvent event = new ContentModificationEvent(
                this, resourceManager.getResourceDTO(r, principal),
                resourceManager.getResourceDTO(original, principal));

            context.publishEvent(event);
        } catch (ResourceLockedException e) {
            OperationLog.failure(OperationLog.STORE_CONTENT, "(" + uri + ")",
                "resource locked", token, principal);
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

        if (!validateURI(uri)) {
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
            this.resourceManager.authorize(r, principal, PrivilegeDefinition.READ);

            String inheritedFrom = null;
            if (r.isInheritedACL()) {
                inheritedFrom = URIUtil.getParentURI(r.getURI());
            }
            
            org.vortikal.repository.Ace[] dtos = resourceManager.toAceList(r.getACL(), inheritedFrom);

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

    public void storeACL(String token, String uri,
        org.vortikal.repository.Ace[] acl)
        throws ResourceNotFoundException, AuthorizationException, 
            AuthenticationException, IllegalOperationException, AclException, 
            ReadOnlyException, IOException {
        Principal principal = tokenManager.getPrincipal(token);

        if (!validateURI(uri)) {
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

        try {
            this.resourceManager.authorize(r, principal, PrivilegeDefinition.WRITE_ACL);

            if (this.readOnly &&
                    !roleManager.hasRole(principal.getQualifiedName(), RoleManager.ROOT)) {
                OperationLog.failure(OperationLog.STORE_ACL, "(" + uri + ")", "read-only",
                    token, principal);
                throw new ReadOnlyException();
            }

            Resource original = (Resource) r.clone();
            ACL origACL = original.getACL();
            boolean wasInheritedACL = original.isInheritedACL();

            this.resourceManager.authorize(r, principal, PrivilegeDefinition.WRITE_ACL);
            validateACL(acl);
            this.resourceManager.storeACL(r, principal, acl);
            OperationLog.success(OperationLog.STORE_ACL, "(" + uri + ")", token, principal);

            String inheritedFrom = null;
            if (r.isInheritedACL()) {
                inheritedFrom = URIUtil.getParentURI(r.getURI());
            }

            ACLModificationEvent event = new ACLModificationEvent(
                this, resourceManager.getResourceDTO(r,principal),
                resourceManager.getResourceDTO(original, principal),
                    resourceManager.toAceList(r.getACL(), inheritedFrom), 
                    resourceManager.toAceList(origACL, inheritedFrom), wasInheritedACL);

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
        } catch (CloneNotSupportedException e) {
            throw new IOException("An internal error occurred: unable to " +
                "clone() resource: " + r);
        }
    }

    /* Internal (not Repository API) methods: */
    public DataAccessor getDataAccessor() {
        return this.dao;
    }

    private boolean validateURI(String uri) {
        if (uri == null) {
            return false;
        }

        if (uri.trim().equals("")) {
            return false;
        }

        if (!uri.startsWith("/")) {
            return false;
        }

        if (uri.indexOf("//") != -1) {
            return false;
        }

        if (uri.length() >= MAX_URI_LENGTH) {
            return false;
        }

        if (uri.indexOf("/../") != -1) {
            return false;
        }

        if (uri.endsWith("/..")) {
            return false;
        }

        if (uri.endsWith("/.")) {
            return false;
        }

        if (uri.indexOf("%") != -1) {
            return false;
        }

        //         /* SQL Strings: */
        //         if (uri.indexOf("'") != -1) {
        //             return false;
        //         }
        //         /* SQL comments: */
        //         if (uri.indexOf("--") != -1) {
        //             return false;
        //         }
        //         /* SQL (C-style) comments: */
        //         if (uri.indexOf("/*") != -1 || uri.indexOf("*/") != -1) {
        //             return false;
        //         }
        return true;
    }
    
    /**
     * Checks the validity of an ACL.
     *
     * @param aceList an <code>Ace[]</code> value
     * @exception AclException if an error occurs
     * @exception IllegalOperationException if an error occurs
     * @exception IOException if an error occurs
     */
    public void validateACL(Ace[] aceList)
        throws AclException, IllegalOperationException {
        /*
         * Enforce ((dav:owner (dav:read dav:write dav:write-acl))
         */
        if (!containsUserPrivilege(aceList, PrivilegeDefinition.WRITE,
                    "dav:owner")) {
            throw new IllegalOperationException(
                "Owner must be granted write privilege in ACL.");
        }

        if (!containsUserPrivilege(aceList, PrivilegeDefinition.READ,
                    "dav:owner")) {
            throw new IllegalOperationException(
                "Owner must be granted read privilege in ACL.");
        }

        if (!containsUserPrivilege(aceList, PrivilegeDefinition.WRITE_ACL,
                    "dav:owner")) {
            throw new IllegalOperationException(
                "Owner must be granted write-acl privilege in ACL.");
        }

        boolean inheritance = aceList[0].getInheritedFrom() != null;

        /*
         * Walk trough the ACL, for every ACE, enforce that:
         * 1) Privileges are never denied (only granted)
         * 2) Either every ACE is inherited or none
         * 3) Every principal is valid
         * 4) Every privilege has a supported namespace and name
         */
        for (int i = 0; i < aceList.length; i++) {
            Ace ace = aceList[i];

            if (!ace.isGranted()) {
                throw new AclException(AclException.GRANT_ONLY,
                    "Privileges may only be granted, not denied.");
            }

            if ((ace.getInheritedFrom() != null) != inheritance) {
                throw new IllegalOperationException(
                    "Either every ACE must be inherited from a resource, " +
                    "or none.");
            }

            org.vortikal.repository.ACLPrincipal principal = ace.getPrincipal();

            if (principal.getType() == org.vortikal.repository.ACLPrincipal.TYPE_URL) {
                boolean validPrincipal = false;

                if (principal.isUser()) {
                    Principal p = null;
                    try {
                        p = principalManager.getPrincipal(principal.getURL());
                    } catch (InvalidPrincipalException e) {
                        throw new AclException("Invalid principal '" 
                                + principal.getURL() + "' in ACL");
                    }
                    validPrincipal = principalManager.validatePrincipal(p);
                } else {
                    validPrincipal = principalManager.validateGroup(principal.getURL());
                }

                if (!validPrincipal) {
                    throw new AclException(AclException.RECOGNIZED_PRINCIPAL,
                        "Unknown principal: " + principal.getURL());
                }
            } else {
                if ((principal.getType() != org.vortikal.repository.ACLPrincipal.TYPE_ALL) &&
                        (principal.getType() != org.vortikal.repository.ACLPrincipal.TYPE_OWNER) &&
                        (principal.getType() != org.vortikal.repository.ACLPrincipal.TYPE_AUTHENTICATED)) {
                    throw new AclException(AclException.RECOGNIZED_PRINCIPAL,
                        "Allowed principal types are " +
                        "either TYPE_ALL, TYPE_OWNER " + "OR  TYPE_URL.");
                }
            }

            Privilege[] privileges = ace.getPrivileges();

            for (int j = 0; j < privileges.length; j++) {
                Privilege privilege = privileges[j];

                if (privilege.getNamespace().equals(PrivilegeDefinition.STANDARD_NAMESPACE)) {
                    if (!(privilege.getName().equals(PrivilegeDefinition.WRITE) ||
                            privilege.getName().equals(PrivilegeDefinition.READ) ||
                            privilege.getName().equals(PrivilegeDefinition.WRITE_ACL))) {
                        throw new AclException(AclException.NOT_SUPPORTED_PRIVILEGE,
                            "Unsupported privilege name: " +
                            privilege.getName());
                    }
                } else if (privilege.getNamespace().equals(org.vortikal.repository.Resource.CUSTOM_NAMESPACE)) {
                    if (!(privilege.getName().equals(org.vortikal.repository.Resource.CUSTOM_PRIVILEGE_READ_PROCESSED))) {
                        throw new AclException(AclException.NOT_SUPPORTED_PRIVILEGE,
                            "Unsupported privilege name: " +
                            privilege.getName());
                    }
                } else {
                    throw new AclException(AclException.NOT_SUPPORTED_PRIVILEGE,
                        "Unsupported privilege namespace: " +
                        privilege.getNamespace());
                }
            }
        }
    }

    /**
     * Checks if an ACL grants a given privilege to a given principal.
     *
     */
    protected static boolean containsUserPrivilege(Ace[] aceList,
        String privilegeName, String principalURL) {
        for (int i = 0; i < aceList.length; i++) {
            Ace ace = aceList[i];

            Privilege[] privileges = ace.getPrivileges();

            for (int j = 0; j < privileges.length; j++) {
                Privilege priv = privileges[j];

                org.vortikal.repository.ACLPrincipal principal = ace.getPrincipal();

                if ((principal.getType() == org.vortikal.repository.ACLPrincipal.TYPE_URL) &&
                        principal.getURL().equals(principalURL) &&
                        priv.getName().equals(privilegeName)) {
                    return true;
                } else if (principal.getType() == org.vortikal.repository.ACLPrincipal.TYPE_OWNER) {
                    return true;
                } else if (principal.getType() == org.vortikal.repository.ACLPrincipal.TYPE_ALL) {
                    if (priv.getName().equals(privilegeName)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }



    public void setPrincipalManager(PrincipalManager principalManager) {
        this.principalManager = principalManager;
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
        if (this.principalManager == null) {
            throw new BeanInitializationException(
                "Bean property 'principalManager' must be set");
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

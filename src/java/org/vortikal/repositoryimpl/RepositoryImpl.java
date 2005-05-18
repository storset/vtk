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
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
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
import org.vortikal.security.PrincipalManager;
import org.vortikal.security.TokenManager;
import org.vortikal.security.roles.RoleManager;



/**
 * A (still non-transactional) implementation of the
 * <code>org.vortikal.repository.Repository</code> interface.
 */
public class RepositoryImpl implements Repository, ApplicationContextAware,
                                       InitializingBean {
    private static Log logger = LogFactory.getLog(RepositoryImpl.class);
    public static final int MAX_URI_LENGTH = 1500;
    private boolean readOnly = false;
    private boolean cleanupLocksWhenReadOnly = false;
    private ApplicationContext context = null;

    private DataAccessor dao;
    private RoleManager roleManager = null;
    private PrincipalManager principalManager;
    //private PrincipalStore principalStore;
    private TokenManager tokenManager;
    private String id;

    public void setApplicationContext(ApplicationContext context) {
        this.context = context;
    }

    /* The CMS API implementation */
    public org.vortikal.repository.Configuration getConfiguration()
        throws IOException {
        return new Configuration(this.readOnly);
    }

    public void setConfiguration(String token,
        org.vortikal.repository.Configuration c)
        throws IOException {

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
            OperationLog.failure("retrieve(" + uri + ")", "resource not found",
                token, principal);

            throw new ResourceNotFoundException(uri);
        }

        Resource r = dao.load(uri);

        if (r == null) {
            OperationLog.failure("retrieve(" + uri + ")", "resource not found",
                token, principal);
            throw new ResourceNotFoundException(uri);
        }

        try {
            /* authorize for the right privilege */
            String privilege = (forProcessing)
                ? Resource.CUSTOM_PRIVILEGE_READ_PROCESSED
                : PrivilegeDefinition.READ;

            r.authorize(principal, privilege, roleManager);
            r.lockAuthorize(principal, privilege, roleManager);

            OperationLog.success("retrieve(" + uri + ")", token, principal);

            return r.getResourceDTO(principal, principalManager, roleManager);
        } catch (ResourceLockedException e) {
            OperationLog.failure("retrieve(" + uri + ")", "resource locked",
                token, principal);
            throw new AuthorizationException();
        } catch (AuthorizationException e) {
            OperationLog.failure("retrieve(" + uri + ")", "not authorized",
                token, principal);
            throw e;
        } catch (AuthenticationException e) {
            OperationLog.failure("retrieve(" + uri + ")", "not authenticated",
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
            OperationLog.failure("create(" + uri + ")", "resource not found.",
                token, principal);
            throw new ResourceNotFoundException(uri);
        }

        if ("/".equals(uri)) {
            OperationLog.failure("create(" + uri + ")",
                "illegal operation. " +
                "Cannot create the root resource ('/')", token, principal);
            throw new IllegalOperationException(
                "Cannot create the root resource ('/')");
        }

        Resource r = dao.load(uri);

        if (r != null) {
            OperationLog.failure("create(" + uri + ")",
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
            OperationLog.failure("create(" + uri + ")",
                "illegal operation: " +
                "either parent is null or parent is document", token, principal);
            throw new IllegalOperationException();
        }

        try {
            ((Collection) r).authorize(principal, PrivilegeDefinition.WRITE,
                roleManager);
            ((Collection) r).lockAuthorize(principal,
                PrivilegeDefinition.WRITE, roleManager);
        } catch (ResourceLockedException e) {
            OperationLog.failure("create(" + uri + ")", "resource was locked",
                token, principal);
            throw e;
        } catch (AuthorizationException e) {
            OperationLog.failure("create(" + uri + ")", "unauthorized", token,
                principal);
            throw e;
        }

        try {
            if (this.readOnly &&
                    !roleManager.hasRole(principal.getQualifiedName(), RoleManager.ROOT)) {
                OperationLog.failure("create(" + uri + ")", "read-only", token,
                    principal);
                throw new ReadOnlyException();
            }

            Document doc = (Document) ((Collection) r).create(principal, uri,
                    roleManager);

            OperationLog.success("create(" + uri + ")", token, principal);

            context.publishEvent(
                new ResourceCreationEvent(
                    this, doc.getResourceDTO(principal, principalManager, roleManager)));

            return doc.getResourceDTO(principal, principalManager, roleManager);
        } catch (AclException e) {
            OperationLog.failure("create(" + uri + ")",
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
            OperationLog.failure("createCollection(" + uri + ")",
                "illegal operation: " + "Invalid URI: '" + uri + "'", token,
                principal);
            throw new IllegalOperationException("Invalid URI: '" + uri + "'");
        }

        if ("/".equals(uri)) {
            OperationLog.failure("createCollection(" + uri + ")",
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
            OperationLog.failure("createCollection(" + uri + ")",
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
            OperationLog.failure("createCollection(" + uri + ")",
                "illegal operation: " +
                "either parent is null or parent is document", token, principal);
            throw new IllegalOperationException();
        }

        try {
            r.authorize(principal, PrivilegeDefinition.WRITE, roleManager);
            r.lockAuthorize(principal, PrivilegeDefinition.WRITE, roleManager);

            if (this.readOnly &&
                    !roleManager.hasRole(principal.getQualifiedName(), RoleManager.ROOT)) {
                OperationLog.failure("createCollection(" + uri + ")",
                    "read-only", token, principal);
                throw new ReadOnlyException();
            }

            Resource newCollection = ((Collection) r).createCollection(principal,
                    path, roleManager);

            OperationLog.success("createCollection(" + uri + ")", token,
                principal);
            context.publishEvent(
                new ResourceCreationEvent(this, newCollection.getResourceDTO(principal, principalManager, roleManager)));

            return newCollection.getResourceDTO(principal, principalManager, roleManager);
        } catch (ResourceLockedException e) {
            OperationLog.failure("createCollection(" + uri + ")",
                "resource was locked", token, principal);
            throw e;
        } catch (AclException e) {
            OperationLog.failure("createCollection(" + uri + ")",
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
            OperationLog.failure("copy(" + srcUri + ", " + destUri + ")",
                "source not found", token, principal);
            throw new ResourceNotFoundException(srcUri);
        }

        if (!validateURI(destUri)) {
            OperationLog.failure("copy(" + srcUri + ", " + destUri + ")",
                "invalid URI: '" + destUri + "'", token, principal);
            throw new IllegalOperationException("Invalid URI: '" + destUri +
                "'");
        }

        Resource src = dao.load(srcUri);

        if (src == null) {
            OperationLog.failure("copy(" + srcUri + ", " + destUri + ")",
                "source not found", token, principal);
            throw new ResourceNotFoundException(srcUri);
        } else if (src instanceof Collection) {
            ((Collection) src).recursiveAuthorize(principal,
                PrivilegeDefinition.READ, roleManager);
        } else {
            src.authorize(principal, PrivilegeDefinition.READ, roleManager);
        }

        String destPath = (destUri.charAt(destUri.length() - 1) == '/')
            ? destUri.substring(0, destUri.length() - 1) : destUri;

        if (destPath.equals(srcUri)) {
            OperationLog.failure("copy(" + srcUri + ", " + destUri + ")",
                "trying to copy a resoure to itself", token, principal);
            throw new IllegalOperationException(
                "Cannot copy a resource to itself");
        }

        if (destPath.startsWith(srcUri)) {
            if (destPath.substring(srcUri.length(), destPath.length())
                            .startsWith("/")) {
                OperationLog.failure("copy(" + srcUri + ", " + destUri + ")",
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
            OperationLog.failure("copy(" + srcUri + ", " + destUri + ")",
                "destination is either a document or does not exist", token,
                principal);
            throw new IllegalOperationException();
        }

        ((Collection) destCollection).authorize(principal,
            PrivilegeDefinition.WRITE, roleManager);

        destCollection.lockAuthorize(principal, PrivilegeDefinition.WRITE,
            roleManager);

        if (this.readOnly &&
                !roleManager.hasRole(principal.getQualifiedName(), RoleManager.ROOT)) {
            OperationLog.failure("copy(" + srcUri + ", " + destUri + ")",
                "read-only", token, principal);
            throw new ReadOnlyException();
        }

        if (dest != null) {
            if (dest instanceof Collection) {
                ((Collection) dest).recursiveLockAuthorize(principal,
                    PrivilegeDefinition.WRITE, roleManager);
            } else {
                dest.lockAuthorize(principal, PrivilegeDefinition.WRITE,
                    roleManager);
            }

            dest.delete(principal, roleManager);
        }

        try {
            ((Collection) destCollection).copy(
                principal, src, destPath, preserveACL, false, principalManager, roleManager);

            // FIXME: Should go something like this instead (for speed):
            //             String[] uris = dao.listSubTree(srcUri);
            //             Resource[] resources = dao.load(uris);
            //             for (int i = 0; i < resources.length; i++) {
            //                 Resource copy = new Resource();
            //                 copy.store();
            //             }
            OperationLog.success("copy(" + srcUri + ", " + destUri + ")",
                token, principal);

            ResourceCreationEvent event = new ResourceCreationEvent(
                this, dao.load(destPath).getResourceDTO(principal, principalManager, roleManager));

            context.publishEvent(event);
        } catch (AclException e) {
            OperationLog.failure("copy(" + srcUri + ", " + destUri + ")",
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
            OperationLog.failure("move(" + srcUri + ", " + destUri + ")",
                "source not found", token, principal);
            throw new ResourceNotFoundException(srcUri);
        }

        if (!validateURI(destUri)) {
            OperationLog.failure("move(" + srcUri + ", " + destUri + ")",
                "invalid URI: '" + destUri + "'", token, principal);
            throw new IllegalOperationException("Invalid URI: '" + destUri +
                "'");
        }

        if ("/".equals(srcUri)) {
            OperationLog.failure("move(" + srcUri + ", " + destUri + ")",
                "cannot move the root resource ('/')", token, principal);
            throw new IllegalOperationException(
                "Cannot move the root resource ('/')");
        }

        Resource src = dao.load(srcUri);

        if (src == null) {
            OperationLog.failure("move(" + srcUri + ", " + destUri + ")",
                "source not found", token, principal);
            throw new ResourceNotFoundException(srcUri);
        }

        if (src instanceof Collection) {
            ((Collection) src).recursiveLockAuthorize(principal,
                PrivilegeDefinition.WRITE, roleManager);
        } else {
            src.lockAuthorize(principal, PrivilegeDefinition.WRITE, roleManager);
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

        srcCollection.authorize(principal, PrivilegeDefinition.WRITE,
            roleManager);
        srcCollection.lockAuthorize(principal, PrivilegeDefinition.WRITE,
            roleManager);

        String destPath = destUri;

        if (destPath.endsWith("/")) {
            destPath = destPath.substring(0, destPath.length() - 1);
        }

        if (destPath.equals(srcUri)) {
            OperationLog.failure("move(" + srcUri + ", " + destUri + ")",
                "trying to move a resoure to itself", token, principal);
            throw new IllegalOperationException(
                "Cannot move a resource to itself");
        }

        if (destPath.startsWith(srcUri)) {
            if (destPath.substring(srcUri.length(), destPath.length())
                            .startsWith("/")) {
                OperationLog.failure("move(" + srcUri + ", " + destUri + ")",
                    "trying to move a resoure into itself", token, principal);
                throw new IllegalOperationException(
                    "Cannot move a resource into itself");
            }
        }

        Resource dest = dao.load(destPath);

        if ((dest != null) && !overwrite) {
            OperationLog.failure("move(" + srcUri + ", " + destUri + ")",
                "destination already existed, no overwrite", token, principal);
            throw new ResourceOverwriteException();
        } else if (dest != null) {
            if (dest instanceof Collection) {
                ((Collection) dest).recursiveLockAuthorize(principal,
                    PrivilegeDefinition.WRITE, roleManager);
            } else {
                dest.lockAuthorize(principal, PrivilegeDefinition.WRITE,
                    roleManager);
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
            OperationLog.failure("move(" + srcUri + ", " + destUri + ")",
                "destination collection either a document or does not exist",
                token, principal);
            throw new IllegalOperationException("Invalid destination resource");
        }

        destCollection.authorize(principal, PrivilegeDefinition.WRITE,
            roleManager);

        if (dest != null) {
            dest.delete(principal, roleManager);
        }

        try {
            if (this.readOnly &&
                    !roleManager.hasRole(principal.getQualifiedName(), RoleManager.ROOT)) {
                OperationLog.failure("move(" + srcUri + ", " + destUri + ")",
                    "read-only", token, principal);
                throw new ReadOnlyException();
            }

            ((Collection) destCollection).copy(
                principal, src, destPath, true, true, principalManager, roleManager);

            src.delete(principal, roleManager);

            OperationLog.success("move(" + srcUri + ", " + destUri + ")",
                token, principal);

            Resource r = dao.load(destUri);
            //ResourceDeletionEvent deletionEvent = new ResourceDeletionEvent(this,
            //        srcUri);
            ResourceDeletionEvent deletionEvent = new ResourceDeletionEvent(this, srcUri,
                   src.getID(), src instanceof Collection);

            context.publishEvent(deletionEvent);

            ResourceCreationEvent creationEvent = new ResourceCreationEvent(
                this, r.getResourceDTO(principal, principalManager, roleManager));

            context.publishEvent(creationEvent);
        } catch (AclException e) {
            OperationLog.failure("move(" + srcUri + ", " + destUri + ")",
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
            OperationLog.failure("delete(" + uri + ")", "resource not found",
                token, principal);
            throw new ResourceNotFoundException(uri);
        }

        if ("/".equals(uri)) {
            OperationLog.failure("delete(" + uri + ")",
                "cannot delete the root resource ('/')", token, principal);
            throw new IllegalOperationException(
                "Cannot delete the root resource ('/')");
        }

        Resource r = dao.load(uri);

        if (r == null) {
            OperationLog.failure("delete(" + uri + ")", "resource not found",
                token, principal);
            throw new ResourceNotFoundException(uri);
        }

        if (r instanceof Collection) {
            ((Collection) r).recursiveLockAuthorize(principal,
                PrivilegeDefinition.WRITE, roleManager);
        } else {
            r.lockAuthorize(principal, PrivilegeDefinition.WRITE, roleManager);
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

        parentCollection.authorize(principal, PrivilegeDefinition.WRITE,
            roleManager);
        parentCollection.lockAuthorize(principal, PrivilegeDefinition.WRITE,
            roleManager);

        if (this.readOnly &&
                !roleManager.hasRole(principal.getQualifiedName(), RoleManager.ROOT)) {
            OperationLog.failure("delete(" + uri + ")", "read-only", token,
                principal);
            throw new ReadOnlyException();
        }

        r.delete(principal, roleManager);
        OperationLog.success("delete(" + uri + ")", token, principal);

        //ResourceDeletionEvent event = new ResourceDeletionEvent(this, uri);
        ResourceDeletionEvent event = new ResourceDeletionEvent(this, uri, r.getID(), 
                                                                r instanceof Collection);

        context.publishEvent(event);
    }

    public boolean exists(String token, String uri)
        throws AuthorizationException, AuthenticationException, IOException {
        Principal principal = tokenManager.getPrincipal(token);

        if (!validateURI(uri)) {
            OperationLog.info("exists(" + uri + "): false", token, principal);

            return false;
        }

        Resource r = dao.load(uri);

        if (r == null) {
            OperationLog.info("exists(" + uri + "): false", token, principal);

            return false;
        }

        OperationLog.info("exists(" + uri + "): true", token, principal);

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
            OperationLog.failure("lock(" + uri + ")", "resource not found",
                token, principal);
            throw new ResourceNotFoundException(uri);
        }

        Resource r = dao.load(uri);

        if (r == null) {
            OperationLog.failure("lock(" + uri + ")", "resource not found",
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
            OperationLog.failure("lock(" + uri + ")", "read-only", token,
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
            r.lockAuthorize(principal, PrivilegeDefinition.WRITE, roleManager);

            String newLockToken = r.lock(principal, ownerInfo, depth,
                                      requestedTimeoutSeconds, roleManager,
                                      (lockToken != null));

            OperationLog.success("lock(" + uri + ")", token, principal);

            return newLockToken;
        } catch (AuthorizationException e) {
            OperationLog.failure("lock(" + uri + ")", "permission denied",
                token, principal);
            throw e;
        } catch (AuthenticationException e) {
            OperationLog.failure("lock(" + uri + ")", "not authenticated",
                token, principal);
            throw e;
        } catch (ResourceLockedException e) {
            OperationLog.failure("lock(" + uri + ")", "already locked", token,
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
            OperationLog.failure("unlock(" + uri + ")", "resource not found",
                token, principal);
            throw new ResourceNotFoundException(uri);
        }

        Resource r = dao.load(uri);

        if (r == null) {
            OperationLog.failure("unlock(" + uri + ")", "resource not found",
                token, principal);
            throw new ResourceNotFoundException(uri);
        }

        if (this.readOnly &&
                !roleManager.hasRole(principal.getQualifiedName(), RoleManager.ROOT)) {
            OperationLog.failure("unlock(" + uri + ")", "read-only", token,
                principal);
            throw new ReadOnlyException();
        }

        try {
            r.unlock(principal, lockToken, roleManager);
            OperationLog.success("unlock(" + uri + ")", token, principal);
        } catch (AuthorizationException e) {
            OperationLog.failure("unlock(" + uri + ")", "permission denied",
                token, principal);
            throw e;
        } catch (AuthenticationException e) {
            OperationLog.failure("unlock(" + uri + ")", "not authenticated",
                token, principal);
            throw e;
        } catch (ResourceLockedException e) {
            OperationLog.failure("unlock(" + uri + ")", "resource locked",
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
            OperationLog.failure("listChildren(" + uri + ")",
                "resource not found", token, principal);
            throw new ResourceNotFoundException(uri);
        }

        Resource r = dao.load(uri);

        if (r == null) {
            OperationLog.failure("listChildren(" + uri + ")",
                "resource not found", token, principal);
            throw new ResourceNotFoundException(uri);
        }

        if (r instanceof Document) {
            OperationLog.failure("listChildren(" + uri + ")",
                "uri is document", token, principal);

            return null;
        }

        try {
            /* authorize for the right privilege: */
            String privilege = (forProcessing)
                ? Resource.CUSTOM_PRIVILEGE_READ_PROCESSED
                : PrivilegeDefinition.READ;

            r.authorize(principal, privilege, roleManager);
            r.lockAuthorize(principal, privilege, roleManager);

            List list = ((Collection) r).getChildren();
            org.vortikal.repository.Resource[] children = new org.vortikal.repository.Resource[list.size()];

            for (int i = 0; i < list.size(); i++) {
                Resource child = (Resource) list.get(i);

                children[i] = child.getResourceDTO(principal, principalManager, roleManager);
            }

            OperationLog.success("listChildren(" + uri + ")", token, principal);

            return children;
        } catch (ResourceLockedException e) {
            OperationLog.failure("listChildren(" + uri + ")",
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
            OperationLog.failure("store(null)", "resource null", token,
                principal);
            throw new IllegalOperationException("I cannot store nothing.");
        }

        String uri = resource.getURI();

        if (!validateURI(uri)) {
            OperationLog.failure("store(" + uri + ")", "invalid uri", token,
                principal);
            throw new IllegalOperationException("Invalid URI: " + uri);
        }

        Resource r = dao.load(uri);

        if (r == null) {
            OperationLog.failure("store(" + uri + ")", "invalid uri", token,
                principal);
            throw new ResourceNotFoundException(uri);
        }

        if (this.readOnly &&
                !roleManager.hasRole(principal.getQualifiedName(), RoleManager.ROOT)) {
            OperationLog.failure("store(" + uri + ")", "read-only", token,
                principal);
            throw new ReadOnlyException();
        }

        try {
            Resource original = (Resource) r.clone();

            r.store(principal, resource, roleManager);
            OperationLog.success("store(" + uri + ")", token, principal);

            ResourceModificationEvent event = new ResourceModificationEvent(
                this, r.getResourceDTO(principal, principalManager, roleManager),
                original.getResourceDTO(principal, principalManager, roleManager));

            context.publishEvent(event);
        } catch (ResourceLockedException e) {
            OperationLog.failure("store(" + uri + ")", "resource locked",
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
            OperationLog.failure("getInputStream(" + uri + ")",
                "resource not found", token, principal);
            throw new ResourceNotFoundException(uri);
        }

        Resource r = dao.load(uri);

        if (r == null) {
            OperationLog.failure("getInputStream(" + uri + ")",
                "resource not found", token, principal);
            throw new ResourceNotFoundException(uri);
        }

        if (r instanceof Collection) {
            OperationLog.failure("getInputStream(" + uri + ")",
                "resource is collection", token, principal);
            throw new IOException("resource is collection");
        }

        /* authorize for the right privilege */
        String privilege = (forProcessing)
            ? Resource.CUSTOM_PRIVILEGE_READ_PROCESSED : PrivilegeDefinition.READ;

        InputStream inStream = ((Document) r).getInputStream(principal,
                privilege, roleManager);

        OperationLog.success("getInputStream(" + uri + ")", token, principal);

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
            OperationLog.failure("storeContent(" + uri + ")",
                "resource not found", token, principal);
            throw new ResourceNotFoundException(uri);
        }

        Resource r = dao.load(uri);

        if (r == null) {
            OperationLog.failure("storeContent(" + uri + ")",
                "resource not found", token, principal);
            throw new ResourceNotFoundException(uri);
        }

        if (r instanceof Collection) {
            OperationLog.failure("storeContent(" + uri + ")",
                "resource is collection", token, principal);
            throw new IOException("Collections have no output stream");
        }

        if (this.readOnly &&
                !roleManager.hasRole(principal.getQualifiedName(), RoleManager.ROOT)) {
            OperationLog.failure("storeContent(" + uri + ")", "read-only",
                token, principal);
            throw new ReadOnlyException();
        }

        OutputStream stream = null;

        try {
            Resource original = (Resource) r.clone();

            stream = ((Document) r).getOutputStream(principal, roleManager);

            /* Write the input data to the resource: */
            byte[] buffer = new byte[1000];
            int n = 0;

            while ((n = byteStream.read(buffer, 0, 1000)) > 0) {
                stream.write(buffer, 0, n);
            }

            stream.flush();
            stream.close();
            byteStream.close();

            OperationLog.success("storeContent(" + uri + ")", token, principal);

            ContentModificationEvent event = new ContentModificationEvent(
                this, r.getResourceDTO(principal, principalManager, roleManager),
                original.getResourceDTO(principal, principalManager, roleManager));

            context.publishEvent(event);
        } catch (ResourceLockedException e) {
            OperationLog.failure("storeContent(" + uri + ")",
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

        Resource r = dao.load(uri);

        if (r == null) {
            OperationLog.failure("getACL(" + uri + ")", "resource not found",
                token, principal);
            throw new ResourceNotFoundException(uri);
        }

        try {
            r.authorize(principal, PrivilegeDefinition.READ, roleManager);

            ACL acl = r.getACL();
            org.vortikal.repository.Ace[] dtos = acl.toAceList();

            OperationLog.success("getACL(" + uri + ")", token, principal);

            return dtos;
        } catch (AuthorizationException e) {
            OperationLog.failure("getACL(" + uri + ")", "permission denied",
                token, principal);
            throw e;
        } catch (AuthenticationException e) {
            OperationLog.failure("getACL(" + uri + ")", "not authenticated",
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
            OperationLog.failure("storeACL(" + uri + ")", "resource not found",
                token, principal);
            throw new ResourceNotFoundException(uri);
        }

        Resource r = dao.load(uri);

        if (r == null) {
            OperationLog.failure("storeACL(" + uri + ")", "resource not found",
                token, principal);
            throw new ResourceNotFoundException(uri);
        }

        try {
            r.authorize(principal, PrivilegeDefinition.WRITE_ACL, roleManager);

            if (this.readOnly &&
                    !roleManager.hasRole(principal.getQualifiedName(), RoleManager.ROOT)) {
                OperationLog.failure("storeACL(" + uri + ")", "read-only",
                    token, principal);
                throw new ReadOnlyException();
            }

            Resource original = (Resource) r.clone();
            ACL origACL = original.getACL();
            boolean wasInheritedACL = original.getInheritedACL();

            r.storeACL(principal, acl, roleManager);
            OperationLog.success("storeACL(" + uri + ")", token, principal);

            ACLModificationEvent event = new ACLModificationEvent(
                this, r.getResourceDTO(principal, principalManager, roleManager),
                original.getResourceDTO(principal, principalManager, roleManager),
                    r.getACL().toAceList(), origACL.toAceList(), wasInheritedACL);

            context.publishEvent(event);
        } catch (AuthorizationException e) {
            OperationLog.failure("storeACL(" + uri + ")", "permission denied",
                token, principal);
            throw e;
        } catch (AuthenticationException e) {
            OperationLog.failure("storeACL(" + uri + ")", "not authenticated",
                token, principal);
            throw e;
        } catch (IllegalOperationException e) {
            OperationLog.failure("storeACL(" + uri + ")", "illegal operation",
                token, principal);
            throw e;
        } catch (AclException e) {
            OperationLog.failure("storeACL(" + uri + ")",
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

    protected boolean validateURI(String uri) {
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

    public void destroy() throws IOException {
        if (dao != null) {
            logger.info("destroying database");
            dao.destroy();
        }
    }

    public void setPrincipalManager(PrincipalManager principalManager) {
        this.principalManager = principalManager;
    }

    public void setTokenManager(TokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }

    /**
     * Cleans up expired locks. Gets called periodically from the
     * maintenance thread. When in read-only mode, locks will only get
     * cleaned up when the property 'cleanupLocksWhenReadOnly' is set
     * to <code>true</code>.
     *
     * @exception IOException if an error occurs
     */
    public void cleanupLocks() throws IOException {
        String[] expiredUris = dao.listLockExpired();

        if (logger.isDebugEnabled()) {
            logger.debug("Cleaning up expired locks for uris: "
                         + Arrays.asList(expiredUris));
        }

        Resource[] expired = new Resource[expiredUris.length];

        for (int i = 0; i < expiredUris.length; i++) {
            Resource r = dao.load(expiredUris[i]);

            expired[i] = r;
        }

        if (this.readOnly && !this.cleanupLocksWhenReadOnly) {
            if (logger.isInfoEnabled()) {
                logger.info(
                    "Repository is read-only, will not expire locks for resources "
                    + Arrays.asList(expiredUris));
            } 
        } else {
            dao.deleteLocks(expired);
        }
    }

    public void setDao(DataAccessor dao) {
        this.dao = dao;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public void setCleanupLocksWhenReadOnly(boolean cleanupLocksWhenReadOnly) {
        this.cleanupLocksWhenReadOnly = cleanupLocksWhenReadOnly;
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
//         if (this.principalStore == null) {
//             throw new BeanInitializationException(
//                 "Bean property 'principalStore' must be set");
//         }
        if (this.tokenManager == null) {
            throw new BeanInitializationException(
                "Bean property 'tokenManager' must be set");
        }
        if (this.id == null) {
            throw new BeanInitializationException(
                "Bean property 'id' must be set");
        }
    }

}

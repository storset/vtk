/* Copyright (c) 2004, 2005, 2006, 2007, University of Oslo, Norway
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


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.vortikal.repository.Acl;
import org.vortikal.repository.AuthorizationException;
import org.vortikal.repository.AuthorizationManager;
import org.vortikal.repository.Comment;
import org.vortikal.repository.FailedDependencyException;
import org.vortikal.repository.IllegalOperationException;
import org.vortikal.repository.PropertySet;
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
import org.vortikal.repositoryimpl.store.CommentDAO;
import org.vortikal.repositoryimpl.store.DataAccessor;
import org.vortikal.repositoryimpl.store.ContentStore;
import org.vortikal.security.AuthenticationException;
import org.vortikal.security.Principal;
import org.vortikal.security.roles.RoleManager;
import org.vortikal.security.token.TokenManager;
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
 * 
 * On copy/move, name of destination resource may change.
 * This is not a problem until we have properties/content modification 
 * dependent properties, using name to evaluate. Currently only 
 * content type uses name, and only on creation. 
 * 
 */
public class RepositoryImpl
  implements Repository, ApplicationContextAware, InitializingBean {

    private ApplicationContext context;
    
    private DataAccessor dao;
    private CommentDAO commentDAO;
    private ContentStore contentStore;
    private RoleManager roleManager;
    private TokenManager tokenManager;
    private LockManager lockManager;
    private RepositoryResourceHelper resourceHelper;
    private AuthorizationManager authorizationManager;
    private URIValidator uriValidator = new URIValidator();
    
    private String id;

    private int maxComments = 1000;

    public boolean isReadOnly() {
        return this.authorizationManager.isReadOnly();
    }
    
    public String getId() {
        return this.id;
    }

    public boolean exists(String token, String uri) throws IOException {
        if (!this.uriValidator.validateURI(uri)) 
            return false;

        if (this.dao.load(uri) != null) 
            return true;

        return false;
    }

    public Resource retrieve(String token, String uri, boolean forProcessing)
        throws ResourceNotFoundException, AuthorizationException, 
            AuthenticationException, IOException {
        Principal principal = this.tokenManager.getPrincipal(token);

        if (!this.uriValidator.validateURI(uri)) throw new ResourceNotFoundException(uri);

        ResourceImpl resource = null;

            
        resource = this.dao.load(uri);

        if (resource == null) 
            throw new ResourceNotFoundException(uri);

        if (forProcessing)
            this.authorizationManager.authorizeReadProcessed(uri, principal);
        else
            this.authorizationManager.authorizeRead(uri, principal);
        
        try {
            return (Resource) resource.clone();

        } catch (CloneNotSupportedException e) {
            throw new IOException("An internal error occurred: unable to " +
                                  "clone() resource: " + resource);
        }

    }

    public InputStream getInputStream(String token, String uri,
            boolean forProcessing) throws ResourceNotFoundException,
            AuthorizationException, AuthenticationException,
            ResourceLockedException, IOException {

        Principal principal = this.tokenManager.getPrincipal(token);

        if (!this.uriValidator.validateURI(uri)) {
            throw new ResourceNotFoundException(uri);
        }

        ResourceImpl r = this.dao.load(uri);

        if (r == null) {
            throw new ResourceNotFoundException(uri);
        } else if (r.isCollection()) {
            throw new IllegalOperationException("Resource is collection");
        }

        if (forProcessing)
            this.authorizationManager.authorizeReadProcessed(uri, principal);
        else
            this.authorizationManager.authorizeRead(uri, principal);

        InputStream is = this.contentStore.getInputStream(uri);
        return is;
    }



    public Resource[] listChildren(String token, String uri, 
            boolean forProcessing) throws ResourceNotFoundException,
            AuthorizationException, AuthenticationException, IOException {

        Principal principal = this.tokenManager.getPrincipal(token);

        if (!this.uriValidator.validateURI(uri)) throw new ResourceNotFoundException(uri);

        ResourceImpl collection = this.dao.load(uri);

        if (collection == null) {
            throw new ResourceNotFoundException(uri);
        } else if (!collection.isCollection()) {
            throw new IllegalOperationException(
                "Can't list children for non-collection resources");
        }

        if (forProcessing)
            this.authorizationManager.authorizeReadProcessed(uri, principal);
        else
            this.authorizationManager.authorizeRead(uri, principal);

        ResourceImpl[] list = this.dao.loadChildren(collection);
        Resource[] children = new Resource[list.length];

        for (int i = 0; i < list.length; i++) {
            try {
                children[i] = (Resource)list[i].clone();

            } catch (CloneNotSupportedException e) {
                throw new IOException("An internal error occurred: unable to " +
                                      "clone() resource: " + list[i]);
            }

        }

        return children;
    }

    public Resource createDocument(String token, String uri) 
    throws IllegalOperationException, AuthorizationException, 
    AuthenticationException, ResourceLockedException, ReadOnlyException, 
    IOException {

        return create(token, uri, false);
    }

    public Resource createCollection(String token, String uri) 
    throws IllegalOperationException, AuthorizationException, 
    AuthenticationException, ResourceLockedException, ReadOnlyException, 
    IOException {

        return create(token, uri, true);
    }

    public void copy(String token, String srcUri, String destUri, String depth,
        boolean overwrite, boolean preserveACL)
        throws IllegalOperationException, AuthorizationException, 
            AuthenticationException, FailedDependencyException, 
            ResourceOverwriteException, ResourceLockedException, 
            ResourceNotFoundException, ReadOnlyException, IOException {

        Principal principal = this.tokenManager.getPrincipal(token);

        if (!this.uriValidator.validateURI(srcUri)) {
            throw new ResourceNotFoundException(srcUri);
        }

        if (!this.uriValidator.validateURI(destUri)) {
            throw new IllegalOperationException("Invalid URI: '" + destUri + "'");
        }
        
        this.uriValidator.validateCopyURIs(srcUri, destUri);        

        ResourceImpl src = this.dao.load(srcUri);
        if (src == null) {
            throw new ResourceNotFoundException(srcUri);
        }

        ResourceImpl dest = this.dao.load(destUri);

        if (dest == null) {
            overwrite = false;
        } else if (!overwrite) {
            throw new ResourceOverwriteException(
                "Copy: cannot overwrite resource " + destUri);
        } 
            
        String destParentUri = URIUtil.getParentURI(destUri);
        ResourceImpl destParent = this.dao.load(destParentUri);
        if ((destParent == null) || !destParent.isCollection()) {
            throw new IllegalOperationException(
                "destination is either a document or does not exist");
        }

        this.authorizationManager.authorizeCopy(srcUri, destUri,
                                                principal, overwrite);
            
        if (dest != null) {
            this.dao.delete(dest);
            this.contentStore.deleteResource(dest.getURI());
            this.context.publishEvent(new ResourceDeletionEvent(this, dest.getURI(), 
                                                                dest.getID(), dest.isCollection()));
        }

        try {
            PropertySet fixedProps = this.resourceHelper.getFixedCopyProperties(
                src, principal, destUri);


            ResourceImpl newResource = src.createCopy(destUri);
            newResource = this.resourceHelper.nameChange(newResource, principal);
            destParent = this.resourceHelper.contentModification(destParent, principal);
            
            this.dao.copy(src, destParent, newResource, preserveACL, fixedProps);
            this.contentStore.copy(src.getURI(), newResource.getURI());

            dest = (ResourceImpl) this.dao.load(destUri).clone();

        } catch (CloneNotSupportedException e) {
            throw new IOException("An internal error occurred: unable to clone()");
        }

        this.context.publishEvent(new ResourceCreationEvent(this, dest));
    }



    public void move(String token, String srcUri, String destUri,
        boolean overwrite)
        throws IllegalOperationException, AuthorizationException, 
            AuthenticationException, FailedDependencyException, 
            ResourceOverwriteException, ResourceLockedException, 
            ResourceNotFoundException, ReadOnlyException, IOException {

        Principal principal = this.tokenManager.getPrincipal(token);

        if (!this.uriValidator.validateURI(srcUri)) {
            throw new ResourceNotFoundException(srcUri);
        }

        if (!this.uriValidator.validateURI(destUri)) {
            throw new IllegalOperationException("Invalid URI: '" + destUri +
                "'");
        }

        this.uriValidator.validateCopyURIs(srcUri, destUri);
        
        
        // Loading and checking source resource
        ResourceImpl src = this.dao.load(srcUri);

        if (src == null) {
            throw new ResourceNotFoundException(srcUri);
        }

        // Checking dest
        ResourceImpl dest = this.dao.load(destUri);

        if (dest == null) {
            overwrite = false;
        } else if (!overwrite) {
            throw new ResourceOverwriteException();
        } 
            
        // checking destParent 
        String destParentUri = URIUtil.getParentURI(destUri);

        ResourceImpl destParent = this.dao.load(destParentUri);

        if ((destParent == null) || !destParent.isCollection()) {
            throw new IllegalOperationException("Invalid destination resource");
        }

        this.authorizationManager.authorizeMove(srcUri, destUri, principal, overwrite);

        // Performing delete operation
        if (dest != null) {
            this.dao.delete(dest);
            this.contentStore.deleteResource(dest.getURI());

            this.context.publishEvent(new ResourceDeletionEvent(this, dest.getURI(), dest.getID(), 
                                                                dest.isCollection()));
        }
            
        try {
            destParent = this.resourceHelper.contentModification(destParent, principal);

            ResourceImpl newResource = src.createCopy(destUri);
            newResource = this.resourceHelper.nameChange(newResource, principal);

            this.dao.move(src, newResource);
            newResource = this.dao.load(newResource.getURI());
            this.contentStore.copy(src.getURI(), newResource.getURI());
            this.contentStore.deleteResource(src.getURI());
            
            this.context.publishEvent(new ResourceCreationEvent(this, newResource));

            this.context.publishEvent(new ResourceDeletionEvent(
                                          this, srcUri, src.getID(), src.isCollection()));

            dest = (ResourceImpl) this.dao.load(destUri).clone();
        } catch (CloneNotSupportedException e) {
            throw new IOException("clone() operation failed");
        }
    }


    


    public void delete(String token, String uri)
        throws IllegalOperationException, AuthorizationException, 
            AuthenticationException, ResourceNotFoundException, 
            ResourceLockedException, FailedDependencyException, 
            ReadOnlyException, IOException {

        Principal principal = this.tokenManager.getPrincipal(token);

        if (!this.uriValidator.validateURI(uri)) {
            throw new ResourceNotFoundException(uri);
        }

        if ("/".equals(uri)) {
            throw new IllegalOperationException(
                    "Cannot delete the root resource ('/')");
        }

        ResourceImpl r = this.dao.load(uri);

        if (r == null) {
            throw new ResourceNotFoundException(uri);
        }

        this.authorizationManager.authorizeDelete(uri, principal);
            
        this.dao.delete(r);
        this.contentStore.deleteResource(r.getURI());
        

        String parent = URIUtil.getParentURI(uri);

        ResourceImpl parentCollection = this.dao.load(parent);

        parentCollection = this.resourceHelper.contentModification(
            parentCollection, principal);
        this.dao.store(parentCollection);

        ResourceDeletionEvent event = 
            new ResourceDeletionEvent(this, uri, r.getID(), r.isCollection());

        this.context.publishEvent(event);
    }


    public Resource lock(String token, String uri, String ownerInfo, String depth,
                         int requestedTimeoutSeconds, 
            String lockToken) throws ResourceNotFoundException, 
            AuthorizationException, AuthenticationException, 
            FailedDependencyException, ResourceLockedException, 
            IllegalOperationException, ReadOnlyException, IOException {
        
        Principal principal = this.tokenManager.getPrincipal(token);

        if (!this.uriValidator.validateURI(uri)) {
            throw new ResourceNotFoundException(uri);
        }

        if ("0".equals(depth) || "infinity".equals(depth)) {
            depth = "0";
        } else {
            throw new IllegalOperationException("Invalid depth parameter: "
                                                + depth);
        }

        ResourceImpl r = this.dao.load(uri);

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
                    + "' does not match existing lock token on resource " + uri);
            }
        }

        this.authorizationManager.authorizeWrite(uri, principal);
            
        String newLockToken = this.lockManager.lockResource(
            r, principal, ownerInfo, depth, requestedTimeoutSeconds,
            (lockToken != null));

        return r;
    }

    public void unlock(String token, String uri, String lockToken)
        throws ResourceNotFoundException, AuthorizationException, 
            AuthenticationException, ResourceLockedException, ReadOnlyException, 
            IOException {
        Principal principal = this.tokenManager.getPrincipal(token);

        if (!this.uriValidator.validateURI(uri)) {
            throw new ResourceNotFoundException(uri);
        }

        ResourceImpl r = this.dao.load(uri);

        if (r == null) {
            throw new ResourceNotFoundException(uri);
        }

        this.authorizationManager.authorizeUnlock(uri, principal);
            
        if (r.getLock() != null) {
            r.setLock(null);
            this.dao.store(r);
        }
    }

    public Resource store(String token, Resource resource)
        throws ResourceNotFoundException, AuthorizationException, 
            ResourceLockedException, AuthenticationException, 
            IllegalOperationException, ReadOnlyException, IOException {

        Principal principal = this.tokenManager.getPrincipal(token);

        if (resource == null) {
            throw new IllegalOperationException("Can't store nothing.");
        }

        if (!(resource instanceof ResourceImpl)) {
            throw new IllegalOperationException("Can't store unknown implementation of resource..");
        }
        String uri = resource.getURI();

        if (!this.uriValidator.validateURI(uri)) {
            throw new IllegalOperationException("Invalid URI: " + uri);
        }

        ResourceImpl original = this.dao.load(uri);

        if (original == null) {
            throw new ResourceNotFoundException(uri);
        }

        this.authorizationManager.authorizeWrite(uri, principal);
            
        try {
            ResourceImpl originalClone = (ResourceImpl) original.clone();

            ResourceImpl newResource = 
                this.resourceHelper.propertiesChange(original, principal, (ResourceImpl)resource);
            this.dao.store(newResource);

            newResource = (ResourceImpl)this.dao.load(uri).clone();

            ResourceModificationEvent event = new ResourceModificationEvent(
                this, newResource, originalClone);

            this.context.publishEvent(event);

            return newResource;
        } catch (CloneNotSupportedException e) {
            throw new IOException("An internal error occurred: unable to " +
                                  "clone() resource: " + original);
        }
    }


    /**
     * Requests that an InputStream be written to a resource.
     */
    public Resource storeContent(String token, String uri, InputStream byteStream)
        throws AuthorizationException, AuthenticationException, 
            ResourceNotFoundException, ResourceLockedException, 
            IllegalOperationException, ReadOnlyException, IOException {

        Principal principal = this.tokenManager.getPrincipal(token);

        if (!this.uriValidator.validateURI(uri)) {
            throw new ResourceNotFoundException(uri);
        }

        ResourceImpl r = this.dao.load(uri);

        if (r == null) {
            throw new ResourceNotFoundException(uri);
        } else if (r.isCollection()) {
            throw new IllegalOperationException("resource is collection");
        }

        this.authorizationManager.authorizeWrite(uri, principal);
        File tempFile = null;
        try {
            // Write to a temporary file to avoid locking:
            tempFile = writeTempFile(r.getName(), byteStream);
            Resource original = (ResourceImpl) r.clone();

            this.contentStore.storeContent(uri, new java.io.BufferedInputStream(
                                      new java.io.FileInputStream(tempFile)));

            r = this.resourceHelper.contentModification(r, principal);
                
            this.dao.store(r);

//             if (true == true) {
//                 throw new RuntimeException("Please roll back..");
//             }

            ContentModificationEvent event = new ContentModificationEvent(
                this, (Resource) r.clone(), original);

            this.context.publishEvent(event);

            return r;

        } catch (CloneNotSupportedException e) {
            throw new IOException("An internal error occurred: unable to " +
                                  "clone() resource: " + r);
        } finally {
                
            if (tempFile != null) tempFile.delete();
        }
    }


    public void storeACL(String token, Resource resource)
        throws ResourceNotFoundException, AuthorizationException, 
            AuthenticationException, IllegalOperationException,
            ReadOnlyException, IOException {

        if (resource == null) {
            throw new IllegalArgumentException("Resource is null");
        }

        Principal principal = this.tokenManager.getPrincipal(token);

        if (!this.uriValidator.validateURI(resource.getURI())) {
            throw new ResourceNotFoundException(resource.getURI());
        }

        ResourceImpl r = this.dao.load(resource.getURI());

        if (r == null) {
            throw new ResourceNotFoundException(resource.getURI());
        } else if ("/".equals(r.getURI()) && r.isInheritedAcl()) {
            throw new IllegalOperationException("The root resource cannot have an inherited ACL");
        }

        this.authorizationManager.authorizeAll(resource.getURI(), principal);
            
        try {
            Resource originalResource = (Resource) r.clone();

            AclImpl newAcl = null;
            if (resource.isInheritedAcl()) {
                /* When the ACL is inherited, make the new ACL a copy
                 * of the parent's ACL, since the supplied one may
                 * contain other ACEs than the one we now inherit
                 * from. */
                ResourceImpl parent = this.dao.load(r.getParent());
                newAcl = (AclImpl) parent.getAcl().clone();
                r.setInheritedAcl(true);
                r.setAclInheritedFrom(parent.getID());
            } else {
                newAcl = (AclImpl) resource.getAcl().clone();
                r.setInheritedAcl(false);
                r.setAclInheritedFrom(PropertySetImpl.NULL_RESOURCE_ID);
            }
                
            r.setAcl(newAcl);
            this.dao.storeACL(r);

            ACLModificationEvent event = new ACLModificationEvent(
                this, (Resource) r.clone(),
                originalResource, r.getAcl(), originalResource.getAcl());

            this.context.publishEvent(event);

        } catch (CloneNotSupportedException e) {
            throw new IOException(e.getMessage());
        }
    }

    
    public List<Comment> getComments(String token, Resource resource) {
        Principal principal = this.tokenManager.getPrincipal(token);

        if (resource == null) {
            throw new IllegalOperationException("Resource argument cannot be NULL");
        }

        if (!(resource instanceof ResourceImpl)) {
            throw new IllegalOperationException("Can't store unknown implementation of resource..");
        }

        try {
            ResourceImpl original = this.dao.load(resource.getURI());
            if (original == null) {
                throw new ResourceNotFoundException(resource.getURI());
            }

            this.authorizationManager.authorizeRead(resource.getURI(), principal);
            List<Comment> comments = this.commentDAO.listCommentsByResource(resource);
            return Collections.unmodifiableList(comments);
        } catch (IOException e) {
            throw new RuntimeException("Unhandled IO exception", e);
        }
    }
    

    public Comment addComment(String token, Resource resource, String title, String text) {
        Principal principal = this.tokenManager.getPrincipal(token);

        if (resource == null) {
            throw new IllegalOperationException("Resource argument cannot be NULL");
        }

        if (!(resource instanceof ResourceImpl)) {
            throw new IllegalOperationException("Can't store unknown implementation of resource..");
        }

        try {
            ResourceImpl original = this.dao.load(resource.getURI());
            if (original == null) {
                throw new ResourceNotFoundException(resource.getURI());
            }

            this.authorizationManager.authorizeAddComment(resource.getURI(), principal);

            List comments = this.commentDAO.listCommentsByResource(resource);
            if (comments.size() > this.maxComments) {
                throw new IllegalOperationException(
                    "Too many comments on resource " + resource.getURI());
            }


            Comment comment = new Comment();
            comment.setURI(original.getURI());
            comment.setTime(new java.util.Date());
            comment.setAuthor(principal.getQualifiedName());
            comment.setTitle(title);
            comment.setContent(text);
            comment.setApproved(true);

            comment = this.commentDAO.create(original, comment);
            return comment;
        } catch (IOException e) {
            throw new RuntimeException("Unhandled IO exception", e);
        }
    }
    

    public void deleteComment(String token, Resource resource, Comment comment) {
        Principal principal = this.tokenManager.getPrincipal(token);

        if (resource == null) {
            throw new IllegalOperationException("Resource argument cannot be NULL");
        }

        if (!(resource instanceof ResourceImpl)) {
            throw new IllegalOperationException("Can't store unknown implementation of resource..");
        }

        try {
            ResourceImpl original = this.dao.load(resource.getURI());
            if (original == null) {
                throw new ResourceNotFoundException(resource.getURI());
            }

            this.authorizationManager.authorizeEditComment(resource.getURI(), principal);
            this.commentDAO.delete(comment);
        } catch (IOException e) {
            throw new RuntimeException("Unhandled IO exception", e);
        }
    }
    

    public void deleteAllComments(String token, Resource resource) {
        Principal principal = this.tokenManager.getPrincipal(token);

        if (resource == null) {
            throw new IllegalOperationException("Resource argument cannot be NULL");
        }

        if (!(resource instanceof ResourceImpl)) {
            throw new IllegalOperationException("Can't store unknown implementation of resource..");
        }

        try {
            ResourceImpl original = this.dao.load(resource.getURI());
            if (original == null) {
                throw new ResourceNotFoundException(resource.getURI());
            }

            this.authorizationManager.authorizeEditComment(resource.getURI(), principal);
            this.commentDAO.deleteAll(resource);
        } catch (IOException e) {
            throw new RuntimeException("Unhandled IO exception", e);
        }
    }
    


    public Comment updateComment(String token, Resource resource, Comment comment) {
        Principal principal = this.tokenManager.getPrincipal(token);

        if (resource == null) {
            throw new IllegalOperationException("Resource argument cannot be NULL");
        }

        if (!(resource instanceof ResourceImpl)) {
            throw new IllegalOperationException("Can't store unknown implementation of resource..");
        }

        try {
            ResourceImpl original = this.dao.load(resource.getURI());
            if (original == null) {
                throw new ResourceNotFoundException(resource.getURI());
            }

            Comment old = null;
            List<Comment> comments = this.commentDAO.listCommentsByResource(resource);
            for (Comment c: comments) {
                if (c.getID() == comment.getID() && c.getURI().equals(comment.getURI())) {
                    old = c;
                    break;
                }
            }
            if (old == null) {
                throw new IllegalArgumentException("Trying to update a non-existing comment");
            }

            this.authorizationManager.authorizeEditComment(resource.getURI(), principal);
            comment = this.commentDAO.update(comment);
            return comment;

        } catch (IOException e) {
            throw new RuntimeException("Unhandled IO exception", e);
        }
    }
    

    private Resource create(String token, String uri, boolean collection)
        throws AuthorizationException, AuthenticationException, 
        IllegalOperationException, ResourceLockedException, 
        ReadOnlyException, IOException {

        Principal principal = this.tokenManager.getPrincipal(token);

        if (!this.uriValidator.validateURI(uri)) throw new ResourceNotFoundException(uri);

        ResourceImpl resource = this.dao.load(uri);

        if (resource != null) { 
            throw new IllegalOperationException("Resource already exists");
        }

        String parentURI = URIUtil.getParentURI(uri);

        ResourceImpl parent = this.dao.load(parentURI);

        if ((parent == null) || !parent.isCollection()) {
            throw new IllegalOperationException("Either parent doesn't exist " +
                                                "or parent is document");
        }

        this.authorizationManager.authorizeCreate(parent.getURI(), principal);
            
        ResourceImpl newResource = 
            this.resourceHelper.create(principal, uri, collection);

        try {
            Acl newAcl = (Acl) parent.getAcl().clone();
            newResource.setAcl(newAcl);
            newResource.setInheritedAcl(true);
            int aclIneritedFrom = parent.isInheritedAcl()
                ? parent.getAclInheritedFrom() : parent.getID();
            newResource.setAclInheritedFrom(aclIneritedFrom);
            this.dao.store(newResource);
            this.contentStore.createResource(newResource.getURI(), collection);

            newResource = this.dao.load(uri);

            parent.addChildURI(uri);
            parent = this.resourceHelper.contentModification(parent, principal);
                
            this.dao.store(parent);

            newResource = (ResourceImpl) newResource.clone();
        } catch (CloneNotSupportedException e) {
            throw new IOException("An internal error occurred: unable to " +
                                  "clone() resource: " + uri);
        }

        this.context.publishEvent(new ResourceCreationEvent(this, newResource));
        return newResource;
    }    


    /**
     * Writes to a temporary file (used to avoid lengthy blocking on
     * file uploads).
     *
     * XXX: should be handled on the client side?
     */
    private File writeTempFile(String name, InputStream byteStream) throws IOException {
        byteStream = new java.io.BufferedInputStream(byteStream);
        String prefix = "tmpfile-" + name;
        File tempFile = File.createTempFile(prefix, null);
        java.io.OutputStream stream = new java.io.FileOutputStream(tempFile);

        // XXX: Review impl.
        /* Write the input data to the resource: */
        byte[] buffer = new byte[100000];
        int n = 0;

        while ((n = byteStream.read(buffer, 0, buffer.length)) != -1) {
            stream.write(buffer, 0, n);
        }

        stream.flush();
        stream.close();
        byteStream.close();
            
        return tempFile;
    }
    


    public void setReadOnly(String token, boolean readOnly) 
        throws AuthorizationException {

        Principal principal = this.tokenManager.getPrincipal(token);
        this.authorizationManager.authorizeRootRoleAction(principal);
        this.authorizationManager.setReadOnly(readOnly);
    }


    public void setTokenManager(TokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }


    public void setDao(DataAccessor dao) {
        this.dao = dao;
    }

    public void setCommentDAO(CommentDAO commentDAO) {
        this.commentDAO = commentDAO;
    }

    public void setContentStore(ContentStore contentStore) {
        this.contentStore = contentStore;
    }

    public void setRoleManager(RoleManager roleManager) {
        this.roleManager = roleManager;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setLockManager(LockManager lockManager) {
        this.lockManager = lockManager;
    }
    
    public void setAuthorizationManager(AuthorizationManager authorizationManager) {
        this.authorizationManager = authorizationManager;
    }

    public void setApplicationContext(ApplicationContext context) {
        this.context = context;
    }

    public void setRepositoryResourceHelper(RepositoryResourceHelper resourceHelper) {
        this.resourceHelper = resourceHelper;
    }

    public void setMaxComments(int maxComments) {
        if (maxComments < 0) {
            throw new IllegalArgumentException(
                "Argument must be an integer >= 0");
        }

        this.maxComments = maxComments;
    }
    

    public void afterPropertiesSet() {
        if (this.dao == null) {
            throw new BeanInitializationException(
                "Bean property 'dao' must be set");
        }
        if (this.commentDAO == null) {
            throw new BeanInitializationException(
                "Bean property 'commentDAO' must be set");
        }
        if (this.contentStore == null) {
            throw new BeanInitializationException(
                "JavaBean property 'contentStore' not specified");
        }
        if (this.roleManager == null) {
            throw new BeanInitializationException(
                "Bean property 'roleManager' must be set");
        }
        if (this.tokenManager == null) {
            throw new BeanInitializationException(
                "Bean property 'tokenManager' must be set");
        }
        if (this.id == null) {
            throw new BeanInitializationException(
                "Bean property 'id' must be set");
        }
        if (this.lockManager == null) {
            throw new BeanInitializationException(
            "Bean property 'lockManager' must be set");
        }
        if (this.resourceHelper == null) {
            throw new BeanInitializationException(
            "Bean property 'resourceHelper' must be set");
        }
        if (this.authorizationManager == null) {
            throw new BeanInitializationException(
            "Bean property 'authorizationManager' must be set");
        }
    }

}

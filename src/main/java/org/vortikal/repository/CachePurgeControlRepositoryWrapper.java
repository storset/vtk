/* Copyright (c) 2009, University of Oslo, Norway
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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.search.QueryException;
import org.vortikal.repository.search.ResultSet;
import org.vortikal.repository.search.Search;
import org.vortikal.repository.store.OldCache;
import org.vortikal.security.AuthenticationException;
import org.vortikal.security.Principal;

/**
 * A hack and necessary evil to fix some cache coherency problems after
 * repository service transactions are completed. Some services need extra cache
 * purge on affected resources <em>after</em> the transactions has completed,
 * especially when the affected resources are loaded while modifications are
 * happening. Otherwise cache can become inconsistent, especially collection
 * resource child URI lists.
 * 
 * Specifically, the following service methods need extra cache purging on
 * affected resources when they complete successfully:
 * <ul>
 * <li>Repository.delete()
 * <li>Repository.createDocument()
 * <li>Repository.createCollection()
 * <li>Repository.copy()
 * <li>Repository.move()
 * </ul>
 * 
 * So wrap Repository and make sure the necessary cache purge operations are
 * done after the relevant transactions complete. For this to work, the AOP tx
 * join points must be set for the wrapped Repository bean and *NOT* this bean.
 * 
 * This is an ugly, but not so intrusive way (code-wise) of handling our
 * layering/model problems with the cache, since we here gain a back-door access
 * to the cache impl, while at the same time having control over repository
 * service transaction calls.
 * 
 * Since we are currently developing a new backend, this should be an acceptable
 * solution to keep the old one humping along a little longer.
 */
@Deprecated
public class CachePurgeControlRepositoryWrapper implements Repository {

    private OldCache cache;
    private Repository wrappedRepository;
    private final Log logger = LogFactory.getLog(CachePurgeControlRepositoryWrapper.class);

    @Override
    public Comment addComment(String token, Resource resource, String title, String text) throws RepositoryException,
            AuthenticationException {
        return this.wrappedRepository.addComment(token, resource, title, text);
    }

    @Override
    public Comment addComment(String token, Comment comment) throws AuthenticationException {
        return this.wrappedRepository.addComment(token, comment); // Tx
    }
    
    @Override
    public void copy(String token, Path srcUri, Path destUri, Depth depth, boolean overwrite, boolean preserveACL)
            throws IllegalOperationException, AuthorizationException, AuthenticationException,
            FailedDependencyException, ResourceOverwriteException, ResourceLockedException, ResourceNotFoundException,
            ReadOnlyException, Exception {

        this.wrappedRepository.copy(token, srcUri, destUri, depth, overwrite, preserveACL); // Tx

        // Purge destination parent URI from cache after transaction has been
        // comitted.
        Path destParentUri = destUri.getParent();
        if (destParentUri != null) {
            if (logger.isDebugEnabled()) {
                logPurge("copy", destParentUri);
            }
            this.cache.purgeFromCache(destParentUri);
        }
    }

    @Override
    public void move(String token, Path srcUri, Path destUri, boolean overwrite) throws IllegalOperationException,
            AuthorizationException, AuthenticationException, FailedDependencyException, ResourceOverwriteException,
            ResourceLockedException, ResourceNotFoundException, ReadOnlyException, Exception {

        this.wrappedRepository.move(token, srcUri, destUri, overwrite); // Tx

        // Purge source, source parent and dest-parent from cache after
        // transaction has been comitted.
        List<Path> affected = new ArrayList<Path>(4);
        affected.add(srcUri);
        affected.add(destUri);

        Path srcParent = srcUri.getParent();
        Path destParent = destUri.getParent();

        if (srcParent != null)
            affected.add(srcParent);
        if (destParent != null && !affected.contains(destParent))
            affected.add(destParent);

        if (logger.isDebugEnabled()) {
            logPurge("move", affected);
        }

        this.cache.purgeFromCache(affected);
    }

    @Override
    public Resource createCollection(String token, Path uri) throws AuthorizationException, AuthenticationException,
            IllegalOperationException, ResourceLockedException, ReadOnlyException, Exception {

        Resource resource = this.wrappedRepository.createCollection(token, uri); // Tx

        Path parent = resource.getURI().getParent();
        if (parent != null) {
            if (logger.isDebugEnabled()) {
                logPurge("createCollection", parent);
            }
            this.cache.purgeFromCache(parent); // Purge parent from cache after
            // transaction has been comitted.
        }

        return resource;
    }

    @Override
    public Resource createDocument(String token, Path uri, InputStream inputStream) throws IllegalOperationException,
            AuthorizationException, AuthenticationException, ResourceLockedException, ReadOnlyException, Exception {

        Resource resource = this.wrappedRepository.createDocument(token, uri, inputStream); // Tx

        Path parent = resource.getURI().getParent();
        if (parent != null) {
            if (logger.isDebugEnabled()) {
                logPurge("createDocument", parent);
            }
            // Purge parent from cache after transaction has been comitted.
            this.cache.purgeFromCache(parent);
        }

        return resource;
    }

    @Override
    public void delete(String token, Path uri, boolean restorable) throws IllegalOperationException,
            AuthorizationException, AuthenticationException, ResourceNotFoundException, ResourceLockedException,
            FailedDependencyException, ReadOnlyException, Exception {

        this.wrappedRepository.delete(token, uri, restorable); // Tx

        List<Path> affected = new ArrayList<Path>(2);
        affected.add(uri);
        Path parent = uri.getParent();
        if (parent != null) {
            affected.add(parent);
        }
        if (logger.isDebugEnabled()) {
            logPurge("delete", affected);
        }
        // Purge parent and deleted resource from cache after transaction has
        // been comitted.
        this.cache.purgeFromCache(affected);

    }

    @Override
    public List<RecoverableResource> getRecoverableResources(String token, Path uri) throws ResourceNotFoundException,
            AuthorizationException, AuthenticationException, Exception {
        return this.wrappedRepository.getRecoverableResources(token, uri);
    }

    @Override
    public void recover(String token, Path parentUri, RecoverableResource recoverableResource)
            throws ResourceNotFoundException, AuthorizationException, AuthenticationException, Exception {
        this.wrappedRepository.recover(token, parentUri, recoverableResource);
    }

    @Override
    public void deleteRecoverable(String token, Path parentUri, RecoverableResource recoverableResource)
            throws Exception {
        this.wrappedRepository.deleteRecoverable(token, parentUri, recoverableResource);
    }

    @Override
    public void deleteAllComments(String token, Resource resource) throws RepositoryException, AuthenticationException {
        this.wrappedRepository.deleteAllComments(token, resource); // Tx

    }

    @Override
    public void deleteComment(String token, Resource resource, Comment comment) throws RepositoryException,
            AuthenticationException {
        this.wrappedRepository.deleteComment(token, resource, comment); // Tx
    }

    @Override
    public boolean exists(String token, Path uri) throws AuthorizationException, AuthenticationException, Exception {
        return this.wrappedRepository.exists(token, uri);
    }

    @Override
    public List<Comment> getComments(String token, Resource resource) throws RepositoryException,
            AuthenticationException {
        return this.wrappedRepository.getComments(token, resource); // Tx
    }

    @Override
    public List<Comment> getComments(String token, Resource resource, boolean deep, int max)
            throws RepositoryException, AuthenticationException {
        return this.wrappedRepository.getComments(token, resource, deep, max); // Tx
    }

    @Override
    public String getId() {
        return this.wrappedRepository.getId();
    }

    @Override
    public InputStream getInputStream(String token, Path uri, boolean forProcessing) throws ResourceNotFoundException,
            AuthorizationException, AuthenticationException, Exception {
        return this.wrappedRepository.getInputStream(token, uri, forProcessing); // Tx
    }

    @Override
    public InputStream getInputStream(String token, Path uri, boolean forProcessing, Revision revision) throws ResourceNotFoundException,
            AuthorizationException, AuthenticationException, Exception {
        return this.wrappedRepository.getInputStream(token, uri, forProcessing, revision); // Tx
    }

    @Override
    public boolean isReadOnly() {
        return this.wrappedRepository.isReadOnly(); // Tx
    }

    @Override
    public Resource[] listChildren(String token, Path uri, boolean forProcessing) throws ResourceNotFoundException,
            AuthorizationException, AuthenticationException, Exception {
        return this.wrappedRepository.listChildren(token, uri, forProcessing); // Tx
    }

    @Override
    public Resource lock(String token, Path uri, String ownerInfo, Depth depth, int requestedTimoutSeconds,
            String lockToken) throws ResourceNotFoundException, AuthorizationException, AuthenticationException,
            FailedDependencyException, ResourceLockedException, IllegalOperationException, ReadOnlyException, Exception {
        return this.wrappedRepository.lock(token, uri, ownerInfo, depth, requestedTimoutSeconds, lockToken); // Tx
    }

    @Override
    public Resource retrieve(String token, Path uri, boolean forProcessing) throws ResourceNotFoundException,
            AuthorizationException, AuthenticationException, Exception {
        return this.wrappedRepository.retrieve(token, uri, forProcessing); // Tx
    }

    @Override
    public Resource retrieve(String token, Path uri, boolean forProcessing, Revision revision) throws ResourceNotFoundException,
            AuthorizationException, AuthenticationException, Exception {
        return this.wrappedRepository.retrieve(token, uri, forProcessing, revision); // Tx
    }

    @Override
    public void setReadOnly(String token, boolean readOnly) throws AuthorizationException, Exception {
        this.wrappedRepository.setReadOnly(token, readOnly); // Tx
    }

    @Override
    public Resource store(String token, Resource resource) throws ResourceNotFoundException, AuthorizationException,
            AuthenticationException, ResourceLockedException, IllegalOperationException, ReadOnlyException, Exception {
        return this.wrappedRepository.store(token, resource); // Tx
    }

    @Override
    public Resource storeACL(String token, Path uri, Acl acl) throws ResourceNotFoundException, AuthorizationException,
            AuthenticationException, IllegalOperationException, ReadOnlyException, Exception {
        return this.wrappedRepository.storeACL(token, uri, acl); // Tx
    }

    @Override
    public Resource storeACL(String token, Path uri, Acl acl, boolean validateACL) throws ResourceNotFoundException,
            AuthorizationException, AuthenticationException, IllegalOperationException, ReadOnlyException, Exception {
        return this.wrappedRepository.storeACL(token, uri, acl, validateACL); // Tx
    }
    
    @Override
    public Resource deleteACL(String token, Path uri)
            throws ResourceNotFoundException, AuthorizationException,
            AuthenticationException, IllegalOperationException,
            ReadOnlyException, Exception {
        return this.wrappedRepository.deleteACL(token, uri); // Tx
    }

    @Override
    public boolean isValidAclEntry(Privilege privilege, Principal principal) {
        return this.wrappedRepository.isValidAclEntry(privilege, principal);
    }

    @Override
    public boolean isBlacklisted(Privilege privilege, Principal principal) {
        return this.wrappedRepository.isBlacklisted(privilege, principal);
    }
    
    @Override
    public Resource storeContent(String token, Path uri, InputStream byteStream) throws AuthorizationException,
            AuthenticationException, ResourceNotFoundException, ResourceLockedException, IllegalOperationException,
            ReadOnlyException, Exception {
        return this.wrappedRepository.storeContent(token, uri, byteStream); // Tx
    }

    @Override
    public Resource storeContent(String token, Path uri, InputStream byteStream, Revision revision) throws AuthorizationException,
            AuthenticationException, ResourceNotFoundException, ResourceLockedException, IllegalOperationException,
            ReadOnlyException, Exception {
        return this.wrappedRepository.storeContent(token, uri, byteStream, revision); // Tx
    }

    @Override
    public void unlock(String token, Path uri, String lockToken) throws ResourceNotFoundException,
            AuthorizationException, AuthenticationException, ResourceLockedException, ReadOnlyException, Exception {
        this.wrappedRepository.unlock(token, uri, lockToken); // Tx
    }

    @Override
    public Comment updateComment(String token, Resource resource, Comment comment) throws RepositoryException,
            AuthenticationException {
        return this.wrappedRepository.updateComment(token, resource, comment); // Tx
    }

    @Override
    public ResultSet search(String token, Search search) throws QueryException {
        return this.wrappedRepository.search(token, search);
    }

    @Override
    public boolean authorize(Principal principal, Acl acl, Privilege privilege) {
        return this.wrappedRepository.authorize(principal, acl, privilege);
    }
    
    @Override
    public boolean isAuthorized(Resource resource, RepositoryAction action, Principal principal, boolean considerLocks)
            throws Exception {
        return this.wrappedRepository.isAuthorized(resource, action, principal, considerLocks);
    }

    @Override
    public TypeInfo getTypeInfo(String token, Path uri) throws Exception {
        return this.wrappedRepository.getTypeInfo(token, uri);
    }

    @Override
    public TypeInfo getTypeInfo(Resource resource) {
        return this.wrappedRepository.getTypeInfo(resource);
    }

    @Required
    public void setCache(OldCache cache) {
        this.cache = cache;
    }

    @Required
    public void setWrappedRepository(Repository wrappedRepository) {
        this.wrappedRepository = wrappedRepository;
    }

    private void logPurge(String serviceMethodName, List<Path> uris) {
        logger.debug(serviceMethodName + "() completed, purging the following URIs from cache:");
        for (Path uri : uris) {
            logger.debug(uri);
        }
    }

    private void logPurge(String serviceMethodName, Path uri) {
        List<Path> uris = new ArrayList<Path>();
        uris.add(uri);
        logPurge(serviceMethodName, uris);
    }
    
    public void purgeTrash() {
        this.wrappedRepository.purgeTrash();
    }

    @Override
    public List<Revision> getRevisions(String token, Path uri) throws AuthorizationException, ResourceNotFoundException, AuthenticationException, IOException {
        return this.wrappedRepository.getRevisions(token, uri);
    }

    @Override
    public Revision createRevision(String token, Path uri, Revision.Type type) throws AuthorizationException, ResourceNotFoundException, AuthenticationException, IOException {
        return this.wrappedRepository.createRevision(token, uri, type);
    }

    @Override
    public void deleteRevision(String token, Path uri, Revision revision)
            throws ResourceNotFoundException, AuthorizationException,
            AuthenticationException, Exception {
        this.wrappedRepository.deleteRevision(token, uri, revision);
    }

}

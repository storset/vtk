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
package org.vortikal.web;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repository.AuthorizationException;
import org.vortikal.repository.FailedDependencyException;
import org.vortikal.repository.IllegalOperationException;
import org.vortikal.repository.ReadOnlyException;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.ResourceLockedException;
import org.vortikal.repository.ResourceNotFoundException;
import org.vortikal.repository.ResourceOverwriteException;
import org.vortikal.security.AuthenticationException;


public class RequestLocalRepository implements InitializingBean, Repository {

    private static Log logger = LogFactory.getLog(RequestLocalRepository.class);
    private Repository repository;


    public void setRepository(Repository repository) {
        this.repository = repository;
    }


    public void afterPropertiesSet() throws Exception {
        if (this.repository == null) {
            throw new BeanInitializationException(
                "Bean property 'repository' must be set");
        }
    }

    public Resource retrieve(String token, String uri, boolean forProcessing)
        throws ResourceNotFoundException, AuthorizationException, 
        AuthenticationException, ResourceLockedException, IOException {

        RepositoryContext ctx = RepositoryContext.getRepositoryContext();
        if (ctx == null) {
            return this.repository.retrieve(token, uri, forProcessing);
        }

        
        Resource r = null;
        Throwable t = null;

        t = ctx.getResourceMiss(token, uri, forProcessing);
        if (t != null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Retrieval of resource " + uri
                             + " caused throwable: " + t);
            }

            throwAppropriateException(uri, t);
        }

        r = ctx.getResourceHit(token, uri, forProcessing);
        if (r != null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Retrieve resource " + uri
                             + ": found in cache");
            }
            return r;
        }

        try {
            if (logger.isDebugEnabled()) {
                logger.debug("Retrieve resource " + uri
                             + ": retrieving from repository");
            }
            r = this.repository.retrieve(token, uri, forProcessing);
            ctx.addResourceHit(token, r, forProcessing);
            return r;
        } catch (Throwable retrieveException) {
            if (logger.isDebugEnabled()) {
                logger.debug("Retrieve resource " + uri
                             + ": caching throwable: " + retrieveException);
            }
            ctx.addResourceMiss(token, uri, retrieveException, forProcessing);
            throwAppropriateException(uri, retrieveException);
            return null;
        }
    }

    public Resource[] listChildren(String token, String uri,
                                   boolean forProcessing)
        throws ResourceNotFoundException, AuthorizationException, 
        AuthenticationException, IOException {
        return this.repository.listChildren(token, uri, forProcessing);
    }

    public Resource store(String token, Resource resource)
        throws ResourceNotFoundException, AuthorizationException, 
        AuthenticationException, ResourceLockedException, 
        IllegalOperationException, ReadOnlyException, IOException {

        RepositoryContext ctx = RepositoryContext.getRepositoryContext();
        if (ctx != null) {
            ctx.clear();
        }
        // XXX: Fix this
        return this.repository.store(token, resource);
    }

    public void storeContent(String token, String uri, InputStream byteStream)
        throws AuthorizationException, AuthenticationException, 
        ResourceNotFoundException, ResourceLockedException, 
        IllegalOperationException, ReadOnlyException, IOException {

        RepositoryContext ctx = RepositoryContext.getRepositoryContext();
        if (ctx != null) {
            ctx.clear();
        }
        this.repository.storeContent(token, uri, byteStream);
    }

    public InputStream getInputStream(String token, String uri,
                                      boolean forProcessing)
        throws ResourceNotFoundException, AuthorizationException, 
        AuthenticationException, ResourceLockedException, IOException {

        return this.repository.getInputStream(token, uri, forProcessing);
    }

    public Resource createDocument(String token, String uri)
        throws IllegalOperationException, AuthorizationException, 
        AuthenticationException, ResourceLockedException, ReadOnlyException, 
        IOException {

        RepositoryContext ctx = RepositoryContext.getRepositoryContext();
        if (ctx != null) {
            ctx.clear();
        }        
        return this.repository.createDocument(token, uri);
    }

    public Resource createCollection(String token, String uri)
        throws AuthorizationException, AuthenticationException, 
        IllegalOperationException, ResourceLockedException, 
        ReadOnlyException, IOException {
        return this.repository.createCollection(token, uri);
    }

    public void copy(String token, String srcUri, String destUri, String depth,
                     boolean overwrite, boolean preserveACL)
        throws IllegalOperationException, AuthorizationException, 
        AuthenticationException, FailedDependencyException, 
        ResourceOverwriteException, ResourceLockedException, 
        ResourceNotFoundException, ReadOnlyException, IOException {

        RepositoryContext ctx = RepositoryContext.getRepositoryContext();
        if (ctx != null) {
            ctx.clear();
        }        
        this.repository.copy(token, srcUri, destUri, depth, overwrite, preserveACL);
    }

    public void move(String token, String srcUri, String destUri,
                     boolean overwrite)
        throws IllegalOperationException, AuthorizationException, 
        AuthenticationException, FailedDependencyException, 
        ResourceOverwriteException, ResourceLockedException, 
        ResourceNotFoundException, ReadOnlyException, IOException {

        RepositoryContext ctx = RepositoryContext.getRepositoryContext();
        if (ctx != null) {
            ctx.clear();
        }
        this.repository.move(token, srcUri, destUri, overwrite);
    }

    public void delete(String token, String uri)
        throws IllegalOperationException, AuthorizationException, 
        AuthenticationException, ResourceNotFoundException, 
        ResourceLockedException, FailedDependencyException, 
        ReadOnlyException, IOException {

        RepositoryContext ctx = RepositoryContext.getRepositoryContext();
        if (ctx != null) {
            ctx.clear();
        }
        this.repository.delete(token, uri);
    }

    public boolean exists(String token, String uri)
        throws AuthorizationException, AuthenticationException, IOException {

        RepositoryContext ctx = RepositoryContext.getRepositoryContext();
        if (ctx != null && (ctx.getResourceHit(token, uri, true) != null
                            || ctx.getResourceHit(token, uri, false) != null)) {
            return true;
        }
        return this.repository.exists(token, uri);
    }

    public Resource lock(String token, String uri, String ownerInfo,
                         String depth, int requestedTimoutSeconds,
                         String lockToken)
        throws ResourceNotFoundException, AuthorizationException, 
        AuthenticationException, FailedDependencyException, 
        ResourceLockedException, IllegalOperationException, 
        ReadOnlyException, IOException {

        RepositoryContext ctx = RepositoryContext.getRepositoryContext();
        if (ctx != null) {
            ctx.clear();
        }
        return this.repository.lock(token, uri, ownerInfo, depth,
                               requestedTimoutSeconds, lockToken);
    }

    public void unlock(String token, String uri, String lockToken)
        throws ResourceNotFoundException, AuthorizationException, 
        AuthenticationException, ResourceLockedException, ReadOnlyException, 
        IOException {

        RepositoryContext ctx = RepositoryContext.getRepositoryContext();
        if (ctx != null) {
            ctx.clear();
        }
        this.repository.unlock(token, uri, lockToken);
    }

    public void storeACL(String token, Resource resource)
        throws ResourceNotFoundException, AuthorizationException, 
        AuthenticationException, IllegalOperationException, 
        ReadOnlyException, IOException {

        RepositoryContext ctx = RepositoryContext.getRepositoryContext();
        if (ctx != null) {
            ctx.clear();
        }
        this.repository.storeACL(token, resource);
    }

    // XXX: Losing stack traces unnecessary
    private void throwAppropriateException(String uri, Throwable t) 
        throws AuthenticationException, AuthorizationException,
        FailedDependencyException, IOException, IllegalOperationException,
        ReadOnlyException, ResourceLockedException, ResourceNotFoundException,
        ResourceOverwriteException {

        t = t.fillInStackTrace();

        if (logger.isDebugEnabled()) {
            logger.debug("Re-throwing exception: " + t);
        }

        if (t instanceof AuthenticationException) {
            throw (AuthenticationException) t;
        }
        if (t instanceof AuthorizationException) {
            throw (AuthorizationException) t;
        }
        if (t instanceof FailedDependencyException) {
            throw (FailedDependencyException) t;
        }
        if (t instanceof IOException) {
            throw (IOException) t;
        }
        if (t instanceof IllegalOperationException) {
            throw (IllegalOperationException) t;
        }
        if (t instanceof ReadOnlyException) {
            throw (ReadOnlyException) t;
        }
        if (t instanceof ResourceLockedException) {
            throw (ResourceLockedException) t;
        }
        if (t instanceof ResourceNotFoundException) {
            throw (ResourceNotFoundException) t;
        }
        if (t instanceof ResourceOverwriteException) {
            throw (ResourceOverwriteException) t;
        }
        throw new RuntimeException(t);
    }
    
    public String getId() {
        return this.repository.getId();
    }


    public boolean isReadOnly() {
        return this.repository.isReadOnly();
    }


    public void setReadOnly(String token, boolean readOnly) throws AuthorizationException, IOException {
        this.repository.setReadOnly(token, readOnly);
    }


}

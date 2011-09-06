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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.vortikal.context.BaseContext;
import org.vortikal.repository.Path;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.service.Service;
import org.vortikal.web.service.URL;


/**
 * Request context. Lives throughout one request, and contains the
 * servlet request, the current {@link Service} and the requested
 * resource URI. The request context can be obtained from application
 * code in the following way:
 *
 * <pre>RequestContext requestContext = RequestContext.getRequestContext();</pre>
 *
 */
public class RequestContext {

    private final HttpServletRequest servletRequest;
    private final SecurityContext securityContext;
    private final boolean inRepository;
    private final Repository repository;
    private final Service service;
    private final Path resourceURI;
    private final Path currentCollection;
    private final Path indexFileURI;
    private final boolean isIndexFile;
    private List<Message> infoMessages = new ArrayList<Message>();
    private List<Message> errorMessages = new ArrayList<Message>();
    
    /**
     * Creates a new request context.
     *
     * @param servletRequest the current servlet request
     * @param service the resolved service
     * @param resource the current resource (may be null)
     * @param uri the URI of the current resource
     * @param indexFileURI the URI of the current index file
     * (<code>null</code> if no index file exists)
     */
    public RequestContext(HttpServletRequest servletRequest,
                          SecurityContext securityContext,
                          Service service, Resource resource, Path uri,
                          Path indexFileURI, boolean isIndexFile, 
                          boolean inRepository, Repository repository) {
        this.servletRequest = servletRequest;
        this.securityContext = securityContext;
        this.indexFileURI = indexFileURI;
        this.service = service;
        this.isIndexFile = isIndexFile;
        this.repository = repository;
        this.inRepository = inRepository;
        if (resource != null) {
            this.resourceURI = resource.getURI();
            if (resource.isCollection()) {
                this.currentCollection = resource.getURI();
            } else {
                this.currentCollection = resource.getURI().getParent();
            }
        } else {
            this.resourceURI = uri;
            this.currentCollection = null;
        }
    }
    
    /**
     * Creates a new request context without a resource object.
     * @deprecated this constructor is used only in unit tests
     */
    public RequestContext(HttpServletRequest servletRequest,
                          Service service, Path uri) {
        this(servletRequest, null, service, null, uri, null, false, true, null);
    }
    
    public static void setRequestContext(RequestContext requestContext) {
        BaseContext ctx = BaseContext.getContext();
        ctx.setAttribute(RequestContext.class.getName(), requestContext);
    }

    public static boolean exists() {
        if (BaseContext.exists()) {
            return BaseContext.getContext().getAttribute(RequestContext.class.getName()) != null;
        }
        return false;
    }

    /**
     * Gets the current request context.
     * 
     */
    public static RequestContext getRequestContext() {
        BaseContext ctx = BaseContext.getContext();
        RequestContext requestContext = (RequestContext)
            ctx.getAttribute(RequestContext.class.getName());
        return requestContext;
    }
    

    /**
     * Gets the current servlet request.
     * 
     * @return the servlet request
     */
    public HttpServletRequest getServletRequest() {
        return this.servletRequest;
    }

    /**
     * Gets the request URL
     */
    public URL getRequestURL() {
        return URL.create(this.servletRequest);
    }

    /**
     * Gets the current {@link Service} that this request executes
     * under.
     * 
     * @return the service, or <code>null</code> if there is no
     * current service.
     */
    public Service getService() {
        return this.service;
    }


    /**
     * Gets the {@link org.vortikal.repository.Resource#getURI URI}
     * that the current request maps to.
     * 
     * @return the URI of the requested resource.
     */
    public Path getResourceURI() {
        return this.resourceURI;
    }
    

    /**
     * Gets the URI of the current collection. If the request is for a
     * collection, the current collection and {@link #getResourceURI
     * resource URI} are the same, otherwise the current collection is
     * the nearest collection towards the root.
     */
    public Path getCurrentCollection() {
        return this.currentCollection;
    }
    

    /**
     * Gets the index file URI.
     * @returns the index file URI, or <code>null</code> if this is
     * not an index file request.
     */
    public Path getIndexFileURI() {
        return this.indexFileURI;
    }

    public void addInfoMessage(Message msg) {
        if (msg == null) {
            throw new IllegalArgumentException("Message cannot be null");
        }
        this.infoMessages.add(msg);
    }

    public List<Message> getInfoMessages() {
        return Collections.unmodifiableList(this.infoMessages);
    }
    
    public void addErrorMessage(Message msg) {
        if (msg == null) {
            throw new IllegalArgumentException("Message cannot be null");
        }
        this.errorMessages.add(msg);
    }

    public List<Message> getErrorMessages() {
        return Collections.unmodifiableList(this.errorMessages);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(getClass().getName());
        sb.append(": [");
        sb.append("resourceURI = ").append(this.resourceURI);
        sb.append(", service = ").append(this.service.getName());
        sb.append(", servletRequest = ").append(this.servletRequest);
        sb.append("]");
        return sb.toString();
    }

    public boolean isIndexFile() {
        return isIndexFile;
    }
    
    public boolean isInRepository() {
        return this.inRepository;
    }
    
    public Repository getRepository() {
        return this.repository;
    }
    
    public String getSecurityToken() {
        return this.securityContext.getToken();
    }
    
    public Principal getPrincipal() {
        return this.securityContext.getPrincipal();
    }
    
    public RepositoryTraversal rootTraversal(String token, Path uri) {
        return new RepositoryTraversal(this.repository, token, uri);
    }
    
    public static RepositoryTraversal rootTraversal(Repository repository, String token, Path uri) {
        return new RepositoryTraversal(repository, token, uri);
    }
    
    public static final class RepositoryTraversal {
        private Repository repository;
        private String token;
        private Path uri;
        private RepositoryTraversal(Repository repository, String token, Path uri) {
            if (repository == null) {
                throw new IllegalArgumentException("Repository is NULL");
            }
            this.repository = repository;
            this.token = token;
            this.uri = uri;
        }
        public void traverse(TraversalCallback callback) {
            Path uri = this.uri;
            while (uri != null) {
                try {
                    Resource resource = this.repository.retrieve(this.token, uri, true);
                    if (!callback.callback(resource)) {
                        return;
                    }
                } catch (Throwable t) {
                    if (!callback.error(uri, t)) {
                        return;
                    }
                }
                uri = uri.getParent();
            }
        }
    }
    
    public static interface TraversalCallback {
        public boolean callback(Resource resource);
        public boolean error(Path uri, Throwable error);
    }
}

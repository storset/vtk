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

import javax.servlet.http.HttpServletRequest;
import org.vortikal.context.BaseContext;
import org.vortikal.repository.Resource;
import org.vortikal.web.service.Service;
import org.vortikal.util.repository.URIUtil;


/**
 * Request context. Lives troughout one request, and contains the
 * servlet request, the current {@link Service} and the requested
 * resource URI. The request context can be obtained from application
 * code in the following way:
 *
 * <pre>RequestContext requestContext = RequestContext.getRequestContext();</pre>
 *
 */
public class RequestContext {

    private final HttpServletRequest servletRequest;
    private final Service service;
    private final String resourceURI;
    private final String currentCollection;
    private final String indexFileURI;
    private final boolean isIndexFile;
    
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
                          Service service, Resource resource, String uri,
                          String indexFileURI, boolean isIndexFile) {
        this.servletRequest = servletRequest;
        this.indexFileURI = indexFileURI;
        this.service = service;
        this.isIndexFile = isIndexFile;
        if (resource != null) {
            this.resourceURI = resource.getURI();
            if (resource.isCollection()) {
                this.currentCollection = resource.getURI();
            } else {
                this.currentCollection = URIUtil.getParentURI(resource.getURI());
            }
        } else {
            this.resourceURI = uri;
            this.currentCollection = null;
        }
    }
    
    /**
     * Creates a new request context without a resource object.
     */
    public RequestContext(HttpServletRequest servletRequest,
                          Service service, String uri) {
        this(servletRequest, service, null, uri, null, false);
    }

    public static void setRequestContext(RequestContext requestContext) {
        BaseContext ctx = BaseContext.getContext();
        ctx.setAttribute(RequestContext.class.getName(), requestContext);
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
    public String getResourceURI() {
        return this.resourceURI;
    }
    

    /**
     * Gets the URI of the current collection. If the request is for a
     * collection, the current collection and {@link #getResourceURI
     * resource URI} are the same, otherwise the current collection is
     * the nearest collection towards the root.
     */
    public String getCurrentCollection() {
        return this.currentCollection;
    }
    

    /**
     * Gets the index file URI.
     * @returns the index file URI, or <code>null</code> if this is
     * not an index file request.
     */
    public String getIndexFileURI() {
        return this.indexFileURI;
    }
    


    public String toString() {
        StringBuffer sb = new StringBuffer(getClass().getName());
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
    
}

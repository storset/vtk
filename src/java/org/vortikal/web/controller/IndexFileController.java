/* Copyright (c) 2006, University of Oslo, Norway
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
package org.vortikal.web.controller;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.servlet.mvc.LastModified;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;
import org.vortikal.web.servlet.VortikalServlet;


/**
 * Index file controller. Searches a list of index file names, and
 * uses a request dispatcher to forward the request to the first child
 * resource that matches any of the names in that list. If the current
 * resource is not a collection, this controller will fail.
 *
 * <p>The controller gets the name of the servlet to
 *   dispatch requests to (a
 *   <code>ServletContext.getNamedDispatcher()</code> from a required request attribute
 *   with id <code>VortikalServlet.SERVLET_NAME_REQUEST_ATTRIBUTE</code>.
 *
 * <p>Configurable JavaBean properties:
 * <ul>
 *   <li><code>indexFiles</code> - the list of index file names.
 *   <li><code>repository</code> - the content {@link Repository repository}
 * </ul>
 */
public class IndexFileController
  implements Controller, LastModified, InitializingBean, ServletContextAware {

    private Log logger = LogFactory.getLog(this.getClass());
    private String[] indexFiles;
    private Repository repository;
    private String servletName;
    private ServletContext servletContext;

    public void setIndexFiles(String[] indexFiles) {
        this.indexFiles = indexFiles;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }
    

    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
      
    }

    public void afterPropertiesSet() {
        if (this.indexFiles == null) {
            throw new BeanInitializationException(
                "JavaBean property 'indexFiles' not set");
        }
        if (this.repository == null) {
            throw new BeanInitializationException(
                "JavaBean property 'repository' not set");
        }
        if (this.servletContext == null) {
            throw new BeanInitializationException(
                "JavaBean property 'servletContext' not set");
        }
    }
    

    public long getLastModified(HttpServletRequest request) {
        return -1;
    }
    

    public ModelAndView handleRequest(HttpServletRequest request,
                                      HttpServletResponse response)
        throws Exception {

        SecurityContext securityContext = SecurityContext.getSecurityContext();
        RequestContext requestContext = RequestContext.getRequestContext();
        
        String token = securityContext.getToken();
        String currentURI = requestContext.getResourceURI();
        Resource res = this.repository.retrieve(token, currentURI, true);
        if (!res.isCollection()) {
            throw new IllegalStateException("Resource " + res + " is not a collection");
        }
        
        Resource indexFile = null;
        for (int i = 0; i < this.indexFiles.length; i++) {
            String indexURI = currentURI + "/" + this.indexFiles[i];
            try {
                indexFile = this.repository.retrieve(token, indexURI, true);
                break;
            } catch (Exception e) {
                continue;
            }
        }

        if (indexFile == null) {
            throw new IllegalStateException("No index file found under " + res);
        }

        long collectionLastMod = res.getLastModified().getTime();

        RequestWrapper requestWrapper = new RequestWrapper(request, indexFile.getURI(),
                                                           collectionLastMod);
        String servletName = (String)request.getAttribute(
                VortikalServlet.SERVLET_NAME_REQUEST_ATTRIBUTE);

        RequestDispatcher rd = this.servletContext.getNamedDispatcher(servletName);
        
        if (rd == null) {
            throw new RuntimeException(
                "No request dispatcher for name '" + this.servletName + "' available");
        }

        try {
            requestWrapper.setAttribute(VortikalServlet.INDEX_FILE_REQUEST_ATTRIBUTE, "true");
            rd.forward(requestWrapper, response);
        } finally {
            requestWrapper.removeAttribute(VortikalServlet.INDEX_FILE_REQUEST_ATTRIBUTE);
        }
        return null;
    }
    

    private class RequestWrapper extends HttpServletRequestWrapper {

        private String uri;
        private HttpServletRequest request;
        private long minLastMod = -1;
        
        public RequestWrapper(HttpServletRequest request, String uri, long minLastMod) {
            super(request);
            this.uri = uri;
            this.request = request;
            this.minLastMod = minLastMod;
        }
        
        public String getRequestURI() {
            return this.uri;
        }

        public String getHeader(String name) {
            if ("If-Modified-Since".equals(name)) {
                long lastMod = this.request.getDateHeader(name);
                if (lastMod > -1) {
                    if (lastMod > this.minLastMod) {
                        return this.request.getHeader(name);
                    }
                    return null;
                }
            }
            return this.request.getHeader(name);
        }
        
        public long getDateHeader(String name) {
            if ("If-Modified-Since".equals(name)) {
                long lastMod = this.request.getDateHeader(name);
                if (lastMod > -1) {
                    if (lastMod > this.minLastMod) {
                        return lastMod;
                    }
                }
                return -1;
            }
            return this.request.getDateHeader(name);
        }

    }

}

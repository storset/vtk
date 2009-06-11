/* Copyright (c) 2006, 2007, University of Oslo, Norway
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
package org.vortikal.web.display.index;

import java.nio.charset.Charset;
import java.util.Date;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.servlet.mvc.LastModified;
import org.vortikal.repository.AuthorizationException;
import org.vortikal.repository.Path;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.AuthenticationException;
import org.vortikal.security.SecurityContext;
import org.vortikal.util.web.HttpUtil;
import org.vortikal.web.RequestContext;
import org.vortikal.web.servlet.ConfigurableRequestWrapper;
import org.vortikal.web.servlet.VortikalServlet;


/**
 * Index file controller. Uses {@link RequestContext#indexFileURI} to
 * retrieve the actual index file. If the current resource is not a
 * collection, this controller will fail.
 *
 * <p>The controller gets the name of the servlet to
 *   dispatch requests to (a
 *   <code>ServletContext.getNamedDispatcher()</code> from a required request attribute
 *   with id <code>VortikalServlet.SERVLET_NAME_REQUEST_ATTRIBUTE</code>.
 *
 * <p>Configurable JavaBean properties:
 * <ul>
 *   <li><code>repository</code> - the content {@link Repository repository}
 *   <li><code>uriCharacterEncoding</code> - the character encoding to
 *   use when encoding the index file request URI. Default is
 *   <code>utf-8</code>.
 * </ul>
 */
public class IndexFileController
  implements Controller, LastModified, InitializingBean, ServletContextAware {

    private Repository repository;
    private ServletContext servletContext;
    private String uriCharacterEncoding = "utf-8";
    

    public void setRepository(Repository repository) {
        this.repository = repository;
    }
    

    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
      
    }

    public void setUriCharacterEncoding(String uriCharacterEncoding) {
        Charset.forName(uriCharacterEncoding);
        this.uriCharacterEncoding = uriCharacterEncoding;
    }
    

    public void afterPropertiesSet() {
        if (this.repository == null) {
            throw new BeanInitializationException(
                "JavaBean property 'repository' not set");
        }
        if (this.servletContext == null) {
            throw new BeanInitializationException(
                "JavaBean property 'servletContext' not set");
        }
        if (this.uriCharacterEncoding == null) {
            throw new BeanInitializationException(
                "JavaBean property 'uriCharacterEncoding' not set");
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
        Path currentURI = requestContext.getResourceURI();
        Resource res = this.repository.retrieve(token, currentURI, true);
        if (!res.isCollection()) {
            throw new IllegalStateException("Resource " + res + " is not a collection");
        }
        
        Path indexURI = requestContext.getIndexFileURI();
        Resource indexFile = null;
        try {
            indexFile = this.repository.retrieve(token, indexURI, true);
        } catch (AuthenticationException e) { 
            throw e;
        } catch (AuthorizationException e) { 
            throw e;
        } catch (Throwable t) { 
            throw new IllegalStateException("No index file found under " + res, t);
        }
        if (indexFile.isCollection()) {
            throw new IllegalStateException("Index file '" + indexURI
                                            + "' not a regular file");
        }

        long collectionLastMod = res.getLastModified().getTime();
        long requestLastMod = request.getDateHeader("If-Modified-Since");
        long ifModSince = -1;

        if (requestLastMod != -1) {
            ifModSince = Math.min(collectionLastMod, requestLastMod);
        }

        String encodedURI = new String(indexURI.toString().getBytes("utf-8"),
                                       this.uriCharacterEncoding);

        ConfigurableRequestWrapper requestWrapper = new ConfigurableRequestWrapper(request);
        requestWrapper.setRequestURI(encodedURI);

        if (ifModSince != -1) {
            requestWrapper.setHeader(
                "If-Modified-Since", HttpUtil.getHttpDateString(new Date(ifModSince)));
        }

        String servletName = (String) request.getAttribute(
                VortikalServlet.SERVLET_NAME_REQUEST_ATTRIBUTE);
        RequestDispatcher rd = this.servletContext.getNamedDispatcher(servletName);
        
        if (rd == null) {
            throw new RuntimeException(
                "No request dispatcher for name '" + servletName + "' available");
        }

        try {
            requestWrapper.setAttribute(VortikalServlet.INDEX_FILE_REQUEST_ATTRIBUTE, "true");
            rd.forward(requestWrapper, response);
        } finally {
            requestWrapper.removeAttribute(VortikalServlet.INDEX_FILE_REQUEST_ATTRIBUTE);
        }
        return null;
    }
    
}

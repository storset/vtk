/* Copyright (c) 2007, University of Oslo, Norway
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
package org.vortikal.web.view.decorating.components;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.springframework.web.context.ServletContextAware;

import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.ResourceNotFoundException;
import org.vortikal.security.SecurityContext;
import org.vortikal.util.cache.ContentCache;
import org.vortikal.util.io.StreamUtil;
import org.vortikal.util.repository.ContentTypeHelper;
import org.vortikal.web.RequestContext;
import org.vortikal.web.servlet.BufferedResponse;
import org.vortikal.web.servlet.VortikalServlet;
import org.vortikal.web.view.decorating.DecoratorRequest;
import org.vortikal.web.view.decorating.DecoratorResponse;

public class IncludeComponent extends AbstractDecoratorComponent implements ServletContextAware {

    private static final String INCLUDE_ATTRIBUTE_NAME =
        IncludeComponent.class.getName() + ".IncludeRequestAttribute";

    private ServletContext servletContext;

    private ContentCache httpIncludeCache;
    private ContentCache virtualIncludeCache;
    
    private Repository repository;

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    public void setHttpIncludeCache(ContentCache httpIncludeCache) {
        this.httpIncludeCache = httpIncludeCache;
    }
    
    public void render(DecoratorRequest request, DecoratorResponse response)
        throws Exception {

        String uri = request.getStringParameter("file");
        if (uri != null) {
            if (uri.startsWith("/"))
                throw new DecoratorComponentException(
                    "Include 'file' takes a relative path as argument");
            handleDirectInclude(uri, request, response);
            return;
        }
        uri = request.getStringParameter("virtual");

        if (uri == null) {
            throw new DecoratorComponentException(
                "One of parameters 'file' or 'virtual' must be specified");
        }
        if (uri.startsWith("/")) {
            handleVirtualInclude(uri, request, response);
        } else if (uri.startsWith("http") || uri.startsWith("https")) {
            handleHttpInclude(uri, request, response);
        } else {
            throw new DecoratorComponentException("Invalid 'virtual' parameter: '" + uri + "'");
        }
    }

    private void handleDirectInclude(String address, DecoratorRequest request,
                                     DecoratorResponse response) throws Exception {
        String token = SecurityContext.getSecurityContext().getToken();

        String uri = RequestContext.getRequestContext().getResourceURI();
        uri = uri.substring(0, uri.lastIndexOf("/") + 1) + address;

        Resource r = null;
        try {
            r = this.repository.retrieve(token, uri, false);
        } catch (ResourceNotFoundException e) {
            throw new DecoratorComponentException(
                    "Resource '" + uri + "' not found");
        }
        
        if (r.isCollection() || !ContentTypeHelper.isTextContentType(r.getContentType())) {
            throw new DecoratorComponentException(
                "Cannot include URI '" + uri + "': not a textual resource");
        }

        String characterEncoding = r.getCharacterEncoding();
        InputStream is = this.repository.getInputStream(token, uri, true);
        byte[] bytes = StreamUtil.readInputStream(is);
        
        response.setCharacterEncoding(characterEncoding);
        OutputStream out = response.getOutputStream();
        out.write(bytes);
        out.close();
    }
    
    private void handleVirtualInclude(String uri, DecoratorRequest request,
                                      DecoratorResponse response) throws Exception {
        
        HttpServletRequest servletRequest = request.getServletRequest();
        if (servletRequest.getAttribute(INCLUDE_ATTRIBUTE_NAME) != null) {
            throw new DecoratorComponentException(
                "Error including URI '" + uri + "': possible include loop detected ");
        }

        // XXX: encode URI?
        String encodedURI = uri;

        RequestWrapper requestWrapper = new RequestWrapper(servletRequest, encodedURI);
        requestWrapper.setAttribute(INCLUDE_ATTRIBUTE_NAME, new Object());
        
        String servletName = (String) servletRequest.getAttribute(
                VortikalServlet.SERVLET_NAME_REQUEST_ATTRIBUTE);

        RequestDispatcher rd = this.servletContext.getNamedDispatcher(servletName);
        
        if (rd == null) {
            throw new RuntimeException(
                "No request dispatcher for name '" + servletName + "' available");
        }

        BufferedResponse servletResponse = new BufferedResponse();
        rd.forward(requestWrapper, servletResponse);
        
        if (!ContentTypeHelper.isTextContentType(servletResponse.getContentType())) {
            throw new DecoratorComponentException(
                "Cannot include URI '" + uri + "': not a textual resource");
        }

        byte[] bytes = servletResponse.getContentBuffer();
        response.setCharacterEncoding(servletResponse.getCharacterEncoding());
        OutputStream out = response.getOutputStream();
        out.write(bytes);
        out.close();
    }
    
    private void handleHttpInclude(String uri, DecoratorRequest request,
            DecoratorResponse response) throws Exception {
        String result = (String) this.httpIncludeCache.get(uri);
        Writer writer = response.getWriter();
        writer.write(result);
        writer.close();
    }



    private class RequestWrapper extends HttpServletRequestWrapper {

        private String uri;
        
        public RequestWrapper(HttpServletRequest request, String uri) {
            super(request);
            this.uri = uri;
        }
        
        public String getRequestURI() {
            return this.uri;
        }
    }

}

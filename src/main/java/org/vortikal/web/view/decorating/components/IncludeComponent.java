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
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.context.ServletContextAware;

import org.vortikal.repository.AuthorizationException;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.ResourceNotFoundException;
import org.vortikal.security.AuthenticationException;
import org.vortikal.security.SecurityContext;
import org.vortikal.util.cache.ContentCache;
import org.vortikal.util.io.StreamUtil;
import org.vortikal.util.repository.ContentTypeHelper;
import org.vortikal.util.web.URLUtil;
import org.vortikal.web.RequestContext;
import org.vortikal.web.servlet.BufferedResponse;
import org.vortikal.web.servlet.VortikalServlet;
import org.vortikal.web.view.decorating.DecoratorRequest;
import org.vortikal.web.view.decorating.DecoratorResponse;

public class IncludeComponent extends AbstractDecoratorComponent
  implements ServletContextAware {

    private static final String PARAMETER_VIRTUAL = "virtual";
    private static final String PARAMETER_VIRTUAL_DESC =
        "Either a complete URL, or a path starting with '/'";
    private static final String PARAMETER_FILE = "file";
    private static final String PARAMETER_FILE_DESC =
        "A relative path to a the file to include";
    private static final String PARAMETER_AS_CURRENT_USER = "authenticated";
    private static final String PARAMETER_AS_CURRENT_USER_DESC = 
        "The default is that only resources readable for everyone is included. " +
            "If this is set to 'true', the include is done as the currently " +
            "logged in user (if any). This should only be used when the same " +
            "permissions apply to the resource including and the resource included." +
            "Note that this doesn't apply to virtual includes of full URLs.";
    
    static final String INCLUDE_ATTRIBUTE_NAME =
        IncludeComponent.class.getName() + ".IncludeRequestAttribute";

    private ServletContext servletContext;

    private ContentCache httpIncludeCache;
    
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

        String uri = request.getStringParameter(PARAMETER_FILE);

        if (uri != null) {
            if (uri.startsWith("/"))
                throw new DecoratorComponentException(
                    "Include 'file' takes a relative path as argument");
            String address = RequestContext.getRequestContext().getResourceURI();
            address = address.substring(0, address.lastIndexOf("/") + 1) + uri;

            handleDirectInclude(address, request, response);
            return;
        }

        uri = request.getStringParameter(PARAMETER_VIRTUAL);
        if (uri == null) {
            throw new DecoratorComponentException(
                "One of parameters 'file' or 'virtual' must be specified");
        }

        if (uri.startsWith("http") || uri.startsWith("https")) {
            handleHttpInclude(uri, request, response);
            return;
        } 

        if (!uri.startsWith("/")) {
            String requestURI = RequestContext.getRequestContext().getResourceURI();
            uri = requestURI.substring(0, requestURI.lastIndexOf("/") + 1) + uri;
        }
        handleVirtualInclude(uri, request, response);
    }


    private void handleDirectInclude(String address, DecoratorRequest request,
                                     DecoratorResponse response) throws Exception {
        String token = null;

        boolean asCurrentPrincipal = "true".equals(request.getStringParameter(
                                                       PARAMETER_AS_CURRENT_USER));

        if (asCurrentPrincipal) {
            token = SecurityContext.getSecurityContext().getToken();
        }
        
        Resource r = null;
        try {
            r = this.repository.retrieve(token, address, false);
        } catch (ResourceNotFoundException e) {
            throw new DecoratorComponentException(
                    "Resource '" + address + "' not found");
        } catch (AuthenticationException e) {
            if (asCurrentPrincipal)
                throw new DecoratorComponentException(
                    "Resource '" + address + "' requires authentication");
            throw new DecoratorComponentException(
                    "Resource '" + address + "' not readable with anonymous access");
        } catch (AuthorizationException e) {
            throw new DecoratorComponentException(
                    "Not authorized to read resource '" + address + "'");
        }
        
        if (r.isCollection() || !ContentTypeHelper.isTextContentType(r.getContentType())) {
            throw new DecoratorComponentException(
                "Cannot include URI '" + address + "': not a textual resource");
        }

        String characterEncoding = r.getCharacterEncoding();
        InputStream is = this.repository.getInputStream(token, address, true);
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

        String decodedURI = uri;

        Map<String, String[]> queryMap = new HashMap<String, String[]>();
        String queryString = null;
        
        if (uri.indexOf("?") != -1) {
            queryString = uri.substring(uri.indexOf("?") + 1);
            decodedURI = uri.substring(0, uri.indexOf("?"));
            queryMap = URLUtil.splitQueryString(queryString);
        }

        decodedURI = URLUtil.urlDecode(decodedURI);
        
        RequestWrapper requestWrapper = new RequestWrapper(
            servletRequest, decodedURI, queryMap, queryString);

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
        
        if (servletResponse.getStatus() != HttpServletResponse.SC_OK) {
            throw new DecoratorComponentException(
                "Included resource '" + uri + "' returned HTTP status code "
                + servletResponse.getStatus());
        }

        requestWrapper.setAttribute(INCLUDE_ATTRIBUTE_NAME, null);
        
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
        private Map<String, String[]> parameters;
        private String queryString;
        
        public RequestWrapper(HttpServletRequest request, String uri,
                              Map<String, String[]> parameters, String queryString) {
            super(request);
            this.uri = uri;
            this.parameters = parameters;
            this.queryString = queryString;
        }
        
        public String getRequestURI() {
            return this.uri;
        }

        public String getQueryString() {
            return this.queryString;
        }

        public String getParameter(String name) {
            String[] values = this.parameters.get(name);
            if (values == null || values.length <= 0) {
                return null;
            }
            return values[0];
        }

        public String[] getParameterValues(String name) {
            return this.parameters.get(name);
        }

        public Map getParameterMap() {
            return Collections.unmodifiableMap(this.parameters);
        }
        
        public Enumeration getParameterNames() {
            return Collections.enumeration(this.parameters.keySet());
        }
    }



    protected String getDescriptionInternal() {
        return "Includes the contents of another document in the page";
    }

    protected Map<String, String> getParameterDescriptionsInternal() {
        Map<String, String> map = new HashMap<String, String>();
        map.put(PARAMETER_FILE, PARAMETER_FILE_DESC);
        map.put(PARAMETER_VIRTUAL, PARAMETER_VIRTUAL_DESC);
        map.put(PARAMETER_AS_CURRENT_USER, PARAMETER_AS_CURRENT_USER_DESC);
        return map;
    }

}

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
package org.vortikal.web.controller;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.SecurityContext;
import org.vortikal.util.web.URLUtil;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;
import org.vortikal.web.servlet.BufferedResponse;
import org.vortikal.web.servlet.BufferedResponseWrapper;
import org.vortikal.web.servlet.HeaderAwareResponseWrapper;

/**
 * Controller for debugging requests. Performs an internal request on
 * the current resource and collects various data from the response.
 *
 * <p>Configurable JavaBean properties:
 * <ul>
 *   <li><code>service</code> - the {@link Service} for which to
 *   construct a URL and make the internal request for</li>
 *   <li><code>repository</code> - the {@link Repository content
 *   repository}</li>
 *   <li><code>viewName</code> - the name of the view to return
 *   (defaults to <code>debugRequest</code>)</li>
 *   <li><code>modelName</code> - name of the sub-model used in the
 *   main MVC model (defaults to <code>responseInfo</code>)</li>
 *   <li><code>identifyingParameter</code> - if set, tells this
 *   controller that this request parameter is used in the handler
 *   mapping to resolve itself, and causes the parameter to be removed
 *   from the set of parameters passed to the internal request. The
 *   default value is <code>debug-request</code>.</li>
 * </ul>
 * </p>
 *
 * <p>Model data provided:
 * <ul>
 *   <li><code>duration</code> - the length of the internal request
 *   (in milliseconds)</li>
 *   <lI><code>requestURL</code> - the URL used for the internal
 *   request</li>
 *   <lI><code>requestParameters</code> - the set of request
 *   parameters on the internal request</li>
 *   <li><code>method</code> - the HTTP method used for the internal
 *   request</li>
 *   <li><code>status</code> - the HTTP status code from the
 *   response</li>
 *   <li><code>headers</code> - the HTTP headers from the
 *   response</li>
 * </ul>
 * </p>
 */
public class DebugController implements Controller, InitializingBean, ServletContextAware  {

    private Service service;
    private Repository repository;
    private ServletContext servletContext;
    private String viewName = "debugRequest";
    private String modelName = "responseInfo";
    private String identifyingParameter = "debug-request";

    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response)
        throws Exception {
        
        String token = SecurityContext.getSecurityContext().getToken();
        String uri = RequestContext.getRequestContext().getResourceURI();
        Resource resource = this.repository.retrieve(token, uri, true);
        String url = this.service.constructLink(resource.getURI());

        String internalRequestURI = url.substring(url.indexOf("//") + 2);
        internalRequestURI = internalRequestURI.substring(internalRequestURI.indexOf("/"));
        
        Map parameters = URLUtil.splitQueryString(request);
        if (parameters.containsKey(this.identifyingParameter)) {
            parameters.remove(this.identifyingParameter);
        }
        RequestWrapper requestWrapper = new RequestWrapper(request, internalRequestURI, parameters);
        HeaderAwareResponseWrapper responseWrapper = new HeaderAwareResponseWrapper(
            new BufferedResponse());
        
        RequestDispatcher rd = servletContext.getRequestDispatcher(uri);
        long before = System.currentTimeMillis();
        rd.forward(requestWrapper, responseWrapper);
        long duration = System.currentTimeMillis() - before;

        Map responseInfo = new HashMap();
        responseInfo.put("duration", new Long(duration));
        responseInfo.put("requestURL", url);
        responseInfo.put("requestParameters", parameters);
        responseInfo.put("method", requestWrapper.getMethod());
        responseInfo.put("status", new Integer(responseWrapper.getStatus()));
        responseInfo.put("headers", responseWrapper.getHeaderMap());

        Map model = new HashMap();
        model.put(this.modelName, responseInfo);
        return new ModelAndView(this.viewName, model);
    }
    
    public void setService(Service service) {
        this.service = service;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }
    
    public void setViewName(String viewName) {
        this.viewName = viewName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public void setIdentifyingParameter(String identifyingParameter) {
        this.identifyingParameter = identifyingParameter;
    }

    public void afterPropertiesSet() {

    }
    
    private class RequestWrapper extends HttpServletRequestWrapper {

        private String uri;
        private Map parameters;

        public RequestWrapper(HttpServletRequest request, String uri, Map parameters) {
            super(request);
            this.uri = uri;
            this.parameters = parameters;
        }

        public String getRequestURI() {
            return super.getRequestURI();
        }

        public String getParameter(String name) {
            String[] values = (String[]) this.parameters.get(name);
            if (values == null || values.length == 0) {
                return null;
            }
            return values[0];
        }

        public Enumeration getParameterNames() {
            return Collections.enumeration(this.parameters.keySet());
        }
        
        
        public String[] getParameterValues(String name) {
            return (String[]) this.parameters.get(name);
        }

        public Map getParameterMap() {
            return Collections.unmodifiableMap(this.parameters);
        }
        
    }
    
}


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
package org.vortikal.web.service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.vortikal.repository.Repository;
import org.vortikal.repository.RepositoryException;
import org.vortikal.repository.Resource;
import org.vortikal.security.AuthenticationException;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;

/**
 * Handler interceptor retrieving resource on before handler excecution, 
 * effectively authorizing for read processed permission.
 * 
 * <p>Optionally delegates further resource evaluation to sub classes through
 * {@link #handleInternal}.
 * 
 * <p>Required JavaBean property:
 * <ul>
 *   <li><code>repository</code> - the {@link Repository}
 * </ul>
 */
public class ResourceRetrievingHandlerInterceptor implements InitializingBean, HandlerInterceptor {

    private Repository repository;
    
    public void setRepository(Repository repository) {
        this.repository = repository;
    }
    
    public final boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Resource resource = retrieveResource();
        return handleInternal(resource, request);
    }

    /**
     * Perform handler interception after resource has been retrieved.
     * @param resource  the resource
     * @param request the servlet request
     */
    protected boolean handleInternal(Resource resource, HttpServletRequest request) {
        return true;
    }

    protected Resource retrieveResource() 
        throws RepositoryException, AuthenticationException, Exception {
        
        RequestContext requestContext = RequestContext.getRequestContext();
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        String token = securityContext.getToken();

        Resource resource = this.repository.retrieve(token, requestContext.getResourceURI(), true);
        return resource;
    }



    public void afterPropertiesSet() throws Exception {
    if (this.repository == null) {
        throw new BeanInitializationException(
            "JavaBean property 'repository' not specified");
    }                
    }

    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
    }

    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
    }

}

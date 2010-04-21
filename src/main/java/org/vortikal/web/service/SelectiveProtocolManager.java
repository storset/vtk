/* Copyright (c) 2010, University of Oslo, Norway
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

import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;

/**
 * This class acts as a request protocol assertion ('*', 'http' or 'https'). 
 * If config property <code>selectiveAccessEnabled</code> is set,
 * it also allows access using unencrypted ('http') protocol for open 
 * resources on given services.
 * 
 * In order to function properly, this bean must be configured 
 * to handle the following aspects of a service:
 * <ul>
 *   <li>assertion</li>
 *   <li>handler interceptor</li>
 *   <li>url post processor</li>
 * </ul>
 */
public class SelectiveProtocolManager extends RequestProtocolAssertion 
    implements Assertion, HandlerInterceptor, URLPostProcessor {

    private Set<Service> fileServices;
    private Set<Service> collectionServices;
    private Repository repository;
    private boolean selectiveAccessEnabled = false;
    
    
    @Override
    public void processURL(URL url, Resource resource, Service service) throws Exception {
        if (!this.selectiveAccessEnabled) {
            return;
        }
        if (resource.isReadRestricted()) {
            return;
        }
        if (resource == null) {
            return;
        }
        Set<Service> services = resource.isCollection() ? 
                this.collectionServices : this.fileServices;
        if (services == null) {
            return;
        }
        if (!services.contains(service)) {
            return;
        }
        url.setProtocol("http");
    }

    @Override
    public void processURL(URL url, Service service) throws Exception {
    }

    @Override
    public boolean matches(HttpServletRequest request, Resource resource,
            Principal principal) {
        if (!this.selectiveAccessEnabled) {
            return super.matches(request, resource, principal);
        }
        if (!request.isSecure()) {
            return true;
        }
        return super.matches(request, resource, principal);
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
            HttpServletResponse response, Object handler) throws Exception {
        if (!this.selectiveAccessEnabled) {
            return true;
        }
        if (request.isSecure()) {
            return true;
        }
        RequestContext requestContext = RequestContext.getRequestContext();
        Resource resource = retrieveResource();

        if (resource.isReadRestricted()) {
            redirect(request, response);
            return false;
        }

        Service service = requestContext.getService();
        Set<Service> services = resource.isCollection() ? 
                this.collectionServices : this.fileServices;
        for (Service s: services) {
            if (service.equals(s) || service.isDescendantOf(s)) {
                return true;
            }
        }
        redirect(request, response);
        return false;
    }
    
    private void redirect(HttpServletRequest request, HttpServletResponse response) throws Exception {
        URL url = URL.create(request);
        url.setProtocol("https");
        response.sendRedirect(url.toString());
    }
    
    @Override
    public void postHandle(HttpServletRequest request,
                           HttpServletResponse response,
                           Object handler, ModelAndView modelAndView) {
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler, Exception ex) {
    }
    
    
    public void setFileServices(Set<Service> services) {
        this.fileServices = services;
    }

    public void setCollectionServices(Set<Service> services) {
        this.collectionServices = services;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }
    
    public void setSelectiveAccessEnabled(boolean selectiveAccessEnabled) {
        this.selectiveAccessEnabled = selectiveAccessEnabled;
    }

    private Resource retrieveResource() throws Exception {
        RequestContext requestContext = RequestContext.getRequestContext();
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        String token = securityContext.getToken();
        Resource resource = this.repository.retrieve(token, requestContext.getResourceURI(), true);
        return resource;
    }
    
}

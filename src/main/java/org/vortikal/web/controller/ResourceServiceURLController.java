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
package org.vortikal.web.controller;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;


/**
 * Controller that provides a reference (URL) to the requested
 * resource.
 *
 * <p>Configurable properties:
 * <ul>
 *   <li><code>repository</code> - the content repository
 *   <li><code>service</code> - the service used to construct the URL</li>
 *   <li><code>viewName</code> - the name of the returned view</li>
 *   <li><code>childNames</code> - a list of resource names. If this
 *   optional property is set, instead of creating a URL to the
 *   requested resource, the list of child names are traversed, and
 *   the first child resource whose name matches in the list will be
 *   used for the URL construction.
 * </ul>
 * </p>
 *
 * <p>Model data provided:
 * <ul>
 *   <li><code>resource</code> - the resource object</li>
 *   <li><code>resourceReference</code> - the URL</li>
 * </ul>
 */
public class ResourceServiceURLController implements InitializingBean, Controller {

    public static final String DEFAULT_VIEW_NAME = "resourceReference";
    
    private Service service = null;
    private String viewName = DEFAULT_VIEW_NAME;
    private String[] childNames = null;
    private Repository repository = null;


    public void setService(Service service) {
        this.service = service;
    }


    public void setViewName(String viewName) {
        this.viewName = viewName;
    }
    

    public void setChildName(String childName) {
        this.childNames = new String[] {childName};
    }
    
    public void setChildNames(String[] childNames) {
        this.childNames = childNames;
    }
    

    public void setRepository(Repository repository) {
        this.repository = repository;
    }
    
    
    public void afterPropertiesSet() throws Exception {
        if (this.service == null) {
            throw new BeanInitializationException(
                "Bean property 'service' must be set");
        }
        if (this.repository == null) {
            throw new BeanInitializationException(
                "Bean property 'repository' must be set");
        }
    }


    public ModelAndView handleRequest(HttpServletRequest arg0,
                                      HttpServletResponse arg1) throws Exception {

        SecurityContext securityContext = SecurityContext.getSecurityContext();
        Principal principal = securityContext.getPrincipal();
        RequestContext requestContext = RequestContext.getRequestContext();
        String token = securityContext.getToken();

        String uri = requestContext.getResourceURI();

        Resource resource = this.repository.retrieve(token, uri, false);
        Resource childResource = null;

        if (this.childNames != null && this.childNames.length > 0) {
            String[] children = resource.getChildURIs();
            for (int i = 0; i < children.length; i++) {
                String childName = children[i].substring(children[i].lastIndexOf("/") + 1);
                for (int j = 0; j < this.childNames.length; j++) {
                    if (this.childNames[j].equals(childName)) {
                        try {
                            childResource = this.repository.retrieve(token, children[i], true);
                            break;
                        } catch (Exception e) { }
                    }
                }
            }
        } 
        Map model = new HashMap();
        String resourceURL = this.service.constructLink(resource, principal, false);
        String childResourceURL = null;
        if (childResource != null) {
            childResourceURL = this.service.constructLink(childResource, principal, false);
        }

        model.put("resource", resource);
        model.put("resourceReference", resourceURL);

        model.put("childResource", childResource);
        model.put("childResourceReference", childResourceURL);
        

        return new ModelAndView(this.viewName, model);
    }
}


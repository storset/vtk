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
package org.vortikal.web.controller.repository;


import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;


/**
 * Controller that copies another resource.
 * TODO: Make this generic
 * 
 * <p>Configurable properties:
 * <ul>
 *   <li><code>repository</code> - the repository is required</li>
 *   <li><code>resourceName</code> - If this is is specified, create new resource with this name.
 *      Otherwise, use copied resource name</li>
 *   <li><code>templateUri</code> - required - Uri of the resource to copy. 
 *   <li><code>trustedToken</code> - If specified, use this token to copy. 
 *      Otherwise use the current security context.
 * 	 <li><code>successView</code> - default is 'redirect'
 * 	 <li><code>errorView</code> - default is 'admin'
 *  
 * </ul>
 * </p>
 *
 * <p>Model data provided:
 * <ul>
 *   <li><code>resource</code> - the resource to redirect to on success
 * 	 <li><code>error</code> - error message on error
 * </ul>
 */
public class CopyResourceController implements Controller,
        InitializingBean {
    
    private Log logger = LogFactory.getLog(CopyResourceController.class);
    private String trustedToken;
    private Repository repository;
    private String resourceName;
    private String templateUri;
    private String errorView = "admin";
    private String successView = "redirect";
    
    
    public ModelAndView handleRequest(HttpServletRequest req,
            HttpServletResponse resp) throws Exception {

        Map<String, Object> model = new HashMap<String, Object>();

        String uri = RequestContext.getRequestContext().getResourceURI();

        String token = this.trustedToken;
        if (token == null)
            token = SecurityContext.getSecurityContext().getToken();

        String name = this.resourceName;
        if (name == null) {
            Resource template = this.repository.retrieve(token, this.templateUri,false);
            name = template.getName();
        }

        // ensure the uri won't start with '//'
        String newResourceUri = "/" + name;
        if (! uri.equals("/")) {
            newResourceUri = uri + newResourceUri;
        }

        boolean exists = this.repository.exists(token, newResourceUri);

        if (exists) {
            model.put("createErrorMessage", "resource.exists");
            return new ModelAndView(this.errorView, model);
        }
            
        this.repository.copy(this.trustedToken, this.templateUri, newResourceUri, "infinity", false, true);
    	 	Resource newResource = this.repository.retrieve(this.trustedToken, newResourceUri, true);
            
        model.put("resource", newResource);
        
        return new ModelAndView(this.successView, model);
    }

    public void afterPropertiesSet() throws Exception {

        if (this.repository == null) 
            throw new BeanInitializationException("Property 'repository' required");
        
        if (this.templateUri == null)
            throw new BeanInitializationException("Property 'templateUri' required");
    
        if (! (this.trustedToken == null || this.repository.exists(this.trustedToken,this.templateUri)))
            //throw new BeanInitializationException("Property 'templateUri' must specify an existing resource");
            this.logger.warn("Property 'templateUri' must specify an existing resource");
    }

    public void setErrorView(String errorView) {
        this.errorView = errorView;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public void setSuccessView(String successView) {
        this.successView = successView;
    }

    public void setTrustedToken(String trustedToken) {
        this.trustedToken = trustedToken;
    }

    public void setTemplateUri(String templateUri) {
        this.templateUri = templateUri;
    }
    
    

}

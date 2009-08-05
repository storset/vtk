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
package org.vortikal.web.actions.delete;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;
import org.vortikal.repository.Path;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;

/**
 * Delete the requested resource from repository.
 * 
 * <p>
 * By default puts the parent resource in the model under the 'resource' name.
 * This can be overridden by specifying a path (relative or absolute) to another
 * resource.
 */
public class DeleteResourceController extends SimpleFormController implements InitializingBean {
	
    private Repository repository;
    private String viewName;
    private String resourcePath;
    private String trustedToken;
    private boolean requestFromResourceMenu;

	public void setRepository(Repository repository) {
        this.repository = repository;
    }


    public void setViewName(String viewName) {
        this.viewName = viewName;
    }


    public void setTrustedToken(String trustedToken) {
        this.trustedToken = trustedToken;
    }

    public void setResourcePath(String resourcePath) {
        this.resourcePath = resourcePath;
    }


    public void afterPropertiesSet() throws Exception {
        if (this.viewName == null)
            throw new BeanInitializationException("Property 'viewName' must be set");
    }


    protected Repository getRepository() {
        return this.repository;
    }


    protected String getTrustedToken() {
        return this.trustedToken;
    }


    protected String getViewName() {
        return this.viewName;
    }
    
	public void setRequestFromResourceMenu(boolean requestFromResourceMenu) {
		this.requestFromResourceMenu = requestFromResourceMenu;
	}

    protected Object formBackingObject(HttpServletRequest request)
    throws Exception {
	    RequestContext requestContext = RequestContext.getRequestContext();
	    SecurityContext securityContext = SecurityContext.getSecurityContext();
	    Service service = requestContext.getService();
	    
	    Path uri = requestContext.getResourceURI();
	    String token = securityContext.getToken();
	    Principal principal = securityContext.getPrincipal();
	
	    Resource resource = this.repository.retrieve(token, uri, false);
	    String url = service.constructLink(resource, principal);
	    
	    return new DeleteResourceCommand(url); 
    }
    
    protected ModelAndView onSubmit(Object command, BindException errors) throws Exception {
    	RequestContext requestContext = RequestContext.getRequestContext();
    	String token = this.trustedToken;
    	
    	if (token == null) {
    		SecurityContext securityContext = SecurityContext.getSecurityContext();
    		token = securityContext.getToken();
    	}
    	
    	Path uri = requestContext.getResourceURI();
    	Path parentUri = uri.getParent();
    	
    	DeleteResourceCommand deleteResourceCommand = (DeleteResourceCommand) command;
    	Resource modelResource = this.repository.retrieve(token, parentUri, false);
    	
    	if (deleteResourceCommand.getDeleteResourceAction() == null) {
    		Resource currentResource = this.repository.retrieve(token, uri, false);
    		// Don't redirect on cancel regarding an collection
    		if (currentResource.isCollection() && requestFromResourceMenu) { 
    			modelResource = currentResource;
    		}
    	} else { 
    		this.repository.delete(token, uri);
    		if (this.resourcePath != null) {
    		    Path newUri = uri.expand(this.resourcePath);
    			if (newUri != null) {
    				try {
    					modelResource = this.repository.retrieve(token, newUri, false);
    				} catch (Exception e) {
    					this.logger.info("Unable to retireve requested resource to view '" + newUri
    							+ "'", e);
    					// Do nothing
    				}
    			}
    		}
    	}
    	Map<String, Object> model = new HashMap<String, Object>();
    	model.put("resource", modelResource);
    	return new ModelAndView(this.viewName, model);
    }

}



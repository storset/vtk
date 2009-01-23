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
package org.vortikal.web.controller.repository.copy;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;
import org.vortikal.repository.Path;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;


public class CopyController extends SimpleFormController {

    private String cancelView;
    
    private CopyAction copyAction;
    private Repository repository = null;
    private String extension;
    private String resourceName;

    private boolean parentViewOnSuccess = false;
    
    @Required
    public void setRepository(Repository repository) {
        this.repository = repository;
    }
    
    public void setResourceName(String resourceName) {
		this.resourceName = resourceName;
	}


    protected Object formBackingObject(HttpServletRequest request) throws Exception {
        RequestContext requestContext = RequestContext.getRequestContext();
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        Service service = requestContext.getService();
        
        Resource resource = repository.retrieve(securityContext.getToken(),
                                                requestContext.getResourceURI(), false);
        String url = service.constructLink(resource, securityContext.getPrincipal());
        
        String name = resource.getName();
        if (name.indexOf(".") > 0)
            name = name.substring(0, name.lastIndexOf("."));
        
        if (this.extension != null)
                name += this.extension;
        
        if(this.resourceName != null)
        		name = this.resourceName;

        CopyCommand command = new CopyCommand(name, url);
        return command;
    }

	protected ModelAndView onSubmit(Object command) throws Exception {

        Map<String, Object> model = new HashMap<String, Object>();

        RequestContext requestContext = RequestContext.getRequestContext();
        String token = SecurityContext.getSecurityContext().getToken();
        
        Path uri = requestContext.getResourceURI();

        CopyCommand copyCommand =
            (CopyCommand) command;

        if (copyCommand.getCancelAction() != null) {
            copyCommand.setDone(true);
            return new ModelAndView(this.cancelView);
        }

        Path copyToCollection = uri.getParent();
        if (copyToCollection == null) copyToCollection = Path.fromString("/");
        Path copyUri = copyToCollection.extend(copyCommand.getName());
        copyAction.process(uri, copyUri);
        copyCommand.setDone(true);        
        
        Resource resource = null;
        
        if (this.parentViewOnSuccess)
            resource = this.repository.retrieve(token, copyToCollection, false);
        else
            resource = this.repository.retrieve(token, copyUri, false);
        
        model.put("resource", resource);

        return new ModelAndView(getSuccessView(), model);
    }

    public void setCancelView(String cancelView) {
        this.cancelView = cancelView;
    }

    @Required
    public void setCopyAction(CopyAction copyAction) {
        this.copyAction = copyAction;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public void setParentViewOnSuccess(boolean parentViewOnSuccess) {
        this.parentViewOnSuccess = parentViewOnSuccess;
    }
    

}


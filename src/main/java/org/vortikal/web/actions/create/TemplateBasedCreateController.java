/* Copyright (c) 2004,2008, University of Oslo, Norway
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
package org.vortikal.web.actions.create;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.servlet.mvc.SimpleFormController;
import org.vortikal.repository.Path;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.Repository.Depth;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;
import org.vortikal.web.templates.ResourceTemplate;
import org.vortikal.web.templates.ResourceTemplateManager;


public class TemplateBasedCreateController extends SimpleFormController {

    private ResourceTemplateManager templateManager;

    private Repository repository;

    @Required
    public void setRepository(Repository repository) {
        this.repository = repository;
    } 


    protected Object formBackingObject(HttpServletRequest request)
    throws Exception {
        RequestContext requestContext = RequestContext.getRequestContext();

        SecurityContext securityContext = SecurityContext.getSecurityContext();
        Service service = requestContext.getService();

        Path uri = requestContext.getResourceURI();
        String token = SecurityContext.getSecurityContext().getToken();

        Resource resource = this.repository.retrieve(securityContext.getToken(),
                requestContext.getResourceURI(), false);

        String url = service.constructLink(resource, securityContext.getPrincipal());

        CreateDocumentCommand command = new CreateDocumentCommand(url);

        List <ResourceTemplate> l = templateManager.getDocumentTemplates(token, uri);        

        // Set first available template as the selected 
        if (!l.isEmpty()) {
            command.setSourceURI(l.get(0).getUri().toString());
        }

        return command;
    }


    @SuppressWarnings("unchecked")
    protected Map referenceData(HttpServletRequest request) throws Exception {       
        RequestContext requestContext = RequestContext.getRequestContext();        
        SecurityContext securityContext = SecurityContext.getSecurityContext();

        Map<String, Object> model = new HashMap<String, Object>();

        Path uri = requestContext.getResourceURI();
        String token = securityContext.getToken();

        List <ResourceTemplate> l = templateManager.getDocumentTemplates(token, uri);

        Map <String, String> tmp = new LinkedHashMap <String, String>();
        for (ResourceTemplate t: l) {
            tmp.put(t.getUri().toString(),t.getName());
        }

        model.put("templates", tmp);		    	
        return model;
    }

    protected void doSubmitAction(Object command) throws Exception {        
        RequestContext requestContext = RequestContext.getRequestContext();
        SecurityContext securityContext = SecurityContext.getSecurityContext();

        CreateDocumentCommand createDocumentCommand =
            (CreateDocumentCommand) command;
        if (createDocumentCommand.getCancelAction() != null) {
            createDocumentCommand.setDone(true);
            return;
        }
        Path uri = requestContext.getResourceURI();
        String token = securityContext.getToken();

        // The location of the file that we shall copy
        Path sourceURI = Path.fromString(createDocumentCommand.getSourceURI());

        Path destinationURI = uri.extend(createDocumentCommand.getName());

        this.repository.copy(token, sourceURI, destinationURI, Depth.ZERO, false, false);
        createDocumentCommand.setDone(true);

    }


    public void setTemplateManager(ResourceTemplateManager templateManager) {
        this.templateManager = templateManager;
    }

}


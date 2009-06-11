/* Copyright (c) 2004, 2008, University of Oslo, Norway
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


import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.mvc.SimpleFormController;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;


public class CreateCollectionController extends SimpleFormController {

    private Repository repository = null;
    private boolean downcaseCollectionNames = false;
    private Map<String, String> replaceNameChars;
    private PropertyTypeDefinition userTitlePropDef;
    
    @Required public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public void setDowncaseCollectionNames(boolean downcaseCollectionNames) {
        this.downcaseCollectionNames = downcaseCollectionNames;
    }

    public void setReplaceNameChars(Map<String, String> replaceNameChars) {
        this.replaceNameChars = replaceNameChars;
    }

    @Required public void setUserTitlePropDef(PropertyTypeDefinition userTitlePropDef) {
        this.userTitlePropDef = userTitlePropDef;
    }

    protected Object formBackingObject(HttpServletRequest request) throws Exception {
        RequestContext requestContext = RequestContext.getRequestContext();
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        Service service = requestContext.getService();
        
        Resource resource = this.repository.retrieve(securityContext.getToken(),
                                                requestContext.getResourceURI(), false);
        String url = service.constructLink(resource, securityContext.getPrincipal());
         
        CreateCollectionCommand command =
            new CreateCollectionCommand(url);
        return command;
    }


    @Override
    protected void onBindAndValidate(HttpServletRequest request,
            Object command, BindException errors) throws Exception {
        super.onBindAndValidate(request, command, errors);
        RequestContext requestContext = RequestContext.getRequestContext();
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        
        CreateCollectionCommand createCollectionCommand =
            (CreateCollectionCommand) command;
        if (createCollectionCommand.getCancelAction() != null) {
            return;
        }
        Path uri = requestContext.getResourceURI();
        String token = securityContext.getToken();

        String name = createCollectionCommand.getName();       
        if (null == name || "".equals(name.trim())) {
            errors.rejectValue("name",
                               "manage.create.collection.missing.name",
                               "A name must be provided for the collection");
            return;
        }

        if (name.indexOf("/") >= 0) {
            errors.rejectValue("name",
                               "manage.create.collection.invalid.name",
                               "This is an invalid collection name");
        }
        name = fixCollectionName(name);
        Path newURI = uri.extend(name);
        try {
            boolean exists = this.repository.exists(token, newURI);
            if (exists) {
                errors.rejectValue("name",
                                   "manage.create.collection.exists",
                                   "A collection with this name already exists");
            }
        } catch (Exception e) {
            logger.warn("Unable to validate collection creation input", e);
        }
    }

    protected void doSubmitAction(Object command) throws Exception {        
        RequestContext requestContext = RequestContext.getRequestContext();
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        
        CreateCollectionCommand createCollectionCommand =
            (CreateCollectionCommand) command;
        if (createCollectionCommand.getCancelAction() != null) {
            createCollectionCommand.setDone(true);
            return;
        }
        Path uri = requestContext.getResourceURI();
        String token = securityContext.getToken();
        
        String title = createCollectionCommand.getName();
        String name = fixCollectionName(title);
        Path newURI = uri.extend(createCollectionCommand.getName());
        
        Resource collection = this.repository.createCollection(token, newURI);
        if (!title.equals(name)) {
            Property titleProp = collection.createProperty(this.userTitlePropDef);
            titleProp.setStringValue(title);
            this.repository.store(token, collection);
        }
        createCollectionCommand.setDone(true);
    }
    
    private String fixCollectionName(String name) {
        if (this.downcaseCollectionNames) {
            name = name.toLowerCase();
        }
        if (this.replaceNameChars != null) {
            for (String regex: this.replaceNameChars.keySet()) {
                String replacement = this.replaceNameChars.get(regex);
                name = name.replaceAll(regex, replacement);
            }
        }
        return name;
    }
}


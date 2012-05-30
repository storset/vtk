/* Copyright (c) 2004, 2008 University of Oslo, Norway
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
import org.springframework.validation.BindException;
import org.springframework.web.servlet.mvc.SimpleFormController;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.Repository.Depth;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;
import org.vortikal.web.templates.ResourceTemplate;
import org.vortikal.web.templates.ResourceTemplateManager;
import org.vortikal.web.view.freemarker.MessageLocalizer;

@SuppressWarnings("deprecation")
public class TemplateBasedCreateCollectionController extends SimpleFormController {

    private static final String NORMAL_FOLDER_IDENTIFIER = "NORMAL_FOLDER";

    private ResourceTemplateManager templateManager;
    private PropertyTypeDefinition userTitlePropDef;
    private PropertyTypeDefinition hiddenPropDef;
    private boolean downcaseCollectionNames = false;
    private Map<String, String> replaceNameChars;

    public void setDowncaseCollectionNames(boolean downcaseCollectionNames) {
        this.downcaseCollectionNames = downcaseCollectionNames;
    }

    public void setReplaceNameChars(Map<String, String> replaceNameChars) {
        this.replaceNameChars = replaceNameChars;
    }

    @Required
    public void setUserTitlePropDef(PropertyTypeDefinition userTitlePropDef) {
        this.userTitlePropDef = userTitlePropDef;
    }

    protected Object formBackingObject(HttpServletRequest request) throws Exception {
        RequestContext requestContext = RequestContext.getRequestContext();
        Service service = requestContext.getService();
        Repository repository = requestContext.getRepository();
        Resource resource = repository.retrieve(requestContext.getSecurityToken(), requestContext.getResourceURI(),
                false);
        String url = service.constructLink(resource, requestContext.getPrincipal());

        CreateCollectionCommand command = new CreateCollectionCommand(url);

        Path uri = requestContext.getResourceURI();
        String token = SecurityContext.getSecurityContext().getToken();

        List<ResourceTemplate> templates = this.templateManager.getFolderTemplates(token, uri);

        // Set first available template as the selected
        if (!templates.isEmpty()) {
            command.setSourceURI(NORMAL_FOLDER_IDENTIFIER);
        }

        return command;
    }

    @SuppressWarnings( { "unchecked" })
    protected Map referenceData(HttpServletRequest request) throws Exception {
        RequestContext requestContext = RequestContext.getRequestContext();
        Map<String, Object> model = new HashMap<String, Object>();

        Path uri = requestContext.getResourceURI();
        String token = requestContext.getSecurityToken();

        List<ResourceTemplate> templates = this.templateManager.getFolderTemplates(token, uri);

        HttpServletRequest servletRequest = requestContext.getServletRequest();
        org.springframework.web.servlet.support.RequestContext springRequestContext = new org.springframework.web.servlet.support.RequestContext(
                servletRequest);
        Map<String, String> tmp = new LinkedHashMap<String, String>();
        Map<String, String> reverseTemplates = new HashMap<String, String>();

        String standardCollectionName = new MessageLocalizer("property.standardCollectionName", "Standard collection",
                null, springRequestContext).get(null).toString();

        // puts normal folder lexicographically correct
        for (ResourceTemplate t : templates) {
            if (standardCollectionName.compareTo(t.getTitle()) < 1) {
                tmp.put(standardCollectionName, NORMAL_FOLDER_IDENTIFIER);
                reverseTemplates.put(NORMAL_FOLDER_IDENTIFIER, standardCollectionName);
            }
            tmp.put(t.getTitle(), t.getUri().toString());
            reverseTemplates.put(t.getUri().toString(), t.getTitle());
        }

        if (!tmp.containsKey(standardCollectionName) && !tmp.isEmpty()) {
            // if normal folder is lexicographically last
            tmp.put(standardCollectionName, NORMAL_FOLDER_IDENTIFIER);
            reverseTemplates.put(NORMAL_FOLDER_IDENTIFIER, standardCollectionName);
        }

        model.put("reverseTemplates", reverseTemplates);
        model.put("templates", tmp);
        return model;
    }

    protected void doSubmitAction(Object command) throws Exception {
        CreateCollectionCommand createFolderCommand = (CreateCollectionCommand) command;
        if (createFolderCommand.getCancelAction() != null) {
            createFolderCommand.setDone(true);
            return;
        }
        RequestContext requestContext = RequestContext.getRequestContext();
        Repository repository = requestContext.getRepository();
        Path uri = requestContext.getResourceURI();
        String token = requestContext.getSecurityToken();

        // The location of the folder to copy
        String source = createFolderCommand.getSourceURI();
        if (source == null || source.equals(NORMAL_FOLDER_IDENTIFIER)) {
            // Just create a new folder if no "folder-template" is selected
            createNewFolder(command, uri, requestContext);
            createFolderCommand.setDone(true);
            return;
        }
        Path sourceURI = Path.fromString(source);

        String title = createFolderCommand.getTitle();
        String name = fixCollectionName(createFolderCommand.getName());

        // Setting the destination to the current folder/uri
        Path destinationURI = uri.extend(name);

        // Copy folder-template to destination (implicit rename)
        repository.copy(token, sourceURI, destinationURI, Depth.ZERO, false, false);
        Resource dest = repository.retrieve(token, destinationURI, false);

        dest.removeProperty(this.userTitlePropDef);

        if (title == null || "".equals(title))
            title = name.substring(0, 1).toUpperCase() + name.substring(1);

        Property titleProp = this.userTitlePropDef.createProperty();
        titleProp.setStringValue(title);
        dest.addProperty(titleProp);

        // hiddenPropDef can only be true or unset.
        if (createFolderCommand.getHidden()) {
            Property hiddenProp = this.hiddenPropDef.createProperty();
            hiddenProp.setBooleanValue(true);
            dest.addProperty(hiddenProp);
        }

        repository.store(token, dest);
        createFolderCommand.setDone(true);
    }

    private void createNewFolder(Object command, Path uri, RequestContext requestContext) throws Exception {
        CreateCollectionCommand createCollectionCommand = (CreateCollectionCommand) command;

        String title = createCollectionCommand.getTitle();
        String name = fixCollectionName(createCollectionCommand.getName());
        Path newURI = uri.extend(name);
        Repository repository = requestContext.getRepository();
        String token = requestContext.getSecurityToken();
        Resource collection = repository.createCollection(token, newURI);

        if (title == null || "".equals(title))
            title = name.substring(0, 1).toUpperCase() + name.substring(1);

        Property titleProp = this.userTitlePropDef.createProperty();
        titleProp.setStringValue(title);
        collection.addProperty(titleProp);

        // hiddenPropDef can only be true or not set.
        if (createCollectionCommand.getHidden()) {
            Property hiddenProp = this.hiddenPropDef.createProperty();
            hiddenProp.setBooleanValue(true);
            collection.addProperty(hiddenProp);
        }

        repository.store(token, collection);
    }

    public void setHiddenPropDef(PropertyTypeDefinition hiddenPropDef) {
        this.hiddenPropDef = hiddenPropDef;
    }

    public void setTemplateManager(ResourceTemplateManager templateManager) {
        this.templateManager = templateManager;
    }

    private String fixCollectionName(String name) {
        if (this.downcaseCollectionNames) {
            name = name.toLowerCase();
        }
        if (this.replaceNameChars != null) {
            for (String regex : this.replaceNameChars.keySet()) {
                String replacement = this.replaceNameChars.get(regex);
                name = name.replaceAll(regex, replacement);
            }
        }
        return name;
    }

    @Override
    protected void onBindAndValidate(HttpServletRequest request, Object command, BindException errors) throws Exception {

        super.onBindAndValidate(request, command, errors);
        RequestContext requestContext = RequestContext.getRequestContext();

        CreateCollectionCommand createCollectionCommand = (CreateCollectionCommand) command;
        if (createCollectionCommand.getCancelAction() != null) {
            return;
        }
        Path uri = requestContext.getResourceURI();
        String token = requestContext.getSecurityToken();
        Repository repository = requestContext.getRepository();

        String name = createCollectionCommand.getName();
        if (null == name || "".equals(name.trim())) {
            errors.rejectValue("name", "manage.create.collection.missing.name",
                    "A name must be provided for the collection");
            return;
        }

        if (name.contains("/")) {
            errors.rejectValue("name", "manage.create.collection.invalid.name", "This is an invalid collection name");
            return;
        }

        name = fixCollectionName(name);

        if (name.isEmpty()) {
            errors.rejectValue("name", "manage.create.collection.invalid.name", "This is an invalid collection name");
            return;
        }

        Path newURI;
        try {
            newURI = uri.extend(name);
        } catch (Throwable t) {
            errors.rejectValue("name", "manage.create.collection.invalid.name", "This is an invalid collection name");
            return;
        }

        if (repository.exists(token, newURI)) {
            errors.rejectValue("name", "manage.create.collection.exists", "A collection with this name already exists");
            return;
        }

        if (newURI.isAncestorOf(uri)) {
            errors.rejectValue("name", "manage.create.collection.invalid.destination",
                    "Cannot copy a collection into itself");
        }
    }
}

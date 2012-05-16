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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.Repository.Depth;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;
import org.vortikal.web.templates.ResourceTemplate;
import org.vortikal.web.templates.ResourceTemplateManager;

@SuppressWarnings("deprecation")
public class TemplateBasedCreateController extends SimpleFormController {

    private ResourceTemplateManager templateManager;
    private boolean downcaseNames = false;
    private Map<String, String> replaceNameChars;
    private String cancelView;
    private PropertyTypeDefinition descriptionPropDef;
    private final String titlePlaceholder = "#title#";

    protected Object formBackingObject(HttpServletRequest request) throws Exception {
        RequestContext requestContext = RequestContext.getRequestContext();
        Service service = requestContext.getService();
        Path uri = requestContext.getResourceURI();
        String token = requestContext.getSecurityToken();
        Repository repository = requestContext.getRepository();

        Resource resource = repository.retrieve(token, uri, false);
        String url = service.constructLink(resource, requestContext.getPrincipal());
        CreateDocumentCommand command = new CreateDocumentCommand(url);

        List<ResourceTemplate> l = this.templateManager.getDocumentTemplates(token, uri);
        // Set first available template as the selected
        if (!l.isEmpty()) {
            command.setSourceURI(l.get(0).getUri().toString());
        }
        return command;
    }

    protected Map<String, Object> referenceData(HttpServletRequest request) throws Exception {
        RequestContext requestContext = RequestContext.getRequestContext();
        String token = requestContext.getSecurityToken();

        Map<String, Object> model = new HashMap<String, Object>();
        Path uri = requestContext.getResourceURI();
        List<ResourceTemplate> l = templateManager.getDocumentTemplates(token, uri);

        Map<String, String> templates = new LinkedHashMap<String, String>();
        Map<String, String> descriptions = new HashMap<String, String>();
        Map<String, Boolean> titles = new HashMap<String, Boolean>();

        Property dp;
        Resource r;
        Repository repository = requestContext.getRepository();
        for (ResourceTemplate t : l) {
            r = repository.retrieve(token, t.getUri(), false);
            if ((dp = r.getProperty(descriptionPropDef)) != null)
                descriptions.put(t.getUri().toString(), dp.getFormattedValue());

            titles.put(t.getUri().toString(), r.getTitle().equals(titlePlaceholder));

            templates.put(t.getUri().toString(), t.getName());
        }
        model.put("templates", templates);
        model.put("descriptions", descriptions);
        model.put("titles", titles);
        return model;
    }

    @Override
    protected void onBindAndValidate(HttpServletRequest request, Object command, BindException errors) throws Exception {
        super.onBindAndValidate(request, command, errors);

        CreateDocumentCommand createDocumentCommand = (CreateDocumentCommand) command;

        if (createDocumentCommand.getCancelAction() != null)
            return;

        if (createDocumentCommand.getSourceURI() == null || createDocumentCommand.getSourceURI().trim().equals("")) {
            errors.rejectValue("sourceURI", "manage.create.document.missing.template",
                    "You must choose a document type");
        }

        RequestContext requestContext = RequestContext.getRequestContext();

        Path uri = requestContext.getResourceURI();
        String token = requestContext.getSecurityToken();
        Repository repository = requestContext.getRepository();

        String title = createDocumentCommand.getTitle();
        Resource source = repository.retrieve(token, Path.fromString(createDocumentCommand.getSourceURI()), false);
        if ((title == null || "".equals(title.trim())) && source.getTitle().equals(titlePlaceholder)) {
            errors.rejectValue("title", "manage.create.document.missing.title",
                    "A title must be provided for the document");
            return;
        }

        String name;
        if (createDocumentCommand.getIsIndex())
            name = "index";
        else
            name = createDocumentCommand.getName();

        if (name == null || "".equals(name.trim())) {
            errors.rejectValue("name", "manage.create.document.missing.name",
                    "A name must be provided for the document");
            return;
        }

        if (name.indexOf("/") >= 0) {
            errors.rejectValue("name", "manage.create.document.invalid.name", "This is an invalid document name");
            return;
        }
        name = fixDocumentName(name);

        if (name.isEmpty()) {
            errors.rejectValue("name", "manage.create.document.invalid.name", "This is an invalid document name");
            return;
        }

        Path destinationURI = uri.extend(name);

        if (repository.exists(token, destinationURI)) {
            errors.rejectValue("name", "manage.create.document.exists", "A resource of this name already exists");
        }
    }

    protected ModelAndView onSubmit(Object command) throws Exception {
        Map<String, Object> model = new HashMap<String, Object>();

        CreateDocumentCommand createDocumentCommand = (CreateDocumentCommand) command;
        if (createDocumentCommand.getCancelAction() != null) {
            createDocumentCommand.setDone(true);
            return new ModelAndView(this.cancelView);
        }
        RequestContext requestContext = RequestContext.getRequestContext();
        Path uri = requestContext.getResourceURI();
        String token = requestContext.getSecurityToken();
        Repository repository = requestContext.getRepository();

        // The location of the file that we will be copying
        Path sourceURI = Path.fromString(createDocumentCommand.getSourceURI());

        String name;
        if (createDocumentCommand.getIsIndex())
            name = "index";
        else
            name = createDocumentCommand.getName();

        name = fixDocumentName(name);
        Path destinationURI = uri.extend(name);

        repository.copy(token, sourceURI, destinationURI, Depth.ZERO, false, false);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(repository.getInputStream(token,
                destinationURI, false)));

        String line;
        while ((line = reader.readLine()) != null) {
            os.write(line.replaceAll(titlePlaceholder, createDocumentCommand.getTitle()).getBytes());
        }

        Resource r = repository.storeContent(token, destinationURI, new ByteArrayInputStream(os.toByteArray()));
        repository.store(token, r);

        createDocumentCommand.setDone(true);

        model.put("resource", r);

        return new ModelAndView(getSuccessView(), model);
    }

    @Required
    public void setTemplateManager(ResourceTemplateManager templateManager) {
        this.templateManager = templateManager;
    }

    public void setReplaceNameChars(Map<String, String> replaceNameChars) {
        this.replaceNameChars = replaceNameChars;
    }

    public void setCancelView(String cancelView) {
        this.cancelView = cancelView;
    }

    public void setDescriptionPropDef(PropertyTypeDefinition descriptionPropDef) {
        this.descriptionPropDef = descriptionPropDef;
    }

    public void setDowncaseNames(boolean downcaseNames) {
        this.downcaseNames = downcaseNames;
    }

    private String fixDocumentName(String name) {
        if (this.downcaseNames) {
            name = name.toLowerCase() + ".html";
        }

        if (this.replaceNameChars != null) {
            for (String regex : this.replaceNameChars.keySet()) {
                String replacement = this.replaceNameChars.get(regex);
                name = name.replaceAll(regex, replacement);
            }
        }
        return name;
    }
}

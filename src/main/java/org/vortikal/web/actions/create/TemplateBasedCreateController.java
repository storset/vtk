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
import java.util.TreeMap;
import java.util.regex.Matcher;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Repository.Depth;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.text.html.HtmlUtil;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;
import org.vortikal.web.templates.ResourceTemplate;
import org.vortikal.web.templates.ResourceTemplateManager;

@SuppressWarnings("deprecation")
public class TemplateBasedCreateController extends SimpleFormController {

    private final String titlePlaceholder = "#title#";

    private ResourceTemplateManager templateManager;
    private boolean downcaseNames = false;
    private Map<String, String> replaceNameChars;
    private String cancelView;
    private PropertyTypeDefinition[] removePropList;
    private PropertyTypeDefinition descriptionPropDef;

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
        // Set first available template
        if (!l.isEmpty()) {
            boolean hasDefault = false;
            String name;
            String[] split;
            Resource r;
            Property dp;
            Repository repo = RequestContext.getRequestContext().getRepository();
            for (ResourceTemplate t : l) {
                try {
                    r = repo.retrieve(token, t.getUri(), false);

                    if ((dp = r.getProperty(this.descriptionPropDef)) != null) {
                        name = dp.getFormattedValue();

                        if (name.contains("|")) {
                            // Need to escape | for split
                            split = name.split("\\|");

                            // Default
                            if (split.length >= 3 && "default".equals(split[2])) {
                                command.setSourceURI(t.getUri().toString());
                                hasDefault = true;
                                break;
                            }
                        }
                    }
                } catch (Exception ignore) {
                }
            }

            // If no default template
            if (!hasDefault)
                command.setSourceURI(l.get(0).getUri().toString());
        }

        return command;
    }

    protected Map<String, Object> referenceData(HttpServletRequest request) throws Exception {
        RequestContext requestContext = RequestContext.getRequestContext();
        String token = requestContext.getSecurityToken();

        Map<String, Object> model = new HashMap<String, Object>();
        Path uri = requestContext.getResourceURI();
        List<ResourceTemplate> l = this.templateManager.getDocumentTemplates(token, uri);

        Map<String, String> sortmap = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
        Map<String, String> templates = new LinkedHashMap<String, String>();
        Map<String, String> descriptions = new HashMap<String, String>();
        Map<String, Boolean> titles = new HashMap<String, Boolean>();

        String name;
        String[] split;
        Resource r;
        Property dp;
        boolean noDefault = true;
        Repository repo = RequestContext.getRequestContext().getRepository();
        for (ResourceTemplate t : l) {
            try {
                r = repo.retrieve(token, t.getUri(), false);

                if ((dp = r.getProperty(this.descriptionPropDef)) != null) {
                    name = dp.getFormattedValue();

                    if (name.contains("|")) {
                        // Need to escape | for split
                        split = name.split("\\|");

                        // Name
                        if (split.length >= 1 && !"".equals(split[0]))
                            name = split[0];
                        else
                            name = t.getName();

                        // Description
                        if (split.length >= 2 && !"".equals(split[1]))
                            descriptions.put(t.getUri().toString(), split[1]);

                        // Default or not
                        if (noDefault && split.length >= 3 && "default".equals(split[2])) {
                            templates.put(t.getUri().toString(), name);
                            noDefault = false;
                        }
                        else
                            sortmap.put(t.getUri().toString(), name);

                    } else
                        sortmap.put(t.getUri().toString(), name);

                } else
                    sortmap.put(t.getUri().toString(), t.getName());

            } catch (Exception ignore) {
                sortmap.put(t.getUri().toString(), t.getName());
            }

            // Title field
            titles.put(t.getUri().toString(), t.getTitle().equals(this.titlePlaceholder));
        }
        // Merge default templates and sorted templates
        templates.putAll(sortmap);

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

        name = fixDocumentName(name);

        if (name.indexOf("/") >= 0) {
            errors.rejectValue("name", "manage.create.document.invalid.name", "This is an invalid document name");
            return;
        }

        if (name.isEmpty()) {
            errors.rejectValue("name", "manage.create.document.invalid.name", "This is an invalid document name");
            return;
        }

        // The location of the file that we will be copying
        Path sourceURI = Path.fromString(createDocumentCommand.getSourceURI());

        String filetype = sourceURI.toString().substring(sourceURI.toString().lastIndexOf('.'));
        if (!name.endsWith(filetype))
            name += filetype;

        // Indexpages can only be html files
        if (createDocumentCommand.getIsIndex() && !filetype.equals(".html")) {
            errors.rejectValue("name", "manage.create.index.invalid.filetype",
                    "This is an invalid filetype for an indexpage");
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

        String filetype = sourceURI.toString().substring(sourceURI.toString().lastIndexOf('.'));
        if (!name.endsWith(filetype))
            name += filetype;

        Path destinationURI = uri.extend(name);

        repository.copy(token, sourceURI, destinationURI, Depth.ZERO, false, false);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(repository.getInputStream(token,
                destinationURI, false)));

        String line, title = createDocumentCommand.getTitle();
        if (title == null)
            title = "";

        String contentType = repository.retrieve(token, destinationURI, false).getContentType();
        if (contentType.equals("application/json")) {
            title = Matcher.quoteReplacement(title);
            title = title.replaceAll("\"", "\\\\\"");
        } else if (contentType.equals("text/html")) {
            title = HtmlUtil.escapeHtmlString(title);
        }
        title = Matcher.quoteReplacement(title);

        while ((line = reader.readLine()) != null) {
            os.write(line.replaceAll(this.titlePlaceholder, title).getBytes());
        }

        Resource r = repository.storeContent(token, destinationURI, new ByteArrayInputStream(os.toByteArray()));

        for (PropertyTypeDefinition ptd : this.removePropList)
            r.removeProperty(ptd);

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

    public void setRemovePropList(PropertyTypeDefinition[] removePropList) {
        this.removePropList = removePropList;
    }

    public void setDescriptionPropDef(PropertyTypeDefinition descriptionPropDef) {
        this.descriptionPropDef = descriptionPropDef;
    }

    public void setCancelView(String cancelView) {
        this.cancelView = cancelView;
    }

    public void setDowncaseNames(boolean downcaseNames) {
        this.downcaseNames = downcaseNames;
    }

    private String fixDocumentName(String name) {
        if (this.downcaseNames) {
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
}
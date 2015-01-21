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
package vtk.web.actions.create;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;
import vtk.repository.Path;
import vtk.repository.Property;
import vtk.repository.Repository;
import vtk.repository.Resource;
import vtk.repository.resourcetype.PropertyTypeDefinition;
import vtk.text.html.HtmlUtil;
import vtk.util.io.StreamUtil;
import vtk.web.RequestContext;
import vtk.web.service.Service;
import vtk.web.templates.ResourceTemplate;
import vtk.web.templates.ResourceTemplateManager;

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

        Resource collection = repository.retrieve(token, uri, false);
        String url = service.constructLink(collection, requestContext.getPrincipal());
        String collectionType = collection.getResourceType();
        
        CreateDocumentCommand command = new CreateDocumentCommand(url);

        List<ResourceTemplate> l = templateManager.getDocumentTemplates(token, uri);
        
        // Set first available template
        if (!l.isEmpty()) {
            boolean hasDefault = false;
            String name;
            String[] split;
            Resource r;
            Property dp;
            for (ResourceTemplate t : l) {
                try {
                    r = repository.retrieve(token, t.getUri(), false);

                    if ((dp = r.getProperty(descriptionPropDef)) != null) {
                        name = dp.getFormattedValue();

                        if (name.contains("|")) {
                            // Need to escape | for split
                            split = name.split("\\|");

                            // Default
                            if (split.length >= 3 && "default".equals(split[2])) {
                                command.setSourceURI(t.getUri().toString());
                                hasDefault = true;
                            }
                            // Recommended (overrides default)
                            if(split.length >= 4 && collectionType.equals(split[3])) {
                                command.setSourceURI(t.getUri().toString());
                                command.setIsRecommended(true);
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
        Repository repo = RequestContext.getRequestContext().getRepository();
        Resource collection = repo.retrieve(token, uri, false);
        String collectionType = collection.getResourceType();
        
        List<ResourceTemplate> l = templateManager.getDocumentTemplates(token, uri);

        Map<String, List<String>> sortmap = new TreeMap<String, List<String>>(String.CASE_INSENSITIVE_ORDER);
        Map<String, String> templates = new LinkedHashMap<String, String>();
        Map<String, String> recommendedTemplates = new LinkedHashMap<String, String>();
        Map<String, String> descriptions = new HashMap<String, String>();
        Map<String, Boolean> titles = new HashMap<String, Boolean>();

        String name;
        String[] split;
        Resource r;
        Property dp;
        boolean noDefault = true, noRecommended = true, put;
        
        for (ResourceTemplate t : l) {
            put = false;
            try {
                r = repo.retrieve(token, t.getUri(), false);

                if ((dp = r.getProperty(descriptionPropDef)) != null) {
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
                            put = true;
                        }
                        // Recommended or not
                        if (noRecommended && split.length >= 4 && collectionType.equals(split[3])) {
                            recommendedTemplates.put(t.getUri().toString(), name);
                            noRecommended = false;
                            put = true;
                        }
                    }

                } else
                    name = t.getName();

            } catch (Exception ignore) {
                name = t.getName();
            }

            if (!put) {
                if (sortmap.containsKey(name)) {
                    List<String> list = sortmap.get(name);
                    list.add(t.getUri().toString());
                } else {
                    ArrayList<String> list = new ArrayList<String>();
                    list.add(t.getUri().toString());
                    sortmap.put(name, list);
                }
            }

            // Title field
            titles.put(t.getUri().toString(), t.getTitle().equals(titlePlaceholder));
        }
        
        // Merge default templates and sorted templates
        for (String key : sortmap.keySet()) {
            List<String> list = sortmap.get(key);
            for (String value : list)
                templates.put(value, key);
        }
        // Merge into recommended templates if not empty
        // XXX: more efficient code?
        if(!recommendedTemplates.isEmpty()) {
          recommendedTemplates.putAll(templates);
          model.put("templates", recommendedTemplates);
        } else {
          model.put("templates", templates);
        }

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
            return;
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
            return new ModelAndView(cancelView);
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

        repository.copy(token, sourceURI, destinationURI, false, false);

        String stream = StreamUtil.streamToString(repository.getInputStream(token, destinationURI, false));

        String title = createDocumentCommand.getTitle();
        if (title == null)
            title = "";

        String contentType = repository.retrieve(token, destinationURI, false).getContentType();
        if (contentType.equals("application/json")) {
            title = Matcher.quoteReplacement(title);
            title = title.replaceAll("\"", "\\\\\"");
        } else if (contentType.equals("text/html")) {
            title = HtmlUtil.encodeBasicEntities(title);
        }
        title = Matcher.quoteReplacement(title);

        Resource r = repository.storeContent(token, destinationURI,
                StreamUtil.stringToStream(stream.replaceAll(titlePlaceholder, title)));

        if (removePropList != null) {
            for (PropertyTypeDefinition ptd : removePropList) {
                r.removeProperty(ptd);
            }
        }

        model.put("resource", repository.store(token, r));

        createDocumentCommand.setDone(true);

        return new ModelAndView(getSuccessView(), model);
    }

    private String fixDocumentName(String name) {
        if (downcaseNames) {
            name = name.toLowerCase();
        }

        if (replaceNameChars != null) {
            for (String regex : replaceNameChars.keySet()) {
                String replacement = replaceNameChars.get(regex);
                name = name.replaceAll(regex, replacement);
            }
        }
        return name;
    }

    @Required
    public void setTemplateManager(ResourceTemplateManager templateManager) {
        this.templateManager = templateManager;
    }

    public void setDowncaseNames(boolean downcaseNames) {
        this.downcaseNames = downcaseNames;
    }

    public void setReplaceNameChars(Map<String, String> replaceNameChars) {
        this.replaceNameChars = replaceNameChars;
    }

    @Required
    public void setCancelView(String cancelView) {
        this.cancelView = cancelView;
    }

    public void setRemovePropList(PropertyTypeDefinition[] removePropList) {
        this.removePropList = removePropList;
    }

    @Required
    public void setDescriptionPropDef(PropertyTypeDefinition descriptionPropDef) {
        this.descriptionPropDef = descriptionPropDef;
    }

}

/* Copyright (c) 2009, University of Oslo, Norway
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
package org.vortikal.resourcemanagement.edit;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import net.sf.json.JSONObject;

import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Repository.Depth;
import org.vortikal.repository.Resource;
import org.vortikal.repository.Revision;
import org.vortikal.repository.store.Revisions;
import org.vortikal.resourcemanagement.EditablePropertyDescription;
import org.vortikal.resourcemanagement.JSONPropertyAttributeDescription;
import org.vortikal.resourcemanagement.JSONPropertyDescription;
import org.vortikal.resourcemanagement.PropertyDescription;
import org.vortikal.resourcemanagement.StructuredResource;
import org.vortikal.resourcemanagement.StructuredResourceDescription;
import org.vortikal.resourcemanagement.StructuredResourceManager;
import org.vortikal.security.Principal;
import org.vortikal.text.html.HtmlFragment;
import org.vortikal.text.html.HtmlPageFilter;
import org.vortikal.text.html.HtmlPageParser;
import org.vortikal.util.io.StreamUtil;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;
import org.vortikal.web.service.URL;

public class StructuredResourceEditor extends SimpleFormController {

    private StructuredResourceManager resourceManager;
    private HtmlPageFilter safeHtmlFilter;
    private Service listComponentsService;

    public StructuredResourceEditor() {
        super();
        setCommandName("form");
    }
    

    @Override
    protected Object formBackingObject(HttpServletRequest request) throws Exception {
        lock();
        RequestContext requestContext = RequestContext.getRequestContext();
        Path uri = requestContext.getResourceURI();
        String token = requestContext.getSecurityToken();
        Repository repository = requestContext.getRepository();
        
        Resource resource = repository.retrieve(token, uri, false);
        boolean published = false;
        Property p = resource.getPropertyByPrefix(null, "published");
        if (p != null && p.getBooleanValue()) {
            published = true;
        }
        StructuredResourceDescription description = this.resourceManager.get(resource.getResourceType());

        Revision workingCopy = null;
        for (Revision rev: repository.getRevisions(token, uri)) {
            if (rev.getType() == Revision.Type.WORKING_COPY) {
                workingCopy = rev;
                break;
            }
        }
        
        InputStream stream = null;

        if (workingCopy !=  null) {
            stream = repository.getInputStream(token, uri, true, workingCopy);
        } else {
            stream = repository.getInputStream(token, uri, true);
        }
        String encoding = resource.getCharacterEncoding();
        if (encoding == null) {
            encoding = "utf-8";
        }
        String source = StreamUtil.streamToString(stream, encoding);
        StructuredResource structuredResource = new StructuredResource(description);
        structuredResource.parse(source);

        URL url = RequestContext.getRequestContext().getService().constructURL(uri);
        URL listComponentServiceURL = listComponentsService.constructURL(uri);

        return new FormSubmitCommand(structuredResource, url, listComponentServiceURL, 
                workingCopy != null, published);
    }

    @Override
    protected ModelAndView onSubmit(Object command) throws Exception {
        FormSubmitCommand form = (FormSubmitCommand) command;
        if (form.getCancelAction() != null) {
            unlock();
            return new ModelAndView(getSuccessView());
        }
        Map<String, Object> model = new HashMap<String, Object>();
        model.put("form", command);
        form.sync();
        RequestContext requestContext = RequestContext.getRequestContext();
        Repository repository = requestContext.getRepository();
        Path uri = RequestContext.getRequestContext().getResourceURI();
        String token = requestContext.getSecurityToken();
        
        boolean saveWorkingCopy = form.getSaveWorkingCopyAction() != null
                || (form.getUpdateAction() != null && form.isWorkingCopy())
                || (form.getUpdateViewAction() != null && form.isWorkingCopy());
        
        boolean makePublicVersion = form.getMakePublicVersionAction() != null;
        boolean deleteWorkingCopy = form.getDeleteWorkingCopyAction() != null;

        Revision workingCopy = null;
        
        for (Revision rev: repository.getRevisions(token, uri)) {
            if (rev.getType() == Revision.Type.WORKING_COPY) {
                workingCopy = rev;
                break;
            }
        }
        
        if (deleteWorkingCopy && workingCopy != null) {
            repository.deleteRevision(token, uri, workingCopy);
            form.setWorkingCopy(false);
            return new ModelAndView(getFormView(), model);
        }

        byte[] buffer = form.getResource().toJSON().toString(3).getBytes("utf-8");
        InputStream stream = new ByteArrayInputStream(buffer);
        
        if (saveWorkingCopy && workingCopy == null) {
            workingCopy = repository.createRevision(token, uri, Revision.Type.WORKING_COPY);
        }

        if (makePublicVersion && workingCopy != null) {
            repository.createRevision(token, uri, Revision.Type.REGULAR);
            
            repository.storeContent(token, uri, stream, workingCopy);
            repository.storeContent(token, uri, 
                    repository.getInputStream(token, uri, false, workingCopy));
            repository.deleteRevision(token, uri, workingCopy);
            form.setWorkingCopy(false);
            
        } else if (saveWorkingCopy) {
            repository.storeContent(token, uri, stream, workingCopy);
            form.setWorkingCopy(true);
            
        } else {
            List<Revision> revisions = repository.getRevisions(token, uri);
            Revision prev = revisions.size() == 0 ? null : revisions.get(0);
            String checksum = Revisions.checksum(buffer);
            
            if (prev == null || !checksum.equals(prev.getChecksum())) {
                // Take snapshot of previous version:
                repository.createRevision(token, uri, Revision.Type.REGULAR);
            }
            repository.storeContent(token, uri, stream);
            form.setWorkingCopy(false);
        }

        if (form.getUpdateViewAction() != null) {
            unlock();
            return new ModelAndView(getSuccessView());
        }
        return new ModelAndView(getFormView(), model);
    }

    @Override
    protected ServletRequestDataBinder createBinder(HttpServletRequest request, Object command) throws Exception {
        FormSubmitCommand form = (FormSubmitCommand) command;
        FormDataBinder binder = new FormDataBinder(command, getCommandName(), form.getResource().getType());
        prepareBinder(binder);
        initBinder(request, binder);
        return binder;
    }

    private class FormDataBinder extends ServletRequestDataBinder {
        private StructuredResourceDescription description;

        public FormDataBinder(Object target, String objectName, StructuredResourceDescription description) {
            super(target, objectName);
            this.description = description;
        }

        @Override
        public void bind(ServletRequest request) {
            List<PropertyDescription> props = this.description.getAllPropertyDescriptions();
            FormSubmitCommand form = (FormSubmitCommand) getTarget();
            for (PropertyDescription desc : props) {
                if (desc instanceof EditablePropertyDescription) {

                    if ("simple_html".equals(desc.getType())) {
                        runSimpleHtmlFilter(request, form, desc);

                    } else if (desc instanceof JSONPropertyDescription) {
                        buildJSONFromInput(request, form, desc);

                    } else {
                        storePostedValue(request, form, desc);
                    }
                }
            }
            super.bind(request);
        }

        private void storePostedValue(ServletRequest request, FormSubmitCommand form, PropertyDescription desc) {
            String posted = request.getParameter(desc.getName());
            if (desc.isTrim()) {
                posted = posted.trim();
            }
            bindObjectToForm(form, desc, posted);
        }

        private void runSimpleHtmlFilter(ServletRequest request, FormSubmitCommand form, PropertyDescription desc) {
            String filteredValue;
            try {
                filteredValue = filterValue(request.getParameter(desc.getName()));
                bindObjectToForm(form, desc, filteredValue);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @SuppressWarnings("unchecked")
        private void buildJSONFromInput(ServletRequest request, FormSubmitCommand form, PropertyDescription desc) {
            JSONPropertyDescription jsonDesc = (JSONPropertyDescription) desc;

            if (!jsonDesc.isMultiple()) {
                JSONObject obj = new JSONObject();
                for (JSONPropertyAttributeDescription attr : jsonDesc.getAttributes()) {
                    String param = desc.getName() + "." + attr.getName() + ".0";
                    String posted = request.getParameter(param);
                    if (posted != null && !"".equals(posted.trim())) {
                        obj.put(attr.getName(), posted);
                    }
                }
                bindObjectToForm(form, desc, obj);
                return;
            }

            Enumeration<String> names = request.getParameterNames();
            int maxIndex = 0;
            while (names.hasMoreElements()) {
                String input = names.nextElement();
                for (JSONPropertyAttributeDescription attr : jsonDesc.getAttributes()) {
                    String prefix = desc.getName() + "." + attr.getName() + ".";
                    if (input.startsWith(prefix)) {
                        int i = Integer.parseInt(input.substring(prefix.length()));
                        maxIndex = Math.max(maxIndex, i);
                    }
                }
            }
            List<JSONObject> resultList = new ArrayList<JSONObject>();
            for (int i = 0; i <= maxIndex; i++) {
                JSONObject obj = new JSONObject();
                for (JSONPropertyAttributeDescription attr : jsonDesc.getAttributes()) {
                    String input = desc.getName() + "." + attr.getName() + "." + i;
                    String posted = request.getParameter(input);
                    if (posted != null && !"".equals(posted.trim())) {
                        obj.put(attr.getName(), posted);
                    }
                }
                if (!obj.isEmpty()) {
                    resultList.add(obj);
                }
            }
            bindObjectToForm(form, desc, resultList);

        }

        private void bindObjectToForm(FormSubmitCommand form, PropertyDescription desc, Object obj) {
            try {
                form.bind(desc.getName(), obj);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private String filterValue(String value) throws Exception {
        HtmlPageParser parser = new HtmlPageParser();
        HtmlFragment fragment;
        fragment = parser.parseFragment(value);
        fragment.filter(safeHtmlFilter);
        return fragment.getStringRepresentation();
    }

    public void unlock() throws Exception {
        RequestContext requestContext = RequestContext.getRequestContext();
        String token = requestContext.getSecurityToken();
        Path uri = requestContext.getResourceURI();
        requestContext.getRepository().unlock(token, uri, null);
    }

    public void lock() throws Exception {
        RequestContext requestContext = RequestContext.getRequestContext();
        String token = requestContext.getSecurityToken();
        Path uri = requestContext.getResourceURI();
        Principal principal = requestContext.getPrincipal();
        requestContext.getRepository().lock(token, uri, principal.getQualifiedName(), Depth.ZERO, 600, null);
    }

    public void setResourceManager(StructuredResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }

    public void setSafeHtmlFilter(HtmlPageFilter safeHtmlFilter) {
        this.safeHtmlFilter = safeHtmlFilter;
    }

    public HtmlPageFilter getSafeHtmlFilter() {
        return safeHtmlFilter;
    }

    public void setListComponentsService(Service listComponentsService) {
        this.listComponentsService = listComponentsService;
    }

    public Service getListComponentsService() {
        return listComponentsService;
    }

}

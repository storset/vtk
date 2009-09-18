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
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.Repository.Depth;
import org.vortikal.resourcemanagement.EditablePropertyDescription;
import org.vortikal.resourcemanagement.JSONPropertyDescription;
import org.vortikal.resourcemanagement.PropertyDescription;
import org.vortikal.resourcemanagement.StructuredResource;
import org.vortikal.resourcemanagement.StructuredResourceDescription;
import org.vortikal.resourcemanagement.StructuredResourceManager;
import org.vortikal.security.Principal;
import org.vortikal.security.SecurityContext;
import org.vortikal.util.io.StreamUtil;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.URL;

public class StructuredResourceEditor extends SimpleFormController {

    private StructuredResourceManager resourceManager;
    private Repository repository;

    public StructuredResourceEditor() {
        super();
        setCommandName("form");
    }

    protected Object formBackingObject(HttpServletRequest request) throws Exception {
        lock();
        Path uri = RequestContext.getRequestContext().getResourceURI();
        String token = SecurityContext.getSecurityContext().getToken();
        Resource resource = this.repository.retrieve(token, uri, false);
        StructuredResourceDescription description = this.resourceManager.get(resource.getResourceType());

        InputStream stream = this.repository.getInputStream(token, uri, true);
        byte[] buff = StreamUtil.readInputStream(stream);
        String encoding = resource.getCharacterEncoding();
        if (encoding == null)
            encoding = "utf-8";
        String source = new String(buff, encoding);
        StructuredResource structuredResource = new StructuredResource(description);
        structuredResource.parse(source);

        URL url = RequestContext.getRequestContext().getService().constructURL(uri);

        return new FormSubmitCommand(structuredResource, url);
    }

    protected ModelAndView onSubmit(Object command) throws Exception {
        FormSubmitCommand form = (FormSubmitCommand) command;
        if (form.getCancelAction() != null) {
            unlock();
            return new ModelAndView(getSuccessView());
        }
        Map<String, Object> model = new HashMap<String, Object>();
        model.put("form", command);
        form.sync();
        Path uri = RequestContext.getRequestContext().getResourceURI();
        String token = SecurityContext.getSecurityContext().getToken();

        InputStream stream = new ByteArrayInputStream(form.getResource().toJSON().toString(3).getBytes("utf-8"));
        this.repository.storeContent(token, uri, stream);

        if (form.getUpdateQuitAction() != null) {
            unlock();
            return new ModelAndView(getSuccessView());
        }
        return new ModelAndView(getFormView(), model);
    }

    public void setResourceManager(StructuredResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
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
        @SuppressWarnings("unchecked")
        public void bind(ServletRequest request) {
            List<PropertyDescription> props = this.description.getAllPropertyDescriptions();
            FormSubmitCommand form = (FormSubmitCommand) getTarget();
            for (PropertyDescription desc : props) {
                if (desc instanceof EditablePropertyDescription) {
                    if (desc instanceof JSONPropertyDescription) {
                        // Build JSON from input, invoke form.bind(JSON)
                        JSONPropertyDescription jsonDesc = (JSONPropertyDescription) desc;
                        if (jsonDesc.isMultiple()) {
                            Enumeration<String> names = request.getParameterNames();
                            int maxIndex = 0;
                            while (names.hasMoreElements()) {
                                String input = names.nextElement();
                                for (String attr : jsonDesc.getAttributes()) {
                                    String prefix = desc.getName() + "." + attr + ".";
                                    if (input.startsWith(prefix)) {
                                        int i = Integer.parseInt(input.substring(prefix.length()));
                                        maxIndex = Math.max(maxIndex, i);
                                    }
                                }
                            }
                            List<JSONObject> resultList = new ArrayList<JSONObject>();
                            for (int i = 0; i <= maxIndex; i++) {
                                JSONObject obj = new JSONObject();
                                for (String attr : jsonDesc.getAttributes()) {
                                    String input = desc.getName() + "." + attr + "." + i;
                                    obj.put(attr, request.getParameter(input));
                                }
                                resultList.add(obj);
                            }
                            try {
                                form.bind(desc.getName(), resultList);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }

                        } else {
                            JSONObject obj = new JSONObject();
                            for (String attr : jsonDesc.getAttributes()) {
                                String param = desc.getName() + "." + attr + ".0";
                                String posted = request.getParameter(param);
                                if (posted != null) {
                                    obj.put(attr, posted);
                                }
                            }
                            try {
                                form.bind(desc.getName(), obj);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }

                        }

                    } else {
                        String posted = request.getParameter(desc.getName());
                        try {
                            form.bind(desc.getName(), posted);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
            super.bind(request);
        }
    }

    public void unlock() throws Exception {
        String token = SecurityContext.getSecurityContext().getToken();
        Path uri = RequestContext.getRequestContext().getResourceURI();
        this.repository.unlock(token, uri, null);
    }

    public void lock() throws Exception {
        String token = SecurityContext.getSecurityContext().getToken();
        Path uri = RequestContext.getRequestContext().getResourceURI();
        Principal principal = SecurityContext.getSecurityContext().getPrincipal();
        this.repository.lock(token, uri, principal.getQualifiedName(), Depth.ZERO, 600, null);
    }

}

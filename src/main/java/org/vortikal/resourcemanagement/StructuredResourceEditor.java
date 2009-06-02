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
package org.vortikal.resourcemanagement;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;
import org.vortikal.repository.Path;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
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
        Path uri = RequestContext.getRequestContext().getResourceURI();
        String token = SecurityContext.getSecurityContext().getToken();
        Resource resource = this.repository.retrieve(token, uri, false);
        StructuredResourceDescription description = this.resourceManager.get(resource.getResourceType());

        InputStream stream = this.repository.getInputStream(token, uri, true);
        byte[] buff = StreamUtil.readInputStream(stream);
        String encoding = resource.getCharacterEncoding();
        if (encoding == null) encoding = "utf-8";
        String source = new String(buff, encoding);
        StructuredResource structuredResource = new StructuredResource(description);
        structuredResource.parse(source);

        URL url = RequestContext.getRequestContext().getService().constructURL(uri);
        
        return new Form(structuredResource, url);
    }
    
    protected ModelAndView onSubmit(Object command) throws Exception {
        Form form = (Form) command;
        Map<String, Object> model = new HashMap<String, Object>();
        model.put("form", command);
        form.sync();
        
        Path uri = RequestContext.getRequestContext().getResourceURI();
        String token = SecurityContext.getSecurityContext().getToken();

        InputStream stream = new ByteArrayInputStream(
                form.getResource().toJSON().toString().getBytes("utf-8"));
        this.repository.storeContent(token, uri, stream);

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
        Form form = (Form) command;
        FormDataBinder binder = new FormDataBinder(command, getCommandName(), form.getResource().getType());
        prepareBinder(binder);
        initBinder(request, binder);
        return binder;
    }

    private class FormDataBinder extends ServletRequestDataBinder {
        private StructuredResourceDescription description;
        
        public FormDataBinder(Object target, String objectName, 
                StructuredResourceDescription description) {
            super(target, objectName);
            this.description = description;
        }
        
        @Override
        public void bind(ServletRequest request) {
            List<PropertyDescription> props = this.description.getAllPropertyDescriptions();
            Form form = (Form) getTarget();
            for (PropertyDescription desc : props) {
                String posted = request.getParameter(desc.getName());
                form.bind(desc.getName(), posted);                
            }
            super.bind(request);
        }
    }
    
    public class Form {
        private URL url;
        private StructuredResource resource;
        private List<FormElement> elements = new ArrayList<FormElement>();
        
        public Form(StructuredResource resource, URL url) {
            this.resource = resource;
            this.url = url;
            StructuredResourceDescription type = resource.getType();
            for (PropertyDescription def: type.getAllPropertyDescriptions()) {
                this.elements.add(new FormElement(def, null, resource.getProperty(def.getName())));
            }
        }
        
        public List<FormElement> getFormElements() {
            return Collections.unmodifiableList(this.elements);
        }
        
        public StructuredResource getResource() {
            return this.resource;
        }
        
        public URL getURL() {
            return this.url;
        }
        
        public void bind(String name, String value) {
            FormElement elem = findElement(name);
            if (elem == null) {
                throw new IllegalArgumentException("No such element: " + name);
            }
            elem.setValue(value);
        }
        
        private FormElement findElement(String name) {
            for (FormElement elem : this.elements) {
                if (elem.getDescription().getName().equals(name)) {
                    return elem;
                }
            }
            return null;
        }
        
        public void sync() {
            List<PropertyDescription> descriptions = 
                this.resource.getType().getAllPropertyDescriptions();
            for (PropertyDescription desc: descriptions) {
                String name = desc.getName();
                FormElement elem = findElement(name);
                Object value = elem.getValue();
                if (value != null) {
                    this.resource.removeProperty(name);
                    this.resource.addProperty(name, value);
                }
            }
        }

    }

    
    public class FormElement {
        private PropertyDescription description;
        private ValidationError error;
        private Object value;
        
        public FormElement(PropertyDescription description, ValidationError error, Object value) {
            this.description = description;
            this.error = error;
            this.value = value;
        }

        public void setDescription(PropertyDescription description) {
            this.description = description;
        }

        public PropertyDescription getDescription() {
            return description;
        }

        public void setError(ValidationError error) {
            this.error = error;
        }

        public ValidationError getError() {
            return error;
        }

        public void setValue(Object value) {
            this.value = value;
        }

        public Object getValue() {
            return value;
        }
    }
    
    
}

/* Copyright (c) 2010, University of Oslo, Norway
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
package org.vortikal.web.actions.properties;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONObject;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.context.ApplicationContext;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.security.SecurityContext;
import org.vortikal.text.JSONUtil;
import org.vortikal.util.repository.JSONBackedMapResource;
import org.vortikal.web.RequestContext;
import org.vortikal.web.actions.UpdateCancelCommand;
import org.vortikal.web.service.URL;

public class ConfigurableJSONPropertyEditor extends SimpleFormController {

    private Repository repository;
    private PropertyTypeDefinition propertyDefinition;
    private String toplevelField;
    private JSONBackedMapResource mapResource;
    private JSONFieldConfig fieldConfig;

    @Override
    protected void initApplicationContext(ApplicationContext context) {
        super.initApplicationContext(context);
        if (this.mapResource == null) {
            throw new IllegalStateException("Javabean property 'mapResource' not configured");
        }
        try {
            this.fieldConfig = new JSONFieldConfig(mapResource);
        } catch (Throwable t) { }
    }

    public void reloadConfig() throws Exception {
        try {
            this.mapResource.load();
            this.fieldConfig = new JSONFieldConfig(mapResource);
        } catch (Throwable t) {
            this.fieldConfig = new JSONFieldConfig(Collections.<JSONField>emptyList());
        }
    }
    
    public ConfigurableJSONPropertyEditor() {
//        super();
//        String json = 
//            "{"
//            +  "'disable-breadcrumb-menu' : {"
//            +    "'type': 'flag',"
//            +    "'i18n': {"
//            +       "'{name}' : 'Disable breadcrumb menu',"
//            +       "'{name}_no': 'Skru av br¿dsmulesti'"
//            +    "}"
//            +  "}"
//            + "}";
//        
//        JSONObject obj = JSONObject.fromObject(json);
//        this.fieldConfig = new JSONFieldConfig(obj);

        /*
        this.fieldConfig = new JSONFieldConfig() {

            @Override
            public List<JSONField> getFields() {
                List<JSONField> fields = new ArrayList<JSONField>();
                // {
                //  disable-breadcrumb-menu { 
                //    type: flag,
                //     i18n: {
                //      {name}: Disable breadcrumb menu
                //      {name}_no: Skru av br¿dsmulesti
                //     }
                //    }
                //  }
                //
                Map<String, String> i18n = new HashMap<String, String>();
                i18n.put("{name}", "Disable breadcrumb menu");
                i18n.put("{name}_no", "Skru av br¿dsmulesti");
                i18n.put("{name}_nn", "Skruva att br¿dsmulesti");
                fields.add(new JSONField("disable-breadcrumb-menu", "flag", i18n));
                i18n = new HashMap<String, String>();
                i18n.put("{name}", "Contact email");
                i18n.put("{name}_no", "Kontaktadresse (epost)");
                i18n.put("{name}_nn", "Kontaktadressa (e-m¾il)");
                fields.add(new JSONField("contact-email", "string", i18n));
                return fields;
            }
        };
        */
    }
    

    protected Object formBackingObject(HttpServletRequest request)
    throws Exception {
        RequestContext requestContext = RequestContext.getRequestContext();
        Path uri = requestContext.getResourceURI();
        String token = SecurityContext.getSecurityContext().getToken();
        Resource resource = this.repository.retrieve(token, uri, false);
        Property property = resource.getProperty(this.propertyDefinition);
        JSONObject toplevel = null;
        if (property != null) {
            JSONObject propertyValue = property.getJSONValue();
            if (propertyValue != null && !propertyValue.isNullObject()) {
                toplevel = propertyValue.getJSONObject(this.toplevelField);
            }
        }
        Locale requestLocale = RequestContextUtils.getLocale(request);
        
        List<FormElement> elements = new ArrayList<FormElement>();
        for (JSONField field: this.fieldConfig.getFields()) {
            FormElement element = new FormElement(field, requestLocale);
            if (toplevel != null && !toplevel.isNullObject()) {
                element.setValue(toplevel.get(field.getIdentifier()));
            }
            elements.add(element);
        }
        URL url = requestContext.getService().constructURL(uri);
        return new Form(url, elements);
    }

    
    @Override
    protected void onBindAndValidate(HttpServletRequest request,
            Object object, BindException errors) throws Exception {
        Locale requestLocale = RequestContextUtils.getLocale(request);
        List<FormElement> elements = new ArrayList<FormElement>();
        for (JSONField field: this.fieldConfig.getFields()) {
            String input = request.getParameter(field.getIdentifier());
            FormElement element = new FormElement(field, requestLocale);
            element.setValue(input);
            elements.add(element);
        }
        ((Form) object).setElements(elements);
    }

    @Override
    protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response,
                                    Object command, BindException errors) throws Exception {    

        Path uri = RequestContext.getRequestContext().getResourceURI();
        String token = SecurityContext.getSecurityContext().getToken();
        Resource resource = this.repository.retrieve(token, uri, false);
        Form form = (Form) command;

        if (form.getCancelAction() != null) {
            return new ModelAndView(getSuccessView());
        }
        
        JSONObject toplevel = new JSONObject();
        for (FormElement element: form.getElements()) {
            Object key = element.getIdentifier();
            Object value = element.getValue();
            if (value == null || "".equals(value.toString().trim())) {
                toplevel.remove(key);
            } else {
                toplevel.put(element.getIdentifier().toString(), element.getValue().toString());
            }
        }
        
        Property property = resource.getProperty(this.propertyDefinition);
        JSONObject propertyValue = null;
        if (property == null) {
            property = this.propertyDefinition.createProperty();
            resource.addProperty(property);
            propertyValue = new JSONObject();
        } else {
            propertyValue = property.getJSONValue();
            if (propertyValue == null || propertyValue.isNullObject()) {
                propertyValue = new JSONObject();
            }
        }
        propertyValue.put(this.toplevelField, toplevel);
        property.setJSONValue(propertyValue);
        
        this.repository.store(token, resource);

        Map<String, Object> model = new HashMap<String, Object>();
        model.put("form", form);
        return new ModelAndView(getFormView(), model);
    }
    
    
    public class Form extends UpdateCancelCommand {
        private List<FormElement> elements;
        public Form(URL url, List<FormElement> elements) {
            super(url.toString());
            this.elements = elements;
        }
        public void setElements(List<FormElement> elements) {
            this.elements = elements;
        }
        public List<FormElement> getElements() {
            return this.elements;
        }
    }
    
    public class FormElement {
        private JSONField desc;
        private Locale locale;
        private Object value;
        
        public FormElement(JSONField desc, Locale locale) {
            this.desc = desc;
            this.locale = locale;
        }
        
        public Object getIdentifier() {
            return this.desc.identifier;
        }
        
        public String getLabel() {
            return this.desc.getLocalizedName(this.locale);
        }
        
        public String getType() {
            return this.desc.type;
        }
        
        public Object getValue() {
            return this.value;
        }
        
        public void setValue(Object value) {
            this.value = value;
        }
    }
    
    @Required
    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    @Required
    public void setPropertyDefinition(PropertyTypeDefinition propertyDefinition) {
        this.propertyDefinition = propertyDefinition;
    }

    @Required
    public void setToplevelField(String toplevelField) {
        this.toplevelField = toplevelField;
    }

    @Required
    public void setConfigFile(JSONBackedMapResource mapResource) {
        this.mapResource = mapResource;
    }


    public class JSONFieldConfig {
        private List<JSONField> fields = new ArrayList<JSONField>();

        public JSONFieldConfig(List<JSONField> fields) {
            this.fields = fields;
        }
        
        public JSONFieldConfig(Map<?, ?> config) {
            for (Object identifier: config.keySet()) {
                Object o = config.get(identifier);
                if (!(o instanceof Map<?, ?>)) {
                    throw new IllegalArgumentException("Invalid configuration: " + config);
                }
                Map<?, ?> descriptor = (Map<?, ?>) o;
                o = descriptor.get("type");
                if (o == null) {
                    throw new IllegalArgumentException("Missing 'type' attribute: " + descriptor);
                }
                String type = o.toString();
                o = descriptor.get("i18n");
                if (o != null) {
                    if (!(o instanceof Map<?, ?>)) {
                        throw new IllegalArgumentException("Attribute 'i18n' must be a map: " + o);
                    }
                }
                Map<?, ?> i18n = (Map<?, ?>) o;
                JSONField field = new JSONField(identifier.toString(), type.toString(), i18n);
                this.fields.add(field);
            }
            
        }
        
        
        public JSONFieldConfig(JSONObject config) {
            if (config == null || config.isEmpty() || config.isArray() || config.isNullObject()) {
                throw new IllegalArgumentException("Invalid JSON structure: " + config);
            }

            for (Iterator<?> iterator = config.keys(); iterator.hasNext();) {
                Object identifier = iterator.next();
                JSONObject descriptor = (JSONObject) config.get(identifier);
                Object type = JSONUtil.select(config, identifier + ".type");
                if (type == null) {
                    throw new IllegalArgumentException("Missing 'type' attribute: " + descriptor);
                }
                Object map = JSONUtil.select(config, identifier + ".i18n");
                Map<String, String> i18n = null;
                if (map != null) {
                    if (!(map instanceof Map<?,?>)) {
                        throw new IllegalArgumentException("Invalid JSON structure: not a map: " + i18n);
                    }
                    i18n = toStringMap((Map<?, ?>) map);
                }
                JSONField field = new JSONField(identifier.toString(), type.toString(), i18n);
                this.fields.add(field);
            }
        }

        public List<JSONField> getFields() {
            return Collections.unmodifiableList(this.fields);
        }

        // Shallow conversion of map entries:
        private Map<String, String> toStringMap(Map<?, ?> map) {
            Map<String, String> result = new HashMap<String, String>();
            for (Object o: map.keySet()) {
                Object v = map.get(o);
                String key = o != null ? o.toString() : null;
                String val = v != null ? v.toString() : null;
                result.put(key, val);
            }
            return result;
        }

    }
    
    public class JSONField {
        private String identifier;
        private String type;
        private Map<?, ?> i18n;
        
        public JSONField(String identifier, String type, Map<?, ?> i18n) {
            if (!("string".equals(type) || "flag".equals(type))) {
                throw new IllegalArgumentException("Illegal type: " + type);
            }
            this.identifier = identifier;
            this.type = type;
            this.i18n = i18n;
        }
        
        public String getIdentifier() {
            return this.identifier;
        }
        
        public String getType() {
            return this.type;
        }
        
        public String getLocalizedName(Locale locale) {
            return getLocalizedString("name", locale);
        }
        
        private String getLocalizedString(String str, Locale locale) {
            if (this.i18n == null) {
                return str;
            }
            String key = "{" + str + "}_" + locale.toString();
            Object o = this.i18n.get(key);
            if (o != null) {
                return o.toString();
            }
            key = "{" + str + "}_" + locale.getLanguage() + "_" + locale.getCountry();
            o = this.i18n.get(key);
            if (o != null) {
                return o.toString();
            }
            key = "{" + str + "}_" + locale.getLanguage();
            o = this.i18n.get(key);
            if (o != null) {
                return o.toString();
            }
            key = "{" + str + "}";
            o = this.i18n.get(key);
            if (o != null) {
                return o.toString();
            }
            return str;
        }
    }
    
}

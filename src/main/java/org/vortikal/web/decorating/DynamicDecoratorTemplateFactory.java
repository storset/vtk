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
package org.vortikal.web.decorating;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import net.sf.json.JSONObject;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.resourcemanagement.view.tl.ComponentInvokerNodeFactory;
import org.vortikal.resourcemanagement.view.tl.JSONAttributeHandler;
import org.vortikal.resourcemanagement.view.tl.ResourcePropObjectValueHandler;
import org.vortikal.security.SecurityContext;
import org.vortikal.text.tl.Context;
import org.vortikal.text.tl.DefineNodeFactory;
import org.vortikal.text.tl.DirectiveNodeFactory;
import org.vortikal.text.tl.IfNodeFactory;
import org.vortikal.text.tl.ListNodeFactory;
import org.vortikal.text.tl.Symbol;
import org.vortikal.text.tl.ValNodeFactory;
import org.vortikal.text.tl.expr.Function;
import org.vortikal.util.repository.PropertyAspectDescription;
import org.vortikal.util.repository.PropertyAspectField;
import org.vortikal.web.RequestContext;

public class DynamicDecoratorTemplateFactory implements TemplateFactory, InitializingBean {

    private Repository repository;
    private PropertyTypeDefinition aspectsPropdef;
    PropertyAspectDescription fieldConfig;
    
    private Map<String, DirectiveNodeFactory> directiveHandlers;
    private Set<Function> functions = new HashSet<Function>();
    private ComponentResolver componentResolver;

    public Template newTemplate(TemplateSource templateSource) throws InvalidTemplateException {
        return new DynamicDecoratorTemplate(templateSource, this.componentResolver, this.directiveHandlers);
    }

    @Required public void setRepository(Repository repository) {
        this.repository = repository;
    }

    @Required public void setAspectsPropdef(PropertyTypeDefinition aspectsPropdef) {
        this.aspectsPropdef = aspectsPropdef;
    }
    
    public void setFieldConfig(PropertyAspectDescription fieldConfig) {
        this.fieldConfig = fieldConfig;
    }

    @Required public void setComponentResolver(ComponentResolver componentResolver) {
        this.componentResolver = componentResolver;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Map<String, DirectiveNodeFactory> directiveHandlers = new HashMap<String, DirectiveNodeFactory>();
        directiveHandlers.put("if", new IfNodeFactory());

        ValNodeFactory val = new ValNodeFactory();
        //val.addValueFormatHandler(PropertyImpl.class, new PropertyValueFormatHandler(this.valueFormatterRegistry));
        //val.addValueFormatHandler(Value.class, new PropertyValueFormatHandler(this.valueFormatterRegistry));
        //val.addValueFormatHandler(Value[].class, new PropertyValueFormatHandler(this.valueFormatterRegistry));
        directiveHandlers.put("val", val);

        ListNodeFactory list = new ListNodeFactory();
        list.setFunctions(this.functions);
        directiveHandlers.put("list", list);
        
        //directiveHandlers.put("resource-props", new ResourcePropsNodeFactory(this.repository));

        DefineNodeFactory def = new DefineNodeFactory();

        Set<Function> functions = new HashSet<Function>();
        functions.addAll(this.functions);
        functions.add(new RequestParameterFunction(new Symbol("request-param")));
        functions.add(new ResourceLocaleFunction(new Symbol("resource-locale")));
        functions.add(new TemplateParameterFunction(new Symbol("template-param")));
        functions.add(new ResourceAspectFunction(new Symbol("resource-aspect"), this.aspectsPropdef, this.fieldConfig));
        functions.add(new ResourcePropObjectValueHandler(new Symbol("resource-prop"), this.repository));
        functions.add(new JSONAttributeHandler(new Symbol("json-attr")));
        def.setFunctions(functions);
        directiveHandlers.put("def", def);

        //directiveHandlers.put("localized", new LocalizationNodeFactory(this.resourceModelKey));
        directiveHandlers.put("call", new ComponentInvokerNodeFactory(new DynamicDecoratorTemplate.ComponentSupport()));

        this.directiveHandlers = directiveHandlers;
    }
    
    public void setFunctions(Set<Function> functions) {
        if (functions == null) {
            throw new IllegalArgumentException("Argument is NULL");
        }
        this.functions = functions;
    }

    private class TemplateParameterFunction extends Function {
        public TemplateParameterFunction(Symbol symbol) {
            super(symbol, 1);
        }

        @Override
        public Object eval(Context ctx, Object... args) throws Exception {
            RequestContext requestContext = RequestContext.getRequestContext();
            HttpServletRequest request = requestContext.getServletRequest();
            Object o = args[0];
            if (o == null) {
                throw new IllegalArgumentException("Argument cannot be NULL");
            }
            String name = o.toString();
            return DynamicDecoratorTemplate.getTemplateParam(request, name);
        }
    }
    
    private class RequestParameterFunction extends Function {
        
        public RequestParameterFunction(Symbol symbol) {
            super(symbol, 1);
        }

        @Override
        public Object eval(Context ctx, Object... args) throws Exception {
            RequestContext requestContext = RequestContext.getRequestContext();
            HttpServletRequest request = requestContext.getServletRequest();
            Object o = args[0];
            if (o == null) {
                throw new IllegalArgumentException("Argument cannot be NULL");
            }
            String name = o.toString();
            return request.getParameter(name);
        }
        
    }
    
    private class ResourceLocaleFunction extends Function {
        
        public ResourceLocaleFunction(Symbol symbol) {
            super(symbol, 0);
        }

        @Override
        public Object eval(Context ctx, Object... args) throws Exception {
            RequestContext requestContext = RequestContext.getRequestContext();
            HttpServletRequest request = requestContext.getServletRequest();
            return RequestContextUtils.getLocale(request).toString();
        }
        
    }
    
    private class ResourceAspectFunction extends Function {
        private PropertyTypeDefinition aspectsPropdef;
        private PropertyAspectDescription fieldConfig;

        public ResourceAspectFunction(Symbol symbol, PropertyTypeDefinition aspectsPropdef, PropertyAspectDescription fieldConfig) {
            super(symbol, 1);
            this.aspectsPropdef = aspectsPropdef;
            this.fieldConfig = fieldConfig;
        }
        
        @Override
        public Object eval(Context ctx, Object... args) throws Exception {
            RequestContext requestContext = RequestContext.getRequestContext();
            SecurityContext securityContext = SecurityContext.getSecurityContext();
            String token = securityContext.getToken();
            
            Object o = args[0];
            if (o == null) {
                throw new IllegalArgumentException("Argument must be a valid name");
            }
            String aspect = o.toString();
            
            JSONObject result = new JSONObject();
            traverse(result, aspect, requestContext.getResourceURI(), token);
            return result;
        }

        private void traverse(JSONObject result, String aspect, Path uri, String token) {
            
            Path traversalURI = Path.fromString(uri.toString());

            while (true) {
                Resource r = null;
                try {
                    r = repository.retrieve(token, traversalURI, true);
                } catch (Throwable t) { }
                if (r != null) {
                    Property property = r.getProperty(aspectsPropdef);
                    if (property != null && property.getType() == PropertyType.Type.JSON) {
                        JSONObject value = property.getJSONValue();
                        if (value.get(aspect) != null) {
                            value = value.getJSONObject(aspect);

                            for (PropertyAspectField field : this.fieldConfig.getFields()) {
                                Object key = field.getIdentifier();
                                Object newValue = value.get(key);
                                if (!field.isInherited() && traversalURI.equals(uri)) {
                                    result.put(key, newValue);
                                } else {
                                    if (result.get(key) == null) {
                                        result.put(key, newValue);
                                    }
                                }
                            }
                        }
                    }
                }
                traversalURI = traversalURI.getParent();
                if (traversalURI == null) {
                    break;
                }
            }
        }
     }
}

/* Copyright (c) 2014 University of Oslo, Norway
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.TypeInfo;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.URL;

@Controller
public class CreateCollectionWithProperties {

    private String formView, successView;

    @RequestMapping(method = RequestMethod.GET)
    public ModelAndView get() {
        Map<String, Object> model = new HashMap<String, Object>();
        URL submitURL = RequestContext.getRequestContext().getRequestURL();
        model.put("submitURL", submitURL);
        return new ModelAndView(getFormView(), model);
    }

    @RequestMapping(method = RequestMethod.POST)
    public ModelAndView post(HttpServletRequest request) throws Exception {
        RequestContext requestContext = RequestContext.getRequestContext();
        CreateOperation operation = bind(request);
        operation.apply(requestContext.getRepository(), requestContext.getSecurityToken());
        Map<String, Object> model = new HashMap<String, Object>();
        return new ModelAndView(getSuccessView(), model);
    }

    public String getFormView() {
        return formView;
    }

    public void setFormView(String formView) {
        this.formView = formView;
    }

    public String getSuccessView() {
        return successView;
    }

    public void setSuccessView(String successView) {
        this.successView = successView;
    }

    private CreateOperation bind(HttpServletRequest request) throws Exception {
        String uri = request.getParameter("uri");
        String type = request.getParameter("type");

        String[] namespaces = request.getParameterValues("propertyNamespace");
        String[] propNames = request.getParameterValues("propertyName");
        String[] propValues = request.getParameterValues("propertyValue");
        Assert.hasText(uri, "Input 'uri' must be defined");

        List<PropertyOperation> propertyOps = new ArrayList<PropertyOperation>();

        if (namespaces != null && propNames != null && propValues != null) {
            if (namespaces.length != propNames.length || namespaces.length != propValues.length) {
                throw new IllegalArgumentException("Inputs 'propertyNamespaces', 'propertyNames' and 'propertyValues' "
                        + "must be of the same length");
            }
            for (int i = 0; i < namespaces.length; i++) {
                if (!"".equals(propNames[i].trim()) && !"".equals(propValues[i].trim())) {

                    PropertyOperation existing = null;
                    for (PropertyOperation op : propertyOps) {
                        if (op.namespace.equals(namespaces[i]) && op.name.equals(propNames[i])) {
                            existing = op;
                            break;
                        }
                    }
                    if (existing != null) {
                        existing.addValue(propValues[i]);
                    } else
                        propertyOps.add(new PropertyOperation(namespaces[i], propNames[i], propValues[i]));

                }
            }
        }
        return new CreateOperation(Path.fromString(uri), type, propertyOps);
    }

    private class CreateOperation {
        Path uri;
        String typeProperty = null;
        List<PropertyOperation> propertyOps;

        @Override
        public String toString() {
            return "CreateOperation(uri=" + uri + ", typeProperty=" + typeProperty + ", propertyOps=" + propertyOps
                    + ")";
        }

        CreateOperation(Path uri, String typeProperty, List<PropertyOperation> propertyOps) {
            this.uri = uri;
            this.typeProperty = typeProperty;
            this.propertyOps = propertyOps;
        }

        public void apply(Repository repository, String token) throws Exception {
            Resource collection = repository.createCollection(token, uri);
            if (this.typeProperty != null) {
                TypeInfo typeInfo = repository.getTypeInfo(collection);
                Namespace ns = Namespace.DEFAULT_NAMESPACE;
                Property property = typeInfo.createProperty(ns, "collection-type", this.typeProperty);
                collection.addProperty(property);
                collection = repository.store(token, collection);
            }

            TypeInfo typeInfo = repository.getTypeInfo(collection);

            if (propertyOps != null) {
                for (PropertyOperation op : propertyOps) {
                    Namespace ns = null;

                    if (op.namespace == null || op.namespace.trim().equals(""))
                        ns = Namespace.DEFAULT_NAMESPACE;
                    else if (op.namespace.startsWith("http://"))
                        ns = Namespace.getNamespace(op.namespace);
                    else
                        ns = Namespace.getNamespaceFromPrefix(op.namespace);

                    PropertyTypeDefinition propDef = typeInfo.getPropertyTypeDefinition(ns, op.name);
                    if (propDef.isMultiple()) {
                        String[] values = op.values.toArray(new String[op.values.size()]);
                        Property property = typeInfo.createProperty(ns, op.name, values);
                        collection.addProperty(property);
                    } else {
                        Property property = typeInfo.createProperty(ns, op.name, op.values.get(0));
                        collection.addProperty(property);
                    }
                }
                repository.store(token, collection);
            }
        }
    }

    private class PropertyOperation {
        String namespace, name;
        List<String> values;

        PropertyOperation(String namespace, String name, String value) {
            this.namespace = namespace;
            this.name = name;
            this.values = new ArrayList<String>();
            this.values.add(value);
        }

        public void addValue(String value) {
            values.add(value);
        }

        @Override
        public String toString() {
            return "PropertyOp(namespace=" + namespace + ", name=" + name + ", values=" + values + ")";
        }
    }
}

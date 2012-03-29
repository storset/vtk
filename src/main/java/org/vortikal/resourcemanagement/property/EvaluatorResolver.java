/* Copyright (c) 2012, University of Oslo, Norway
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
package org.vortikal.resourcemanagement.property;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import net.sf.json.JSONObject;

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertyEvaluationContext;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyEvaluator;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.repository.resourcetype.ValueFormatter;
import org.vortikal.repository.resourcetype.PropertyType.Type;
import org.vortikal.repository.resourcetype.property.PropertyEvaluationException;
import org.vortikal.resourcemanagement.BinaryPropertyDescription;
import org.vortikal.resourcemanagement.DerivedPropertyDescription;
import org.vortikal.resourcemanagement.DerivedPropertyEvaluationDescription;
import org.vortikal.resourcemanagement.JSONPropertyDescription;
import org.vortikal.resourcemanagement.PropertyDescription;
import org.vortikal.resourcemanagement.ServiceDefinition;
import org.vortikal.resourcemanagement.SimplePropertyDescription;
import org.vortikal.resourcemanagement.StructuredResourceDescription;
import org.vortikal.resourcemanagement.DerivedPropertyEvaluationDescription.EvaluationElement;
import org.vortikal.resourcemanagement.service.ExternalServiceInvoker;
import org.vortikal.util.text.JSON;

public class EvaluatorResolver {

    // XXX Reconsider this whole setup. No good implementation.
    private ExternalServiceInvoker serviceInvoker;

    public PropertyEvaluator createPropertyEvaluator(PropertyDescription desc,
            StructuredResourceDescription resourceDesc) {
        if (desc instanceof SimplePropertyDescription) {
            return createSimplePropertyEvaluator((SimplePropertyDescription) desc, resourceDesc);
        } else if (desc instanceof JSONPropertyDescription) {
            return createJSONPropertyEvaluator((JSONPropertyDescription) desc, resourceDesc);
        } else if (desc instanceof BinaryPropertyDescription) {
            return new BinaryPropertyEvaluator(desc);
        }
        return createDerivedPropertyEvaluator((DerivedPropertyDescription) desc, resourceDesc);
    }

    private PropertyEvaluator createSimplePropertyEvaluator(final SimplePropertyDescription desc,
            final StructuredResourceDescription resourceDesc) {
        return new JSONPropertyEvaluator(resourceDesc, desc);
    }

    private PropertyEvaluator createJSONPropertyEvaluator(JSONPropertyDescription desc,
            StructuredResourceDescription resourceDesc) {
        return new JSONPropertyEvaluator(resourceDesc, desc);
    }

    private PropertyEvaluator createDerivedPropertyEvaluator(final DerivedPropertyDescription desc,
            final StructuredResourceDescription resourceDesc) {
        return new DerivedPropertyEvaluator(desc, resourceDesc);
    }

    private class JSONPropertyEvaluator implements PropertyEvaluator {

        private final StructuredResourceDescription resourceDesc;
        private final PropertyDescription propertyDesc;

        private JSONPropertyEvaluator(StructuredResourceDescription resourceDesc, PropertyDescription desc) {
            this.resourceDesc = resourceDesc;
            this.propertyDesc = desc;
        }

        public String toString() {
            return getClass().getName() + ": " + propertyDesc.getName();
        }

        @SuppressWarnings("unchecked")
        public boolean evaluate(Property property, PropertyEvaluationContext ctx) throws PropertyEvaluationException {

            Object value = null;
            if (propertyDesc.hasExternalService()) {
                Object o = ctx.getEvaluationAttribute(propertyDesc.getExternalService());
                if (o != null) {
                    Map<String, Object> map = (Map<String, Object>) o;
                    value = map.get(property.getDefinition().getName());
                    // No value was found for this prop, don't show anything
                    if (value == null) {
                        return false;
                    }
                }
            }

            if (!emptyValue(value)) {
                setPropValue(property, value);
            } else {
                JSONObject json;
                try {
                    json = (JSONObject) ctx.getContent().getContentRepresentation(JSONObject.class);
                } catch (Exception e) {
                    throw new PropertyEvaluationException("Unable to get JSON representation of content", e);
                }
                String expression = "properties." + property.getDefinition().getName();
                value = JSON.select(json, expression);
                if (emptyValue(value)) {
                    if (propertyDesc.isOverrides()) {
                        // XXX Consider the order of how this is done
                        if (!property.getDefinition().isMandatory()) {
                            // XXX What about structured namespace?
                            ctx.getNewResource().removeProperty(Namespace.DEFAULT_NAMESPACE,
                                    propertyDesc.getOverrides());
                        } else {
                            Property overriddenProp = ctx.getNewResource().getProperty(Namespace.DEFAULT_NAMESPACE,
                                    propertyDesc.getOverrides());
                            if (overriddenProp != null) {
                                if (overriddenProp.getDefinition().isMultiple()) {
                                    setPropValue(property, overriddenProp.getValues());
                                } else {
                                    setPropValue(property, overriddenProp.getValue());
                                }
                            }
                            return true;
                        }
                    }
                    return false;
                }
                setPropValue(property, value);
            }
            invokeService(property, ctx, propertyDesc, resourceDesc);
            return true;
        }
    }

    private boolean emptyValue(Object value) {
        if (value == null) {
            return true;
        }
        if (value.toString().trim().equals("")) {
            return true;
        }
        if ((value instanceof Collection<?>) && ((Collection<?>) value).isEmpty()) {
            return true;
        }
        if (value instanceof Collection<?>) {
            @SuppressWarnings("unchecked")
            Collection<Object> coll = (Collection<Object>) value;
            for (Object object : coll) {
                if (object == null || "".equals(object.toString().trim())) {
                    return true;
                }
            }
        }
        return false;
    }

    private class DerivedPropertyEvaluator implements PropertyEvaluator {

        private final DerivedPropertyDescription desc;
        private final StructuredResourceDescription resourceDesc;

        private DerivedPropertyEvaluator(DerivedPropertyDescription desc, StructuredResourceDescription resourceDesc) {
            this.desc = desc;
            this.resourceDesc = resourceDesc;
        }

        public boolean evaluate(Property property, PropertyEvaluationContext ctx) throws PropertyEvaluationException {

            if (ctx.getEvaluationType() != PropertyEvaluationContext.Type.ContentChange
                    && ctx.getEvaluationType() != PropertyEvaluationContext.Type.SystemPropertiesChange
                    && ctx.getEvaluationType() != PropertyEvaluationContext.Type.Create) {
                return ctx.getOriginalResource().getProperty(property.getDefinition()) != null;
            }

            try {
                Object value = getEvaluatedValue(desc, property, ctx);
                if (emptyValue(value)) {
                    // Evaluated value returned empty, check any default
                    // value that might exist, than finally check for any
                    // overridden value
                    Property prop = null;
                    if (desc.hasDefaultProperty()) {
                        prop = getProperty(ctx.getNewResource(), desc.getDefaultProperty());
                    }
                    if (prop == null) {
                        prop = getProperty(ctx.getNewResource(), property.getDefinition().getName());
                    }
                    if (prop != null) {
                        value = prop.getDefinition().isMultiple() ? prop.getValues() : prop.getValue();
                    }
                }
                if (!emptyValue(value)) {
                    setPropValue(property, value);
                    invokeService(property, ctx, desc, resourceDesc);
                    return true;
                }
                return false;
            } catch (Exception e) {
                return false;
            }
        }

        @SuppressWarnings("unchecked")
        private Object getEvaluatedValue(DerivedPropertyDescription desc, Property property,
                PropertyEvaluationContext ctx) {

            if (desc.hasExternalService()) {
                Object o = ctx.getEvaluationAttribute(desc.getExternalService());
                if (o != null) {
                    Map<String, Object> map = (Map<String, Object>) o;
                    return map.get(property.getDefinition().getName());
                }
            }

            DerivedPropertyEvaluationDescription evaluationDescription = desc.getEvaluationDescription();
            if (evaluationDescription.getEvaluationCondition() != null) {
                String propName = evaluationDescription.getEvaluationElements().get(0).getValue();
                return getEvaluatedConditionalValue(ctx, propName);
            }

            StringBuilder value = new StringBuilder();
            for (EvaluationElement evaluationElement : evaluationDescription.getEvaluationElements()) {
                String propName = evaluationElement.getValue();
                if (evaluationElement.isString()) {
                    value.append(evaluationElement.getValue());
                    continue;
                } else {
                    Property prop = getProperty(ctx.getNewResource(), propName);
                    if (prop == null) {
                        continue;
                    }
                    value.append(prop.getValue().toString());
                }
            }
            return value.toString();
        }

        private Object getEvaluatedConditionalValue(PropertyEvaluationContext ctx, String propName) {
            Property prop = getProperty(ctx.getNewResource(), propName);
            if (prop != null) {
                return Boolean.valueOf(true);
            } else {
                JSONObject json;
                try {
                    json = (JSONObject) ctx.getContent().getContentRepresentation(JSONObject.class);
                } catch (Exception e) {
                    throw new PropertyEvaluationException("Unable to get JSON representation of content", e);
                }
                String expression = "properties." + propName;
                Object jsonObject = JSON.select(json, expression);
                return Boolean.valueOf(jsonObject != null);
            }
        }
    }

    private class BinaryPropertyEvaluator implements PropertyEvaluator {

        private final PropertyDescription propertyDesc;

        private BinaryPropertyEvaluator(PropertyDescription desc) {
            this.propertyDesc = desc;
        }

        public String toString() {
            return getClass().getName() + ": " + propertyDesc.getName();
        }

        @SuppressWarnings("unchecked")
        public boolean evaluate(Property property, PropertyEvaluationContext ctx) throws PropertyEvaluationException {

            if (property.isValueInitialized()
                    && ctx.getEvaluationType() != PropertyEvaluationContext.Type.ContentChange
                    && ctx.getEvaluationType() != PropertyEvaluationContext.Type.Create) {
                return true;
            }

            Object value = null;
            if (this.propertyDesc.hasExternalService()) {
                Object o = ctx.getEvaluationAttribute(this.propertyDesc.getExternalService());
                if (o != null) {
                    Map<String, Object> map = (Map<String, Object>) o;
                    value = map.get(property.getDefinition().getName());
                    // No value was found for this prop, don't show anything
                    if (value == null) {
                        return false;
                    }
                }
            }
            if (value != null) {
                setPropValue(property, value);
                return true;
            }
            return false;
        }
    }

    private void setPropValue(Property property, Object value) {

        if (property.getType() == Type.BINARY) {
            // Store the value of the property
            property.setBinaryValue(value.toString().getBytes(), "application/json");
        } else if (!property.getDefinition().isMultiple()) {
            // If value is collection, pick first element
            if (value instanceof Collection<?>) {
                Collection<?> c = (Collection<?>) value;
                if (c.isEmpty()) {
                    value = null;
                } else {
                    value = c.toArray()[0];
                }
            }
            Value v = property.getDefinition().getValueFormatter().stringToValue(value.toString(), null, null);
            property.setValue(v);

        } else {
            List<Object> values = new ArrayList<Object>();
            if (value instanceof Collection<?> || value instanceof Value[]) {
                values.addAll((Collection<?>) value);
            } else {
                values.add(value);
            }
            ValueFormatter vf = property.getDefinition().getValueFormatter();
            List<Value> result = new ArrayList<Value>();
            for (Object object : values) {
                Value v = vf.stringToValue(object.toString(), null, null);
                result.add(v);
            }
            property.setValues(result.toArray(new Value[result.size()]));
        }
    }

    private Property getProperty(Resource resource, String propName) {
        Property prop = resource.getProperty(Namespace.STRUCTURED_RESOURCE_NAMESPACE, propName);
        if (prop == null) {
            prop = resource.getProperty(Namespace.DEFAULT_NAMESPACE, propName);
        }
        return prop;
    }

    private void invokeService(Property property, PropertyEvaluationContext ctx, PropertyDescription desc,
            StructuredResourceDescription resourceDesc) {
        List<ServiceDefinition> services = resourceDesc.getServices();
        if (services != null && services.size() > 0) {
            for (ServiceDefinition serviceDefinition : services) {
                if (serviceDefinition.getName().equals(desc.getName())) {
                    this.serviceInvoker.invokeService(property, ctx, serviceDefinition);
                }
            }
        }
    }

    @Required
    public void setServiceInvoker(ExternalServiceInvoker serviceInvoker) {
        this.serviceInvoker = serviceInvoker;
    }

}

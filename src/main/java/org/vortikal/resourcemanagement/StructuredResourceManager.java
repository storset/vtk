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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.json.JSONObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertyEvaluationContext;
import org.vortikal.repository.RepositoryAction;
import org.vortikal.repository.Resource;
import org.vortikal.repository.ResourceTypeTree;
import org.vortikal.repository.resourcetype.OverridablePropertyTypeDefinitionImpl;
import org.vortikal.repository.resourcetype.OverridingPropertyTypeDefinitionImpl;
import org.vortikal.repository.resourcetype.PrimaryResourceTypeDefinition;
import org.vortikal.repository.resourcetype.PrimaryResourceTypeDefinitionImpl;
import org.vortikal.repository.resourcetype.PropertyEvaluator;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.resourcetype.PropertyType.Type;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.ResourceTypeDefinition;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.repository.resourcetype.ValueFactory;
import org.vortikal.repository.resourcetype.ValueFormatter;
import org.vortikal.repository.resourcetype.ValueFormatterRegistry;
import org.vortikal.repository.resourcetype.property.PropertyEvaluationException;
import org.vortikal.resourcemanagement.DerivedPropertyEvaluationDescription.EvaluationElement;
import org.vortikal.resourcemanagement.parser.ParserConstants;
import org.vortikal.resourcemanagement.service.ExternalServiceInvoker;
import org.vortikal.util.text.JSON;
import org.vortikal.web.service.RepositoryAssertion;

public class StructuredResourceManager {
    
    private static final Map<String, PropertyType.Type> PROPTYPE_MAP = new HashMap<String, PropertyType.Type>();
    static {
        PROPTYPE_MAP.put(ParserConstants.PROPTYPE_STRING, PropertyType.Type.STRING);
        PROPTYPE_MAP.put(ParserConstants.PROPTYPE_HTML, PropertyType.Type.HTML);
        PROPTYPE_MAP.put(ParserConstants.PROPTYPE_SIMPLEHTML, PropertyType.Type.HTML);
        PROPTYPE_MAP.put(ParserConstants.PROPTYPE_BOOLEAN, PropertyType.Type.BOOLEAN);
        PROPTYPE_MAP.put(ParserConstants.PROPTYPE_INT, PropertyType.Type.INT);
        PROPTYPE_MAP.put(ParserConstants.PROPTYPE_DATETIME, PropertyType.Type.TIMESTAMP);
        PROPTYPE_MAP.put(ParserConstants.PROPTYPE_IMAGEREF, PropertyType.Type.IMAGE_REF);
        PROPTYPE_MAP.put(ParserConstants.PROPTYPE_MEDIAREF, PropertyType.Type.IMAGE_REF);
        PROPTYPE_MAP.put(ParserConstants.PROPTYPE_RESOURCEREF, PropertyType.Type.IMAGE_REF);
        PROPTYPE_MAP.put(ParserConstants.PROPTYPE_BINARY, PropertyType.Type.BINARY);
        PROPTYPE_MAP.put(ParserConstants.PROPTYPE_JSON, PropertyType.Type.JSON);
    }
    private ResourceTypeTree resourceTypeTree;
    private PrimaryResourceTypeDefinition baseType;
    private JSONObjectSelectAssertion assertion;
    private Namespace namespace = Namespace.STRUCTURED_RESOURCE_NAMESPACE;

    private Map<String, StructuredResourceDescription> types = new HashMap<String, StructuredResourceDescription>();
    private ValueFactory valueFactory;
    private ValueFormatterRegistry valueFormatterRegistry;
    private ExternalServiceInvoker serviceInvoker;

    public void register(StructuredResourceDescription description) throws Exception {
        String name = description.getName();
        ResourceTypeDefinition existing = null;
        try {
            existing = resourceTypeTree.getResourceTypeDefinitionByName(name);
        } catch (Exception e) {
            // Ignore
        }
        if (existing != null) {
            throw new IllegalArgumentException("Resource type of name " + name + " already exists");
        }
        description.validate();
        PrimaryResourceTypeDefinition def = createResourceType(description);
        this.resourceTypeTree.registerDynamicResourceType(def);

        this.types.put(name, description);
    }
    
    public void registrationComplete() {
        Log logger = LogFactory.getLog(getClass());
        logger.info("Resource type tree:");
        logger.info(this.resourceTypeTree.getResourceTypeTreeAsString());
    }
    
    private PrimaryResourceTypeDefinition createResourceType(StructuredResourceDescription description)
            throws Exception {
        PrimaryResourceTypeDefinitionImpl def = new PrimaryResourceTypeDefinitionImpl();

        def.setName(description.getName());
        def.setNamespace(this.namespace);

        ResourceTypeDefinition parentDefinition = this.baseType;

        PropertyTypeDefinition[] descPropDefs = createPropDefs(description);

        List<PropertyTypeDefinition> allPropDefs = new ArrayList<PropertyTypeDefinition>();
        for (PropertyTypeDefinition d: descPropDefs) {
            allPropDefs.add(d);
        }
//        allPropDefs.add(this.linksDef);
        def.setPropertyTypeDefinitions(allPropDefs.toArray(new PropertyTypeDefinition[allPropDefs.size()]));

        List<RepositoryAssertion> assertions = createAssertions(description);
        def.setAssertions(assertions.toArray(new RepositoryAssertion[assertions.size()]));

        if (description.getInheritsFrom() != null) {
            String parent = description.getInheritsFrom();
            if (!this.types.containsKey(parent)) {
                throw new IllegalArgumentException("Can only inherit from other structured resource types: ["
                        + description.getName() + ":" + parent + "]");
            }
            parentDefinition = this.resourceTypeTree.getResourceTypeDefinitionByName(parent);
        }

        if (parentDefinition instanceof PrimaryResourceTypeDefinitionImpl) {
            def.setParentTypeDefinition((PrimaryResourceTypeDefinitionImpl) parentDefinition);
            updateAssertions(parentDefinition, description.getName());
        }

        def.afterPropertiesSet();
        return def;
    }

    private void updateAssertions(ResourceTypeDefinition parent, String name) {

        while (true) {
            if (!(parent instanceof PrimaryResourceTypeDefinitionImpl)) {
                break;
            }
            PrimaryResourceTypeDefinitionImpl impl = (PrimaryResourceTypeDefinitionImpl) parent;

            for (RepositoryAssertion assertion : impl.getAssertions()) {
                if (assertion instanceof JSONObjectSelectAssertion) {
                    JSONObjectSelectAssertion jsonAssertion = (JSONObjectSelectAssertion) assertion;
                    if ("resourcetype".equals(jsonAssertion.getExpression())) {
                        jsonAssertion.addExpectedValue(name);
                    }
                }
            }
            if (parent == this.baseType) {
                break;
            }
            parent = impl.getParentTypeDefinition();
        }
    }

    private List<RepositoryAssertion> createAssertions(StructuredResourceDescription description) {
        List<RepositoryAssertion> assertions = new ArrayList<RepositoryAssertion>();
        JSONObjectSelectAssertion typeElementAssertion = this.assertion.createAssertion("resourcetype", description
                .getName());
        assertions.add(typeElementAssertion);

        for (PropertyDescription propDesc : description.getPropertyDescriptions()) {
            if (propDesc instanceof SimplePropertyDescription) {
                if (((SimplePropertyDescription) propDesc).isRequired()) {
                    JSONObjectSelectAssertion propAssertion = this.assertion.createAssertion("properties."
                            + propDesc.getName());
                    assertions.add(propAssertion);
                }
            }
        }
        return assertions;
    }

    private PropertyTypeDefinition[] createPropDefs(StructuredResourceDescription description) throws Exception {
        List<PropertyDescription> propertyDescriptions = description.getPropertyDescriptions();
        List<PropertyTypeDefinition> result = new ArrayList<PropertyTypeDefinition>();

        for (PropertyDescription d : propertyDescriptions) {
            PropertyTypeDefinition def = createPropDef(d, description);
            if (def != null) {
                result.add(def);
            }
        }
        return result.toArray(new PropertyTypeDefinition[result.size()]);
    }

    private OverridablePropertyTypeDefinitionImpl resolveOverride(PropertyDescription propDesc,
            StructuredResourceDescription resourceDesc) {
        String name = propDesc.getOverrides();
        // Allow overriding of only "internal" properties for now:
        Namespace namespace = Namespace.DEFAULT_NAMESPACE;
        PropertyTypeDefinition target = this.resourceTypeTree.getPropertyTypeDefinition(namespace, name);

        String typeName = resourceDesc.getInheritsFrom();
        if (typeName == null) {
            typeName = this.baseType.getName();
        }
        ResourceTypeDefinition startingPoint = this.resourceTypeTree.getResourceTypeDefinitionByName(typeName);
        List<PropertyTypeDefinition> allProps = this.resourceTypeTree
                .getPropertyTypeDefinitionsIncludingAncestors(startingPoint);
        boolean foundDef = false;
        for (PropertyTypeDefinition propDef : allProps) {
            if (propDef.getNamespace().equals(target.getNamespace()) && propDef.getName().equals(target.getName())) {
                foundDef = true;
                break;
            }
        }
        if (!foundDef) {
            throw new IllegalArgumentException("Property " + resourceDesc.getName() + "." + propDesc.getName()
                    + " cannot override property '" + name + "' from an unrelated resource type");
        }
        if (!(target instanceof OverridablePropertyTypeDefinitionImpl)) {
            throw new IllegalArgumentException("Property " + resourceDesc.getName() + "." + propDesc.getName()
                    + " cannot override property '" + name + "' (not overridable)");
        }
        return (OverridablePropertyTypeDefinitionImpl) target;
    }

    private PropertyTypeDefinition createPropDef(PropertyDescription propertyDescription,
            StructuredResourceDescription resourceDescription) throws Exception {
        if (propertyDescription.isNoExtract()) {
            return null;
        }
        if (propertyDescription.getOverrides() != null) {
            return createOverridingPropDef(propertyDescription, resourceDescription);
        }
        return createRegularPropDef(propertyDescription, resourceDescription);
    }

    private PropertyTypeDefinition createOverridingPropDef(PropertyDescription propertyDescription,
            StructuredResourceDescription resourceDescription) throws Exception {
        OverridablePropertyTypeDefinitionImpl overridableDef = resolveOverride(propertyDescription, resourceDescription);

        OverridingPropertyTypeDefinitionImpl overridingDef = new OverridingPropertyTypeDefinitionImpl();
        overridingDef.setOverriddenPropDef(overridableDef);
        overridingDef.setPropertyEvaluator(createPropertyEvaluator(propertyDescription, resourceDescription));
        overridingDef.afterPropertiesSet();
        return overridingDef;
    }

    private PropertyTypeDefinition createRegularPropDef(PropertyDescription propertyDescription,
            StructuredResourceDescription resourceDescription) {
        OverridablePropertyTypeDefinitionImpl def = new OverridablePropertyTypeDefinitionImpl();

        def.setName(propertyDescription.getName());
        def.setNamespace(this.namespace);
        if (propertyDescription instanceof DerivedPropertyDescription) {
            def.setType(Type.STRING);
        } else if (propertyDescription instanceof JSONPropertyDescription) {
            def.setType(Type.JSON);
            if (((JSONPropertyDescription)propertyDescription).getIndexableAttributes().size() >= 1) {
                def.addMetadata(PropertyTypeDefinition.METADATA_INDEXABLE_JSON, true);
            }
        } else {            

            def.setType(mapType(propertyDescription));
        }
        def.setProtectionLevel(RepositoryAction.UNEDITABLE_ACTION);
        boolean mandatory = false;
        if (propertyDescription instanceof SimplePropertyDescription) {
            mandatory = ((SimplePropertyDescription) propertyDescription).isRequired();
        }
        def.setMandatory(mandatory);
        def.setMultiple(propertyDescription.isMultiple());
        def.setValueFactory(this.valueFactory);
        def.setValueFormatterRegistry(this.valueFormatterRegistry);
        def.setPropertyEvaluator(createPropertyEvaluator(propertyDescription, resourceDescription));

        if (propertyDescription instanceof SimplePropertyDescription) {
            SimplePropertyDescription spd = ((SimplePropertyDescription) propertyDescription);
            Map<String, Object> edithints = spd.getEdithints();
            if (edithints != null) {
                def.addMetadata(PropertyTypeDefinition.METADATA_EDITING_HINTS, edithints);
            }
            String defaultValue = spd.getDefaultValue();
            if (defaultValue != null) {
                this.setDefaultValue(def, defaultValue);
            }
        }
        def.afterPropertiesSet();
        return def;
    }

    private void setDefaultValue(OverridablePropertyTypeDefinitionImpl def, String defaultValue) {
        Type type = def.getType();
        switch (type) {
        case STRING:
            def.setDefaultValue(new Value(defaultValue, type));
            return;
        case BOOLEAN:
            if ("true".equalsIgnoreCase(defaultValue) || "false".equalsIgnoreCase(defaultValue)) {
                def.setDefaultValue(new Value(Boolean.valueOf(defaultValue)));
                return;
            }
            throw new IllegalArgumentException("Default value of a boolean property can only be 'true' or 'false'");
        case INT:
            Integer numb = null;
            try {
                numb = Integer.parseInt(defaultValue);
            } catch (NumberFormatException nfe) {
                throw new IllegalArgumentException("Default value of an int property can only be a valid number");
            }
            def.setDefaultValue(new Value(numb));
            return;
        default:
            return;
        }
    }

    private PropertyEvaluator createPropertyEvaluator(PropertyDescription desc,
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

    private Type mapType(PropertyDescription d) {
        String type = d.getType();
        Type result = PROPTYPE_MAP.get(type);
        if (result == null) {
            throw new IllegalArgumentException("Unmapped property type: " + type);
        }
        return result;
    }

    public StructuredResourceDescription get(String name) {
        StructuredResourceDescription description = this.types.get(name);
        return description;
    }

    public List<StructuredResourceDescription> list() {
        List<StructuredResourceDescription> result = new ArrayList<StructuredResourceDescription>();
        result.addAll(this.types.values());
        return result;
    }

    @Required
    public void setResourceTypeTree(ResourceTypeTree resourceTypeTree) {
        this.resourceTypeTree = resourceTypeTree;
    }

    @Required
    public void setBaseType(PrimaryResourceTypeDefinition baseType) {
        this.baseType = baseType;
    }

    @Required
    public void setValueFactory(ValueFactory valueFactory) {
        this.valueFactory = valueFactory;
    }

    @Required
    public void setValueFormatterRegistry(ValueFormatterRegistry valueFormatterRegistry) {
        this.valueFormatterRegistry = valueFormatterRegistry;
    }

    @Required
    public void setAssertion(JSONObjectSelectAssertion assertion) {
        this.assertion = assertion;
    }

    @Required
    public void setServiceInvoker(ExternalServiceInvoker serviceInvoker) {
        this.serviceInvoker = serviceInvoker;
    }

    public void setNamespace(Namespace namespace) {
        this.namespace = namespace;
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

    private Property getProperty(Resource resource, String propName) {
        Property prop = resource.getProperty(Namespace.STRUCTURED_RESOURCE_NAMESPACE, propName);
        if (prop == null) {
            prop = resource.getProperty(Namespace.DEFAULT_NAMESPACE, propName);
        }
        return prop;
    }
}

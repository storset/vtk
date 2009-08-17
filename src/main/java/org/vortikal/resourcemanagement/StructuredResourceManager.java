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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.json.JSONObject;

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertyEvaluationContext;
import org.vortikal.repository.RepositoryAction;
import org.vortikal.repository.ResourceTypeTree;
import org.vortikal.repository.resourcetype.OverridablePropertyTypeDefinitionImpl;
import org.vortikal.repository.resourcetype.OverridingPropertyTypeDefinitionImpl;
import org.vortikal.repository.resourcetype.PrimaryResourceTypeDefinition;
import org.vortikal.repository.resourcetype.PrimaryResourceTypeDefinitionImpl;
import org.vortikal.repository.resourcetype.PropertyEvaluator;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.ResourceTypeDefinition;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.repository.resourcetype.ValueFactory;
import org.vortikal.repository.resourcetype.ValueFormatter;
import org.vortikal.repository.resourcetype.ValueFormatterRegistry;
import org.vortikal.repository.resourcetype.PropertyType.Type;
import org.vortikal.repository.resourcetype.property.PropertyEvaluationException;
import org.vortikal.resourcemanagement.DerivedPropertyDescription.EvalDescription;
import org.vortikal.resourcemanagement.parser.ParserConstants;
import org.vortikal.resourcemanagement.service.ExternalServiceInvoker;
import org.vortikal.text.JSONUtil;
import org.vortikal.web.service.JSONObjectSelectAssertion;
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

    private PrimaryResourceTypeDefinition createResourceType(StructuredResourceDescription description)
            throws Exception {
        PrimaryResourceTypeDefinitionImpl def = new PrimaryResourceTypeDefinitionImpl();

        def.setName(description.getName());
        def.setNamespace(this.namespace);

        ResourceTypeDefinition parentDefinition = this.baseType;

        PropertyTypeDefinition[] propertyTypeDefinitions = createPropDefs(description);
        def.setPropertyTypeDefinitions(propertyTypeDefinitions);

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
                .getPropertyTypeDefinitionsForResourceTypeIncludingAncestors(startingPoint);
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
            def.setType(PropertyType.Type.STRING);
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
            Map<String, Object> edithints = ((SimplePropertyDescription) propertyDescription).getEdithints();
            if (edithints != null) {
                def.addMetadata("editingHints", edithints);
            }
        }
        def.afterPropertiesSet();
        return def;
    }

    private PropertyEvaluator createPropertyEvaluator(PropertyDescription desc,
            StructuredResourceDescription resourceDesc) {
        if (desc instanceof SimplePropertyDescription) {
            return createSimplePropertyEvaluator((SimplePropertyDescription) desc, resourceDesc);
        }
        return createDerivedPropertyEvaluator((DerivedPropertyDescription) desc, resourceDesc);
    }

    private PropertyEvaluator createSimplePropertyEvaluator(final SimplePropertyDescription desc,
            final StructuredResourceDescription resourceDesc) {

        return new PropertyEvaluator() {

            public String toString() {
                return getClass().getName() + ": " + desc.getName();
            }

            public boolean evaluate(Property property, PropertyEvaluationContext ctx)
                    throws PropertyEvaluationException {

                if (ctx.getEvaluationType() == PropertyEvaluationContext.Type.Create) {
                    return false;
                }

                JSONObject json;
                try {
                    json = (JSONObject) ctx.getContent().getContentRepresentation(JSONObject.class);
                } catch (Exception e) {
                    throw new PropertyEvaluationException("Unable to get JSON representation of content", e);
                }
                String expression = "properties." + property.getDefinition().getName();
                Object value = JSONUtil.select(json, expression);
                if (value == null) {
                    return false;
                }
                if (value.toString().trim().equals("")) {
                    return false;
                }
                setPropValue(property, value);

                invokeService(ctx, desc, resourceDesc);

                return true;
            }
        };
    }

    private PropertyEvaluator createDerivedPropertyEvaluator(final DerivedPropertyDescription desc,
            final StructuredResourceDescription resourceDesc) {

        return new PropertyEvaluator() {

            public boolean evaluate(Property property, PropertyEvaluationContext ctx)
                    throws PropertyEvaluationException {

                if (ctx.getEvaluationType() == PropertyEvaluationContext.Type.Create) {
                    return false;
                }
                if (ctx.getEvaluationType() != PropertyEvaluationContext.Type.ContentChange) {
                    return ctx.getOriginalResource().getProperty(property.getDefinition()) != null;
                }

                try {
                    StringBuilder value = new StringBuilder();
                    for (EvalDescription evalDescription : desc.getEvalDescriptions()) {
                        if (evalDescription.isString()) {
                            value.append(evalDescription.getValue());
                            continue;
                        }
                        String propName = evalDescription.getValue();
                        Property p = ctx.getNewResource()
                                .getProperty(Namespace.STRUCTURED_RESOURCE_NAMESPACE, propName);
                        if (p == null) {
                            p = ctx.getNewResource().getProperty(Namespace.DEFAULT_NAMESPACE, propName);
                        }
                        if (p == null) {
                            return false;
                        }
                        value.append(p.getValue().toString());
                    }
                    setPropValue(property, value);

                    invokeService(ctx, desc, resourceDesc);

                    return true;
                } catch (Exception e) {
                    return false;
                }
            }
        };
    }

    private void invokeService(PropertyEvaluationContext ctx, PropertyDescription desc,
            StructuredResourceDescription resourceDesc) {
        List<ServiceDefinition> services = resourceDesc.getServices();
        if (services != null && services.size() > 0) {
            for (ServiceDefinition serviceDefinition : services) {
                if (serviceDefinition.getName().equals(desc.getName())) {
                    this.serviceInvoker.invokeService(ctx, serviceDefinition);
                }
            }
        }
    }

    private void setPropValue(Property property, Object value) {
        if (!property.getDefinition().isMultiple()) {
            Value v = property.getDefinition().getValueFormatter().stringToValue(value.toString(), null, null);
            property.setValue(v);

        } else {
            List<Object> values = new ArrayList<Object>();
            if (value instanceof Collection<?>) {
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

    private PropertyType.Type mapType(PropertyDescription d) {
        String type = d.getType();
        Type result = PROPTYPE_MAP.get(type);
        if (result == null) {
            throw new IllegalArgumentException("Unmapped property type: " + type);
        }
        return result;
    }

    public StructuredResourceDescription get(String name) {
        StructuredResourceDescription description = this.types.get(name);
        if (description == null) {
            throw new IllegalArgumentException("No resource type definition found for name '" + name + "'");
        }
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

}

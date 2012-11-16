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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.RepositoryAction;
import org.vortikal.repository.ResourceTypeTree;
import org.vortikal.repository.resourcetype.OverridablePropertyTypeDefinitionImpl;
import org.vortikal.repository.resourcetype.OverridingPropertyTypeDefinitionImpl;
import org.vortikal.repository.resourcetype.PrimaryResourceTypeDefinition;
import org.vortikal.repository.resourcetype.PrimaryResourceTypeDefinitionImpl;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.resourcetype.PropertyType.Type;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.ResourceTypeDefinition;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.repository.resourcetype.ValueFactory;
import org.vortikal.repository.resourcetype.ValueFormatterRegistry;
import org.vortikal.resourcemanagement.EditRule.EditRuleType;
import org.vortikal.resourcemanagement.parser.ParserConstants;
import org.vortikal.resourcemanagement.property.EvaluatorResolver;
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
    private EvaluatorResolver evaluatorResolver;

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
        for (PropertyTypeDefinition d : descPropDefs) {
            allPropDefs.add(d);
        }
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
        JSONObjectSelectAssertion typeElementAssertion = this.assertion.createAssertion("resourcetype",
                description.getName());
        assertions.add(typeElementAssertion);
        if (description.getPropertyDescriptions() != null) {
            for (PropertyDescription propDesc : description.getPropertyDescriptions()) {
                if (propDesc instanceof SimplePropertyDescription) {
                    if (((SimplePropertyDescription) propDesc).isRequired()) {
                        JSONObjectSelectAssertion propAssertion = this.assertion.createAssertion("properties."
                                + propDesc.getName());
                        assertions.add(propAssertion);
                    }
                }
            }
        }
        return assertions;
    }

    private PropertyTypeDefinition[] createPropDefs(StructuredResourceDescription description) throws Exception {
        List<PropertyDescription> propertyDescriptions = description.getPropertyDescriptions();
        List<PropertyTypeDefinition> result = new ArrayList<PropertyTypeDefinition>();
        if (propertyDescriptions != null) {
            for (PropertyDescription d : propertyDescriptions) {
                PropertyTypeDefinition def = createPropDef(d, description);
                if (def != null) {
                    result.add(def);
                }
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
        overridingDef.setPropertyEvaluator(this.evaluatorResolver.createPropertyEvaluator(propertyDescription,
                resourceDescription));
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
            def.addMetadata(PropertyTypeDefinition.METADATA_INDEXABLE_JSON, true);
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
        def.setPropertyEvaluator(this.evaluatorResolver.createPropertyEvaluator(propertyDescription,
                resourceDescription));

        if (propertyDescription instanceof SimplePropertyDescription) {

            SimplePropertyDescription spd = ((SimplePropertyDescription) propertyDescription);

            List<EditRule> editRules = resourceDescription.getEditRules();
            if (editRules != null && editRules.size() > 0) {
                Map<String, String> editHints = new HashMap<String, String>();
                for (EditRule editRule : editRules) {
                    if (EditRuleType.EDITHINT.equals(editRule.getType())) {
                        if (editRule.getName().equals(propertyDescription.getName())) {
                            editHints.put(editRule.getEditHintKey(), editRule.getEditHintValue());
                        }
                    }
                }
                def.addMetadata("editingHints", editHints);
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

    public void setNamespace(Namespace namespace) {
        this.namespace = namespace;
    }

    @Required
    public void setEvaluatorResolver(EvaluatorResolver evaluatorResolver) {
        this.evaluatorResolver = evaluatorResolver;
    }

}

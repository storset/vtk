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
package org.vortikal.spezialentwicklung;

import java.util.ArrayList;
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
import org.vortikal.repository.resourcetype.ValueFactory;
import org.vortikal.repository.resourcetype.ValueFormatterRegistry;
import org.vortikal.repository.resourcetype.property.PropertyEvaluationException;
import org.vortikal.text.JSONUtil;
import org.vortikal.web.service.JSONObjectSelectAssertion;
import org.vortikal.web.service.RepositoryAssertion;

public class StructuredResourceManager {

    private ResourceTypeTree resourceTypeTree;
    private PrimaryResourceTypeDefinition baseType;
    private JSONObjectSelectAssertion assertion;
    private Namespace namespace = Namespace.STRUCTURED_RESOURCE_NAMESPACE;
    
    private Map<String, StructuredResourceDescription> types = 
        new HashMap<String, StructuredResourceDescription>();
    private ValueFactory valueFactory;
    private ValueFormatterRegistry valueFormatterRegistry;
    
    public void register(StructuredResourceDescription description) {
        String name = description.getName();
        ResourceTypeDefinition existing = null;
        try {
            existing =
            resourceTypeTree.getResourceTypeDefinitionByName(name);
        } catch (Exception e) {
            // Ignore
        }
        if (existing != null) {
            throw new IllegalArgumentException("Resource type of name " 
                    + name + " already exists");
        }

        PrimaryResourceTypeDefinition def = createResourceType(description);
        this.resourceTypeTree.registerDynamicResourceType(def);
        
        this.types.put(name, description);
    }

    private PrimaryResourceTypeDefinition createResourceType(StructuredResourceDescription description) {
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
                throw new IllegalArgumentException(
                        "Can only inherit from other structured resource types.");
            }
            parentDefinition = 
                this.resourceTypeTree.getResourceTypeDefinitionByName(parent);
        }

        if (parentDefinition instanceof PrimaryResourceTypeDefinitionImpl) {
            def.setParentTypeDefinition((PrimaryResourceTypeDefinitionImpl)parentDefinition);
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
        JSONObjectSelectAssertion typeElementAssertion = 
            this.assertion.createAssertion("resourcetype", description.getName());
        assertions.add(typeElementAssertion);

        for (PropertyDescription propDesc: description.getPropertyDescriptions()) {
            if (propDesc.isRequired()) {
                JSONObjectSelectAssertion propAssertion = 
                    this.assertion.createAssertion("properties." + propDesc.getName());
                assertions.add(propAssertion);
            }
        }
        return assertions;        
    }
    
    private PropertyTypeDefinition[] createPropDefs(StructuredResourceDescription description) {
        
        List<PropertyDescription> propertyDescriptions = description.getPropertyDescriptions();
        List<PropertyTypeDefinition> result = new ArrayList<PropertyTypeDefinition>();
        
        for (PropertyDescription d: propertyDescriptions) {
            PropertyTypeDefinition def = createPropDef(d);
            result.add(def);
        }
        return result.toArray(new PropertyTypeDefinition[result.size()]);
    }

    private PropertyTypeDefinition createPropDef(PropertyDescription d) {

        if (d.getOverrides() != null) {
            Namespace namespace = Namespace.DEFAULT_NAMESPACE; // XXX
            String name = d.getOverrides();
            
            PropertyTypeDefinition original = 
                this.resourceTypeTree.getPropertyTypeDefinition(namespace, name);
            if (!(original instanceof OverridablePropertyTypeDefinitionImpl)) {
                throw new IllegalArgumentException("Cannot override property " + name);
            }
            OverridablePropertyTypeDefinitionImpl overridableDef = 
                (OverridablePropertyTypeDefinitionImpl) original;
            OverridingPropertyTypeDefinitionImpl overridingDef = 
                new OverridingPropertyTypeDefinitionImpl();
            overridingDef.setOverriddenPropDef(overridableDef);
            if (!d.isNoExtract()) {
                overridingDef.setPropertyEvaluator(createPropertyEvaluator());
            }
            return overridingDef;
        } else {
            OverridablePropertyTypeDefinitionImpl def = 
            new OverridablePropertyTypeDefinitionImpl();
        
            def.setName(d.getName());
            def.setNamespace(this.namespace);
            def.setType(mapType(d));
            def.setProtectionLevel(RepositoryAction.UNEDITABLE_ACTION);
            def.setMandatory(d.isRequired());
            def.setValueFactory(this.valueFactory);
            def.setValueFormatterRegistry(this.valueFormatterRegistry);
        
            if (!d.isNoExtract()) {
                def.setPropertyEvaluator(createPropertyEvaluator());
            }
            return def;
        }
    }

    private PropertyEvaluator createPropertyEvaluator() {
        return new PropertyEvaluator() {

            public boolean evaluate(Property property,
                    PropertyEvaluationContext ctx)
            throws PropertyEvaluationException {
                try {
                    JSONObject json = (JSONObject) 
                    ctx.getContent().getContentRepresentation(JSONObject.class);
                    String expression = "properties." 
                        + property.getDefinition().getName();
                    Object value = JSONUtil.select(json, expression);
                    if (value == null) {
                        return false;
                    }
                    property.setStringValue(value.toString());
                return true;
                } catch (Exception e) {
                    return false;
                }
            }
        };        
    }
    
    private PropertyType.Type mapType(PropertyDescription d) {
        String type = d.getType();
        
        if ("text".equals(type)) {
            return PropertyType.Type.STRING;
        }
        if ("int".equals(type)) {
            return PropertyType.Type.INT;
        }
        if ("date".equals(type)) {
            return PropertyType.Type.DATE;
        }
        return PropertyType.Type.STRING;
    }
    
    public StructuredResourceDescription get(String name) {
        StructuredResourceDescription description = this.types.get(name);
        if (description == null) {
            throw new IllegalArgumentException("No resource type definition found for name '" + name + "'");
        }
        return description;
    }

    @Required public void setResourceTypeTree(ResourceTypeTree resourceTypeTree) {
        this.resourceTypeTree = resourceTypeTree;
    }
    
    @Required public void setBaseType(PrimaryResourceTypeDefinition baseType) {
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
    
    
}

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

package org.vortikal.testing.mocktypes;

import java.util.List;

import org.vortikal.repository.Namespace;
import org.vortikal.repository.ResourceTypeTree;
import org.vortikal.repository.resourcetype.HierarchicalNode;
import org.vortikal.repository.resourcetype.MixinResourceTypeDefinition;
import org.vortikal.repository.resourcetype.PrimaryResourceTypeDefinition;
import org.vortikal.repository.resourcetype.PrimaryResourceTypeDefinitionImpl;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.PropertyTypeDefinitionImpl;
import org.vortikal.repository.resourcetype.ResourceTypeDefinition;
import org.vortikal.repository.resourcetype.ValueFormatter;

public class MockResourceTypeTree implements ResourceTypeTree {

    public List<MixinResourceTypeDefinition> getMixinTypes(
            PrimaryResourceTypeDefinition def) {
        return null;
    }

    public Namespace getNamespace(String namespaceUrl) {
        return null;
    }

    public PrimaryResourceTypeDefinition[] getPrimaryResourceTypesForPropDef(
            PropertyTypeDefinition definition) {
        return null;
    }

    public PropertyTypeDefinition getPropertyDefinitionByPrefix(String prefix,
            String name) {
        PropertyTypeDefinitionImpl propDef = new PropertyTypeDefinitionImpl();
        propDef.setNamespace(Namespace.getNamespaceFromPrefix(prefix));
        propDef.setName(name);
        return propDef;
    }

    public PropertyTypeDefinition getPropertyTypeDefinition(
            Namespace namespace, String name) {
        return null;
    }

    public List<PropertyTypeDefinition> getPropertyTypeDefinitions() {
        return null;
    }

    public List<PropertyTypeDefinition> getPropertyTypeDefinitionsIncludingAncestors(
            ResourceTypeDefinition def) {
        return null;
    }

    public ResourceTypeDefinition getResourceTypeDefinitionByName(String name) {
        PrimaryResourceTypeDefinitionImpl def = new PrimaryResourceTypeDefinitionImpl();
        def.setName(name);
        def.setNamespace(Namespace.DEFAULT_NAMESPACE);
        def.afterPropertiesSet();
        return def;
    }
    
    public PropertyTypeDefinition getPropertyDefinitionByPointer(String pointer) {
        return null;
    }

    public List<PrimaryResourceTypeDefinition> getResourceTypeDefinitionChildren(
            PrimaryResourceTypeDefinition def) {
        return null;
    }

    public String getResourceTypeTreeAsString() {
        return null;
    }

    public PrimaryResourceTypeDefinition getRoot() {
        return null;
    }

    public boolean isContainedType(ResourceTypeDefinition def,
            String resourceTypeName) {
        return false;
    }

    public void registerDynamicResourceType(PrimaryResourceTypeDefinition def) {
    }

    public List<String> getDescendants(String entry) {
        return null;
    }

    public List<HierarchicalNode<String>> getRootNodes() {
        return null;
    }

    public String[] getAllowedValues() {
        return null;
    }

    public ValueFormatter getValueFormatter() {
        return null;
    }

}

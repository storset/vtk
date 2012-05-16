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
package org.vortikal.repository;

import org.vortikal.repository.resourcetype.PrimaryResourceTypeDefinition;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.ResourceTypeDefinition;
import org.vortikal.repository.resourcetype.ValueFormatException;

public final class TypeInfo {

    private PrimaryResourceTypeDefinition resourceTypeDefinition;
    private ResourceTypeTree resourceTypeTree;
    
    public TypeInfo(ResourceTypeTree resourceTypeTree, String type) {
        this.resourceTypeTree = resourceTypeTree;
        ResourceTypeDefinition t = resourceTypeTree.getResourceTypeDefinitionByName(type);
        if (!(t instanceof PrimaryResourceTypeDefinition)) {
            throw new IllegalArgumentException("Type '" + type 
                    + "' is not a primary resource type definition");
        }
        this.resourceTypeDefinition = (PrimaryResourceTypeDefinition) t;
    }
    
    public PrimaryResourceTypeDefinition getResourceType() {
        return this.resourceTypeDefinition;
    }
    
    /**
     * Determines whether this type is of a given resource type. 
     *
     * @param type a resource type definition
     * @return <code>true</code> if this type's definition is the
     * same as or a descendant of the supplied resource type or the
     * supplied definition is one of this type's mixin types,
     * <code>false</code> otherwise.
     */
    public boolean isOfType(ResourceTypeDefinition type) {
        return this.resourceTypeTree.isContainedType(type, this.resourceTypeDefinition.getName());
    }
    
    /**
     * Determines whether this type is of a given resource type. 
     *
     * @param name the name of the resource type
     * @return <code>true</code> if this type's definition is the
     * same as or a descendant of the supplied resource type or the
     * supplied definition is one of this type's mixin types,
     * <code>false</code> otherwise.
     */
    public boolean isOfType(String name) {
        ResourceTypeDefinition type = this.resourceTypeTree.getResourceTypeDefinitionByName(name);
        return isOfType(type);
    }

    /**
     * Creates a property with a given name space and name
     *
     * @param namespace the name space of the property
     * @param name the name of the property
     * @return the created property
     */
    public Property createProperty(Namespace namespace, String name) {
        PropertyTypeDefinition propDef = this.resourceTypeTree.getPropertyTypeDefinition(namespace, name);
        return propDef.createProperty();
    }

    /**
     * Creates a property with a given namespace, name and value. The
     * type is set according to its {@link PropertyTypeDefinition property
     * definition}, or {@link PropertyType.TYPE_STRING} if it is a "dead" property
     * 
     * @param namespace the namespace
     * @param name the name
     * @return a property instance
     * @throws ValueFormatException
     *             if the supplied value's type does not match that of the
     *             property definition
     */
    public Property createProperty(Namespace namespace, String name, Object value) throws ValueFormatException {
        PropertyTypeDefinition propDef = this.resourceTypeTree.getPropertyTypeDefinition(namespace, name);
        return propDef.createProperty(value);
    }

    /**
     * Gets the {@link PropertyTypeDefinition} for property by namespace and name.
     * 
     * @param ns the namspace
     * @param name the name
     * @return the <code>PropertyTypeDefinition</code> instance.
     */
    public PropertyTypeDefinition getPropertyTypeDefinition(Namespace ns, String name) {
        return this.resourceTypeTree.getPropertyTypeDefinition(ns, name);
    }

}

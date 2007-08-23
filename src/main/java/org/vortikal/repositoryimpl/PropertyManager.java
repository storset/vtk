/* Copyright (c) 2006, University of Oslo, Norway
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
package org.vortikal.repositoryimpl;


import org.vortikal.repository.Namespace;
import org.vortikal.repository.Property;
import org.vortikal.repository.ResourceTypeTree;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.ValueFormatException;

public interface PropertyManager {


    /**
     * Creates (instantiates) a property with a given namespace and
     * name. The created property will have no value set. The type is
     * set according to its {@link PropertyDefinition property
     * definition}, or {@link PropertyType.TYPE_STRING} if it has no
     * definition
     *
     * @param namespace the namespace
     * @param name the name
     * @return a property instance
     */
    public Property createProperty(Namespace namespace, String name);


    /**
     * Creates (instantiates) a property with a given namespace, name
     * and value. The type is set according to its {@link
     * PropertyDefinition property definition}, or {@link
     * PropertyType.TYPE_STRING} if it has no definition
     *
     * @param namespace the namespace
     * @param name the name
     * @return a property instance
     * @throws ValueFormatException if the supplied value's type does
     * not match that of the property definition
     */
    public Property createProperty(Namespace namespace, String name,
            Object value) throws ValueFormatException;


    /**
     * Creates (instantiates) a property with a given namespace, name
     * and values. The type is set according to its {@link
     * PropertyDefinition property definition}, or {@link
     * PropertyType.TYPE_STRING} if it has no definition
     *
     * @param namespace the namespace
     * @param name the name
     * @return a property instance
     * @throws ValueFormatException if the supplied values' type does
     * not match that of the property definition
     */
    public Property createProperty(String namespaceUrl, String name,
            String[] stringValues) throws ValueFormatException;

    /**
     * Create a {@link PropertySet} instance using the supplied
     * {@link PropertyTypeDefinition}.
     * 
     * @param definition 
     * @return
     */
    public Property createProperty(PropertyTypeDefinition definition);
    
    /**
     * Get an instance of {@link ResourceTypeTree}.
     *  
     * @return An instance of {@link ResourceTypeTree}
     */
    public ResourceTypeTree getResourceTypeTree();
    
}

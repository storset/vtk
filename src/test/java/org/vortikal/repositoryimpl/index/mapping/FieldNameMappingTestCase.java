/* Copyright (c) 2007, University of Oslo, Norway
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
package org.vortikal.repositoryimpl.index.mapping;

import junit.framework.TestCase;

import org.vortikal.repository.Namespace;
import org.vortikal.repository.resourcetype.PropertyTypeDefinitionImpl;
import org.vortikal.repositoryimpl.PropertyImpl;

public class FieldNameMappingTestCase extends TestCase {

    public void testGetSearchFieldNameProperty() {
        
        PropertyImpl prop = new PropertyImpl();
        prop.setName("foo");
        prop.setNamespace(Namespace.getNamespaceFromPrefix("bar"));
        
        assertEquals("bar:foo", FieldNameMapping.getSearchFieldName(prop));
        
        prop = new PropertyImpl();
        prop.setName("lastModified");
        prop.setNamespace(Namespace.DEFAULT_NAMESPACE);
        
        assertEquals("lastModified", FieldNameMapping.getSearchFieldName(prop));
        
    }

    public void testGetSearchFieldNamePropertyTypeDefinition() {

        PropertyTypeDefinitionImpl def = 
            new PropertyTypeDefinitionImpl();
        
        def.setName("foo");
        def.setNamespace(Namespace.getNamespaceFromPrefix("bar"));
        
        assertEquals("bar:foo", FieldNameMapping.getSearchFieldName(def));
        
        def = new PropertyTypeDefinitionImpl();
        def.setName("lastModified");
        def.setNamespace(Namespace.DEFAULT_NAMESPACE);
        
        assertEquals("lastModified", FieldNameMapping.getSearchFieldName(def));
        
    }

    public void testGetSearchFieldNameStringString() {
        
        String fieldName = FieldNameMapping.getSearchFieldName("foo", null);
        assertEquals("foo", fieldName);
        
        fieldName = FieldNameMapping.getSearchFieldName("bar", "foo");
        assertEquals("foo:bar", fieldName);
        
    }

    public void testGetStoredFieldNameProperty() {
        
        PropertyImpl prop = new PropertyImpl();
        prop.setName("foo");
        prop.setNamespace(Namespace.getNamespaceFromPrefix("bar"));
        
        String fieldName = FieldNameMapping.getStoredFieldName(prop);
        
        assertEquals("_bar:foo", fieldName);
        
        prop = new PropertyImpl();
        prop.setName("lastModified");
        prop.setNamespace(Namespace.DEFAULT_NAMESPACE);
        
        fieldName = FieldNameMapping.getStoredFieldName(prop);
        assertEquals("_lastModified", fieldName);
        
    }

    public void testGetStoredFieldNamePropertyTypeDefinition() {
        
        PropertyTypeDefinitionImpl def = 
            new PropertyTypeDefinitionImpl();
        def.setName("foo");
        def.setNamespace(Namespace.getNamespaceFromPrefix("bar"));
        
        String fieldName = FieldNameMapping.getStoredFieldName(def);
        
        assertEquals("_bar:foo", fieldName);
        
        def = new PropertyTypeDefinitionImpl();
        def.setName("lastModified");
        def.setNamespace(Namespace.DEFAULT_NAMESPACE);
        
        fieldName = FieldNameMapping.getStoredFieldName(def);
        
        assertEquals("_lastModified", fieldName);
        
    }

    public void testGetPropertyNamespacePrefixFromStoredFieldName() {
        
        String fieldName = "_foo";
        
        String nsPrefix = FieldNameMapping.getPropertyNamespacePrefixFromStoredFieldName(fieldName);
        
        assertNull(nsPrefix);
        
        fieldName = "_bar:foo";
        
        nsPrefix = FieldNameMapping.getPropertyNamespacePrefixFromStoredFieldName(fieldName);
        
        assertEquals("bar", nsPrefix);
    }

    public void testGetPropertyNameFromStoredFieldName() {
        String fieldName = "_foo";
        
        String name = FieldNameMapping.getPropertyNameFromStoredFieldName(fieldName);
        
        assertEquals("foo", name);
        
        fieldName = "_bar:foo";
        
        name = FieldNameMapping.getPropertyNameFromStoredFieldName(fieldName);
        
        assertEquals("foo", name);
        
    }

}

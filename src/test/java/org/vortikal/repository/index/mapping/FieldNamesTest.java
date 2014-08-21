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
package org.vortikal.repository.index.mapping;

import java.util.Locale;

import org.vortikal.repository.Namespace;
import org.vortikal.repository.Property;
import org.vortikal.repository.resourcetype.IllegalValueTypeException;
import org.vortikal.repository.resourcetype.PropertyTypeDefinitionImpl;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.repository.resourcetype.ValueFormatter;

import static org.vortikal.repository.resourcetype.PropertyType.Type;

import static org.junit.Assert.*;
import org.junit.Test;

public class FieldNamesTest {

    private Property getTypedProperty(Namespace namespace, String name, final Type type) {
        PropertyTypeDefinitionImpl propDef = new PropertyTypeDefinitionImpl();
        propDef.setNamespace(namespace);
        propDef.setName(name);
        propDef.setType(type);
        propDef.setValueFormatter(new ValueFormatter() {
            @Override
            public Value stringToValue(String string, String format,
                    Locale locale) {
                return new Value(string, type);
            }

            @Override
            public String valueToString(Value value, String format,
                    Locale locale) throws IllegalValueTypeException {
                return value.toString();
            }
            
        });
        propDef.afterPropertiesSet();
        return propDef.createProperty();
    }
    
    private Property getStringProperty(Namespace namespace, String name) {
        return getTypedProperty(namespace, name, Type.STRING);
    }
    
    @Test
    public void sortFieldName() {
        Property prop = getStringProperty(Namespace.getNamespaceFromPrefix("bar"), "foo");
        assertEquals("p_s_bar:foo", FieldNames.sortFieldName(prop.getDefinition()));
        
        prop = getTypedProperty(Namespace.DEFAULT_NAMESPACE, "integer", Type.INT);
        assertEquals("p_integer", FieldNames.sortFieldName(prop.getDefinition()));
    }
    
    @Test
    public void jsonSortFieldName() {
        Property prop = getTypedProperty(Namespace.getNamespaceFromPrefix("bar"), "foo", Type.JSON);
        assertEquals("p_s_bar:foo@baz", FieldNames.jsonSortFieldName(prop.getDefinition(), "baz"));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void combineLowercaseAndSortInSameField() {
        FieldNames.propertyFieldName("foo", "bar", true, true);
    }
    
    @Test
    public void isLowercaseField() {
        assertFalse(FieldNames.isLowercaseField("p_foo"));
        assertFalse(FieldNames.isLowercaseField("p_bar:foo"));
        assertTrue(FieldNames.isLowercaseField("p_l_bar:foo"));
        assertFalse(FieldNames.isLowercaseField(FieldNames.NAME_FIELD_NAME));
        assertTrue(FieldNames.isLowercaseField(FieldNames.NAME_LC_FIELD_NAME));
        assertFalse(FieldNames.isLowercaseField("what:ever"));
    }
    
    @Test
    public void isPropertyField() {
        assertTrue(FieldNames.isPropertyField("p_foo"));
        assertTrue(FieldNames.isPropertyField("p_bar:foo"));
        assertTrue(FieldNames.isPropertyField("p_l_bar:foo"));
        assertFalse(FieldNames.isPropertyField(FieldNames.NAME_FIELD_NAME));
        assertFalse(FieldNames.isPropertyField(FieldNames.NAME_LC_FIELD_NAME));
        assertFalse(FieldNames.isPropertyField("what:ever"));
    }

    @Test
    public void getSearchFieldNameProperty() {
        
        Property prop = getStringProperty(Namespace.getNamespaceFromPrefix("bar"), "foo");
        
        assertEquals("p_bar:foo", FieldNames.propertyFieldName(prop.getDefinition(), false));
        assertEquals("p_l_bar:foo", FieldNames.propertyFieldName(prop.getDefinition(), true));
        
        prop = getStringProperty(Namespace.DEFAULT_NAMESPACE, "lastModified");
        
        assertEquals("p_lastModified", FieldNames.propertyFieldName(prop.getDefinition(), false));
        
        assertEquals("p_l_lastModified", FieldNames.propertyFieldName(prop.getDefinition(), true));
        
    }
    
    @Test
    public void getJsonSearchFieldName() {
        Property prop = getStringProperty(Namespace.STRUCTURED_RESOURCE_NAMESPACE, "complex");
        assertEquals("p_resource:complex@attr1", FieldNames.jsonFieldName(prop.getDefinition(), "attr1", false));
        assertEquals("p_l_resource:complex@attr1", FieldNames.jsonFieldName(prop.getDefinition(), "attr1", true));
        
        prop = getStringProperty(Namespace.DEFAULT_NAMESPACE, "system-job-status");
        assertEquals("p_system-job-status@attr1", FieldNames.jsonFieldName(prop.getDefinition(), "attr1", false));
        assertEquals("p_l_system-job-status@attr1", FieldNames.jsonFieldName(prop.getDefinition(), "attr1", true));
    }
    
    @Test
    public void isStoredFieldInNamespace() {
        assertTrue(FieldNames.isPropertyFieldInNamespace("p_title", Namespace.DEFAULT_NAMESPACE));
        assertTrue(FieldNames.isPropertyFieldInNamespace("p_owner", Namespace.DEFAULT_NAMESPACE));
        assertFalse(FieldNames.isPropertyFieldInNamespace("p_resource:author", Namespace.DEFAULT_NAMESPACE));
        
        assertTrue(FieldNames.isPropertyFieldInNamespace("p_resource:author", Namespace.STRUCTURED_RESOURCE_NAMESPACE));
        assertFalse(FieldNames.isPropertyFieldInNamespace("p_resource:author", Namespace.DEFAULT_NAMESPACE));

        assertFalse(FieldNames.isPropertyFieldInNamespace("p_content:keywords", Namespace.STRUCTURED_RESOURCE_NAMESPACE));
        assertFalse(FieldNames.isPropertyFieldInNamespace("p_content:keywords", Namespace.DEFAULT_NAMESPACE));
    }

    @Test
    public void getSearchFieldNamePropertyTypeDefinition() {

        PropertyTypeDefinitionImpl def = 
            new PropertyTypeDefinitionImpl();
        
        def.setName("foo");
        def.setNamespace(Namespace.getNamespaceFromPrefix("bar"));
        
        assertEquals("p_bar:foo", FieldNames.propertyFieldName(def, false));
        assertEquals("p_l_bar:foo", FieldNames.propertyFieldName(def, true));
        
        def = new PropertyTypeDefinitionImpl();
        def.setName("lastModified");
        def.setNamespace(Namespace.DEFAULT_NAMESPACE);
        
        assertEquals("p_lastModified", FieldNames.propertyFieldName(def, false));
        assertEquals("p_l_lastModified",FieldNames.propertyFieldName(def, true));
        
    }

    @Test
    public void getSearchFieldNameStringString() {
        
        String fieldName = FieldNames.propertyFieldName("foo", null, false, false);
        assertEquals("p_foo", fieldName);
        
        fieldName = FieldNames.propertyFieldName("foo", null, true, false);
        assertEquals("p_l_foo", fieldName);
        
        fieldName = FieldNames.propertyFieldName("bar", "foo", false, false);
        assertEquals("p_foo:bar", fieldName);
        
        fieldName = FieldNames.propertyFieldName("bar", "foo", true, false);
        assertEquals("p_l_foo:bar", fieldName);
    }

    @Test
    public void getStoredFieldNamePropertyTypeDefinition() {
        
        PropertyTypeDefinitionImpl def = 
            new PropertyTypeDefinitionImpl();
        def.setName("foo");
        def.setNamespace(Namespace.getNamespaceFromPrefix("bar"));
        
        String fieldName = FieldNames.propertyFieldName(def);
        
        assertEquals("p_bar:foo", fieldName);
        
        def = new PropertyTypeDefinitionImpl();
        def.setName("lastModified");
        def.setNamespace(Namespace.DEFAULT_NAMESPACE);
        
        fieldName = FieldNames.propertyFieldName(def);
        
        assertEquals("p_lastModified", fieldName);
        
    }

    @Test
    public void getPropertyNamespacePrefixFromStoredFieldName() {
        
        String fieldName = "p_foo";
        
        String nsPrefix = FieldNames.propertyNamespace(fieldName);
        
        assertNull(nsPrefix);
        
        fieldName = "p_bar:foo";
        
        nsPrefix = FieldNames.propertyNamespace(fieldName);
        
        assertEquals("bar", nsPrefix);
    }

    @Test
    public void getPropertyNameFromStoredFieldName() {
        String fieldName = "p_foo";
        
        String name = FieldNames.propertyName(fieldName);
        
        assertEquals("foo", name);
        
        fieldName = "p_bar:foo";
        
        name = FieldNames.propertyName(fieldName);
        
        assertEquals("foo", name);
    }
    
}

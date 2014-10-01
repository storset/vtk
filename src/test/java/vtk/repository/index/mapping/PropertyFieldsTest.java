/* Copyright (c) 2014, University of Oslo, Norway
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

package vtk.repository.index.mapping;

import java.util.Locale;

import vtk.repository.Namespace;
import vtk.repository.Property;
import vtk.repository.resourcetype.IllegalValueTypeException;
import vtk.repository.resourcetype.PropertyTypeDefinitionImpl;
import vtk.repository.resourcetype.Value;
import vtk.repository.resourcetype.ValueFormatter;

import static vtk.repository.resourcetype.PropertyType.Type;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 */
public class PropertyFieldsTest {
    
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
        assertEquals("p_s_bar:foo", PropertyFields.sortFieldName(prop.getDefinition()));
        
        prop = getTypedProperty(Namespace.DEFAULT_NAMESPACE, "integer", Type.INT);
        assertEquals("p_integer", PropertyFields.sortFieldName(prop.getDefinition()));
    }
    
    @Test
    public void jsonSortFieldName() {
        Property prop = getTypedProperty(Namespace.getNamespaceFromPrefix("bar"), "foo", Type.JSON);
        assertEquals("p_s_bar:foo@baz", PropertyFields.jsonSortFieldName(prop.getDefinition(), "baz"));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void combineLowercaseAndSortInSameField() {
        PropertyFields.propertyFieldName("foo", "bar", true, true);
    }
    
    @Test
    public void isPropertyField() {
        assertTrue(PropertyFields.isPropertyField("p_foo"));
        assertTrue(PropertyFields.isPropertyField("p_bar:foo"));
        assertTrue(PropertyFields.isPropertyField("p_l_bar:foo"));
        assertFalse(PropertyFields.isPropertyField(ResourceFields.NAME_FIELD_NAME));
        assertFalse(PropertyFields.isPropertyField(ResourceFields.NAME_LC_FIELD_NAME));
        assertFalse(PropertyFields.isPropertyField("what:ever"));
    }

    @Test
    public void getSearchFieldNameProperty() {
        
        Property prop = getStringProperty(Namespace.getNamespaceFromPrefix("bar"), "foo");
        
        assertEquals("p_bar:foo", PropertyFields.propertyFieldName(prop.getDefinition(), false));
        assertEquals("p_l_bar:foo", PropertyFields.propertyFieldName(prop.getDefinition(), true));
        
        prop = getStringProperty(Namespace.DEFAULT_NAMESPACE, "lastModified");
        
        assertEquals("p_lastModified", PropertyFields.propertyFieldName(prop.getDefinition(), false));
        
        assertEquals("p_l_lastModified", PropertyFields.propertyFieldName(prop.getDefinition(), true));
        
    }
    
    @Test
    public void getJsonSearchFieldName() {
        Property prop = getStringProperty(Namespace.STRUCTURED_RESOURCE_NAMESPACE, "complex");
        assertEquals("p_resource:complex@attr1", PropertyFields.jsonFieldName(prop.getDefinition(), "attr1", false));
        assertEquals("p_l_resource:complex@attr1", PropertyFields.jsonFieldName(prop.getDefinition(), "attr1", true));
        
        prop = getStringProperty(Namespace.DEFAULT_NAMESPACE, "system-job-status");
        assertEquals("p_system-job-status@attr1", PropertyFields.jsonFieldName(prop.getDefinition(), "attr1", false));
        assertEquals("p_l_system-job-status@attr1", PropertyFields.jsonFieldName(prop.getDefinition(), "attr1", true));
    }
    
    @Test
    public void isStoredFieldInNamespace() {
        assertTrue(PropertyFields.isPropertyFieldInNamespace("p_title", Namespace.DEFAULT_NAMESPACE));
        assertTrue(PropertyFields.isPropertyFieldInNamespace("p_owner", Namespace.DEFAULT_NAMESPACE));
        assertFalse(PropertyFields.isPropertyFieldInNamespace("p_resource:author", Namespace.DEFAULT_NAMESPACE));
        
        assertTrue(PropertyFields.isPropertyFieldInNamespace("p_resource:author", Namespace.STRUCTURED_RESOURCE_NAMESPACE));
        assertFalse(PropertyFields.isPropertyFieldInNamespace("p_resource:author", Namespace.DEFAULT_NAMESPACE));

        assertFalse(PropertyFields.isPropertyFieldInNamespace("p_content:keywords", Namespace.STRUCTURED_RESOURCE_NAMESPACE));
        assertFalse(PropertyFields.isPropertyFieldInNamespace("p_content:keywords", Namespace.DEFAULT_NAMESPACE));
    }

    @Test
    public void getSearchFieldNamePropertyTypeDefinition() {

        PropertyTypeDefinitionImpl def = 
            new PropertyTypeDefinitionImpl();
        
        def.setName("foo");
        def.setNamespace(Namespace.getNamespaceFromPrefix("bar"));
        
        assertEquals("p_bar:foo", PropertyFields.propertyFieldName(def, false));
        assertEquals("p_l_bar:foo", PropertyFields.propertyFieldName(def, true));
        
        def = new PropertyTypeDefinitionImpl();
        def.setName("lastModified");
        def.setNamespace(Namespace.DEFAULT_NAMESPACE);
        
        assertEquals("p_lastModified", PropertyFields.propertyFieldName(def, false));
        assertEquals("p_l_lastModified",PropertyFields.propertyFieldName(def, true));
        
    }

    @Test
    public void getSearchFieldNameStringString() {
        
        String fieldName = PropertyFields.propertyFieldName("foo", null, false, false);
        assertEquals("p_foo", fieldName);
        
        fieldName = PropertyFields.propertyFieldName("foo", null, true, false);
        assertEquals("p_l_foo", fieldName);
        
        fieldName = PropertyFields.propertyFieldName("bar", "foo", false, false);
        assertEquals("p_foo:bar", fieldName);
        
        fieldName = PropertyFields.propertyFieldName("bar", "foo", true, false);
        assertEquals("p_l_foo:bar", fieldName);
    }

    @Test
    public void getStoredFieldNamePropertyTypeDefinition() {
        
        PropertyTypeDefinitionImpl def = 
            new PropertyTypeDefinitionImpl();
        def.setName("foo");
        def.setNamespace(Namespace.getNamespaceFromPrefix("bar"));
        
        String fieldName = PropertyFields.propertyFieldName(def);
        
        assertEquals("p_bar:foo", fieldName);
        
        def = new PropertyTypeDefinitionImpl();
        def.setName("lastModified");
        def.setNamespace(Namespace.DEFAULT_NAMESPACE);
        
        fieldName = PropertyFields.propertyFieldName(def);
        
        assertEquals("p_lastModified", fieldName);
        
    }

    @Test
    public void getPropertyNamespacePrefixFromStoredFieldName() {
        
        String fieldName = "p_foo";
        
        String nsPrefix = PropertyFields.propertyNamespace(fieldName);
        
        assertNull(nsPrefix);
        
        fieldName = "p_bar:foo";
        
        nsPrefix = PropertyFields.propertyNamespace(fieldName);
        
        assertEquals("bar", nsPrefix);
    }

    @Test
    public void getPropertyNameFromStoredFieldName() {
        String fieldName = "p_foo";
        
        String name = PropertyFields.propertyName(fieldName);
        
        assertEquals("foo", name);
        
        fieldName = "p_bar:foo";
        
        name = PropertyFields.propertyName(fieldName);
        
        assertEquals("foo", name);
    }
    
}

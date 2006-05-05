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
package org.vortikal.repositoryimpl.query;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Property;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.repository.resourcetype.ValueFactory;
import org.vortikal.repositoryimpl.PropertyManagerImpl;
import org.vortikal.repositoryimpl.PropertySetImpl;

/**
 * Simple mapping from Lucene 
 * {@link org.apache.lucene.document.Document} to 
 * {@link org.vortikal.repository.PropertySet} 
 * objects and vice-versa.
 * 
 * XXX: more error-checking
 * 
 * @author oyviste
 */
public class DocumentMapper implements InitializingBean {

    Log logger = LogFactory.getLog(DocumentMapper.class);
    
    public static final String NAME_FIELD_NAME = "name";
    public static final String URI_FIELD_NAME = "uri";
    public static final String RESOURCETYPE_FIELD_NAME = "resourcetype";
    public static final String ANCESTORIDS_FIELD_NAME = "_ANCESTORIDS";
    public static final String ID_FIELD_NAME = "_ID";
    public static final String ACL_INHERITED_FROM_FIELD_NAME = "_ACL_INHERITED_FROM";
    
    public static final String FIELD_NAMESPACEPREFIX_NAME_SEPARATOR = ":";
    
    private static final Set RESERVED_FIELD_NAMES;
    static {
        RESERVED_FIELD_NAMES = new HashSet();
        RESERVED_FIELD_NAMES.add(NAME_FIELD_NAME);
        RESERVED_FIELD_NAMES.add(URI_FIELD_NAME);
        RESERVED_FIELD_NAMES.add(RESOURCETYPE_FIELD_NAME);
        RESERVED_FIELD_NAMES.add(ANCESTORIDS_FIELD_NAME);
        RESERVED_FIELD_NAMES.add(ID_FIELD_NAME);
        RESERVED_FIELD_NAMES.add(ACL_INHERITED_FROM_FIELD_NAME);
    }
    
    public DocumentMapper() {}
    
    private PropertyManagerImpl propertyManager;
    private ValueFactory valueFactory;
    
    public void afterPropertiesSet() {
        if (valueFactory == null) {
            throw new BeanInitializationException("Property 'valueFactory' not set.");
        } else if (propertyManager == null) {
            throw new BeanInitializationException("Proeprty 'propertyManager' not set.");
        }
    }
    
    public Document getDocument(PropertySetImpl propSet) throws DocumentMappingException {
        
        Document doc = new Document();
        
        // Special fields
        Field uriField = FieldMapper.getKeywordField(URI_FIELD_NAME, propSet.getURI());
        doc.add(uriField);
        
        Field nameField = FieldMapper.getKeywordField(NAME_FIELD_NAME, propSet.getName());
        doc.add(nameField);
        
        Field resourceTypeField =
            FieldMapper.getKeywordField(RESOURCETYPE_FIELD_NAME, propSet.getResourceType());
        doc.add(resourceTypeField);
        
        Field parentIdsField = 
            FieldMapper.getUnencodedMultiValueFieldFromIntegers(ANCESTORIDS_FIELD_NAME, 
                                                                    propSet.getAncestorIds());
        doc.add(parentIdsField);
        
        Field idField = FieldMapper.getKeywordField(ID_FIELD_NAME, propSet.getID());
        doc.add(idField);
        
        Field aclField = FieldMapper.getKeywordField(ACL_INHERITED_FROM_FIELD_NAME, 
                                            propSet.getAclInheritedFrom());
        doc.add(aclField);
        
        // Add all props
        for (Iterator i = propSet.getProperties().iterator(); i.hasNext();) {
            Field field = getFieldFromProperty((Property)i.next());
            doc.add(field);
        }
        
        return doc;
    }

    /**
     * 
     * @param doc
     * @return
     * @throws DocumentMappingException
     */
    public PropertySetImpl getPropertySet(Document doc) throws DocumentMappingException {
        
        // XXX: exception handling
        PropertySetImpl propSet = new PropertySetImpl(doc.get(URI_FIELD_NAME));
        propSet.setAclInheritedFrom(Integer.parseInt(doc.get(ACL_INHERITED_FROM_FIELD_NAME)));
        propSet.setID(Integer.parseInt(doc.get(ID_FIELD_NAME)));
        propSet.setResourceType(doc.get(RESOURCETYPE_FIELD_NAME));
        propSet.setAncestorIds(FieldMapper.getIntegersFromUnencodedMultiValueField(
                doc.getField(ANCESTORIDS_FIELD_NAME)));
        
        Enumeration e = doc.fields();
        while (e.hasMoreElements()) {
            Field field = (Field)e.nextElement();
            if (RESERVED_FIELD_NAMES.contains(field.name())) continue;
            
            Property prop = getPropertyFromField(field);
            propSet.addProperty(prop);
        }
        
        return propSet;
    }
    
    /**
     * 
     * @param fields
     * @return
     * @throws FieldMappingException
     */
    private Property getPropertyFromField(Field field) throws FieldMappingException {
        
        String[] fieldNameComponents = field.name().split(FIELD_NAMESPACEPREFIX_NAME_SEPARATOR);
        String nsPrefix = null;
        String name = null;
        if (fieldNameComponents.length == 1) {
            // Default namespace (no prefix)
            name = fieldNameComponents[0];
        } else if (fieldNameComponents.length == 2) {
            nsPrefix = fieldNameComponents[0];
            name = fieldNameComponents[1];
        } else {
            logger.warn("Invalid index field name: '" + field.name() + "'");
            throw new FieldMappingException("Invalid index field name: '" 
                    + field.name() + "'");
        }
        
        Namespace ns = Namespace.getNamespaceFromPrefix(nsPrefix);
        Property property = propertyManager.createProperty(ns, name);
        
        PropertyTypeDefinition def = property.getDefinition();
        
        if (def != null) {
            if (def.isMultiple()) {
                property.setValues(FieldMapper.getValuesFromField(field, valueFactory, 
                        def.getType()));
            } else {
                property.setValue(FieldMapper.getValueFromField(field, valueFactory, 
                        def.getType()));
            }
        } else {
            property.setValue(FieldMapper.getValueFromField(field, valueFactory, 
                    PropertyType.TYPE_STRING));
        }
        
        return property;
    }    
    
    
    private Field getFieldFromProperty(Property property) throws FieldMappingException {
        String name = property.getName();
        String prefix = property.getNamespace().getPrefix();
        String fieldName = null;
        if (prefix == null) {
            fieldName = name;
        } else {
            fieldName = prefix + FIELD_NAMESPACEPREFIX_NAME_SEPARATOR + name;
        }

        if (RESERVED_FIELD_NAMES.contains(fieldName)) {
            throw new FieldMappingException("Property name '" + fieldName 
                    + "' maps to reserved index field.");
        }
        
        PropertyTypeDefinition def = property.getDefinition();
        if (def != null && def.isMultiple()) {
                Value[] values = property.getValues();
                return FieldMapper.getFieldFromValues(fieldName, values);
        } else {
            return FieldMapper.getFieldFromValue(fieldName, property.getValue());
        }

    }    
    
    public void setPropertyManager(PropertyManagerImpl propertyManager) {
        this.propertyManager = propertyManager;
    }

    public void setValueFactory(ValueFactory valueFactory) {
        this.valueFactory = valueFactory;
    }
}

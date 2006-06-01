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
import org.vortikal.repositoryimpl.PropertyManager;
import org.vortikal.repositoryimpl.PropertySetImpl;

/**
 * Simple mapping from Lucene 
 * {@link org.apache.lucene.document.Document} to 
 * {@link org.vortikal.repository.PropertySet} 
 * objects and vice-versa.
 * 
 * XXX: more error-checking
 * TODO: Javadoc
 * 
 * @author oyviste
 */
public class DocumentMapper implements InitializingBean {

    Log logger = LogFactory.getLog(DocumentMapper.class);
    
    public static final String NAME_FIELD_NAME = "name";
    public static final String URI_FIELD_NAME = "uri";
    public static final String RESOURCETYPE_FIELD_NAME = "resourceType";
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
    
    private PropertyManager propertyManager;
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
        // uri
        Field uriField = FieldValueMapper.getKeywordField(URI_FIELD_NAME, propSet.getURI());
        doc.add(uriField);
        
        // name
        Field nameField = FieldValueMapper.getKeywordField(NAME_FIELD_NAME, propSet.getName());
        doc.add(nameField);
        
        // resourceType
        Field resourceTypeField =
            FieldValueMapper.getKeywordField(RESOURCETYPE_FIELD_NAME, propSet.getResourceType());
        doc.add(resourceTypeField);
        
        // _ANCESTOR_IDS (index system field)
        Field ancestorIdsField = 
            FieldValueMapper.getUnencodedMultiValueFieldFromIntegers(ANCESTORIDS_FIELD_NAME, 
                                                                    propSet.getAncestorIds());
        doc.add(ancestorIdsField);
        
        // _ID (index system field)
        Field idField = FieldValueMapper.getKeywordField(ID_FIELD_NAME, propSet.getID());
        doc.add(idField);
        
        // _ACL_INHERITED_FROM (index system field)
        Field aclField = FieldValueMapper.getKeywordField(ACL_INHERITED_FROM_FIELD_NAME, 
                                            propSet.getAclInheritedFrom());
        doc.add(aclField);
        
        // Add all other properties except dead ones.
        for (Iterator i = propSet.getProperties().iterator(); i.hasNext();) {
            Field field = getFieldFromProperty((Property)i.next());
            if (field != null) doc.add(field);
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
        propSet.setAncestorIds(FieldValueMapper.getIntegersFromUnencodedMultiValueField(
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
     * @throws FieldValueMappingException
     */
    private Property getPropertyFromField(Field field) throws FieldValueMappingException {
        
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
            throw new FieldValueMappingException("Invalid index field name: '" 
                    + field.name() + "'");
        }
        
        PropertyTypeDefinition def = propertyManager.getPropertyDefinitionByPrefix(nsPrefix, name);
        Namespace ns = def == null ? Namespace.getNamespaceFromPrefix(nsPrefix) : def.getNamespace();
        Property property = propertyManager.createProperty(ns, name);
        
        if (def != null) {
            if (def.isMultiple()) {
                property.setValues(FieldValueMapper.getValuesFromField(field, valueFactory, 
                        def.getType()));
            } else {
                property.setValue(FieldValueMapper.getValueFromField(field, valueFactory, 
                        def.getType()));
            }
        } else {
            property.setValue(FieldValueMapper.getValueFromField(field, valueFactory, 
                    PropertyType.TYPE_STRING));
        }
        
        return property;
    }    
    

    /**
     * Creates a Lucene field from a property.
     *
     * @param property the property
     * @return the Lucene field, or <code>null</code> if no property
     * definition exists (i.e. the property is dead)
     * @exception FieldValueMappingException if an error occurs
     */
    private Field getFieldFromProperty(Property property) throws FieldValueMappingException {
        String name = property.getName();
        String prefix = property.getNamespace().getPrefix();
        String fieldName = null;
        if (prefix == null) {
            fieldName = name;
        } else {
            fieldName = prefix + FIELD_NAMESPACEPREFIX_NAME_SEPARATOR + name;
        }

        if (RESERVED_FIELD_NAMES.contains(fieldName)) {
            throw new FieldValueMappingException("Property field name '" + fieldName 
                    + "' is a reserved index field.");
        }
        
        PropertyTypeDefinition def = property.getDefinition();
        if (def != null && def.isMultiple()) {
                Value[] values = property.getValues();
                return FieldValueMapper.getFieldFromValues(fieldName, values);
        } else if (def != null) {
            return FieldValueMapper.getFieldFromValue(fieldName, property.getValue());
        }
        
        // Dead property
        return null;
    }
    
    public static String getFieldName(PropertyTypeDefinition def) {
        if (def == null) {
            throw new IllegalArgumentException("Definition cannot be null");
        }
        
        String name = def.getName();
        String prefix = def.getNamespace().getPrefix();
        if (prefix == null) {
            return name;
        } else {
            return prefix + FIELD_NAMESPACEPREFIX_NAME_SEPARATOR + name;
        }
    }
    

    public void setPropertyManager(PropertyManager propertyManager) {
        this.propertyManager = propertyManager;
    }

    public void setValueFactory(ValueFactory valueFactory) {
        this.valueFactory = valueFactory;
    }
}

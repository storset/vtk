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
import java.util.LinkedList;
import java.util.List;
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
import org.vortikal.util.repository.URIUtil;

/**
 * Simple mapping from Lucene 
 * {@link org.apache.lucene.document.Document} to 
 * {@link org.vortikal.repository.PropertySet} 
 * objects and vice-versa.
 * 
 * TODO: Optimize even more the time critical code that maps from <code>Document</code>s to
 * <code>PropertySet</code>s used when building result sets.
 * 
 * XXX: more error-checking
 * TODO: Javadoc
 * 
 * @author oyviste
 */
public class DocumentMapper implements InitializingBean {

    private static Log logger = LogFactory.getLog(DocumentMapper.class);
    
    /* Special, reserved fields */
    public static final String NAME_FIELD_NAME =               "name";
    public static final String URI_FIELD_NAME =                "uri";
    public static final String URI_DEPTH_FIELD_NAME =          "uriDepth";
    public static final String RESOURCETYPE_FIELD_NAME =       "resourceType";
    public static final String ANCESTORIDS_FIELD_NAME =        "ANCESTORIDS";
    public static final String ID_FIELD_NAME =                 "ID";
    public static final String ACL_INHERITED_FROM_FIELD_NAME = "ACL_INHERITED_FROM";
    
    private static final Set RESERVED_FIELD_NAMES;
    static {
        RESERVED_FIELD_NAMES = new HashSet();
        RESERVED_FIELD_NAMES.add(NAME_FIELD_NAME);
        RESERVED_FIELD_NAMES.add(URI_FIELD_NAME);
        RESERVED_FIELD_NAMES.add(RESOURCETYPE_FIELD_NAME);
        RESERVED_FIELD_NAMES.add(ANCESTORIDS_FIELD_NAME);
        RESERVED_FIELD_NAMES.add(ID_FIELD_NAME);
        RESERVED_FIELD_NAMES.add(ACL_INHERITED_FROM_FIELD_NAME);
        RESERVED_FIELD_NAMES.add(URI_DEPTH_FIELD_NAME);
    }

    /* Special field characters and prefixes */
    public static final String FIELD_NAMESPACEPREFIX_NAME_SEPARATOR = ":";
    public static final String STORED_BINARY_FIELD_PREFIX = "_";
    
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
        // Uri
        Field uriField = FieldValueMapper.getStoredKeywordField(URI_FIELD_NAME, propSet.getURI());
        doc.add(uriField);
        
        // Uri depth (not stored, but indexed for use in searches)
        int uriDepth = URIUtil.getUriDepth(propSet.getURI());
        Field uriDepthField = FieldValueMapper.getKeywordField(URI_DEPTH_FIELD_NAME, uriDepth);
        doc.add(uriDepthField);
        
        // Name
        Field nameField = FieldValueMapper.getStoredKeywordField(NAME_FIELD_NAME, propSet.getName());
        doc.add(nameField);
        
        // ResourceType
        Field resourceTypeField =
            FieldValueMapper.getStoredKeywordField(RESOURCETYPE_FIELD_NAME, propSet.getResourceType());
        doc.add(resourceTypeField);
        
        // ANCESTOR_IDS (index system field)
        Field ancestorIdsField = 
            FieldValueMapper.getUnencodedMultiValueFieldFromIntegers(ANCESTORIDS_FIELD_NAME, 
                                                                    propSet.getAncestorIds());
        doc.add(ancestorIdsField);
        
        // ID (index system field)
        Field idField = FieldValueMapper.getStoredKeywordField(ID_FIELD_NAME, propSet.getID());
        doc.add(idField);
        
        // ACL_INHERITED_FROM (index system field)
        Field aclField = FieldValueMapper.getStoredKeywordField(ACL_INHERITED_FROM_FIELD_NAME, 
                                            propSet.getAclInheritedFrom());
        doc.add(aclField);
        
        // Add all other properties except dead ones.
        for (Iterator i = propSet.getProperties().iterator(); i.hasNext();) {
            Property property = (Property)i.next();
            if (property.getDefinition() == null) continue; // Skip dead props
           
            // The field used for searching on the property
            Field indexedField = getIndexedFieldFromProperty(property);
            if (indexedField != null) {
                doc.add(indexedField);
                
                // Stored fields used for re-creating the actual property value
                // from binary data.
                Field[] storedFields = getStoredFieldsFromProperty(property);
                for (int u=0; u < storedFields.length; u++) {
                    doc.add(storedFields[u]);
                }
            }
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
        
        // XXX: I don't think applications/clients are really interested in this
        //      so we don't bother doing the expensive parse-work involved.
        //propSet.setAncestorIds(FieldValueMapper.getIntegersFromUnencodedMultiValueField(
        //      doc.getField(ANCESTORIDS_FIELD_NAME)));
        
        // Loop through all stored binary fields and re-create properties with
        // values. Multi-valued properties are stored as a sequence of fields
        // (with the same name) in the index.
        Enumeration e = doc.fields();
        String currentName = null;
        List fields = null;
        while (e.hasMoreElements()) {
            Field field = (Field)e.nextElement();

            if (RESERVED_FIELD_NAMES.contains(field.name())) {
                currentName = null;
                continue;
            }
            
            if (currentName == null) {
                // New field
                currentName = field.name();
                fields = new LinkedList();
            }
            
            if (field.name().equals(currentName)) {
                fields.add(field);
            } else {
                propSet.addProperty(getPropertyFromStoredFieldValues(currentName,  
                                                                     fields));

                fields = new LinkedList();
                currentName = field.name();
                fields.add(field);
            }
        }
        
        // Make sure we don't forget the last field
        if (currentName != null && fields != null) {
            propSet.addProperty(getPropertyFromStoredFieldValues(currentName,
                    fields));        
        }
        
        return propSet;
    }
    
    /**
     * Re-create a <code>Property</code> from index fields.
     * @param fields
     * @return
     * @throws FieldValueMappingException
     */
    private Property getPropertyFromStoredFieldValues(String fieldName, 
                                                      List storedValueFields) 
        throws FieldValueMappingException {
        
        int sfpLength = STORED_BINARY_FIELD_PREFIX.length();
        int pos = fieldName.indexOf(FIELD_NAMESPACEPREFIX_NAME_SEPARATOR, sfpLength);
        
        String name = null;
        String nsPrefix = null;
        if (pos == -1) {
            // Default namespace (no prefix)
            name = fieldName.substring(sfpLength, fieldName.length());
        } else {
            nsPrefix = fieldName.substring(sfpLength, pos);
            name = fieldName.substring(pos+1, fieldName.length());
        } 
        
        PropertyTypeDefinition def = propertyManager.getPropertyDefinitionByPrefix(nsPrefix, name);
        Namespace ns = def == null ? Namespace.getNamespaceFromPrefix(nsPrefix) : def.getNamespace();
        Property property = propertyManager.createProperty(ns, name);
        
        if (def != null) {          // Def should really not be null here, unless the config has changed
            if (def.isMultiple()) { // and indexes haven't been updated to reflect this.
                
                Value[] values = 
                    BinaryFieldValueMapper.getValuesFromBinaryFields(
                            storedValueFields, valueFactory, def.getType());
                
                property.setValues(values);
            } else {
                if (storedValueFields.size() != 1) {
                    // Fail hard if multiple stored fields found for single value property
                    throw new DocumentMappingException("Single value property '"
                            + nsPrefix + FIELD_NAMESPACEPREFIX_NAME_SEPARATOR
                            + name + "' has an invalid number of stored values (!= 1) in index.");
                }
                
                Value value = BinaryFieldValueMapper.getValueFromBinaryField(
                        (Field)storedValueFields.get(0), valueFactory, def.getType());
                
                property.setValue(value);
            }
        } else {
            logger.warn("Definition for property '" + nsPrefix 
                    + FIELD_NAMESPACEPREFIX_NAME_SEPARATOR + name + "' not found by "
                    + " property manager. Config might have been updated without "
                    + " updating index(es)");
            
            Value value = BinaryFieldValueMapper.getValueFromBinaryField(
                    (Field)storedValueFields.get(0), valueFactory, PropertyType.TYPE_STRING);
            
            property.setValue(value);
        }

        return property;
    }    
    

    /**
     * Creates an indexable Lucene field from a property.
     *
     * @param property the property
     * @return the Lucene field, or <code>null</code> if no property
     * definition exists (i.e. the property is dead)
     * @exception FieldValueMappingException if an error occurs
     */
    private Field getIndexedFieldFromProperty(Property property) throws FieldValueMappingException {
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
        if (def.isMultiple()) {
                Value[] values = property.getValues();
                return FieldValueMapper.getFieldFromValues(fieldName, values);
        } else {
            return FieldValueMapper.getFieldFromValue(fieldName, property.getValue());
        }
    }

    private Field[] getStoredFieldsFromProperty(Property property)
        throws FieldValueMappingException {
        
        String name = property.getName();
        String prefix = property.getNamespace().getPrefix();
        String fieldName = null;
        if (prefix == null) {
            fieldName = STORED_BINARY_FIELD_PREFIX + name;
        } else {
            fieldName = STORED_BINARY_FIELD_PREFIX + prefix + 
            FIELD_NAMESPACEPREFIX_NAME_SEPARATOR + name;
        }

        if (RESERVED_FIELD_NAMES.contains(fieldName)) {
            throw new DocumentMappingException("Property field name '" + fieldName 
                    + "' is a reserved index field.");
        }
        
        PropertyTypeDefinition def = property.getDefinition();
        if (def.isMultiple()) {
                Value[] values = property.getValues();
                return BinaryFieldValueMapper.getBinaryFieldsFromValues(fieldName, values);
        } else {
            Field[] singleField = new Field[1];
            singleField[0] = BinaryFieldValueMapper.getBinaryFieldFromValue(fieldName, 
                    property.getValue());
            return singleField;
        }
    }

    public static String getFieldName(Property prop) {
        return getFieldName(prop.getName(), prop.getNamespace().getPrefix());
    }
    
    public static String getFieldName(PropertyTypeDefinition def) {
        if (def == null) {
            throw new IllegalArgumentException("Definition cannot be null");
        }
        
        return getFieldName(def.getName(), def.getNamespace().getPrefix());
    }
    
    private static String getFieldName(String propName, String propPrefix) {
        if (propPrefix == null) {
            return propName;
        } else {
            return propPrefix + FIELD_NAMESPACEPREFIX_NAME_SEPARATOR + propName;
        }
    }
    
    public void setPropertyManager(PropertyManager propertyManager) {
        this.propertyManager = propertyManager;
    }

    public void setValueFactory(ValueFactory valueFactory) {
        this.valueFactory = valueFactory;
    }
}

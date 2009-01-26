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
package org.vortikal.repository.index.mapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.document.FieldSelectorResult;
import org.apache.lucene.index.IndexReader;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySetImpl;
import org.vortikal.repository.ResourceTypeTree;
import org.vortikal.repository.domain.ContextManager;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.ResourceTypeDefinition;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.repository.search.PropertySelect;
import org.vortikal.repository.search.WildcardPropertySelect;
import org.vortikal.repository.resourcetype.PropertyType.Type;

/**
 * Simple mapping from Lucene {@link org.apache.lucene.document.Document} to
 * {@link org.vortikal.repository.PropertySet} objects and vice-versa.
 * 
 * XXX: more error-checking
 * TODO: Javadoc
 * 
 */
public class DocumentMapperImpl implements DocumentMapper {

    private static Log logger = LogFactory.getLog(DocumentMapperImpl.class);
    
    /* Fast lookup from stored field name in index -> PropertyTypeDefinition */
    private Map<String, PropertyTypeDefinition> fieldNamePropDefMap = 
        new HashMap<String, PropertyTypeDefinition>(); 

    private ResourceTypeTree resourceTypeTree;
    
    private FieldValueMapper fieldValueMapper;

    private ContextManager contextManager;
    
    /**
     * Map <code>PropertySetImpl</code> to Lucene <code>Document</code>.
     */
    public Document getDocument(PropertySetImpl propSet) throws DocumentMappingException {
        Document doc = new Document();
        
        // Special fields
        // uri
        Field uriField = this.fieldValueMapper.getStoredKeywordField(FieldNameMapping.URI_FIELD_NAME, 
                                                                     propSet.getURI().toString());
        doc.add(uriField);
        
        // uriDepth (not stored, but indexed for use in searches)
        int uriDepth = propSet.getURI().getDepth();
        Field uriDepthField = this.fieldValueMapper.getKeywordField(FieldNameMapping.URI_DEPTH_FIELD_NAME, 
                                                                    uriDepth);
        doc.add(uriDepthField);
        
        // name
        Field nameField = this.fieldValueMapper.getStoredKeywordField(FieldNameMapping.NAME_FIELD_NAME, propSet.getName());
        doc.add(nameField);
        
        // name (lowercased)
        Field nameFieldLc = this.fieldValueMapper.getKeywordField(FieldNameMapping.NAME_LC_FIELD_NAME, 
                                                                  propSet.getName().toLowerCase());
        doc.add(nameFieldLc);
        
        // resourceType
        Field resourceTypeField =
            this.fieldValueMapper.getStoredKeywordField(FieldNameMapping.RESOURCETYPE_FIELD_NAME, propSet.getResourceType());
        doc.add(resourceTypeField);
        
        // ANCESTORIDS (index system field)
        Field ancestorIdsField = 
            this.fieldValueMapper.getUnencodedMultiValueFieldFromIntegers(FieldNameMapping.ANCESTORIDS_FIELD_NAME, 
                                                                    propSet.getAncestorIds());
        doc.add(ancestorIdsField);
        
        // ID (index system field)
        Field idField = this.fieldValueMapper.getKeywordField(FieldNameMapping.ID_FIELD_NAME, propSet.getID());
        doc.add(idField);
        Field storedIdField = this.fieldValueMapper.getStoredBinaryIntegerField(FieldNameMapping.STORED_ID_FIELD_NAME, 
                                                                    propSet.getID());
        doc.add(storedIdField);
        
        // ACL_INHERITED_FROM (index system field)
        Field aclField = this.fieldValueMapper.getStoredBinaryIntegerField(
                FieldNameMapping.ACL_INHERITED_FROM_FIELD_NAME, propSet.getAclInheritedFrom());
        doc.add(aclField);
        
        ResourceTypeDefinition resourceDef = 
                this.resourceTypeTree.getResourceTypeDefinitionByName(propSet.getResourceType());
        
        List<PropertyTypeDefinition> propDefs = 
            this.resourceTypeTree.getPropertyTypeDefinitionsForResourceTypeIncludingAncestors(resourceDef);
        
        // Index only properties that satisfy the following conditions:
        // 1) Belongs to the resource type's definition
        // 2) Exists in the property set.
        // 3) Is not of binary type.
        for (PropertyTypeDefinition propDef: propDefs) {
            Property property = propSet.getProperty(propDef);
            
            if (property == null || property.getType() == Type.BINARY) continue;
           
            // The field used for searching on the property (w/multi-values encoded for proper analysis)
            Field indexedField = getIndexedFieldFromProperty(property, false);
            doc.add(indexedField);
            
            // Lower-case version of searchable field (only for property types STRING and HTML)
            switch (property.getDefinition().getType()){
            case HTML:
            case STRING:
                Field lowercaseIndexedField = getIndexedFieldFromProperty(property, true);
                doc.add(lowercaseIndexedField);
            default:
                // Don't add lowercase-versions for other types
            }
            
            // The field(s) used for storing the property value(s) (in binary form) 
            Field[] storedFields = getStoredFieldsFromProperty(property);
            for (Field storedField: storedFields) {
                doc.add(storedField);
            }
        }
        
        return doc;
    }
    
    public FieldSelector getDocumentFieldSelector(final PropertySelect select) {
        
        FieldSelector selector = null;
        if (select == WildcardPropertySelect.WILDCARD_PROPERTY_SELECT) {
            selector = new FieldSelector() {
                private static final long serialVersionUID = -8502087584408029619L;

                public FieldSelectorResult accept(String fieldName) {
                    return FieldSelectorResult.LOAD;
                }
                
            };
        } else {
            selector = new FieldSelector() {
                private static final long serialVersionUID = 3919989917870796613L;

                public FieldSelectorResult accept(String fieldName) {
                    PropertyTypeDefinition def = 
                        DocumentMapperImpl.this.fieldNamePropDefMap.get(fieldName);
                    
                    if (def == null) {
                        // Reserved field
                        return FieldSelectorResult.LOAD;
                    }
                    
                    if (!select.isIncludedProperty(def)
                        && !fieldName.equals(FieldNameMapping.OWNER_PROPERTY_STORED_FIELD_NAME)) {
                            
                            return FieldSelectorResult.NO_LOAD;
                    } 
                    
                    // Default policy is to load otherwise unknown fields
                    return FieldSelectorResult.LOAD;
                }
            };
        }
        
        return selector;
    }
    
    /**
     * Map from Lucene <code>Document</code> instance to a  repository 
     * <code>PropertySetImpl</code> instance. 
     * 
     * This method is heavily used when generating query results and
     * is critical for general query performance. Emphasis should be placed
     * on optimizing it as much as possible. Preferably use 
     * {@link #loadDocumentWithFieldSelection(IndexReader, int, PropertySelect)}
     * to load the document before passing it to this method. Only loaded
     * fields will be mapped to properties.
     * 
     * @param doc The {@link org.apache.lucene.document.Document} to map from
     * @param select A {@link PropertySelect} instance determining which
     *               properties that should be mapped.
     *
     * @return
     * @throws DocumentMappingException
     */
    @SuppressWarnings("unchecked")
    public PropertySetImpl getPropertySet(Document doc) 
        throws DocumentMappingException {
        
        PropertySetImpl propSet = new PropertySetImpl();
        propSet.setUri(Path.fromString(doc.get(FieldNameMapping.URI_FIELD_NAME)));
        propSet.setAclInheritedFrom(this.fieldValueMapper.getIntegerFromStoredBinaryField(
                doc.getField(FieldNameMapping.ACL_INHERITED_FROM_FIELD_NAME)));
        propSet.setID(this.fieldValueMapper.getIntegerFromStoredBinaryField(
                doc.getField(FieldNameMapping.STORED_ID_FIELD_NAME)));
        propSet.setResourceType(doc.get(FieldNameMapping.RESOURCETYPE_FIELD_NAME));
        
        // Loop through all stored binary fields and re-create properties with
        // values. Multi-valued properties are stored as a _sequence_of_binary_fields_
        // (with the same name) in the index.
        // Note that the iteration will _only_ contain _stored_ fields.
        String currentName = null;
        List<Field> fields = new ArrayList<Field>();
        
        for(Iterator<Field> iterator = doc.getFields().iterator(); iterator.hasNext();) {
            Field field = iterator.next();
            String name = field.name();
            
            // Skip reserved fields
            if (FieldNameMapping.isReservedField(name)) continue;
            
            if (currentName == null) {
                currentName = name;
            }
            
            // Field.name() returns an internalized String instance. 
            // Optimize by only comparing reference instead of calling 
            // String.equals(Object o). This saves a method call, 
            // and a full string comparison when the references differ.
            if (name == currentName) {
                fields.add(field);
            } else {
                Property prop = getPropertyFromStoredFieldValues(currentName, 
                                                                 fields);
                propSet.addProperty(prop);
                fields.clear();
                currentName = name;
                fields.add(field);
            }
        }
        
        // Make sure we don't forget the last field
        if (currentName != null) {
            Property prop = getPropertyFromStoredFieldValues(currentName,
                                                             fields);
            propSet.addProperty(prop);
        }
        
        if (this.contextManager != null) {
            addContext(propSet);
        }

        return propSet;
    }

    private void addContext(PropertySetImpl propSet) {
        Path uri = propSet.getURI();
        Map<PropertyTypeDefinition, String> context = this.contextManager.getContext(uri);

        if (context == null)
            return;
        
        for (PropertyTypeDefinition propDef : context.keySet()) {
            String stringValue = context.get(propDef);
            Property property = propDef.createProperty();
            // XXX: only handles string values in context! 
            property.setStringValue(stringValue);
            propSet.addProperty(property);
        }
    }
    
    /**
     * Re-create a <code>Property</code> from index fields.
     * @param fields
     * @return
     * @throws FieldValueMappingException
     */
    private Property getPropertyFromStoredFieldValues(
                                                  String fieldName, 
                                                  List<Field> storedValueFields) 
        throws FieldValueMappingException {
        
        PropertyTypeDefinition def = this.fieldNamePropDefMap.get(fieldName);
        
        if (def == null) {
            // No definition found, make it a String type and log a warning
            String name = 
                FieldNameMapping.getPropertyNameFromStoredFieldName(fieldName);
            String nsPrefix = 
                FieldNameMapping.getPropertyNamespacePrefixFromStoredFieldName(fieldName);

            def = this.resourceTypeTree.getPropertyTypeDefinition(Namespace.getNamespaceFromPrefix(nsPrefix), name);
            
            logger.warn("Definition for property '"
                            + nsPrefix
                            + FieldNameMapping.FIELD_NAMESPACEPREFIX_NAME_SEPARATOR
                            + name
                            + "' not found by "
                            + " property manager. Config might have been updated without "
                            + " updating index(es)");

        } 

        Property property = def.createProperty();
            
        if (def.isMultiple()) {

            Value[] values = this.fieldValueMapper
            .getValuesFromBinaryFields(storedValueFields, def.getType());

            property.setValues(values);
        } else {
            if (storedValueFields.size() != 1) {
                // Fail hard if multiple stored fields found for single
                // value property
                throw new FieldValueMappingException(
                        "Single value property '"
                        + def.getNamespace().getPrefix()
                        + FieldNameMapping.FIELD_NAMESPACEPREFIX_NAME_SEPARATOR
                        + def.getName()
                        + "' has an invalid number of stored values ("
                        + storedValueFields.size() + ") in index.");
            }

            Value value = this.fieldValueMapper.getValueFromBinaryField(
                    storedValueFields.get(0), def.getType());

            property.setValue(value);
        }

        return property;
    }    
    
   /**
     * Creates an indexable <code>Field</code> from a <code>Property</code>.
     *
     */
    private Field getIndexedFieldFromProperty(Property property, boolean lowercase) 
        throws FieldValueMappingException {
        
        String fieldName = FieldNameMapping.getSearchFieldName(property, lowercase);

        if (FieldNameMapping.isReservedField(fieldName)) {
            throw new FieldValueMappingException("Property field name '" + fieldName 
                    + "' is a reserved index field.");
        }   
        
        PropertyTypeDefinition def = property.getDefinition();
        if (def == null) {
            throw new FieldValueMappingException(
                    "Cannot create indexed field for property with null definition");
        }
        
        Field field = null;
        if (def.isMultiple()) {
            Value[] values = property.getValues();
            field = this.fieldValueMapper.getFieldFromValues(fieldName, values, lowercase);
        } else {
            field = this.fieldValueMapper.getFieldFromValue(fieldName, property.getValue(), lowercase);
        }
        
        return field;
    }

    /**
     * Creates a stored (binary) <code>Field</code> from a <code>Property</code>.
     * 
     */
    private Field[] getStoredFieldsFromProperty(Property property)
        throws FieldValueMappingException {
        
        String fieldName 
            = FieldNameMapping.getStoredFieldName(property);

        if (FieldNameMapping.isReservedField(fieldName)) {
            throw new DocumentMappingException("Property field name '" + fieldName 
                    + "' is a reserved index field.");
        }
        
        PropertyTypeDefinition def = property.getDefinition();
        if (def == null) {
            throw new FieldValueMappingException(
                    "Cannot create stored field for a property with null definition");
        }
        
        if (def.isMultiple()) {
                Value[] values = property.getValues();
                return this.fieldValueMapper.getBinaryFieldsFromValues(fieldName, values);
        }
        Field[] singleField = new Field[1];
        singleField[0] = this.fieldValueMapper.getBinaryFieldFromValue(fieldName, 
                property.getValue());
        return singleField;
    }

    public void setContextManager(ContextManager contextManager) {
        this.contextManager = contextManager;
    }

    @Required
    public void setResourceTypeTree(ResourceTypeTree resourceTypeTree) {
        this.resourceTypeTree = resourceTypeTree;
        // Populate map for fast lookup of stored index field name -> PropertyTypeDefinition
        for (PropertyTypeDefinition def: this.resourceTypeTree.getPropertyTypeDefinitions()) {
            this.fieldNamePropDefMap.put(FieldNameMapping.getStoredFieldName(def), def);
        }
    }
    
    @Required
    public void setFieldValueMapper(FieldValueMapper fieldValueMapper) {
        this.fieldValueMapper = fieldValueMapper;
    }

}

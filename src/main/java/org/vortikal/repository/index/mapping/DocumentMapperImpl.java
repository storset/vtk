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

import org.vortikal.repository.resourcetype.ResourceTypeDefinition;
import static org.vortikal.repository.resourcetype.PropertyType.Type.BINARY;
import static org.vortikal.repository.resourcetype.PropertyType.Type.JSON;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.document.FieldSelectorResult;
import org.apache.lucene.document.Fieldable;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySetImpl;
import org.vortikal.repository.ResourceTypeTree;
import org.vortikal.repository.resourcetype.PrimaryResourceTypeDefinition;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.repository.search.PropertySelect;
import org.vortikal.repository.search.WildcardPropertySelect;
import org.vortikal.resourcemanagement.JSONPropertyAttributeDescription;
import org.vortikal.security.Principal;
import org.vortikal.security.PrincipalFactory;
import org.vortikal.text.JSONUtil;

/**
 * Simple mapping from Lucene {@link org.apache.lucene.document.Document} to
 * {@link org.vortikal.repository.PropertySet} objects and vice-versa.
 * 
 * XXX: doc
 * 
 */
public class DocumentMapperImpl implements DocumentMapper, InitializingBean {

    private final Log logger = LogFactory.getLog(DocumentMapperImpl.class);

    private ResourceTypeTree resourceTypeTree;
    private FieldValueMapper fieldValueMapper;

    // Fast lookup maps for flat list of resource type prop defs and
    // stored field-name to prop-def map
    Map<String, PropertyTypeDefinition> storedFieldNamePropDefMap = new HashMap<String, PropertyTypeDefinition>();

    @Override
    public void afterPropertiesSet() {
        populateTypeInfoCacheMaps(this.storedFieldNamePropDefMap, this.resourceTypeTree.getRoot());
    }

    private void populateTypeInfoCacheMaps(Map<String, PropertyTypeDefinition> storedFieldPropDefMap, 
            PrimaryResourceTypeDefinition rtDef) {

        // Prop-defs from mixins are included here.
        List<PropertyTypeDefinition> propDefs
                = this.resourceTypeTree.getPropertyTypeDefinitionsIncludingAncestors(rtDef);

        for (PropertyTypeDefinition propDef : propDefs) {
            String fieldName = FieldNameMapping.getStoredFieldName(propDef);
            if (!storedFieldPropDefMap.containsKey(fieldName)) {
                storedFieldPropDefMap.put(fieldName, propDef);
            }
        }

        for (PrimaryResourceTypeDefinition child : this.resourceTypeTree.getResourceTypeDefinitionChildren(rtDef)) {
            populateTypeInfoCacheMaps(storedFieldPropDefMap, child);
        }

    }

    /**
     * Map <code>PropertySetImpl</code> to Lucene <code>Document</code>. Used
     * when indexing.
     */
    @Override
    public Document getDocument(PropertySetImpl propSet, Set<Principal> aclReadPrincipals)
            throws DocumentMappingException {
        Document doc = new Document();

        // Special fields
        // uri
        Field uriField = this.fieldValueMapper.getStoredKeywordField(FieldNameMapping.URI_FIELD_NAME, propSet.getURI()
                .toString());
        doc.add(uriField);

        // uriDepth (not stored, but indexed for use in searches)
        int uriDepth = propSet.getURI().getDepth();
        Field uriDepthField = this.fieldValueMapper.getKeywordField(FieldNameMapping.URI_DEPTH_FIELD_NAME, uriDepth);
        doc.add(uriDepthField);

        // name
        Field nameField = this.fieldValueMapper.getStoredKeywordField(FieldNameMapping.NAME_FIELD_NAME, propSet
                .getName());
        doc.add(nameField);
        // name (lowercased, indexed, but not stored)
        Field nameFieldLc = this.fieldValueMapper.getKeywordField(FieldNameMapping.NAME_LC_FIELD_NAME, propSet
                .getName().toLowerCase());
        doc.add(nameFieldLc);

        // resourceType
        Field resourceTypeField = this.fieldValueMapper.getStoredKeywordField(FieldNameMapping.RESOURCETYPE_FIELD_NAME,
                propSet.getResourceType());
        doc.add(resourceTypeField);

        // ANCESTORIDS (index system field)
        Field ancestorIdsField = this.fieldValueMapper.getUnencodedMultiValueFieldFromIntegers(
                FieldNameMapping.ANCESTORIDS_FIELD_NAME, propSet.getAncestorIds());
        doc.add(ancestorIdsField);

        // ID (index system field)
        Field idField = this.fieldValueMapper.getKeywordField(FieldNameMapping.ID_FIELD_NAME, propSet.getID());
        doc.add(idField);
        Field storedIdField = this.fieldValueMapper.getStoredBinaryIntegerField(FieldNameMapping.STORED_ID_FIELD_NAME,
                propSet.getID());
        doc.add(storedIdField);

        // ACL_INHERITED_FROM (index system field)
        Field aclField = this.fieldValueMapper.getKeywordField(FieldNameMapping.ACL_INHERITED_FROM_FIELD_NAME, propSet
                .getAclInheritedFrom());
        doc.add(aclField);
        Field storedAclField = this.fieldValueMapper.getStoredBinaryIntegerField(
                FieldNameMapping.STORED_ACL_INHERITED_FROM_FIELD_NAME, propSet.getAclInheritedFrom());
        doc.add(storedAclField);

        // ACL_READ_PRINCIPALS (index system field)
        if (aclReadPrincipals != null) {
            // XXX: should probably fail harder if missing ACL read field data,
            // however, if missing
            // the document will never be allowed in query results (so really
            // not a problem).
            String[] qualifiedNames = getAclReadPrincipalsFieldValues(aclReadPrincipals);

            Field aclReadPrincipalsField = getAclReadPrincipalsField(qualifiedNames);
            doc.add(aclReadPrincipalsField);

            Field[] storedAclReadPrincipalsFields = getStoredAclReadPrincipalsFields(qualifiedNames);
            for (Field f : storedAclReadPrincipalsFields) {
                doc.add(f);
            }
        }

        final ResourceTypeDefinition rtDef = 
                this.resourceTypeTree.getResourceTypeDefinitionByName(propSet.getResourceType());
        if (rtDef == null) {
            logger.warn("Missing type information for resource type '" + propSet.getResourceType()
                    + "', cannot create complete index document.");
            return doc;
        }
        
        // Index only properties that satisfy the following conditions:
        // 1) Belongs to the resource type's definition
        // 2) Exists in the property set.
        // 3) Is of type that is sensible to index for searching.
        // Store *all* properties, except those of type BINARY.
        for (PropertyTypeDefinition propDef : this.resourceTypeTree.getPropertyTypeDefinitionsIncludingAncestors(rtDef)) {
            Property property = propSet.getProperty(propDef);

            if (property == null)
                continue;

            // Create indexed fields
            switch (property.getType()) {
            case BINARY:
                continue; // Don't index or store BINARY property values

            case JSON:
                // Add any indexable JSON value attributes (both as lowercase
                // and regular)
                for (Field jsonAttrField : getIndexedFieldsFromJSONProperty(property, false)) {
                    doc.add(jsonAttrField);
                }
                for (Field jsonAttrFieldLc : getIndexedFieldsFromJSONProperty(property, true)) {
                    doc.add(jsonAttrFieldLc);
                }
                break;

            case STRING:
            case HTML:
                // Add lowercase version of search field for STRING and HTML
                // types
                Field lowercaseIndexedField = getIndexedFieldFromProperty(property, true);
                doc.add(lowercaseIndexedField);

            default:
                // Create regular searchable index field of value(s)
                Field indexedField = getIndexedFieldFromProperty(property, false);
                doc.add(indexedField);
            }

            // Create stored field-value(s) for all types except BINARY
            if (property.getType() != BINARY) {
                for (Field storedField : getStoredFieldsFromProperty(property)) {
                    doc.add(storedField);
                }
            }

        }

        return doc;
    }

    @Override
    public FieldSelector getDocumentFieldSelector(final PropertySelect select) {

        FieldSelector selector = null;
        if (select == WildcardPropertySelect.WILDCARD_PROPERTY_SELECT) {
            selector = new FieldSelector() {
                private static final long serialVersionUID = -8502087584408029619L;

                @Override
                public FieldSelectorResult accept(String fieldName) {
                    return FieldSelectorResult.LOAD;
                }

            };
        } else {
            selector = new FieldSelector() {
                private static final long serialVersionUID = 3919989917870796613L;

                @Override
                public FieldSelectorResult accept(String fieldName) {
                    PropertyTypeDefinition def = DocumentMapperImpl.this.storedFieldNamePropDefMap.get(fieldName);

                    if (def == null) {
                        // Reserved field, load it
                        return FieldSelectorResult.LOAD;
                    }

                    if (!select.isIncludedProperty(def)) {
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
     * Map from Lucene <code>Document</code> instance to a repository
     * <code>PropertySetImpl</code> instance.
     * 
     * Used when searching.
     * 
     * This method is heavily used when generating query results and is critical
     * for general query performance. Emphasis should be placed on optimizing it
     * as much as possible. Preferably use
     * {@link #loadDocumentWithFieldSelection(IndexReader, int, PropertySelect)}
     * to load the document before passing it to this method. Only loaded fields
     * will be mapped to properties.
     * 
     * @param doc
     *            The {@link org.apache.lucene.document.Document} to map from
     * @param select
     *            A {@link PropertySelect} instance determining which properties
     *            that should be mapped.
     * 
     * @return
     * @throws DocumentMappingException
     */
    @SuppressWarnings("unchecked")
    @Override
    public PropertySetImpl getPropertySet(Document doc) throws DocumentMappingException {

        PropertySetImpl propSet = new PropertySetImpl();
        propSet.setUri(Path.fromString(doc.get(FieldNameMapping.URI_FIELD_NAME)));
        propSet.setAclInheritedFrom(this.fieldValueMapper.getIntegerFromStoredBinaryField(doc
                .getField(FieldNameMapping.STORED_ACL_INHERITED_FROM_FIELD_NAME)));
        propSet.setID(this.fieldValueMapper.getIntegerFromStoredBinaryField(doc
                .getField(FieldNameMapping.STORED_ID_FIELD_NAME)));
        propSet.setResourceType(doc.get(FieldNameMapping.RESOURCETYPE_FIELD_NAME));

        // Loop through all stored binary fields and re-create properties with
        // values. Multi-valued properties are stored as a
        // _sequence_of_binary_fields_
        // (with the same name) in the index.
        // Note that the iteration will _only_ contain _stored_ fields.
        String currentName = null;
        List<Fieldable> fields = new ArrayList<Fieldable>();
        for (Fieldable field: doc.getFields()) {

            String fieldName = field.name();

            if (FieldNameMapping.isReservedField(fieldName))
                continue;

            if (currentName == null) {
                currentName = fieldName;
            }

            if (fieldName == currentName) { // Interned string comparison
                // Add field for current property
                fields.add(field);
            } else {
                // We have accumulated all fields necessary to create next
                // property
                // (fields retrieved from Lucene document are properly ordered)
                Property prop = getPropertyFromStoredFieldValues(currentName, fields);
                propSet.addProperty(prop);

                // Prepare for next property
                fields.clear();
                currentName = fieldName;
                fields.add(field);
            }
        }

        // Make sure we don't forget the last field
        if (currentName != null) {
            Property prop = getPropertyFromStoredFieldValues(currentName, fields);
            propSet.addProperty(prop);
        }

        return propSet;
    }

    /**
     * Re-create a <code>Property</code> from index fields.
     * 
     * @param fields
     * @return
     * @throws FieldValueMappingException
     */
    private Property getPropertyFromStoredFieldValues(String fieldName, List<Fieldable> storedValueFields)
            throws FieldValueMappingException {

        PropertyTypeDefinition def = this.storedFieldNamePropDefMap.get(fieldName);

        if (def == null) {
            // No definition found, make it a String type and log a warning
            String name = FieldNameMapping.getPropertyNameFromStoredFieldName(fieldName);
            String nsPrefix = FieldNameMapping.getPropertyNamespacePrefixFromStoredFieldName(fieldName);

            def = this.resourceTypeTree.getPropertyTypeDefinition(Namespace.getNamespaceFromPrefix(nsPrefix), name);

            logger.warn("Definition for property '" + nsPrefix + FieldNameMapping.FIELD_NAMESPACEPREFIX_NAME_SEPARATOR
                    + name + "' not found by " + " property manager. Config might have been updated without "
                    + " updating index(es)");

        }

        Property property = def.createProperty();

        if (def.isMultiple()) {
            Value[] values = this.fieldValueMapper.getValuesFromStoredBinaryFields(storedValueFields, def.getType());

            property.setValues(values);
        } else {
            if (storedValueFields.size() != 1) {
                // Fail hard if multiple stored fields found for single
                // value property
                throw new FieldValueMappingException("Single value property '" + def.getNamespace().getPrefix()
                        + FieldNameMapping.FIELD_NAMESPACEPREFIX_NAME_SEPARATOR + def.getName()
                        + "' has an invalid number of stored values (" + storedValueFields.size() + ") in index.");
            }

            Value value = this.fieldValueMapper.getValueFromStoredBinaryField(storedValueFields.get(0), def.getType());

            property.setValue(value);
        }

        return property;
    }

    /**
     * Creates an indexable <code>Field</code> from a <code>Property</code>.
     * 
     */
    private Field getIndexedFieldFromProperty(Property property, boolean lowercase) throws FieldValueMappingException {

        PropertyTypeDefinition def = property.getDefinition();
        if (def == null) {
            throw new DocumentMappingException("Cannot create indexed field for property with null definition");
        }

        String fieldName = FieldNameMapping.getSearchFieldName(def, lowercase);
        if (FieldNameMapping.isReservedField(fieldName)) {
            throw new DocumentMappingException(
                    "Property type definition has name which collides with reserved index field:" + fieldName);
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

    @SuppressWarnings("unchecked")
    private Field[] getIndexedFieldsFromJSONProperty(Property prop, boolean lowercase) {

        PropertyTypeDefinition def = prop.getDefinition();
        if (def == null || def.getType() != JSON) {
            throw new DocumentMappingException(
                    "Cannot create indexed JSON fields for property with no definition or non-JSON type");
        }

        List<JSONPropertyAttributeDescription> indexableJsonAttributes = def.getIndexableAttributes();
        if (indexableJsonAttributes == null || indexableJsonAttributes.isEmpty()) {
            return new Field[0]; // Nothing to index for this JSON prop
        }

        List<Field> fields = new ArrayList<Field>();
        Value[] jsonPropValues = null;
        if (def.isMultiple()) {
            jsonPropValues = prop.getValues();
        } else {
            jsonPropValues = new Value[1];
            jsonPropValues[0] = prop.getValue();
        }

        try {
            for (JSONPropertyAttributeDescription jsonAttribute : indexableJsonAttributes) {
                
                String jsonAttributeName = jsonAttribute.getName();
                
                List<Object> fieldValues = new ArrayList<Object>();

                // Gather up values for current field
                for (Value jsonValue : jsonPropValues) {
                    JSONObject json = JSONObject.fromObject(jsonValue.getObjectValue());
                    Object selection = JSONUtil.select(json, jsonAttributeName);
                    // Selection can be of type JSONArray, in which case we
                    // index it as multi-value
                    if (selection instanceof JSONArray) {
                        for (Iterator iter = ((JSONArray) selection).iterator(); iter.hasNext();) {
                            Object nextVal = iter.next();
                            if (nextVal != null) {
                                fieldValues.add(nextVal);
                            }
                        }
                    } else if (selection != null) {
                        fieldValues.add(selection);
                    }
                }

                String fieldName = FieldNameMapping.getJSONSearchFieldName(def, jsonAttributeName, lowercase);
                Field field = this.fieldValueMapper.getUnencodedMultiValueFieldfFromObjects(fieldName, fieldValues
                        .toArray(), lowercase);
                fields.add(field);
            }
        } catch (JSONException je) {
            logger.warn("JSON property " + prop + " has a value containing invalid JSON data: " + je.getMessage()
                    + ", will not index any of this property's JSON values.");
            // Don't index any JSON attributes of this prop, since some data
            // apparently is broken.
            return new Field[0];
        }

        return fields.toArray(new Field[fields.size()]);
    }

    /**
     * Creates a stored (binary) <code>Field</code> array from a
     * <code>Property</code>.
     */
    private Field[] getStoredFieldsFromProperty(Property property) throws FieldValueMappingException {

        PropertyTypeDefinition def = property.getDefinition();
        if (def == null) {
            throw new DocumentMappingException("Cannot create stored field for a property with null definition");
        }

        String fieldName = FieldNameMapping.getStoredFieldName(def);

        if (def.isMultiple()) {
            Value[] values = property.getValues();
            return this.fieldValueMapper.getStoredBinaryFieldsFromValues(fieldName, values);
        }
        Field[] singleField = new Field[1];
        singleField[0] = this.fieldValueMapper.getStoredBinaryFieldFromValue(fieldName, property.getValue());
        return singleField;
    }

    @Override
    public Set<String> getACLReadPrincipalNames(Document doc) throws DocumentMappingException {

        Field[] fields = doc.getFields(FieldNameMapping.STORED_ACL_READ_PRINCIPALS_FIELD_NAME);

        if (fields.length == 0) {
            throw new DocumentMappingException("The field " + FieldNameMapping.STORED_ACL_READ_PRINCIPALS_FIELD_NAME
                    + " does not exist or is not loaded for document: " + doc);
        }

        Set<String> retval = new HashSet<String>(
                  Arrays.asList(this.fieldValueMapper.getStringsFromStoredBinaryFields(fields)));

        return retval;
    }

    private String[] getAclReadPrincipalsFieldValues(Set<Principal> aclReadPrincipals) {
        String[] qualifiedNames;

        if (aclReadPrincipals.contains(PrincipalFactory.ALL)) {
            // Read-for-all, only index this field
            qualifiedNames = new String[] { PrincipalFactory.NAME_ALL };
        } else {
            qualifiedNames = new String[aclReadPrincipals.size()];
            int i = 0;
            for (Principal principal : aclReadPrincipals) {
                qualifiedNames[i++] = principal.getQualifiedName();
            }
        }

        return qualifiedNames;
    }

    private Field getAclReadPrincipalsField(String[] qualifiedNames) {
        return this.fieldValueMapper.getUnencodedMultiValueFieldFromStrings(
                FieldNameMapping.ACL_READ_PRINCIPALS_FIELD_NAME, qualifiedNames);
    }

    private Field[] getStoredAclReadPrincipalsFields(String[] qualifiedNames) {
        return this.fieldValueMapper.getStoredBinaryFieldsFromStrings(
                FieldNameMapping.STORED_ACL_READ_PRINCIPALS_FIELD_NAME, qualifiedNames);
    }

    @Required
    public void setResourceTypeTree(ResourceTypeTree resourceTypeTree) {
        this.resourceTypeTree = resourceTypeTree;
    }

    @Required
    public void setFieldValueMapper(FieldValueMapper fieldValueMapper) {
        this.fieldValueMapper = fieldValueMapper;
    }

}
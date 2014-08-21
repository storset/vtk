/* Copyright (c) 2006,2012 University of Oslo, Norway
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

import java.io.IOException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.PropertySetImpl;
import org.vortikal.repository.ResourceTypeTree;
import org.vortikal.repository.resourcetype.PrimaryResourceTypeDefinition;
import org.vortikal.repository.resourcetype.PropertyType.Type;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.ResourceTypeDefinition;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.repository.search.PropertySelect;
import org.vortikal.security.Principal;
import org.vortikal.security.PrincipalFactory;

import static org.vortikal.repository.index.mapping.FieldValues.FieldSpec.INDEXED;
import static org.vortikal.repository.index.mapping.FieldValues.FieldSpec.STORED;
import static org.vortikal.repository.index.mapping.FieldValues.FieldSpec.INDEXED_STORED;
import static org.vortikal.repository.index.mapping.FieldValues.FieldSpec.INDEXED_LOWERCASE;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.lucene.document.DocumentStoredFieldVisitor;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.StoredFieldVisitor;
import org.vortikal.repository.PropertyImpl;
import org.vortikal.repository.index.mapping.FieldValues.FieldSpec;

import static org.vortikal.repository.index.mapping.FieldNames.*;

/**
 * Mapping from Lucene {@link org.apache.lucene.document.Document} to
 * {@link org.vortikal.repository.PropertySet} objects and vice-versa.
 * 
 */
public class DocumentMapper implements InitializingBean {

    private final Log logger = LogFactory.getLog(DocumentMapper.class);

    private ResourceTypeTree resourceTypeTree;
    private FieldValues fvm;

    // Fast lookup maps for flat list of resource type prop defs and
    // stored field-name to prop-def map
    private Map<String, PropertyTypeDefinition> fieldNamePropDefMap 
                                  = new HashMap<String, PropertyTypeDefinition>();

    @Override
    public void afterPropertiesSet() {
        populateTypeInfoCacheMaps(fieldNamePropDefMap, resourceTypeTree.getRoot());
    }

    private void populateTypeInfoCacheMaps(Map<String, PropertyTypeDefinition> fieldPropDefMap, 
            PrimaryResourceTypeDefinition rtDef) {

        // Prop-defs from mixins are included here.
        List<PropertyTypeDefinition> propDefs
                = this.resourceTypeTree.getPropertyTypeDefinitionsIncludingAncestors(rtDef);

        for (PropertyTypeDefinition propDef : propDefs) {
            String fieldName = propertyFieldName(propDef);
            if (!fieldPropDefMap.containsKey(fieldName)) {
                fieldPropDefMap.put(fieldName, propDef);
            }
        }

        for (PrimaryResourceTypeDefinition child : this.resourceTypeTree.getResourceTypeDefinitionChildren(rtDef)) {
            populateTypeInfoCacheMaps(fieldPropDefMap, child);
        }
    }

    /**
     * Map <code>PropertySetImpl</code> to Lucene <code>Document</code>. Used
     * at indexing time.
     * 
     * @param propSet the property set to create an index document of.
     * @param aclReadPrincipals set of prinipals which are allowed to read the
     * resource (derived from the ACL).
     * 
     * @return a <code>Document</code> with index fields corresponding to the
     * property set metadata and properties.
     */
    public Document getDocument(PropertySetImpl propSet, Set<Principal> aclReadPrincipals)
            throws DocumentMappingException {
        final List<Field> fields = new ArrayList<Field>();

        // Special fields
        // uri
        fields.addAll(fvm.makeFields(URI_FIELD_NAME, INDEXED_STORED, 
                Type.STRING, propSet.getURI().toString()));
        fields.add(fvm.makeSortField(URI_SORT_FIELD_NAME, propSet.getURI().toString()));
        
        // URI depth (not stored, but indexed for use in searches)
        int uriDepth = propSet.getURI().getDepth();
        fields.addAll(fvm.makeFields(URI_DEPTH_FIELD_NAME, INDEXED, Type.INT, uriDepth));
        
        // Ancestor URIs (system field used for hierarchical queries)
        fields.addAll(fvm.makeFields(URI_ANCESTORS_FIELD_NAME,
                INDEXED, Type.STRING, (Object[])getPathAncestorStrings(propSet.getURI())));
        
        // URI name
        fields.addAll(fvm.makeFields(NAME_FIELD_NAME, INDEXED, Type.STRING, propSet.getName()));
        fields.addAll(fvm.makeFields(NAME_LC_FIELD_NAME, INDEXED_LOWERCASE, Type.STRING, propSet.getName()));
        fields.add(fvm.makeSortField(NAME_SORT_FIELD_NAME, propSet.getName()));

        // resourceType, stored and indexed
        fields.addAll(fvm.makeFields(RESOURCETYPE_FIELD_NAME, INDEXED_STORED, Type.STRING, 
                propSet.getResourceType()));

        // ID (system field, stored and indexed, but only as a string type)
        fields.addAll(fvm.makeFields(ID_FIELD_NAME, INDEXED_STORED, 
                Type.STRING, Integer.toString(propSet.getID())));
        
        
        // ACL_INHERITED_FROM (index system field, stored and indexed, but only as a string)
        fields.addAll(fvm.makeFields(ACL_INHERITED_FROM_FIELD_NAME, INDEXED_STORED, 
                Type.STRING, Integer.toString(propSet.getAclInheritedFrom())));

        // ACL_READ_PRINCIPALS (index system field)
        if (aclReadPrincipals != null) {
            String[] qualifiedNames = getAclReadPrincipalsFieldValues(aclReadPrincipals);
            fields.addAll(fvm.makeFields(ACL_READ_PRINCIPALS_FIELD_NAME, INDEXED_STORED, 
                    Type.STRING, (Object[])qualifiedNames));
        }

        final ResourceTypeDefinition rtDef = 
                this.resourceTypeTree.getResourceTypeDefinitionByName(propSet.getResourceType());
        if (rtDef == null) {
            logger.warn("Missing type information for resource type '" + propSet.getResourceType()
                    + "', cannot create complete index document.");
            Document doc = new Document();
            for (Field f: fields) {
                doc.add(f);
            }
            return doc;
        }
        
        List<PropertyTypeDefinition> rtPropDefs =
                this.resourceTypeTree.getPropertyTypeDefinitionsIncludingAncestors(rtDef);
        for (Property property: propSet) {
            // Completely ignore any Property without a definition
            if (property.getDefinition() == null) continue;
            
            // Resolve canonical prop-def instance
            String propFieldName = propertyFieldName(property.getDefinition());
            PropertyTypeDefinition canonicalDef = this.fieldNamePropDefMap.get(propFieldName);

            // Skip all props not part of type and that are not inheritable
            if (!rtPropDefs.contains(canonicalDef) && !property.getDefinition().isInheritable()) {
                continue;
            }

            // Create indexed fields
            switch (property.getType()) {
            case BINARY:
                break; // Don't store any binary property value types

            case JSON:
                // Add any indexable JSON value attributes (both as lowercase
                // and regular), sorting field(s) and stored field(s)
                fields.addAll(getJSONPropertyFields(property));
                break;

            case STRING:
                if (!canonicalDef.isMultiple()) {
                    fields.add(getPropertySortField(property));
                }
            
            case HTML:
                // Add lowercase version of search field for STRING and HTML
                // types
                fields.addAll(getPropertyFields(property, true));

            default:
                // Create searchable and stored index fields of value(s)
                fields.addAll(getPropertyFields(property, false));
            }
        }

        Document doc = new Document();
        for (Field f: fields) {
            doc.add(f);
        }
        return doc;
    }

    /**
     * Obtain a {@link StoredFieldVisitor} from a {@link PropertySelect}. Can
     * be used for selective field loading.
     * 
     * @param select a <code>PropertySelect</code> specifying the desired properties
     * to load from index documents.
     * 
     * @return a <code>StoredFieldVisitor</code> to be used when loading documents
     * from Lucene.
     */
    public DocumentStoredFieldVisitor newStoredFieldVisitor(final PropertySelect select) {
        if (select == PropertySelect.ALL || select == null) {
            return new DocumentStoredFieldVisitor();
        } else if (select == PropertySelect.NONE) {
            return new DocumentStoredFieldVisitor() {
                boolean haveUri = false, haveResourceType = false;
                @Override
                public StoredFieldVisitor.Status needsField(FieldInfo fieldInfo) throws IOException {
                    if (URI_FIELD_NAME.equals(fieldInfo.name)) {
                        haveUri = true;
                        return StoredFieldVisitor.Status.YES;
                    }
                    if (RESOURCETYPE_FIELD_NAME.equals(fieldInfo.name)) {
                        haveResourceType = true;
                        return StoredFieldVisitor.Status.YES;
                    }
                    
                    if (haveResourceType && haveUri) {
                        return StoredFieldVisitor.Status.STOP;
                    }
                    
                    return StoredFieldVisitor.Status.NO;
                }
            };
        } else {
            return new DocumentStoredFieldVisitor() {
                @Override
                public StoredFieldVisitor.Status needsField(FieldInfo fieldInfo) throws IOException {
                    PropertyTypeDefinition def = fieldNamePropDefMap.get(fieldInfo.name);
                    if (def != null) {
                        if (select.isIncludedProperty(def)) {
                            return StoredFieldVisitor.Status.YES;
                        } else {
                            return StoredFieldVisitor.Status.NO;
                        }
                    }
                    // Check for required reserved fields
                    if (URI_FIELD_NAME.equals(fieldInfo.name)
                            || RESOURCETYPE_FIELD_NAME.equals(fieldInfo.name)) {
                        return StoredFieldVisitor.Status.YES;
                    }
                    
                    // Skip others
                    return StoredFieldVisitor.Status.NO;
                }
            };
        }
    }
    
    /**
     * Map from Lucene <code>Document</code> instance to a repository
     * <code>PropertySetImpl</code> instance.
     * 
     * @param doc The {@link org.apache.lucene.document.Document} to map from.
     * Only loaded fields will be mapped to available properties.
     * 
     * @return a <code>PropertySet</code> with containing properties for all
     * loaded property fields present in the document.
     * 
     * @throws DocumentMappingException in case of errors mapping from document.
     */
    @SuppressWarnings("unchecked")
    public PropertySet getPropertySet(Document doc) throws DocumentMappingException {
        return new LazyMappedPropertySet(doc, this);
    }
    
    Property getPropertyFromFieldValues(String fieldName, List<IndexableField> fieldValues)
            throws DocumentMappingException {
        
        PropertyTypeDefinition def = this.fieldNamePropDefMap.get(fieldName);

        if (def == null) {
            // No definition found, make it a String type and log a warning
            String name = propertyName(fieldName);
            String nsPrefix = propertyNamespace(fieldName);

            def = this.resourceTypeTree.getPropertyTypeDefinition(Namespace.getNamespaceFromPrefix(nsPrefix), name);

            logger.warn("Definition for property '" 
                    + nsPrefix + NAMESPACEPREFIX_NAME_SEPARATOR
                    + name + "' not found by " + " property manager.");
        }
        
        PropertyImpl property = new PropertyImpl();
        property.setDefinition(def);
        
        if (def.isMultiple()) {
            Value[] values = fvm.valuesFromFields(def.getType(), fieldValues);
            property.setValues(values, false);
        } else {
            if (fieldValues.size() != 1) {
                logger.warn("Single value property '" + def.getNamespace().getPrefix()
                        + NAMESPACEPREFIX_NAME_SEPARATOR + def.getName()
                        + "' has an invalid number of stored values (" + fieldValues.size() + ") in index");
            }

            Value value = fvm.valueFromField(def.getType(), fieldValues.get(0));
            property.setValue(value, false);
        }

        return property;
    }
    
    private Field getPropertySortField(Property property) {
        if (property.getDefinition().getType() != Type.STRING) {
            throw new IllegalArgumentException("Sorting fields can only be created for STRING properties");
        }
        if (property.getDefinition().isMultiple()) {
            throw new IllegalArgumentException("Sorting fields cannot be created for multi-value properties");
        }
        String fieldName = sortFieldName(property.getDefinition());
        return fvm.makeSortField(fieldName, property.getStringValue());
    }
    
    /**
     * Create a list of <code>Field</code> from a <code>Property</code>.
     * 
     * @param lowercase if <code>true</code>, then lowercase the value as apporpriate.
     * <em>When lowercased, fields for storing will not be created.</em>
     */
    private List<Field> getPropertyFields(Property property, boolean lowercase) 
            throws DocumentMappingException {

        PropertyTypeDefinition def = property.getDefinition();
        if (def == null) {
            throw new DocumentMappingException("Cannot create indexed field for property with null definition");
        }

        String fieldName = propertyFieldName(def, lowercase);
        FieldSpec spec = lowercase ? INDEXED_LOWERCASE : INDEXED_STORED;
        if (def.isMultiple()) {
            Value[] values = property.getValues();
            return fvm.makeFields(fieldName, spec, values);
        } else {
            return fvm.makeFields(fieldName, spec, property.getValue());
        }
    }

    /**
     * Returns all fields for a given JSON property, both lowercased
     * and regular variants, and a stored field for the raw JSON string value.
     * @param prop
     * @return 
     */
    @SuppressWarnings("unchecked")
    private List<Field> getJSONPropertyFields(Property prop) {

        PropertyTypeDefinition def = prop.getDefinition();
        if (def == null || def.getType() != Type.JSON) {
            throw new DocumentMappingException(
                    "Cannot create indexed JSON fields for property with no definition or non-JSON type");
        }
        
        final List<Field> fields = new ArrayList<Field>();
        Value[] jsonPropValues;
        if (def.isMultiple()) {
            jsonPropValues = prop.getValues();
        } else {
            jsonPropValues = new Value[1];
            jsonPropValues[0] = prop.getValue();
        }
        
        // Create stored-only fields
        fields.addAll(fvm.makeFields(propertyFieldName(def), STORED, jsonPropValues));

        Map<String, Object> metadata = def.getMetadata();
        if (! metadata.containsKey(PropertyTypeDefinition.METADATA_INDEXABLE_JSON)) {
            // No indexing hint for this JSON property, return only stored fields.
            return fields;
        }
        
        // Create possible sub-fields for values within JSON structure
        try {
            final List<Object> indexFieldValues = new ArrayList<Object>();
            for (Value jsonValue : jsonPropValues) {
                JSONObject json = jsonValue.getJSONValue();
                for (Iterator it = json.entrySet().iterator(); it.hasNext();) {
                    indexFieldValues.clear();
                    Map.Entry entry = (Map.Entry) it.next();
                    final String jsonAttribute = (String)entry.getKey();
                    final Object value = entry.getValue();
                    if (value == null) continue;
                    
                    // Only index primitives and arrays of primitives, exactly ONE level deep.
                    if (value.getClass() == JSONArray.class) {
                        for (Iterator iter = ((JSONArray) value).iterator(); iter.hasNext();) {
                            Object nextVal = iter.next();
                            if (nextVal != null 
                                    && nextVal.getClass() != JSONObject.class
                                    && nextVal.getClass() != JSONArray.class) {
                                indexFieldValues.add(nextVal);
                            }
                        }
                    } else if (value.getClass() != JSONObject.class) {
                        indexFieldValues.add(value);
                    }
                    // Set up type for searchable JSON field
                    Type dataType = FieldValues.getJsonFieldDataType(def, jsonAttribute);
                    
                    // Create regular searchable field for all values
                    String fieldName = jsonFieldName(def, jsonAttribute, false);
                    fields.addAll(fvm.makeFields(fieldName, INDEXED, dataType, indexFieldValues.toArray()));
                    
                    // Lowercased searchable field for all values with type hint STRING or HTML
                    if (dataType == Type.STRING || dataType == Type.HTML) {
                        fieldName = jsonFieldName(def, jsonAttribute, true);
                        fields.addAll(fvm.makeFields(
                                fieldName, INDEXED_LOWERCASE, dataType, indexFieldValues.toArray()));
                    }
                    
                    // Sort field for all single-values with type STRING
                    if (dataType == Type.STRING && indexFieldValues.size() == 1) {
                        fieldName = jsonSortFieldName(def, jsonAttribute);
                        fields.add(fvm.makeSortField(fieldName, indexFieldValues.get(0).toString()));
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("JSON property " 
                    + prop + " has a value(s) containing invalid or non-indexable JSON data: " 
                    + e.getMessage());
        }

        return fields;
    }

    private String[] getPathAncestorStrings(Path path) {
        List<Path> ancestors = path.getAncestors();
        String[] ancestorStrings = new String[ancestors.size()];
        for (int i=0; i<ancestorStrings.length; i++) {
            ancestorStrings[i] = ancestors.get(i).toString();
        }
        return ancestorStrings;
    }

    public Set<String> getACLReadPrincipalNames(Document doc) throws DocumentMappingException {

        String[] values = doc.getValues(ACL_READ_PRINCIPALS_FIELD_NAME);
        if (values.length == 0) {
            throw new DocumentMappingException("The field " + ACL_READ_PRINCIPALS_FIELD_NAME
                    + " does not exist or is not loaded for document: " + doc);
        }

        return new HashSet<String>(Arrays.asList(values));
    }
    
    public int getResourceId(Document doc) throws DocumentMappingException {
        String id = doc.get(ID_FIELD_NAME);
        if (id == null) {
            throw new DocumentMappingException("Document is missing field " + ID_FIELD_NAME);
        }
        try {
            return Integer.parseInt(id);
        } catch (NumberFormatException nfe) {
            throw new DocumentMappingException("Illegal stored value for field " + ID_FIELD_NAME);
        }
    }
    
    public int getAclInheritedFrom(Document doc) throws DocumentMappingException {
        
        String id = doc.get(ACL_INHERITED_FROM_FIELD_NAME);
        if (id == null) {
            throw new DocumentMappingException("Document is missing field " 
                    + ACL_INHERITED_FROM_FIELD_NAME);
        }
        try {
            return Integer.parseInt(id);
        } catch (NumberFormatException nfe) {
            throw new DocumentMappingException("Illegal stored value for field "
                    + ACL_INHERITED_FROM_FIELD_NAME);
        }
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

    @Required
    public void setResourceTypeTree(ResourceTypeTree resourceTypeTree) {
        this.resourceTypeTree = resourceTypeTree;
    }

    @Required
    public void setFieldValueMapper(FieldValues fieldValueMapper) {
        this.fvm = fieldValueMapper;
    }

}
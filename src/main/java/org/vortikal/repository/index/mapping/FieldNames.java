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

import java.util.HashSet;
import java.util.Set;

import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;

/**
 * 
 * @author oyviste
 */
public final class FieldNames {

    /* Special field characters and prefixes */
    public static final String FIELD_NAMESPACEPREFIX_NAME_SEPARATOR = ":";
    public static final String JSON_ATTRIBUTE_SEPARATOR = "@";
    public static final String LOWERCASE_FIELD_PREFIX = "l_";
    public static final String STORED_BINARY_FIELD_PREFIX = "b_";
    
    /* Special, reserved fields */
    public static final String NAME_FIELD_NAME =         PropertySet.NAME_IDENTIFIER;
    public static final String NAME_LC_FIELD_NAME =      LOWERCASE_FIELD_PREFIX + NAME_FIELD_NAME;
    public static final String URI_FIELD_NAME =          PropertySet.URI_IDENTIFIER;
    public static final String URI_DEPTH_FIELD_NAME =    "uriDepth";
    public static final String URI_ANCESTORS_FIELD_NAME = "uriAncestors";
    public static final String RESOURCETYPE_FIELD_NAME = "resourceType";
    public static final String ID_FIELD_NAME =           "ID";
    public static final String ACL_INHERITED_FROM_FIELD_NAME = "ACL_INHERITED_FROM";
    public static final String ACL_READ_PRINCIPALS_FIELD_NAME = "ACL_READ_PRINCIPALS";
    
    public static final String STORED_ACL_READ_PRINCIPALS_FIELD_NAME = 
        STORED_BINARY_FIELD_PREFIX + ACL_READ_PRINCIPALS_FIELD_NAME;
    
    public static final String STORED_ID_FIELD_NAME = 
        STORED_BINARY_FIELD_PREFIX + ID_FIELD_NAME;
    
    public static final String STORED_ACL_INHERITED_FROM_FIELD_NAME = 
        STORED_BINARY_FIELD_PREFIX + "ACL_INHERITED_FROM";
    
    private static final Set<String> RESERVED_FIELD_NAMES = new HashSet<String>();

    static {
        RESERVED_FIELD_NAMES.add(NAME_FIELD_NAME);
        RESERVED_FIELD_NAMES.add(URI_FIELD_NAME);
        RESERVED_FIELD_NAMES.add(URI_ANCESTORS_FIELD_NAME);
        RESERVED_FIELD_NAMES.add(URI_DEPTH_FIELD_NAME);
        RESERVED_FIELD_NAMES.add(RESOURCETYPE_FIELD_NAME);
        RESERVED_FIELD_NAMES.add(ID_FIELD_NAME);
        RESERVED_FIELD_NAMES.add(STORED_ID_FIELD_NAME);
        RESERVED_FIELD_NAMES.add(ACL_READ_PRINCIPALS_FIELD_NAME);
        RESERVED_FIELD_NAMES.add(STORED_ACL_READ_PRINCIPALS_FIELD_NAME);
        RESERVED_FIELD_NAMES.add(STORED_ACL_INHERITED_FROM_FIELD_NAME);
    }
    
    public static boolean isReservedField(String fieldName) {
        return RESERVED_FIELD_NAMES.contains(fieldName);
    }
    
    public static String getSearchFieldName(Property prop, boolean lowercase) {
        return getSearchFieldName(prop.getDefinition(), lowercase);
    }
    
    public static String getSearchFieldName(PropertyTypeDefinition def, boolean lowercase) {
        switch (def.getType()) {
        case STRING:
        case HTML:
        case JSON:
            break;
        
        default:
            lowercase = false; // No lowercasing-support for type
        }

        return getSearchFieldName(def.getName(), def.getNamespace().getPrefix(), lowercase);
    }

    protected static String getSearchFieldName(String propName, String propPrefix, boolean lowercase) {
        StringBuilder fieldName = new StringBuilder();
        if (lowercase) {
            fieldName.append(LOWERCASE_FIELD_PREFIX);
        }
        
        if (propPrefix != null) {
            fieldName.append(propPrefix).append(FIELD_NAMESPACEPREFIX_NAME_SEPARATOR);
        }
        
        fieldName.append(propName);
        return fieldName.toString();
    }

    public static String getStoredFieldName(Property property) {
        StringBuilder fieldName = new StringBuilder(STORED_BINARY_FIELD_PREFIX);
        
        String name = property.getDefinition().getName();
        String nsPrefix = property.getDefinition().getNamespace().getPrefix();
        
        if (nsPrefix != null) {
            fieldName.append(nsPrefix);
            fieldName.append(FIELD_NAMESPACEPREFIX_NAME_SEPARATOR);
        }
        
        fieldName.append(name);
        
        return fieldName.toString();
    }
    
    public static String getJSONSearchFieldName(Property prop, String jsonAttrKey, boolean lowercase) {
        StringBuilder fieldName = new StringBuilder(getSearchFieldName(prop, lowercase));
        fieldName.append(JSON_ATTRIBUTE_SEPARATOR).append(jsonAttrKey);
        return fieldName.toString();
    }
    
    public static String getJsonSearchFieldName(PropertyTypeDefinition def, String jsonAttrKey, 
                                                                             boolean lowercase) {
        StringBuilder fieldName = new StringBuilder(getSearchFieldName(def, lowercase));
        fieldName.append(JSON_ATTRIBUTE_SEPARATOR).append(jsonAttrKey);
        return fieldName.toString();
    }
    
    protected static String getJSONSearchFieldName(String propName, String propPrefix, 
                                                        String jsonAttrKey, boolean lowercase) {
        StringBuilder fieldName = 
                        new StringBuilder(getSearchFieldName(propName, propPrefix, lowercase));
        fieldName.append(JSON_ATTRIBUTE_SEPARATOR).append(jsonAttrKey);
        return fieldName.toString();
    }

    public static String getStoredFieldName(PropertyTypeDefinition def) {
        StringBuilder name = new StringBuilder(STORED_BINARY_FIELD_PREFIX);
        String nsPrefix = def.getNamespace().getPrefix();
        if (nsPrefix != null) {
            name.append(nsPrefix);
            name.append(FIELD_NAMESPACEPREFIX_NAME_SEPARATOR);
        }
        name.append(def.getName());
        
        return name.toString();
    }

    public static String getPropertyNamespacePrefixFromStoredFieldName(String fieldName) {
        
        int sfpLength = STORED_BINARY_FIELD_PREFIX.length();
        int pos = fieldName.indexOf(FIELD_NAMESPACEPREFIX_NAME_SEPARATOR, sfpLength);

        if (pos == -1) {
            return null;
        } else {
            return fieldName.substring(sfpLength, pos);
        }
        
    }
    
    public static String getPropertyNameFromStoredFieldName(String fieldName){
        int sfpLength = STORED_BINARY_FIELD_PREFIX.length();
        int pos = fieldName.indexOf(FIELD_NAMESPACEPREFIX_NAME_SEPARATOR, sfpLength);

        if (pos == -1) {
            return fieldName.substring(sfpLength);
        } else {
            return fieldName.substring(pos + 1);
        }
    }
}

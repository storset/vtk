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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.DateField;
import org.apache.lucene.document.Field;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Property;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.repository.resourcetype.ValueFactory;
import org.vortikal.repository.resourcetype.ValueFormatException;
import org.vortikal.repositoryimpl.PropertyManagerImpl;

/**
 * Create {@link org.apache.lucene.document.Field} objects from 
 * {@org.vortikal.repository.Property} instances and vice-versa + misc
 * utility methods for mapping field-values.
 * 
 * @author oyviste
 */
public class FieldMapper implements InitializingBean {
    
    Log logger = LogFactory.getLog(FieldMapper.class);
    
    public static final String FIELD_NAMESPACEPREFIX_NAME_SEPARATOR = ":";

    public static final String FIELD_RESERVED_PREFIX = "_";
    
    public static final String FIELD_VALUE_LIST_SEPARATOR = ";";
    
    private ValueFactory valueFactory;
    private PropertyManagerImpl propertyManager;
    
    public FieldMapper() {}
    
    public void afterPropertiesSet() {
        if (valueFactory == null) {
            throw new BeanInitializationException("Property 'valueFactory' not set.");
        } else if (propertyManager == null) {
            throw new BeanInitializationException("Proeprty 'propertyManager' not set.");
        }
    }
    
    public Property getPropertyFromField(Field field) throws FieldMappingException {
        
        String[] f = field.name().split(FIELD_NAMESPACEPREFIX_NAME_SEPARATOR);
        String nsPrefix = null;
        String name = null;
        if (f.length == 1) {
            // Default namespace (no prefix)
            name = f[0];
        } else if (f.length == 2) {
            nsPrefix = f[0];
            name = f[1];
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
                property.setValues(getValuesFromField(field, def.getType()));
            } else {
                property.setValue(getValueFromField(field, def.getType()));
            }
        } else {
            property.setValue(getValueFromField(field, PropertyType.TYPE_STRING));
        }
        
        return property;
    }
    
    public Field getFieldFromProperty(Property property) throws FieldMappingException {
        StringBuffer fieldValue = new StringBuffer();
        Field field = null;
        String name = property.getName();
        String prefix = property.getNamespace().getPrefix();
        String fieldName = null;
        if (prefix == null) {
            fieldName = name;
        } else {
            fieldName = prefix + FIELD_NAMESPACEPREFIX_NAME_SEPARATOR + name;
        }
        
        PropertyTypeDefinition def = property.getDefinition();
        if (def != null) {
            int type = def.getType();
            if (def.isMultiple()) {
                Value[] values = property.getValues();
                for (int i=0; i<values.length; i++) {
                    
                    String encoded = 
                        encodeIndexFieldValue(values[i].getNativeStringRepresentation(), type);
                    
                    String escaped =
                        escapeIndexFieldValue(encoded);
                    
                    fieldValue.append(escaped);
                    if (i < values.length-1) {
                        fieldValue.append(FIELD_VALUE_LIST_SEPARATOR);
                    }
                    
                    // Tokenized field
                    field = new Field(fieldName, fieldValue.toString(), Field.Store.YES, 
                                                                Field.Index.TOKENIZED);
                }
            } else {
                Value value = property.getValue();
                
                fieldValue.append(
                        encodeIndexFieldValue(value.getNativeStringRepresentation(), type));
                
                field = new Field(fieldName, fieldValue.toString(), Field.Store.YES, 
                                         Field.Index.NO_NORMS);
            }
        } else {
            String encoded = encodeIndexFieldValue(property.getStringValue(), PropertyType.TYPE_STRING);
            fieldValue.append(encoded);
            field = new Field(fieldName, fieldValue.toString(), Field.Store.YES, 
                    Field.Index.NO_NORMS);
        }
        
        return field;
        
    }
    
    // No encoding (un-typed)
    public Field getKeywordField(String name, int value) {
        return getKeywordField(name, Integer.toString(value));
    }
    
    // No encoding (un-typed)
    public Field getKeywordField(String name, String value) {
        return new Field(name, value, Field.Store.YES, 
                Field.Index.NO_NORMS);
    }
    
    // No encoding (un-typed)
    // Creates a multi-value field that can be analyzed (split) by 
    // MultiValueFieldAnalyzer
    public Field getMultiValueKeywordField(String name, String[] values) {
        StringBuffer fieldValue = new StringBuffer();
        for (int i=0; i<values.length; i++) {
            fieldValue.append(escapeIndexFieldValue(values[i]));
            
            if (i < values.length-1) {
                fieldValue.append(FIELD_VALUE_LIST_SEPARATOR);
            }
        }
        
        return new Field(name, fieldValue.toString(), Field.Store.YES, Field.Index.TOKENIZED);
    }

    private Value getValueFromField(Field field, int type) {
        String fieldValue = field.stringValue();
        
        String decodedFieldValue = decodeIndexFieldValue(fieldValue, type);
        
        return valueFactory.createValue(decodedFieldValue, type);
    }
    
    private Value[] getValuesFromField(Field field, int type) {
        String fieldValue = field.stringValue();
        
        String[] stringValues = fieldValue.split("\\\\" + FIELD_VALUE_LIST_SEPARATOR);
        
        for (int i=0; i<stringValues.length; i++) {
            stringValues[i] = 
                decodeIndexFieldValue(unescapeIndexFieldValue(stringValues[i]), type);
        }
        
        return valueFactory.createValues(stringValues, type);
    }
    
    public String decodeIndexFieldValue(String fieldValue, int type) 
        throws ValueFormatException {
        
        switch (type) {
        
        case (PropertyType.TYPE_BOOLEAN):
        case (PropertyType.TYPE_STRING):
        case (PropertyType.TYPE_PRINCIPAL):

            return fieldValue; // Stored as-is in index

        case (PropertyType.TYPE_DATE):
            long time = DateField.stringToTime(fieldValue);
            return Long.toString(time);

            
        // XXX: sorting of negative integers does not work with this encoding
        // XXX: Possible solution is to shift scale to [0 - 2*Integer.MAX_VALUE], so
        //      that we avoid negative numbers (which cannot be compared directly
        //      lexicographically with other numbers, because "bigger" is in fact smaller
        //      on the negative side of the scale).        
        // Index representations: '+0000002005', '-0009999999', '+0000000000'
        case (PropertyType.TYPE_INT):
            int n;
            try {
                // Remove explicit '+' sign, if positive number
                if (fieldValue.startsWith("+")) {
                    fieldValue = fieldValue.substring(1, fieldValue.length());
                }
                // Remove any leading-zero-padding
                n = Integer.parseInt(fieldValue);
            } catch (NumberFormatException nfe) {
                throw new ValueFormatException("Unable to decode field value " 
                        + " to string representation", nfe);
            }
            return Integer.toString(n);
            
        case (PropertyType.TYPE_LONG):
            long l;
            try {
                // Remove explicit '+' sign, if positive number (breaks Integer.parseInt)
                if (fieldValue.startsWith("+")) {
                    fieldValue = fieldValue.substring(1, fieldValue.length());
                }
                // Remove any leading-zero-padding
                l = Long.parseLong(fieldValue);
            } catch (NumberFormatException nfe) {
                throw new ValueFormatException("Unable to decode field value " 
                        + " to string representation", nfe);
            }
            
            return Long.toString(l);
            
        default: throw new ValueFormatException("Unknown type " + type);
                
        }
    }
    
    /**
     * Encode a native value string representation into a form suitable for indexing
     * (make the various type representations lexicographically sortable, 
     * basically).
     * 
     * Only native string representations are supported by this method, and it
     * is indexing/Lucene-specific.
     * 
     */
    public String encodeIndexFieldValue(String stringValue, int type) 
        throws ValueFormatException {
        
        switch (type) {
        case(PropertyType.TYPE_STRING):
        case(PropertyType.TYPE_BOOLEAN):
        case(PropertyType.TYPE_PRINCIPAL):
            return stringValue;
        
        case(PropertyType.TYPE_DATE):
            try {
                long l = Long.parseLong(stringValue);
                return DateField.timeToString(l);
            } catch (NumberFormatException nfe) {
                throw new ValueFormatException("Unable to encode date string value to " 
                        + "to index field value representation: " + nfe.getMessage());
            }
            
        case(PropertyType.TYPE_INT):
            try {
                // Validate and pad
                int n = Integer.parseInt(stringValue);
                return intToZeroPaddedString(n);
            } catch (NumberFormatException nfe) {
                throw new ValueFormatException("Unable to encode integer string value to "
                        + "to index field value representation: " + nfe.getMessage());
            }
            
        case(PropertyType.TYPE_LONG):
            try {
                // Validate and pad
                long l = Long.parseLong(stringValue);
                return longToZeroPaddedString(l);
            } catch (NumberFormatException nfe) {
                throw new ValueFormatException(
                        "Unable to encode long integer string value to "
                                + "to index field value representation: "
                                + nfe.getMessage());
            }
            
        default: throw new ValueFormatException("Unknown type " + type);
        
        }

    }
    
    // XXX: sorting of negative long integers does not work with this encoding.
    
    public static String longToZeroPaddedString(long l) {
        // Zero-pad and add positive/negative sign.
        char[] encodedChars = "+00000000000000000000".toCharArray(); // Total length is 21
        char[] chars = Long.toString(l).toCharArray();

        if (l < 0) {
            encodedChars[0] = '-';
            System.arraycopy(chars, 1, encodedChars, 22 - chars.length,
                    chars.length - 1);
        } else {
            System.arraycopy(chars, 0, encodedChars, 21 - chars.length,
                    chars.length);
        }

        return String.valueOf(encodedChars);
        
    }
    
    // XXX: sorting of negative integers does not work with this encoding
    // XXX: Possible solution is to shift scale from 0 - 2*Integer.MAX_VALUE, so
    //      that we avoid negative numbers (which cannot be compared directly
    //      lexicographically with other numbers, because "bigger" is in fact smaller
    //      on the negative side of the scale).
    public static String intToZeroPaddedString(int i) {
        // Zero-pad and add positive/negative sign.
        char[] encodedChars = "+0000000000".toCharArray(); // Total length is 11
        char[] chars = Integer.toString(i).toCharArray();
            
        if (i < 0) {
            encodedChars[0] = '-';
            System.arraycopy(chars, 1, encodedChars, 12-chars.length, 
                    chars.length-1);
        } else {
            System.arraycopy(chars, 0, encodedChars, 11-chars.length, 
                    chars.length);
        }
        
        return String.valueOf(encodedChars);

    }
    
    public static String unescapeIndexFieldValue(String value) {
        StringBuffer buffer = new StringBuffer(value);
        String escapedSeparator = "\\" + FIELD_VALUE_LIST_SEPARATOR;
        int p = 0;
        while ((p = buffer.indexOf(escapedSeparator, p)) != -1) {
            buffer.delete(p, p+1);
        }
        
        return buffer.toString();
    }
    
    public static String escapeIndexFieldValue(String value) {
        StringBuffer buffer = new StringBuffer(value);
        int p = 0;
        while ((p = buffer.indexOf(FIELD_VALUE_LIST_SEPARATOR, p)) != -1) {
            buffer.insert(p, "\\");
        }
        
        return buffer.toString();
    }
    
    public void setPropertyManager(PropertyManagerImpl propertyManager) {
        this.propertyManager = propertyManager;
    }

    public void setValueFactory(ValueFactory valueFactory) {
        this.valueFactory = valueFactory;
    }

}

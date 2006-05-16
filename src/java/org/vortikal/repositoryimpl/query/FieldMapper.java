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

import org.apache.lucene.document.Field;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.repository.resourcetype.ValueFactory;
import org.vortikal.repository.resourcetype.ValueFormatException;

/**
 * Utility methods for mapping between <code>Value</code> and 
 * <code>org.apache.lucene.document.Field</code> objects.
 * 
 * @author oyviste
 */
public final class FieldMapper {
    

    public static final char MULTI_VALUE_FIELD_SEPARATOR = ';';
    
    private FieldMapper() {} // Util
     
    // No encoding (un-typed)
    public static Field getKeywordField(String name, int value) {
        return getKeywordField(name, Integer.toString(value));
    }
    
    // No encoding (un-typed)
    public static Field getKeywordField(String name, String value) {
        return new Field(name, value, Field.Store.YES, 
                Field.Index.NO_NORMS);
    }

    /**
     * Create <code>Field</code> from multiple <code>Value</code>s.
     *  
     * @param name
     * @param values
     * @return
     */
    public static Field getFieldFromValues(String name, Value[] values) {

        StringBuffer fieldValue = new StringBuffer();
        for (int i=0; i<values.length; i++) {
            String encoded = encodeIndexFieldValue(values[i]);
            String escaped = escapeCharacter(MULTI_VALUE_FIELD_SEPARATOR, encoded);
            fieldValue.append(escaped);
            if (i < values.length-1) fieldValue.append(MULTI_VALUE_FIELD_SEPARATOR);
        }

        Field field = new Field(name, fieldValue.toString(), Field.Store.YES, Field.Index.TOKENIZED);
        
        return field;
    }
    
    /**
     * Create <code>Field</code> from single <code>Value</code>.
     *  
     * @param name
     * @param value
     * @return
     */
    public static Field getFieldFromValue(String name, Value value) {
        String encoded = encodeIndexFieldValue(value);
        return getKeywordField(name, encoded);
    }
    
    /**
     * Create single <code>Value</code> from <code>Field</code> with the given
     * datatype.
     * 
     * @param field
     * @param valueFactory
     * @param type
     * @return
     */
    public static Value getValueFromField(Field field, ValueFactory valueFactory, int type) {
        String fieldValue = field.stringValue();
        
        String decodedFieldValue = decodeIndexFieldValueToString(fieldValue, type);
        
        return valueFactory.createValue(decodedFieldValue, type);
    }
    
    /**
     * Create multiple <code>Value</code>s from <code>Field</code> with the
     * given datatype.
     * 
     * @param field
     * @param valueFactory
     * @param type
     * @return
     */
    public static Value[] getValuesFromField(Field field, ValueFactory valueFactory, 
                                                int type) {

        String[] stringValues = 
            field.stringValue().split(Character.toString(MULTI_VALUE_FIELD_SEPARATOR));
        Value[] values = new Value[stringValues.length];
        for (int i=0; i<stringValues.length; i++) {
            String stringValue = 
                decodeIndexFieldValueToString(unescapeCharacter(MULTI_VALUE_FIELD_SEPARATOR, 
                                                            stringValues[i]), type);
            
            values[i] = valueFactory.createValue(stringValue, type);
        }
        
        return values;
    }
    
    // XXX: Used for generating ancestor ids field. We don't bother to encode
    //      it because of its system-specific nature, and that it should never
    //      be used as a sane sorting key or in range queries.
    public static Field getUnencodedMultiValueFieldFromIntegers(String name, 
                                                                int[] integers) {
        StringBuffer fieldValue = new StringBuffer();
        for (int i=0; i<integers.length; i++) {
            fieldValue.append(Integer.toString(integers[i]));
            if (i < integers.length-1) {
                fieldValue.append(MULTI_VALUE_FIELD_SEPARATOR);
            }
        }
        
        return new Field(name, fieldValue.toString(), Field.Store.YES, 
                                                      Field.Index.TOKENIZED);
    }

    // XXX: Used for ancestor ids field.
    public static int[] getIntegersFromUnencodedMultiValueField(Field field) {
        if ("".equals(field.stringValue())) return new int[0];
        
        String[] stringValues = field.stringValue().split(
                Character.toString(MULTI_VALUE_FIELD_SEPARATOR));
        int[] integers = new int[stringValues.length];
        for (int i=0; i<stringValues.length; i++) {
            integers[i] = Integer.parseInt(stringValues[i]);
        }
        
        return integers;
    }
    
    public static int getIntegerFromUnencodedField(Field field) {
        return Integer.parseInt(field.stringValue());
    }
    
    public static String decodeIndexFieldValueToString(String fieldValue, int type) 
        throws FieldValueEncodingException {
        
        switch (type) {
        
        case (PropertyType.TYPE_BOOLEAN):
        case (PropertyType.TYPE_STRING):
        case (PropertyType.TYPE_PRINCIPAL):
            return fieldValue; // No need to decode any of these. Native String
                               // representation already present in index.

        case (PropertyType.TYPE_DATE):
            return Long.toString(FieldValueEncoder.decodeDateValue(fieldValue));
            
        case (PropertyType.TYPE_INT):
            return Integer.toString(FieldValueEncoder.decodeInteger(fieldValue));
            
        case (PropertyType.TYPE_LONG):
            return Long.toString(FieldValueEncoder.decodeLong(fieldValue));
            
        default: throw new ValueFormatException("Unknown type " + type);
                
        }
    }
    
    public static String encodeIndexFieldValue(Value value) 
        throws FieldValueEncodingException {
        
        switch (value.getType()) {
        case(PropertyType.TYPE_STRING):
        case(PropertyType.TYPE_BOOLEAN):
        case(PropertyType.TYPE_PRINCIPAL):
            return value.getNativeStringRepresentation();
        
        case(PropertyType.TYPE_DATE):
            return FieldValueEncoder.encodeDateValue(value.getDateValue().getTime());
        
        case(PropertyType.TYPE_INT):
            return FieldValueEncoder.encodeInteger(value.getIntValue());
        
        case(PropertyType.TYPE_LONG):
            return FieldValueEncoder.encodeLong(value.getLongValue());
        
        default: throw new IllegalArgumentException("Unknown type: " + value.getType());
        
        }
        
    }
    
    /**
     * Encode a native value string representation into a form suitable for indexing
     * and searching (make the various type representations lexicographically sortable, 
     * basically).
     * 
     * Only native string representations are supported by this method, and it
     * is indexing/Lucene-specific.
     */
    public static String encodeIndexFieldValue(String stringValue, int type) 
        throws ValueFormatException, FieldValueEncodingException {
        
        switch (type) {
        case(PropertyType.TYPE_STRING):
        case(PropertyType.TYPE_BOOLEAN):
        case(PropertyType.TYPE_PRINCIPAL):
            return stringValue;
        
        case(PropertyType.TYPE_DATE):
            try {
                long l = Long.parseLong(stringValue);
                return FieldValueEncoder.encodeDateValue(l);
            } catch (NumberFormatException nfe) {
                throw new ValueFormatException("Unable to encode date string value to " 
                        + "to index field value representation: " + nfe.getMessage());
            }
            
        case(PropertyType.TYPE_INT):
            try {
                // Validate and encode 
                int n = Integer.parseInt(stringValue);
                return FieldValueEncoder.encodeInteger(n);
            } catch (NumberFormatException nfe) {
                throw new ValueFormatException("Unable to encode integer string value to "
                        + "to index field value representation: " + nfe.getMessage());
            }
            
        case(PropertyType.TYPE_LONG):
            try {
                // Validate and pad
                long l = Long.parseLong(stringValue);
                return FieldValueEncoder.encodeLong(l);
            } catch (NumberFormatException nfe) {
                throw new ValueFormatException(
                        "Unable to encode long integer string value to "
                                + "to index field value representation: "
                                + nfe.getMessage());
            }
            
        default: throw new IllegalArgumentException("Unknown type " + type);
        
        }

    }
    
    /**
     * Un-backslash-escape given character.
     * @param c
     * @param s
     * @return
     */
    public static String unescapeCharacter(char c, String s) {
        StringBuffer buffer = new StringBuffer(s);
        String escapedSeparator = "\\" + c;
        int p = 0;              
        while ((p = buffer.indexOf(escapedSeparator, p)) != -1) {
            buffer.delete(p, p+1);
        }
        
        return buffer.toString();
    }
    
    /**
     * Backslash-escape given character.
     * @param c
     * @param s
     * @return
     */
    public static String escapeCharacter(char c, String s) {
        StringBuffer buffer = new StringBuffer(s);
        String escapeChar = Character.toString(c);
        int p = 0;               
        while ((p = buffer.indexOf(escapeChar, p)) != -1) {
            buffer.insert(p, "\\");
        }
        
        return buffer.toString();
    }

}

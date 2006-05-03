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

import java.text.ParseException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.DateTools.Resolution;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.repository.resourcetype.ValueFactory;
import org.vortikal.repository.resourcetype.ValueFormatException;

/**
 * <ul>
 *  <li>Utility methods for mapping between <code>Value</code> and <code>org.apache.lucene.document.Field</code>
 * objects.
 *  <li>Methods for encoding and decoding index field values</li>
 * </ul> 
 * 
 * @author oyviste
 */
public final class FieldMapper {
    
    private static Log logger = LogFactory.getLog(FieldMapper.class);
    
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

    public static Field[] getFieldsFromValues(String name, Value[] values) {
        
        Field[] fields = new Field[values.length];
        for (int i=0; i<values.length; i++) {
            String encoded = encodeIndexFieldValue(values[i].getNativeStringRepresentation(), values[i].getType());
            fields[i] = getKeywordField(name, encoded);
        }
        
        return fields;
    }
    
    public static Field getFieldFromValue(String name, Value value) {
        String encoded = encodeIndexFieldValue(value.getNativeStringRepresentation(), value.getType());
        return getKeywordField(name, encoded);
    }
    
    public static Value getValueFromField(Field field, ValueFactory valueFactory, int type) {
        String fieldValue = field.stringValue();
        
        String decodedFieldValue = decodeIndexFieldValue(fieldValue, type);
        
        return valueFactory.createValue(decodedFieldValue, type);
    }
    
    public static Value[] getValuesFromFields(Field[] fields, ValueFactory valueFactory, 
                                                int type) {

        Value[] values = new Value[fields.length];
        for (int i=0; i<fields.length; i++) {
            String stringValue = fields[i].stringValue();
            values[i] = valueFactory.createValue(stringValue, type);
        }
        
        return values;
    }
    
    public static String decodeIndexFieldValue(String fieldValue, int type) 
        throws ValueFormatException {
        
        switch (type) {
        
        case (PropertyType.TYPE_BOOLEAN):
        case (PropertyType.TYPE_STRING):
        case (PropertyType.TYPE_PRINCIPAL):

            return fieldValue; // Stored as-is in index

        case (PropertyType.TYPE_DATE):
            try {
                long time = DateTools.stringToTime(fieldValue);
                return Long.toString(time);
            } catch (ParseException pe) {
                throw new ValueFormatException(pe.getMessage());
            }
            

            
        // XXX: sorting of negative integers does not work with this encoding
        // XXX: Possible solution is to shift scale to [0 - 2*Integer.MAX_VALUE], so
        //      that we avoid negative numbers (which cannot be compared directly
        //      lexicographically with other numbers, because "bigger" is in fact smaller
        //      on the negative side of the scale).
        //  TODO: use hex encoding, which is more compact.
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
    public static String encodeIndexFieldValue(String stringValue, int type) 
        throws ValueFormatException {
        
        switch (type) {
        case(PropertyType.TYPE_STRING):
        case(PropertyType.TYPE_BOOLEAN):
        case(PropertyType.TYPE_PRINCIPAL):
            return stringValue;
        
        case(PropertyType.TYPE_DATE):
            try {
                long l = Long.parseLong(stringValue);
                return DateTools.timeToString(l, Resolution.SECOND);
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
    
    /**
     * Un-backslash-escape given character
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
     * Backslash-escape given character
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

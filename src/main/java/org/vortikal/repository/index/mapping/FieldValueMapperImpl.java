/* Copyright (c) 2008, University of Oslo, Norway
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

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.lucene.document.Field;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.repository.resourcetype.ValueFactory;
import org.vortikal.repository.resourcetype.ValueFormatException;
import org.vortikal.repository.resourcetype.PropertyType.Type;
import org.vortikal.util.cache.ReusableObjectCache;
import org.vortikal.util.text.SimpleDateFormatCache;

/**
 * Utility methods for mapping between <code>Value</code> and
 * <code>org.apache.lucene.document.Field</code> objects.
 * 
 * Contains methods that map to/from:
 * <ul>
 * <li>String representations of our data types to a searchable index string representation.</li>
 * <li>Binary values for storage in index</li>
 * </ul>
 * 
 * @author oyviste
 */
public class FieldValueMapperImpl implements FieldValueMapper {

    private static final String STRING_VALUE_ENCODING = "UTF-8";

    private static final ReusableObjectCache[] CACHED_DATE_FORMATS = new SimpleDateFormatCache[] {
        new SimpleDateFormatCache("yyyy-MM-dd HH:mm:ss Z", 5),
        new SimpleDateFormatCache("yyyy-MM-dd HH:mm:ss", 5),
        new SimpleDateFormatCache("yyyy-MM-dd HH:mm", 5),
        new SimpleDateFormatCache("yyyy-MM-dd HH", 5),
        new SimpleDateFormatCache("yyyy-MM-dd", 5) 
    };

    public static final char MULTI_VALUE_FIELD_SEPARATOR = ';';

    public static final char ESCAPE_CHAR = '\\';

    private ValueFactory valueFactory;
    
    // No encoding (un-typed)
    public Field getKeywordField(String name, int value) {
        return new Field(name, Integer.toString(value), Field.Store.NO,
                Field.Index.NO_NORMS);
    }

    // No encoding (un-typed)
    public Field getKeywordField(String name, String value) {
        return new Field(name, value, Field.Store.NO, Field.Index.NO_NORMS);
    }

    // No encoding (un-typed)
    public Field getStoredKeywordField(String name, int value) {
        return new Field(name, Integer.toString(value), Field.Store.YES,
                Field.Index.NO_NORMS);
    }

    // No encoding (un-typed)
    public Field getStoredKeywordField(String name, String value) {
        return new Field(name, value, Field.Store.YES, Field.Index.NO_NORMS);
    }

    /**
     * Create indexed (but not stored) <code>Field</code> from multiple
     * <code>Value</code> objects. Should be analyzed with
     * {@link EscapedMultiValueFieldAnalyzer}.
     * 
     * @param name
     * @param values
     * @return
     */
    public Field getFieldFromValues(String name, Value[] values) {
        String[] encodedValues = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            encodedValues[i] = encodeIndexFieldValue(values[i]);
        }

        return new Field(name, new StringArrayTokenStream(encodedValues));
    }

    /**
     * Create indexed (but not stored) <code>Field</code> from single
     * <code>Value</code>.
     * 
     * @param name
     * @param value
     * @return
     */
    public Field getFieldFromValue(String name, Value value) {
        String encoded = encodeIndexFieldValue(value);
        return getKeywordField(name, encoded);
    }

    /**
     * Create single <code>Value</code> from <code>Field</code> with the
     * given datatype.
     * 
     * @param field
     * @param valueFactory
     * @param type
     * @return
     */
    public Value getValueFromField(Field field, Type type) {
        String fieldValue = field.stringValue();

        String decodedFieldValue = decodeIndexFieldValueToString(fieldValue,
                type);

        return this.valueFactory.createValue(decodedFieldValue, type);
    }

    /**
     * Used for generating ancestor ids field. We don't bother to encode it
     * because of its system-specific nature, and that it should never be used
     * as a sane sorting key or in range queries.
     */
    public Field getUnencodedMultiValueFieldFromIntegers(String name,
            int[] integers) {

        String[] values = new String[integers.length];
        for (int i = 0; i < integers.length; i++) {
            values[i] = Integer.toString(integers[i]);
        }

        return new Field(name, new StringArrayTokenStream(values));
    }

    public int getIntegerFromUnencodedField(Field field) {
        return Integer.parseInt(field.stringValue());
    }

    public String decodeIndexFieldValueToString(String fieldValue, Type type)
            throws FieldDataEncodingException {

        switch (type) {

        case BOOLEAN:
        case STRING:
        case HTML:
        case IMAGE_REF:
        case PRINCIPAL:
            return fieldValue; // No need to decode any of these.
            // Index string representation already in native
            // format.

        case DATE:
        case TIMESTAMP:
            return Long.toString(FieldDataEncoder
                    .decodeDateValueFromString(fieldValue));

        case INT:
            return Integer.toString(FieldDataEncoder
                    .decodeIntegerFromString(fieldValue));

        case LONG:
            return Long.toString(FieldDataEncoder
                    .decodeLongFromString(fieldValue));

        default:
            throw new FieldDataEncodingException("Unknown type " + type);

        }
    }

    public String encodeIndexFieldValue(Value value)
            throws FieldDataEncodingException {

        switch (value.getType()) {
        case STRING:
        case HTML:
        case IMAGE_REF:
        case BOOLEAN:
        case PRINCIPAL:
            return value.getNativeStringRepresentation();

        case DATE:
        case TIMESTAMP:
            return FieldDataEncoder.encodeDateValueToString(value
                    .getDateValue().getTime());

        case INT:
            return FieldDataEncoder.encodeIntegerToString(value.getIntValue());

        case LONG:
            return FieldDataEncoder.encodeLongToString(value.getLongValue());

        default:
            throw new FieldDataEncodingException("Unknown type: "
                    + value.getType());

        }

    }

    /**
     * Encode a string representation into a form suitable for indexing and
     * searching (make the various type representations lexicographically
     * sortable, basically).
     * 
     * Only native string representations are supported by this method.
     */
    public String encodeIndexFieldValue(String stringValue, Type type)
            throws ValueFormatException, FieldDataEncodingException {

        switch (type) {
        case STRING:
        case HTML:
        case IMAGE_REF:
        case BOOLEAN:
        case PRINCIPAL:
            return stringValue;

        case DATE:
        case TIMESTAMP:
            try {
                long l = Long.parseLong(stringValue);
                return FieldDataEncoder.encodeDateValueToString(l);
            } catch (NumberFormatException nfe) {
            }

            Date d = null;
            for (int i = 0; i < CACHED_DATE_FORMATS.length; i++) {
                SimpleDateFormat formatter = (SimpleDateFormat) CACHED_DATE_FORMATS[i]
                        .getInstance();
                try {
                    d = formatter.parse(stringValue);
                    break;
                } catch (Exception e) {
                } finally {
                    CACHED_DATE_FORMATS[i].putInstance(formatter);
                }
            }
            if (d == null) {
                throw new ValueFormatException(
                        "Unable to encode date string value '" + stringValue
                                + "' to index field value representation");
            }

            return FieldDataEncoder.encodeDateValueToString(d.getTime());

        case INT:
            try {
                // Validate and encode
                int n = Integer.parseInt(stringValue);
                return FieldDataEncoder.encodeIntegerToString(n);
            } catch (NumberFormatException nfe) {
                throw new ValueFormatException(
                        "Unable to encode integer string value to "
                                + "to index field value representation: "
                                + nfe.getMessage());
            }

        case LONG:
            try {
                // Validate and pad
                long l = Long.parseLong(stringValue);
                return FieldDataEncoder.encodeLongToString(l);
            } catch (NumberFormatException nfe) {
                throw new ValueFormatException(
                        "Unable to encode long integer string value to "
                                + "to index field value representation: "
                                + nfe.getMessage());
            }

        default:
            throw new FieldDataEncodingException("Unknown type " + type);

        }

    }

    /**
     * Un-escape given backslash-escaped character in <code>String</code>.
     * Escaped back-slashes are also un-escaped.
     * 
     * @param c
     * @param s
     * @return XXX: no longer in use
     */
    public static String unescapeCharacter(char c, String s) {

        int length = s.length();
        char[] output = new char[length];
        int p = 0;

        for (int i = 0; i < length; i++) {
            char current = s.charAt(i);
            if (current == ESCAPE_CHAR) {
                if (i == length - 1) {
                    throw new IllegalArgumentException(
                            "Invalid escape-character sequence in string");
                }

                char next = s.charAt(i + 1);

                if (next != c && next != ESCAPE_CHAR) {
                    throw new IllegalArgumentException(
                            "Invalid escape-character sequence in string");
                }

                output[p++] = next;
                ++i;
            } else {
                output[p++] = current;
            }
        }

        return new String(output, 0, p);
    }

    /**
     * Escape given character using the backslash character '\\'. Note that
     * occurences of the backslash character in the input string are also escaped.
     * 
     * @param c
     * @param s
     * @return XXX: no longer in use.
     */
    public static String escapeCharacter(char c, String s) {
        int length = s.length();
        char[] output = new char[length];
        int p = 0;

        for (int i = 0; i < length; i++) {
            char current = s.charAt(i);
            if (p >= output.length - 1) {
                char[] doubled = new char[output.length * 2];
                System.arraycopy(output, 0, doubled, 0, p);
                output = doubled;
            }

            if (current == ESCAPE_CHAR || current == c) {
                output[p++] = ESCAPE_CHAR;
            }

            output[p++] = current;
        }

        return new String(output, 0, p);
    }
    
    /* Binary field value mapping methods below */

    public Field getBinaryFieldFromValue(String name, Value value)
            throws FieldDataEncodingException {

        byte[] byteValue = null;
        switch (value.getType()) {
        case BOOLEAN:
            byteValue = FieldDataEncoder.encodeBooleanToBinary(value
                    .getBooleanValue());
            break;

        case DATE:
        case TIMESTAMP:
            byteValue = FieldDataEncoder.encodeDateValueToBinary(value
                    .getDateValue().getTime());
            break;

        case INT:
            byteValue = FieldDataEncoder.encodeIntegerToBinary(value
                    .getIntValue());
            break;

        case LONG:
            byteValue = FieldDataEncoder.encodeLongToBinary(value
                    .getLongValue());
            break;

        case PRINCIPAL:
        case STRING:
        case IMAGE_REF:
        case HTML:
            try {
                byteValue = value.getNativeStringRepresentation().getBytes(
                        STRING_VALUE_ENCODING);
            } catch (UnsupportedEncodingException ue) {
            } // Should never occur.
            break;

        default:
            throw new FieldDataEncodingException("Unknown type: "
                    + value.getType());

        }

        return new Field(name, byteValue, Field.Store.YES);
    }

    /**
     * For multi-valued props
     * 
     * @param name
     * @param values
     * @return
     */
    public Field[] getBinaryFieldsFromValues(String name, Value[] values)
            throws FieldDataEncodingException {

        if (values.length == 0) {
            throw new IllegalArgumentException(
                    "Length of values must be greater than zero.");
        }

        Field[] fields = new Field[values.length];
        for (int i = 0; i < values.length; i++) {
            fields[i] = getBinaryFieldFromValue(name, values[i]);
        }

        return fields;
    }

    public Value getValueFromBinaryField(Field field, Type type)
            throws FieldDataEncodingException, ValueFormatException {

        byte[] value = field.binaryValue();

        switch (type) {
        case PRINCIPAL:
        case IMAGE_REF:
        case HTML:
        case STRING:
            try {
                String stringValue = new String(value, STRING_VALUE_ENCODING);
                return this.valueFactory.createValue(stringValue, type);
            } catch (UnsupportedEncodingException ue) {
            } // Won't happen.

        case BOOLEAN:
            boolean b = FieldDataEncoder.decodeBooleanFromBinary(value);
            return new Value(b);

        case DATE:
            long time = FieldDataEncoder.decodeDateValueFromBinary(value);
            return new Value(new Date(time), true);

        case TIMESTAMP:
            long time2 = FieldDataEncoder.decodeDateValueFromBinary(value);
            return new Value(new Date(time2), false);

        case INT:
            int n = FieldDataEncoder.decodeIntegerFromBinary(value);
            return new Value(n);

        case LONG:
            long l = FieldDataEncoder.decodeLongFromBinary(value);
            return new Value(l);
        }

        throw new FieldDataEncodingException("Unknown type: " + type);
    }

    public Value[] getValuesFromBinaryFields(List<Field> fields, Type type)
            throws FieldDataEncodingException, ValueFormatException {

        if (fields.size() == 0) {
            throw new IllegalArgumentException(
                    "Length of fields must be greater than zero");
        }

        Value[] values = new Value[fields.size()];
        int u = 0;
        for (Field field : fields) {
            values[u++] = getValueFromBinaryField(field, type);
        }

        return values;
    }

    public String getStringFromStoredBinaryField(Field f)
            throws FieldValueMappingException {
        try {
            return new String(f.binaryValue(), STRING_VALUE_ENCODING);
        } catch (UnsupportedEncodingException uee) {
            throw new FieldValueMappingException(uee.getMessage());
        }
    }

    public Field getStoredBinaryIntegerField(String name, int value)
            throws FieldValueMappingException {
        return new Field(name, FieldDataEncoder.encodeIntegerToBinary(value),
                Field.Store.YES);
    }

    public int getIntegerFromStoredBinaryField(Field f)
            throws FieldValueMappingException {

        if (!f.isBinary()) {
            throw new FieldValueMappingException("Not a binary stored field");
        }

        return FieldDataEncoder.decodeIntegerFromBinary(f.binaryValue());
    }
    

    @Required
    public void setValueFactory(ValueFactory valueFactory) {
        this.valueFactory = valueFactory;
    }

}

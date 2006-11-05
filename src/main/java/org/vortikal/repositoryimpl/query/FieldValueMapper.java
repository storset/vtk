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

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.lucene.document.Field;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.repository.resourcetype.ValueFactory;
import org.vortikal.repository.resourcetype.ValueFormatException;
import org.vortikal.util.cache.ReusableObjectCache;
import org.vortikal.util.text.SimpleDateFormatCache;

/**
 * Utility methods for mapping between <code>Value</code> and
 * <code>org.apache.lucene.document.Field</code> objects.
 * 
 * Also contains a method that maps from string representations of our data
 * types to a searchable index string representation.
 * 
 * @author oyviste
 */
public final class FieldValueMapper {

    // XXX: This is not thread safe, date formats can't handle multiple
    // threads in parse()...
//    public static final SimpleDateFormat[] DATE_FORMATS = new SimpleDateFormat[] {
//            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z"),
//            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"),
//            new SimpleDateFormat("yyyy-MM-dd HH:mm"),
//            new SimpleDateFormat("yyyy-MM-dd HH"),
//            new SimpleDateFormat("yyyy-MM-dd") };

    public static final ReusableObjectCache[] CACHED_DATE_FORMATS = new SimpleDateFormatCache[] {
            new SimpleDateFormatCache("yyyy-MM-dd HH:mm:ss Z", 5),
            new SimpleDateFormatCache("yyyy-MM-dd HH:mm:ss", 5),
            new SimpleDateFormatCache("yyyy-MM-dd HH:mm", 5),
            new SimpleDateFormatCache("yyyy-MM-dd HH", 5),
            new SimpleDateFormatCache("yyyy-MM-dd", 5) 
        };

    public static final char MULTI_VALUE_FIELD_SEPARATOR = ';';

    public static final char ESCAPE_CHAR = '\\';

    private FieldValueMapper() {  } // Util

    // No encoding (un-typed)
    public static Field getKeywordField(String name, int value) {
        return new Field(name, Integer.toString(value), Field.Store.NO,
                Field.Index.NO_NORMS);
    }

    // No encoding (un-typed)
    public static Field getKeywordField(String name, String value) {
        return new Field(name, value, Field.Store.NO, Field.Index.NO_NORMS);
    }

    // No encoding (un-typed)
    public static Field getStoredKeywordField(String name, int value) {
        return new Field(name, Integer.toString(value), Field.Store.YES,
                Field.Index.NO_NORMS);
    }

    // No encoding (un-typed)
    public static Field getStoredKeywordField(String name, String value) {
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
    public static Field getFieldFromValues(String name, Value[] values) {

        StringBuffer fieldValue = new StringBuffer();
        for (int i = 0; i < values.length; i++) {
            String encoded = encodeIndexFieldValue(values[i]);
            String escaped = escapeCharacter(MULTI_VALUE_FIELD_SEPARATOR,
                    encoded);
            fieldValue.append(escaped);
            if (i < values.length - 1)
                fieldValue.append(MULTI_VALUE_FIELD_SEPARATOR);
        }

        Field field = new Field(name, fieldValue.toString(), Field.Store.NO,
                Field.Index.TOKENIZED);

        return field;
    }

    /**
     * Create indexed (but not stored) <code>Field</code> from single
     * <code>Value</code>.
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
     * Create single <code>Value</code> from <code>Field</code> with the
     * given datatype.
     * 
     * @param field
     * @param valueFactory
     * @param type
     * @return
     */
    public static Value getValueFromField(Field field,
            ValueFactory valueFactory, int type) {
        String fieldValue = field.stringValue();

        String decodedFieldValue = decodeIndexFieldValueToString(fieldValue,
                type);

        return valueFactory.createValue(decodedFieldValue, type);
    }

    /**
     * Create multiple <code>Value</code>s from <code>Field</code> with the
     * given datatype. The field values should be an escaped multi-value field
     * with native string encoding of each value.
     * 
     * @param field
     * @param valueFactory
     * @param type
     * @return
     */
    public static Value[] getValuesFromField(Field field,
            ValueFactory valueFactory, int type) {

        String[] stringValues = field.stringValue().split(
                Character.toString(MULTI_VALUE_FIELD_SEPARATOR));
        Value[] values = new Value[stringValues.length];
        for (int i = 0; i < stringValues.length; i++) {
            String stringValue = decodeIndexFieldValueToString(
                    unescapeCharacter(MULTI_VALUE_FIELD_SEPARATOR,
                            stringValues[i]), type);

            values[i] = valueFactory.createValue(stringValue, type);
        }

        return values;
    }

    /**
     * Used for generating ancestor ids field. We don't bother to encode it
     * because of its system-specific nature, and that it should never be used
     * as a sane sorting key or in range queries.
     */
    public static Field getUnencodedMultiValueFieldFromIntegers(String name,
            int[] integers) {
        StringBuffer fieldValue = new StringBuffer();
        for (int i = 0; i < integers.length; i++) {
            fieldValue.append(Integer.toString(integers[i]));
            if (i < integers.length - 1) {
                fieldValue.append(MULTI_VALUE_FIELD_SEPARATOR);
            }
        }

        return new Field(name, fieldValue.toString(), Field.Store.NO,
                Field.Index.TOKENIZED);
    }

    // XXX: Used for re-creating a stored ancestor ids field. Expensive and
    // currently not in use (see BinaryFieldValueMapper) !
    public static int[] getIntegersFromUnencodedMultiValueField(Field field) {
        if ("".equals(field.stringValue()))
            return new int[0];

        String[] stringValues = field.stringValue().split(
                Character.toString(MULTI_VALUE_FIELD_SEPARATOR));
        int[] integers = new int[stringValues.length];
        for (int i = 0; i < stringValues.length; i++) {
            integers[i] = Integer.parseInt(stringValues[i]);
        }

        return integers;
    }

    public static int getIntegerFromUnencodedField(Field field) {
        return Integer.parseInt(field.stringValue());
    }

    public static String decodeIndexFieldValueToString(String fieldValue,
            int type) throws FieldValueEncodingException {

        switch (type) {

        case (PropertyType.TYPE_BOOLEAN):
        case (PropertyType.TYPE_STRING):
        case (PropertyType.TYPE_PRINCIPAL):
            return fieldValue; // No need to decode any of these. Native String
            // representation already present in index.

        case (PropertyType.TYPE_DATE):
            return Long.toString(FieldValueEncoder
                    .decodeDateValueFromString(fieldValue));

        case (PropertyType.TYPE_INT):
            return Integer.toString(FieldValueEncoder
                    .decodeIntegerFromString(fieldValue));

        case (PropertyType.TYPE_LONG):
            return Long.toString(FieldValueEncoder
                    .decodeLongFromString(fieldValue));

        default:
            throw new FieldValueEncodingException("Unknown type " + type);

        }
    }

    public static String encodeIndexFieldValue(Value value)
            throws FieldValueEncodingException {

        switch (value.getType()) {
        case (PropertyType.TYPE_STRING):
        case (PropertyType.TYPE_BOOLEAN):
        case (PropertyType.TYPE_PRINCIPAL):
            return value.getNativeStringRepresentation();

        case (PropertyType.TYPE_DATE):
            return FieldValueEncoder.encodeDateValueToString(value
                    .getDateValue().getTime());

        case (PropertyType.TYPE_INT):
            return FieldValueEncoder.encodeIntegerToString(value.getIntValue());

        case (PropertyType.TYPE_LONG):
            return FieldValueEncoder.encodeLongToString(value.getLongValue());

        default:
            throw new FieldValueEncodingException("Unknown type: "
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
    public static String encodeIndexFieldValue(String stringValue, int type)
            throws ValueFormatException, FieldValueEncodingException {

        switch (type) {
        case (PropertyType.TYPE_STRING):
        case (PropertyType.TYPE_BOOLEAN):
        case (PropertyType.TYPE_PRINCIPAL):
            return stringValue;

        case (PropertyType.TYPE_DATE):
            try {
                long l = Long.parseLong(stringValue);
                return FieldValueEncoder.encodeDateValueToString(l);
            } catch (NumberFormatException nfe) {
            }

            Date d = null;
            for (int i = 0; i < CACHED_DATE_FORMATS.length; i++) {
                SimpleDateFormat formatter = (SimpleDateFormat)CACHED_DATE_FORMATS[i].getInstance();
                try {
                    //d = DATE_FORMATS[i].parse(stringValue);
                    d = formatter.parse(stringValue);
                    break;
                } catch (Exception e) {}
                 finally {
                     CACHED_DATE_FORMATS[i].putInstance(formatter);
                 }
            }
            if (d == null) {
                throw new ValueFormatException(
                        "Unable to encode date string value '" + stringValue
                                + "' to index field value representation");
            }
            return FieldValueEncoder.encodeDateValueToString(d.getTime());

        case (PropertyType.TYPE_INT):
            try {
                // Validate and encode
                int n = Integer.parseInt(stringValue);
                return FieldValueEncoder.encodeIntegerToString(n);
            } catch (NumberFormatException nfe) {
                throw new ValueFormatException(
                        "Unable to encode integer string value to "
                                + "to index field value representation: "
                                + nfe.getMessage());
            }

        case (PropertyType.TYPE_LONG):
            try {
                // Validate and pad
                long l = Long.parseLong(stringValue);
                return FieldValueEncoder.encodeLongToString(l);
            } catch (NumberFormatException nfe) {
                throw new ValueFormatException(
                        "Unable to encode long integer string value to "
                                + "to index field value representation: "
                                + nfe.getMessage());
            }

        default:
            throw new FieldValueEncodingException("Unknown type " + type);

        }

    }

    /**
     * Un-escape given backslash-escaped character in <code>String</code>.
     * Escaped back-slashes are also un-escaped.
     * 
     * @param c
     * @param s
     * @return
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
     * occurences of the backslash character in the input string are also
     * escaped.
     * 
     * Avoided use of synchronized StringBuffer; We copy only one character at a
     * time, so we need things to go fast.
     * 
     * @param c
     * @param s
     * @return
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

}

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
import org.apache.lucene.document.Fieldable;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.resourcetype.PropertyType.Type;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.repository.resourcetype.ValueFactory;
import org.vortikal.repository.resourcetype.ValueFormatException;
import org.vortikal.util.cache.ReusableObjectArrayStackCache;
import org.vortikal.util.cache.ReusableObjectCache;

/**
 * Utility methods for mapping between <code>Value</code> and
 * <code>org.apache.lucene.document.Field</code> objects.
 * 
 * Contains methods that map to/from:
 * <ul>
 * <li>String representations of our data types to a searchable index string
 * representation.</li>
 * <li>Binary values for storage in index</li>
 * </ul>
 *
 * TODO Consider using NumericField (Lucene 3) for numeric values.
 * 
 * @author oyviste
 */
public final class FieldValueMapper {

    private static final String STRING_VALUE_ENCODING = "utf-8";

    // Note that order (complex towards simpler format) is important here.
    private static final String[] SUPPORTED_DATE_FORMATS = {
                                                "yyyy-MM-dd HH:mm:ss Z",
                                                "yyyy-MM-dd HH:mm:ss",
                                                "yyyy-MM-dd HH:mm",
                                                "yyyy-MM-dd HH",
                                                "yyyy-MM-dd" };

    private static final ReusableObjectCache<SimpleDateFormat>[] CACHED_DATE_FORMAT_PARSERS;

    static {
        // Create parser caches for each date format (maximum capacity of 3
        // instances per format)
        CACHED_DATE_FORMAT_PARSERS = new ReusableObjectCache[SUPPORTED_DATE_FORMATS.length];

        for (int i = 0; i < SUPPORTED_DATE_FORMATS.length; i++) {
            CACHED_DATE_FORMAT_PARSERS[i] = new ReusableObjectArrayStackCache<SimpleDateFormat>(3);
        }
    }

    public static final char MULTI_VALUE_FIELD_SEPARATOR = ';';

    private ValueFactory valueFactory;

    // No encoding (un-typed)
    public Field getKeywordField(String name, int value) {
        
        Field field = new Field(name, Integer.toString(value), Field.Store.NO,
                Field.Index.NOT_ANALYZED_NO_NORMS);

        field.setOmitTermFreqAndPositions(true);
        return field;
    }

    // No encoding (un-typed)
    public Field getKeywordField(String name, String value) {
        Field field = new Field(name, value, Field.Store.NO, Field.Index.NOT_ANALYZED_NO_NORMS);
        field.setOmitTermFreqAndPositions(true);
        return field;
    }

    // No encoding (un-typed)
    public Field getStoredKeywordField(String name, int value) {
        Field field = new Field(name, Integer.toString(value), Field.Store.YES,
                Field.Index.NOT_ANALYZED_NO_NORMS);
        field.setOmitTermFreqAndPositions(true);
        return field;
    }


    // No encoding (un-typed)
    public Field getStoredKeywordField(String name, String value) {
        Field field = new Field(name, value, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS);
        field.setOmitTermFreqAndPositions(true);
        return field;
    }


    /**
     * Create indexed (but not stored) <code>Field</code> from multiple
     * <code>Value</code> objects.
     * 
     * @param name
     * @param values
     * @return
     */
    public Field getFieldFromValues(String name, Value[] values, boolean lowercase) {
        String[] encodedValues = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            encodedValues[i] = encodeIndexFieldValue(values[i], lowercase);
        }



        Field field = new Field(name, new StringArrayTokenStream(encodedValues));
        field.setOmitTermFreqAndPositions(true);
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
    public Field getFieldFromValue(String name, Value value, boolean lowercase) {
        String encoded = encodeIndexFieldValue(value, lowercase);
        return getKeywordField(name, encoded);
    }


    /**
     * Create indexed (but not stored) <code>Field</code> from array of Object. No special encoding
     * of value is applied, tokenization is done per object value, optionally applying lowercasing.
     *  
     * @param name
     * @param values
     * @param lowercase
     * @return
     */
    public Field getUnencodedMultiValueFieldfFromObjects(String name, Object[] values, boolean lowercase) {
        String[] strValues = new String[values.length];
        for (int i=0; i<values.length; i++) {
            strValues[i] = lowercase ? values[i].toString().toLowerCase() : values[i].toString();
        }
        return getUnencodedMultiValueFieldFromStrings(name, strValues);
    }
    
    /**
     * Used for generating ancestor ids field. We don't bother to encode it
     * because of its system-specific nature, and that it should never be used
     * as a sane sorting key or in range queries.
     */
    public Field getUnencodedMultiValueFieldFromIntegers(String name, int[] integers) {

        String[] values = new String[integers.length];
        for (int i = 0; i < integers.length; i++) {
            values[i] = Integer.toString(integers[i]);
        }
        
        return getUnencodedMultiValueFieldFromStrings(name, values);
    }
    
    public Field getUnencodedMultiValueFieldFromStrings(String name, String[] values) {
        Field field = new Field(name, new StringArrayTokenStream(values));
        field.setOmitTermFreqAndPositions(true);
        return field;
    }

    public String encodeIndexFieldValue(Value value, boolean lowercase)
            throws FieldDataEncodingException {

        switch (value.getType()) {
        case STRING:
        case HTML:
        case JSON:
            if (lowercase) {
                return value.getNativeStringRepresentation().toLowerCase();
            }
            return value.getNativeStringRepresentation();

        case IMAGE_REF:
        case BOOLEAN:
        case PRINCIPAL:
            return value.getNativeStringRepresentation();

        case DATE:
        case TIMESTAMP:
            return FieldDataEncoder.encodeDateValueToString(value.getDateValue().getTime());

        case INT:
            return FieldDataEncoder.encodeIntegerToString(value.getIntValue());

        case LONG:
            return FieldDataEncoder.encodeLongToString(value.getLongValue());

        default:
            throw new FieldDataEncodingException("Unknown or unsupported type: " + value.getType());
        }

    }


    /**
     * Encode a string representation into a form suitable for indexing and
     * searching (make the various type representations lexicographically
     * sortable, basically).
     * 
     * Only native string representations are supported by this method.
     *
     * This method is mostly used when constructing searchable values from query
     * building process.
     */
    public String encodeIndexFieldValue(String stringValue, Type type, boolean lowercase)
            throws ValueFormatException, FieldDataEncodingException {

        switch (type) {
        case STRING:
        case HTML:
        case JSON:
            if (lowercase) {
                return stringValue.toLowerCase();
            }
            return stringValue;

        case IMAGE_REF:
        case BOOLEAN:
        case PRINCIPAL:
            return stringValue;

        case DATE:
        case TIMESTAMP:
            try {
                return FieldDataEncoder.encodeDateValueToString(Long.parseLong(stringValue));
            } catch (NumberFormatException nfe) { 
                // Failed to parse "long" format, ignore.
            }

            Date d = null;
            for (int i = 0; i < SUPPORTED_DATE_FORMATS.length; i++) {
                SimpleDateFormat formatter = CACHED_DATE_FORMAT_PARSERS[i].getInstance();
                if (formatter == null) {
                    formatter = new SimpleDateFormat(SUPPORTED_DATE_FORMATS[i]);
                }

                try {
                    d = formatter.parse(stringValue);
                    break;
                } catch (Exception e) {
                    // Ignore failed parsing attempt
                } finally {
                    // Cache the constructed date parser for re-use
                    CACHED_DATE_FORMAT_PARSERS[i].putInstance(formatter);
                }
            }

            if (d == null) {
                throw new ValueFormatException("Unable to encode date string value '" + stringValue
                        + "' to index field value representation");
            }

            // Finally, create encoded index field representation of the parsed
            // date.
            return FieldDataEncoder.encodeDateValueToString(d.getTime());

        case INT:
            try {
                // Validate and encode
                int n = Integer.parseInt(stringValue);
                return FieldDataEncoder.encodeIntegerToString(n);
            } catch (NumberFormatException nfe) {
                throw new ValueFormatException("Unable to encode integer string value to "
                        + "to index field value representation: " + nfe.getMessage());
            }

        case LONG:
            try {
                // Validate and pad
                long l = Long.parseLong(stringValue);
                return FieldDataEncoder.encodeLongToString(l);
            } catch (NumberFormatException nfe) {
                throw new ValueFormatException("Unable to encode long integer string value to "
                        + "to index field value representation: " + nfe.getMessage());
            }

        default:
            throw new FieldDataEncodingException("Unknown or unsupported type " + type);

        }

    }


    /* Binary field value mapping methods below */

    public Field getStoredBinaryFieldFromValue(String name, Value value)
            throws FieldDataEncodingException {

        byte[] byteValue = null;
        switch (value.getType()) {
        case BOOLEAN:
            byteValue = FieldDataEncoder.encodeBooleanToBinary(value.getBooleanValue());
            break;

        case DATE:
        case TIMESTAMP:
            byteValue = FieldDataEncoder.encodeDateValueToBinary(value.getDateValue().getTime());
            break;

        case INT:
            byteValue = FieldDataEncoder.encodeIntegerToBinary(value.getIntValue());
            break;

        case LONG:
            byteValue = FieldDataEncoder.encodeLongToBinary(value.getLongValue());
            break;

        case PRINCIPAL:
        case STRING:
        case IMAGE_REF:
        case HTML:
        case JSON:
            try {
                byteValue = value.getNativeStringRepresentation().getBytes(STRING_VALUE_ENCODING);
            } catch (UnsupportedEncodingException ue) {
            } // Should never occur.
            break;

        default:
            throw new FieldDataEncodingException("Unknown or unsupported type: " + value.getType());

        }

        Field field = new Field(name, byteValue, Field.Store.YES);
        field.setOmitTermFreqAndPositions(true);
        return field;
    }


    /**
     * For multi-valued props
     * 
     * @param name
     * @param values
     * @return
     */
    public Field[] getStoredBinaryFieldsFromValues(String name, Value[] values)
            throws FieldDataEncodingException {

        if (values.length == 0) {
            throw new IllegalArgumentException("Length of values must be greater than zero.");
        }

        Field[] fields = new Field[values.length];
        for (int i = 0; i < values.length; i++) {
            fields[i] = getStoredBinaryFieldFromValue(name, values[i]);
        }

        return fields;
    }
    


    public Value getValueFromStoredBinaryField(Fieldable field, Type type) throws FieldDataEncodingException,
            ValueFormatException {

        byte[] valueBuf = field.getBinaryValue();

        switch (type) {
        case PRINCIPAL:
        case IMAGE_REF:
        case HTML:
        case STRING:
        case JSON:
            try {
                String stringValue = new String(valueBuf, field.getBinaryOffset(),
                                                       field.getBinaryLength(),
                                                       STRING_VALUE_ENCODING);
                return this.valueFactory.createValue(stringValue, type);
            } catch (UnsupportedEncodingException ue) {
            } // Won't happen.

        case BOOLEAN:
            boolean b = FieldDataEncoder.decodeBooleanFromBinary(valueBuf,
                                                                 field.getBinaryOffset(),
                                                                 field.getBinaryLength());
            return new Value(b);

        case DATE:
            long time = FieldDataEncoder.decodeDateValueFromBinary(valueBuf,
                                                                   field.getBinaryOffset(),
                                                                   field.getBinaryLength());
            return new Value(new Date(time), true);

        case TIMESTAMP:
            long time2 = FieldDataEncoder.decodeDateValueFromBinary(valueBuf,
                                                                    field.getBinaryOffset(),
                                                                    field.getBinaryLength());
            return new Value(new Date(time2), false);

        case INT:
            int n = FieldDataEncoder.decodeIntegerFromBinary(valueBuf,
                                                             field.getBinaryOffset(),
                                                             field.getBinaryLength());
            return new Value(n);

        case LONG:
            long l = FieldDataEncoder.decodeLongFromBinary(valueBuf,
                                                           field.getBinaryOffset(),
                                                           field.getBinaryLength());
            return new Value(l);
        }

        throw new FieldDataEncodingException("Unknown type: " + type);
    }


    public Value[] getValuesFromStoredBinaryFields(List<Fieldable> fields, Type type)
            throws FieldDataEncodingException, ValueFormatException {

        if (fields.isEmpty()) {
            throw new IllegalArgumentException("Length of fields must be greater than zero");
        }

        Value[] values = new Value[fields.size()];
        int u = 0;
        for (Fieldable field : fields) {
            values[u++] = getValueFromStoredBinaryField(field, type);
        }

        return values;
    }

    public Field getStoredBinaryIntegerField(String name, int value)
        throws FieldValueMappingException {
        Field field = new Field(name,
                FieldDataEncoder.encodeIntegerToBinary(value), Field.Store.YES);
        field.setOmitTermFreqAndPositions(true);
        return field;
    }

    public String getStringFromStoredBinaryField(Field f) throws FieldValueMappingException {
        try {
            return new String(f.getBinaryValue(),
                              f.getBinaryOffset(),
                              f.getBinaryLength(), STRING_VALUE_ENCODING);
        } catch (UnsupportedEncodingException uee) {
            throw new FieldValueMappingException(uee.getMessage());
        }
    }
    
    public String[] getStringsFromStoredBinaryFields(Field[] fields) 
                throws FieldValueMappingException {

        String[] values = new String[fields.length];
        try {
            for (int i=0; i<fields.length; i++) { 
                values[i] = new String(fields[i].getBinaryValue(),
                                       fields[i].getBinaryOffset(),
                                       fields[i].getBinaryLength(),
                                                    STRING_VALUE_ENCODING);
            }
        } catch (UnsupportedEncodingException uee) {
            throw new FieldValueMappingException(uee.getMessage());
        }
        
        return values;
    }
    
    public Field[] getStoredBinaryFieldsFromStrings(String name, String[] values) {

        Field[] fields = new Field[values.length];
        try {
            for (int i = 0; i < values.length; i++) {
                fields[i] = new Field(name, values[i].getBytes(STRING_VALUE_ENCODING), 
                                      Field.Store.YES);
                fields[i].setOmitTermFreqAndPositions(true);
            }
        } catch (UnsupportedEncodingException uee) {
            throw new FieldValueMappingException(uee.getMessage());
        }

        return fields;
    }
        

    public int getIntegerFromStoredBinaryField(Field f) throws FieldValueMappingException {

        if (!f.isBinary()) {
            throw new FieldValueMappingException("Not a binary stored field");
        }

        return FieldDataEncoder.decodeIntegerFromBinary(f.getBinaryValue(), 
                f.getBinaryOffset(), f.getBinaryLength());
    }


    @Required
    public void setValueFactory(ValueFactory valueFactory) {
        this.valueFactory = valueFactory;
    }

}
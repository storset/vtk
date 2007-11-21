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

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.List;

import org.apache.lucene.document.Field;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.repository.resourcetype.ValueFactory;
import org.vortikal.repository.resourcetype.ValueFormatException;
import org.vortikal.repository.resourcetype.PropertyType.Type;

/**
 * Map to/from stored binary value fields.
 */
public final class BinaryFieldValueMapper {

    private static final String STRING_VALUE_ENCODING = "UTF-8";
    
    private BinaryFieldValueMapper() {} // static Util class, no instances

    public static Field getBinaryFieldFromValue(String name, Value value) 
        throws FieldValueEncodingException {
        
        byte[] byteValue = null;
        switch (value.getType()) {
        case BOOLEAN:
            byteValue = FieldValueEncoder.encodeBooleanToBinary(value.getBooleanValue());
            break;

        case DATE:    
        case TIMESTAMP:
            byteValue = FieldValueEncoder.encodeDateValueToBinary(value.getDateValue().getTime());
            break;
            
        case INT:
            byteValue = FieldValueEncoder.encodeIntegerToBinary(value.getIntValue());
            break;
        
        case LONG:
            byteValue = FieldValueEncoder.encodeLongToBinary(value.getLongValue());
            break;
        
        case PRINCIPAL:
        case STRING:
            try {
                byteValue = value.getNativeStringRepresentation().getBytes(STRING_VALUE_ENCODING);
            } catch (UnsupportedEncodingException ue) {}
            break;
        
        default: throw new FieldValueEncodingException("Unknown type: " + value.getType()); 
    
        }
        
        return new Field(name, byteValue, Field.Store.YES);
    }

    /**
     * For multi-valued props
     * @param name
     * @param values
     * @return
     */
    public static Field[] getBinaryFieldsFromValues(String name, Value[] values) 
        throws FieldValueEncodingException {
        
        if (values.length == 0) {
            throw new IllegalArgumentException("Length of values must be greater than zero.");
        }
        
        Field[] fields = new Field[values.length];
        for (int i=0; i < values.length; i++) {
            fields[i] = getBinaryFieldFromValue(name, values[i]);
        }
        
        return fields;
    }
    
    public static Value getValueFromBinaryField(Field field, Type type)
        throws FieldValueEncodingException, ValueFormatException {
        
        ValueFactory vf = ValueFactory.getInstance();
        
        byte[] value = field.binaryValue();
    
        switch (type) {
        case PRINCIPAL:
        case STRING:
            try {
                String stringValue = new String(value, STRING_VALUE_ENCODING);
                return vf.createValue(stringValue, type);
            } catch (UnsupportedEncodingException ue) {} // won't happen
            
        case BOOLEAN:
            boolean b = FieldValueEncoder.decodeBooleanFromBinary(value);
            return new Value(b);
            
        case DATE:
            long time = FieldValueEncoder.decodeDateValueFromBinary(value);
            return new Value(new Date(time), true);

        case TIMESTAMP:
            long time2 = FieldValueEncoder.decodeDateValueFromBinary(value);
            return new Value(new Date(time2), false);
            
        case INT:
            int n = FieldValueEncoder.decodeIntegerFromBinary(value);
            return new Value(n);
            
        case LONG:
            long l = FieldValueEncoder.decodeLongFromBinary(value);
            return new Value(l);
        }            

        throw new FieldValueEncodingException("Unknown type: " + type); 
    }

    public static Value[] getValuesFromBinaryFields(List<Field> fields, Type type) 
        throws FieldValueEncodingException, ValueFormatException {
        
        if (fields.size() == 0) {
            throw new IllegalArgumentException("Length of fields must be greater than zero");
        }
        
        Value[] values = new Value[fields.size()];
        int u = 0;
        for (Field field: fields) {
            values[u++] = getValueFromBinaryField(field, type);
        }
        
        return values;
    }
    
    public static String getStringFromStoredBinaryField(Field f) 
        throws FieldValueMappingException {
        try {
            return new String(f.binaryValue(), STRING_VALUE_ENCODING);
        } catch (UnsupportedEncodingException uee) {
            throw new FieldValueMappingException(uee.getMessage());
        }
    }
    
    public static Field getStoredBinaryIntegerField(String name, int value) 
        throws FieldValueMappingException {
        return new Field(name, FieldValueEncoder.encodeIntegerToBinary(value), 
                Field.Store.YES);
    }
    
    public static int getIntegerFromStoredBinaryField(Field f) 
        throws FieldValueMappingException {
        
        if (! f.isBinary()) {
            throw new FieldValueMappingException("Not a binary stored field");
        }
        
        return FieldValueEncoder.decodeIntegerFromBinary(f.binaryValue());
    }
    
}

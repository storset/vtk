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

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.lucene.document.Field;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.repository.resourcetype.ValueFactory;
import org.vortikal.repository.resourcetype.ValueFormatException;

/**
 * Map to/from stored binary value fields.
 * 
 * TODO: javadoc
 * 
 * @author oyviste
 *
 */
public final class BinaryFieldValueMapper {

    private static final String STRING_VALUE_ENCODING = "UTF-8";
    
    private BinaryFieldValueMapper() {} // Util

    /**
     * 
     * @param name
     * @param value
     * @return
     * @throws FieldValueEncodingException
     */
    public static Field getBinaryFieldFromValue(String name, Value value) 
        throws FieldValueEncodingException {
        
        byte[] byteValue = null;
        switch (value.getType()) {
        case (PropertyType.TYPE_BOOLEAN):
            byteValue = FieldValueEncoder.encodeBooleanToBinary(value.getBooleanValue());
            break;
            
        case (PropertyType.TYPE_DATE):
            byteValue = FieldValueEncoder.encodeDateValueToBinary(value.getDateValue().getTime());
            break;
            
        case (PropertyType.TYPE_INT):
            byteValue = FieldValueEncoder.encodeIntegerToBinary(value.getIntValue());
            break;
        
        case (PropertyType.TYPE_LONG):
            byteValue = FieldValueEncoder.encodeLongToBinary(value.getLongValue());
            break;
        
        case (PropertyType.TYPE_PRINCIPAL):
        case (PropertyType.TYPE_STRING):
            try {
                byteValue = value.getNativeStringRepresentation().getBytes(STRING_VALUE_ENCODING);
            } catch (UnsupportedEncodingException ue) {}
            break;
        
        default: throw new FieldValueEncodingException("Unknown type: " + value.getType()); 
    
        }
        
        return new Field(name, byteValue, Field.Store.YES);
    }

    /**
     * For multivalued props
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
    
    /**
     * 
     * @param field
     * @param vf
     * @param type
     * @return
     * @throws FieldValueEncodingException
     * @throws ValueFormatException
     */
    public static Value getValueFromBinaryField(Field field, ValueFactory vf, int type)
        throws FieldValueEncodingException, ValueFormatException {
        
        byte[] value = field.binaryValue();
    
        Value v = new Value();
        switch (type) {
        case (PropertyType.TYPE_PRINCIPAL):
        case (PropertyType.TYPE_STRING):
            try {
                String stringValue = new String(value, STRING_VALUE_ENCODING);
                v = vf.createValue(stringValue, type);
            } catch (UnsupportedEncodingException ue) {}
            break;
            
        case (PropertyType.TYPE_BOOLEAN):
            boolean b = FieldValueEncoder.decodeBooleanFromBinary(value);
            v.setBooleanValue(b);
            break;
            
        case (PropertyType.TYPE_DATE):
            long time = FieldValueEncoder.decodeDateValueFromBinary(value);
            v.setDateValue(new Date(time));
            break;
            
        case (PropertyType.TYPE_INT):
            int n = FieldValueEncoder.decodeIntegerFromBinary(value);
            v.setIntValue(n);
            break;
            
        case (PropertyType.TYPE_LONG):
            long l = FieldValueEncoder.decodeLongFromBinary(value);
            v.setLongValue(l);
            break;
            
        default: throw new FieldValueEncodingException("Unknown type: " + type); 
        }
        
        return v;
    }

    /**
     * 
     * @param fields
     * @param vf
     * @param type
     * @return
     * @throws FieldValueEncodingException
     * @throws ValueFormatException
     */
    public static Value[] getValuesFromBinaryFields(List fields, ValueFactory vf, int type) 
        throws FieldValueEncodingException, ValueFormatException {
        
        if (fields.size() == 0) {
            throw new IllegalArgumentException("Length of fields must be greater than zero");
        }
        
        Value[] values = new Value[fields.size()];
        int u = 0;
        for (Iterator i = fields.iterator(); i.hasNext();) {
            values[u++] = getValueFromBinaryField((Field)i.next(), vf, type);
        }
        
        return values;
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

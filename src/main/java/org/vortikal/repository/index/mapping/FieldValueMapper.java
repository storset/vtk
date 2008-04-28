/* Copyright (c) 2006, 2008, University of Oslo, Norway
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

import java.util.List;

import org.apache.lucene.document.Field;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.repository.resourcetype.ValueFormatException;
import org.vortikal.repository.resourcetype.PropertyType.Type;

/**
 * Methods for mapping between <code>Value</code> and
 * <code>org.apache.lucene.document.Field</code> objects.
 * 
 * Also contains a method that maps from string representations of our data
 * types to a searchable index string representation.
 * 
 * @author oyviste
 */
public interface FieldValueMapper {

    // No encoding (un-typed)
    public Field getKeywordField(String name, int value) ;

    // No encoding (un-typed)
    public Field getKeywordField(String name, String value);

    // No encoding (un-typed)
    public Field getStoredKeywordField(String name, int value);

    // No encoding (un-typed)
    public Field getStoredKeywordField(String name, String value);

    /**
     * Create indexed (but not stored) <code>Field</code> from multiple
     * <code>Value</code> objects. Should be analyzed with
     * {@link EscapedMultiValueFieldAnalyzer}.
     * 
     * @param name
     * @param values
     * @return
     */
    public Field getFieldFromValues(String name, Value[] values) ;
    
    /**
     * Create indexed (but not stored) <code>Field</code> from single
     * <code>Value</code>.
     * 
     * @param name
     * @param value
     * @return
     */
    public Field getFieldFromValue(String name, Value value) ;

    /**
     * Create single <code>Value</code> from <code>Field</code> with the
     * given datatype.
     * 
     * @param field
     * @param valueFactory
     * @param type
     * @return
     */
    public Value getValueFromField(Field field, Type type);

    /**
     * Used for generating ancestor ids field. We don't bother to encode it
     * because of its system-specific nature, and that it should never be used
     * as a sane sorting key or in range queries.
     */
    public Field getUnencodedMultiValueFieldFromIntegers(String name,
            int[] integers) ;

    public int getIntegerFromUnencodedField(Field field);

    public String decodeIndexFieldValueToString(String fieldValue,
            Type type) throws FieldDataEncodingException ;

    public String encodeIndexFieldValue(Value value)
            throws FieldDataEncodingException ;

    /**
     * Encode a string representation into a form suitable for indexing and
     * searching (make the various type representations lexicographically
     * sortable, basically).
     * 
     * Only native string representations are supported by this method.
     */
    public String encodeIndexFieldValue(String stringValue, Type type)
            throws ValueFormatException, FieldDataEncodingException;
    
    
    
    
    public Field getBinaryFieldFromValue(String name, Value value)
    throws FieldDataEncodingException;

    
    /* Methods for binary field value mapping below */
    
    /**
     * For multi-valued props
     * 
     * @param name
     * @param values
     * @return
     */
    public Field[] getBinaryFieldsFromValues(String name, Value[] values)
            throws FieldDataEncodingException;

    public Value getValueFromBinaryField(Field field, Type type)
            throws FieldDataEncodingException, ValueFormatException;

    public Value[] getValuesFromBinaryFields(List<Field> fields, Type type)
            throws FieldDataEncodingException, ValueFormatException;

    public String getStringFromStoredBinaryField(Field f)
            throws FieldValueMappingException;

    public Field getStoredBinaryIntegerField(String name, int value)
            throws FieldValueMappingException;

    public int getIntegerFromStoredBinaryField(Field f)
            throws FieldValueMappingException;
    
}

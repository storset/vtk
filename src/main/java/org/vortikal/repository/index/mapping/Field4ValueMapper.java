/* Copyright (c) 2014, University of Oslo, Norway
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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.NumericUtils;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.resourcetype.PropertyType.Type;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.repository.resourcetype.ValueFactory;
import org.vortikal.repository.resourcetype.ValueFormatException;

import org.vortikal.util.cache.ReusableObjectArrayStackCache;
import org.vortikal.util.cache.ReusableObjectCache;

/**
 * Field value mapping from/to Lucene.
 */
public class Field4ValueMapper {
    
    // Note that order (complex towards simpler format) is important here.
    public static final String[] SUPPORTED_DATE_FORMATS = {
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

    private ValueFactory valueFactory;
    
    /**
     * Return an indexed keyword string field with no encoding from an integer number.
     * 
     * @param name field name
     * @param value integer field value (no special encoding is applied for indexing)
     * @param store whether to make the field stored <em>as well as indexed</em>.
     * 
     * @return an instance of <code>StringField</code>.
     */
    public Field keywordField(String name, int value, boolean store) {
        return new StringField(name, Integer.toString(value), store ? Field.Store.YES : Field.Store.NO);
    }
    
    /**
     * Return an indexed keyword string field from a string value.
     * 
     * @param name field name
     * @param value string value (not tokenized)
     * @param store whether to make the field stored <em>as well as indexed</em>.
     * 
     * @return an instance of <code>StringField</code>.
     */
    public Field keywordField(String name, String value, boolean store) {
        return new StringField(name, value, store ? Field.Store.YES : Field.Store.NO);
    }

    /**
     * Return an array of indexed keyword string fields from string values.
     * 
     * @param name the name
     * @param values the values
     * @param store whether to make the fields stored <em>as well as indexed</em>
     * 
     * @return an array of <code>Field</code> with the fields corresponding to
     * the string values.
     */
    public Field[] keywordFields(String name, boolean store, String...values) {
        Field[] fields = new Field[values.length];
        for (int i=0; i<fields.length; i++) {
            fields[i] = keywordField(name, values[i], store);
        }
        return fields;
    }
    
    public Field[] indexedFields(String name, boolean lowercase, Value... values) {
        Field[] fields = new Field[values.length];
        for (int i=0; i<values.length; i++) {
            fields[i] = indexedField(name, lowercase, values[i]);
        }
        return fields;
    }
    
    public Field[] indexedFields(String name, boolean lowercase, Type type, Object... values) {
        Field[] fields = new Field[values.length];
        for (int i=0; i<values.length; i++) {
            fields[i] = indexedField(name, lowercase, values[i], type);
        }
        return fields;
    }

    public Field[] storedFields(String name, Value... values) {
        Field[] fields = new Field[values.length];
        for (int i=0; i<values.length; i++) {
            fields[i] = storedField(name, values[i]);
        }
        return fields;
    }
    
    public Value[] valuesFromFields(Type type, List<IndexableField> fields) {
        Value[] values = new Value[fields.size()];
        for (int i=0; i<fields.size(); i++) {
            values[i] = valueFromField(type, fields.get(i));
        }
        return values;
    }
    
    public Term queryTerm(String fieldName, Object value, Type type, boolean lowercase) {
        switch (type) {
        case STRING:
        case HTML:
        case JSON:
        case IMAGE_REF:
        case BOOLEAN:
        case PRINCIPAL:
            if (lowercase) {
                return new Term(fieldName, value.toString().toLowerCase());
            }
            return new Term(fieldName, value.toString());

        case DATE:
        case TIMESTAMP:
            return new Term(fieldName, dateValueToIndexTerm(value));

        case INT:
            return new Term(fieldName, integerValueToIndexTerm(value));
            
        case LONG:
            return new Term(fieldName, longValueToIndexTerm(value));

        default:
            throw new IllegalArgumentException("Unknown or unsupported type " + type);
        }
        
    }

    /**
     * Parse object as date. If object is a <code>Long</code>, value is interpreted
     * as number of milliseconds since epoch, otherwise an attempt to parse 
     * {@link Object#toString() toString} in various formats is done.
     * 
     * <p>The returned value is rounded to nearest second.
     * 
     * @see #SUPPORTED_DATE_FORMATS
     * @param value value of date, as object. 
     * @return long value as number of milliseconds since epoch, rounded to nearest second
     */
    public long parseDate(Object value) {
        Long longValue = null;
        if (value.getClass() == Long.class) {
            longValue = (Long) value;
        } else if (value.getClass() == Date.class) {
            longValue = ((Date)value).getTime();
        }

        if (longValue == null) {
            String stringValue = value.toString();
            try {
                longValue = Long.parseLong(stringValue);
            } catch (NumberFormatException nfe) {
                // Failed to parse "long" format, try other formats
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
                if (d != null) {
                    longValue = d.getTime();
                }
            }
        }

        if (longValue == null) {
            throw new ValueFormatException("Unable to parse date value '" + value
                    + "'");
        }
        
        return DateTools.round(longValue, DateTools.Resolution.SECOND);
    }
    
    private BytesRef dateValueToIndexTerm(Object value) {
        long longValue = parseDate(value);
        
        // Finally, create encoded index field representation of the parsed
        // date.
        BytesRef bytes = new BytesRef();
        NumericUtils.longToPrefixCoded(longValue, 0, bytes);
        return bytes;
    }
    
    private BytesRef integerValueToIndexTerm(Object value) {
        final Integer intValue;
        if (value.getClass() == Integer.class) {
            intValue = (Integer) value;
        } else {
            try {
                // Validate and encode
                intValue = Integer.parseInt(value.toString());
            } catch (NumberFormatException nfe) {
                throw new ValueFormatException("Unable to encode integer string value to "
                        + "to index field value representation: " + nfe.getMessage());
            }
        }
        BytesRef bytes = new BytesRef();
        NumericUtils.intToPrefixCoded(intValue, 0, bytes);
        return bytes;
    }
    
    private BytesRef longValueToIndexTerm(Object value) {
        final Long longValue;
        if (value.getClass() == Long.class) {
            longValue = (Long)value;
        } else {
            try {
                // Validate and encode
                longValue = Long.parseLong(value.toString());
            } catch (NumberFormatException nfe) {
                throw new ValueFormatException("Unable to encode long integer string value to "
                        + "to index field value representation: " + nfe.getMessage());
            }
        }
        BytesRef bytes = new BytesRef();
        NumericUtils.longToPrefixCoded(longValue, 0, bytes);
        return bytes;
    }
    
    public Value valueFromField(Type type, IndexableField f) {
        switch (type) {
        case STRING:
        case HTML:
        case JSON:
        case IMAGE_REF:
        case PRINCIPAL:
        case BOOLEAN:
            String stringValue = f.stringValue();
            if (stringValue == null) {
                throw new IllegalArgumentException("Field " + f + " has no stored string value");
            }
            return this.valueFactory.createValue(stringValue, type);

        case DATE:
        case TIMESTAMP:
        case LONG:
            Number longNumber = f.numericValue();
            if (longNumber == null) {
                throw new IllegalArgumentException("Field " + f + " has no stored numeric long value");
            }
            long longValue = longNumber.longValue();
            if (type == Type.LONG) {
                return new Value(longValue);
            } else {
                return new Value(new Date(longValue), type == Type.DATE);
            }
            
        case INT:
            Number intNumber = f.numericValue();
            if (intNumber == null) {
                throw new IllegalArgumentException("Field " + f + " has no stored numeric integer value");
            }
            return new Value(intNumber.intValue());

        default:
            throw new IllegalArgumentException("Unsupported value type: " + type);
        }
    }
    
    private Field indexedField(String fieldName, boolean lowercase, Value value) {
        switch (value.getType()) {
        case STRING:
        case HTML:
        case JSON:
        case IMAGE_REF:
        case BOOLEAN:
        case PRINCIPAL:
            String stringValue = value.getNativeStringRepresentation();
            if (lowercase) {
                stringValue = stringValue.toLowerCase();
            }
            
            return new StringField(fieldName, stringValue, Field.Store.NO);

        case LONG:
        case DATE:
        case TIMESTAMP:
            long longValue;
            if (value.getType() != Type.LONG) {
                longValue = DateTools.round(value.getDateValue().getTime(), DateTools.Resolution.SECOND);
            } else {
                longValue = value.getLongValue();
            }
            
            return new LongField(fieldName, longValue, Field.Store.NO);

        case INT:
            return new IntField(fieldName, value.getIntValue(), Field.Store.NO);

        default:
            throw new IllegalArgumentException("Unsupported value type: " + value.getType());
        }
    }
    
    private Field indexedField(String fieldName, boolean lowercase, Object value, Type type) {
        switch (type) {
        case STRING:
        case HTML:
        case JSON:
        case IMAGE_REF:
        case BOOLEAN:
        case PRINCIPAL:
            String stringValue = (String)value;
            if (lowercase) {
                stringValue = stringValue.toLowerCase();
            }
            
            return new StringField(fieldName, stringValue, Field.Store.NO);

        case LONG:
        case DATE:
        case TIMESTAMP:
            long longValue = (Long)value;
            if (type != Type.LONG) {
                longValue = DateTools.round(longValue, DateTools.Resolution.SECOND);
            }
            
            return new LongField(fieldName, longValue, Field.Store.NO);

        case INT:
            return new IntField(fieldName, (Integer)value, Field.Store.NO);

        default:
            throw new IllegalArgumentException("Unsupported value type: " + type);
        }
    }
    
    private Field storedField(String fieldName, Value value) {
        switch (value.getType()) {
        case STRING:
        case HTML:
        case JSON:
        case IMAGE_REF:
        case BOOLEAN:
        case PRINCIPAL:
            return new StoredField(fieldName, value.getNativeStringRepresentation());

        case LONG:
        case DATE:
        case TIMESTAMP:
            final long longValue = value.getType() == Type.LONG ?
                    value.getLongValue() : value.getDateValue().getTime();
            return new StoredField(fieldName, longValue);

        case INT:
            return new StoredField(fieldName, value.getIntValue());

        default:
            throw new IllegalArgumentException("Unsupported value type: " + value.getType());
        }
    }
    
    /**
     * Get suitable data type for a JSON field. Only accepts property
     * defs with value type JSON. Uses JSON field type hints set in property
     * definition metadata.
     * 
     * @param def
     * @param jsonFieldName Name of a JSON field.
     * @return 
     */
    public static Type getJsonFieldDataType(PropertyTypeDefinition def, String jsonFieldName) {
        if (def.getType() != PropertyType.Type.JSON) {
            throw new IllegalArgumentException("Type JSON required: " + def);
        }
        Map<String,Object> metadata = def.getMetadata();
        String typeHint = (String) metadata.get(
                PropertyTypeDefinition.METADATA_INDEXABLE_JSON_TYPEHINT_FIELD_PREFIX + jsonFieldName);
        if (typeHint == null) {
            typeHint = (String) metadata.get(
                    PropertyTypeDefinition.METADATA_INDEXABLE_JSON_TYPEHINT_DEFAULT);
        }
        return typeHint != null ? Type.valueOf(typeHint) : Type.STRING;
    }
    
    @Required
    public void setValueFactory(ValueFactory valueFactory) {
        this.valueFactory = valueFactory;
    }
}

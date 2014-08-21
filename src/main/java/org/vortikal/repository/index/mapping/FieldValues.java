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

import com.ibm.icu.text.Collator;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.lucene.collation.ICUCollationAttributeFactory;

import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;


import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.NumericUtils;
import org.springframework.beans.factory.InitializingBean;
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
 * 
 * TODO missing JavaDoc for most methods
 */
public class FieldValues implements InitializingBean {
    
    // Note that order (complex towards simpler format) is important here.
    public static final String[] SUPPORTED_DATE_FORMATS = {
                                                "yyyy-MM-dd HH:mm:ss Z",
                                                "yyyy-MM-dd HH:mm:ss",
                                                "yyyy-MM-dd HH:mm",
                                                "yyyy-MM-dd HH",
                                                "yyyy-MM-dd" };
    
    private static final FieldType STRING_SORT_FIELDTYPE = new FieldType();

    private static final ReusableObjectCache<SimpleDateFormat>[] CACHED_DATE_FORMAT_PARSERS;

    static {
        STRING_SORT_FIELDTYPE.setIndexed(true);
        STRING_SORT_FIELDTYPE.setOmitNorms(true);
        STRING_SORT_FIELDTYPE.setIndexOptions(FieldInfo.IndexOptions.DOCS_ONLY);
        STRING_SORT_FIELDTYPE.setTokenized(true);
        STRING_SORT_FIELDTYPE.freeze();

        // Create parser caches for each date format (maximum capacity of 3
        // instances per format)
        CACHED_DATE_FORMAT_PARSERS = new ReusableObjectCache[SUPPORTED_DATE_FORMATS.length];

        for (int i = 0; i < SUPPORTED_DATE_FORMATS.length; i++) {
            CACHED_DATE_FORMAT_PARSERS[i] = new ReusableObjectArrayStackCache<SimpleDateFormat>(3);
        }
    }
    
    private ValueFactory valueFactory;
    private ICUCollationAttributeFactory caFactory;
    private Locale locale;

    @Override
    public void afterPropertiesSet() throws Exception {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        
        caFactory = new ICUCollationAttributeFactory(Collator.getInstance(locale));
    }
            
    /**
     * Specify indexing characteristics for field.
     * 
     * <p>Values:
     * <ul>
     *   <li>{@link #INDEXED} - Only indexed.
     *   <li>{@link #INDEXED_STORED} - Both indexed and stored.
     *   <li>{@link #INDEXED_LOWERCASE} - Lowercase-indexed, but not stored.
     *   <li>{@link #STORED} - Only stored (not searchable, but retrievable from docs).
     * </ul>
     */
    public enum FieldSpec {
        INDEXED,
        INDEXED_STORED,
        INDEXED_LOWERCASE,
        STORED
    }
    
    /**
     * Create special field used only for sorting string values in a localized
     * fashion. The field value will be encoded as a collation key using the
     * configured default locale.
     * 
     * @param name the field name
     * @param value the field value, a string.
     * @return 
     */
    public Field makeSortField(String name, String value) {
        if (caFactory != null) {
            // Use a "token stream", even though we only have one value. This is due to
            // Lucene API not allowing creation of raw binary indexable terms without going
            // through this path.
            StringArrayTokenStream ts = new StringArrayTokenStream(caFactory, value);
            return new Field(name, ts, STRING_SORT_FIELDTYPE);
        }
        
        // No configured locale
        return new StringField(name, value, Field.Store.NO);
    }

    /**
     * Create fields according to spec for a list of property values.
     * 
     * @param name
     * @param spec
     * @param values
     * @return 
     */
    public List<Field> makeFields(String name, FieldSpec spec, Value... values) {
        List<Field> fields = new ArrayList<Field>(values.length*2);
        for (int i=0; i<values.length; i++) {
            fields.addAll(makeFields(name, spec, values[i]));
        }
        return fields;
    }
    
    /**
     * Create fields according to spec for a list of value objects having the
     * given value type.
     * 
     * @param name
     * @param spec
     * @param values
     * @return 
     */
    public List<Field> makeFields(String name, FieldSpec spec, Type type, Object... values) {
        List<Field> fields = new ArrayList<Field>(values.length*2);
        for (int i=0; i<values.length; i++) {
            fields.addAll(makeFields(name, spec, type, values[i]));
        }
        return fields;
    }

    public List<Field> makeFields(String fieldName, FieldSpec spec, Value value) {
        switch (value.getType()) {
        case STRING:
        case HTML:
        case JSON:
        case IMAGE_REF:
        case BOOLEAN:
        case PRINCIPAL:
            String stringValue = value.getNativeStringRepresentation();
            return makeFields(fieldName, spec, value.getType(), stringValue);

        case LONG:
        case DATE:
        case TIMESTAMP:
            long longValue;
            if (value.getType() != Type.LONG) {
                longValue = value.getDateValue().getTime();
            } else {
                longValue = value.getLongValue();
            }
            return makeFields(fieldName, spec, value.getType(), longValue);
            
        case INT:
            Integer intValue = value.getIntValue();
            return makeFields(fieldName, spec, value.getType(), intValue);
            
        default:
            throw new IllegalArgumentException("Unsupported value type: " + value.getType());
        }
    }
    
    public List<Field> makeFields(String fieldName, FieldSpec spec, Type type, Object value) {
        List<Field> fields = new ArrayList<Field>(2);
        switch (type) {
        case STRING:
        case HTML:
        case JSON:
        case IMAGE_REF:
        case BOOLEAN:
        case PRINCIPAL:
            String stringValue = (String)value;
            if (isIndex(spec)) {
                if (isLowercase(spec)) {
                    stringValue = lowercase(stringValue);
                }
                fields.add(new StringField(fieldName, stringValue, Field.Store.NO));
            }
            if (isStore(spec)) {
                fields.add(new StoredField(fieldName, stringValue));
            }
            break;

        case LONG:
        case DATE:
        case TIMESTAMP:
            long longValue = (Long)value;
            if (isIndex(spec)) {
                long indexValue = type != Type.LONG ? 
                        DateTools.round(longValue, DateTools.Resolution.SECOND) : longValue;
                fields.add(new LongField(fieldName, indexValue, Field.Store.NO));
            }
            if (isStore(spec)) {
                fields.add(new StoredField(fieldName, longValue));
            }
            break;
            
        case INT:
            Integer intValue = (Integer)value;
            if (isIndex(spec)) {
                fields.add(new IntField(fieldName, intValue, Field.Store.NO));
            }
            if (isStore(spec)) {
                fields.add(new StoredField(fieldName, intValue));
            }
            break;

        default:
            throw new IllegalArgumentException("Unsupported value type: " + type);
        }
        
        return fields;
    }
    
    private boolean isIndex(FieldSpec spec) {
        return spec == FieldSpec.INDEXED 
                || spec == FieldSpec.INDEXED_LOWERCASE 
                || spec == FieldSpec.INDEXED_STORED;
    }
    
    private boolean isStore(FieldSpec spec) {
        return spec == FieldSpec.STORED || spec == FieldSpec.INDEXED_STORED;
    }
    
    private boolean isLowercase(FieldSpec spec) {
        return spec == FieldSpec.INDEXED_LOWERCASE;
    }
    
    /**
     * Create a (possibly encoded) query term for the given value object with the
     * given value type, optionally lowercasing. (Lowercasing is only applicable
     * to string value types.)
     * 
     * @param fieldName
     * @param value
     * @param type
     * @param lowercase
     * @return 
     */
    public Term queryTerm(String fieldName, Object value, Type type, boolean lowercase) {
        switch (type) {
        case STRING:
        case HTML:
        case JSON:
        case IMAGE_REF:
        case BOOLEAN:
        case PRINCIPAL:
            if (lowercase) {
                return new Term(fieldName, lowercase(value.toString()));
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
    
    // Lower case using default locale
    private String lowercase(String value) {
        if (locale != null){
            return value.toLowerCase(locale);
        } 
        
        return value.toLowerCase();
    }
    
    public Value[] valuesFromFields(Type type, List<IndexableField> fields) {
        Value[] values = new Value[fields.size()];
        for (int i=0; i<fields.size(); i++) {
            values[i] = valueFromField(type, fields.get(i));
        }
        return values;
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

    /**
     * Set locale to use for locale-specific sorting and lowercasing of
     * string values as index terms. When set, specialized sorting fields will
     * be encoded as collation keys at indexing time.
     * 
     * @param locale the locale instance to set.
     * 
     * Default value: <code>Locale.getDefault()</code>.
     */
    public void setLocale(Locale locale) {
        this.locale = locale;
    }
}

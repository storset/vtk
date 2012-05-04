/* Copyright (c) 2004, University of Oslo, Norway
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
package org.vortikal.repository;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import net.sf.json.JSONObject;

import org.vortikal.repository.resourcetype.ConstraintViolationException;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.resourcetype.PropertyType.Type;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.repository.resourcetype.ValueFormatException;
import org.vortikal.repository.resourcetype.ValueFormatter;
import org.vortikal.repository.resourcetype.ValueSeparator;
import org.vortikal.security.Principal;


/**
 * This class represents meta information about resources. A resource
 * may have several properties set on it, each of which are identified
 * by a namespace and a name. Properties may contain arbitrary string
 * values, such as XML. The application programmer is responsible for
 * the interpretation and processing of properties.
 * 
 * XXX: Fail in all getters if value not initialized ?
 */
public class PropertyImpl implements Serializable, Cloneable, Property {

    private static final long serialVersionUID = 3762531209208410417L;

    private static final Map<PropertyType.Type, Set<PropertyType.Type>> COMPATIBILITY_MAP;
    static {
        COMPATIBILITY_MAP = new EnumMap<Type, Set<Type>>(Type.class);

        Set<Type> STRING = EnumSet.noneOf(Type.class);
        STRING.add(Type.HTML);
        STRING.add(Type.IMAGE_REF);
        STRING.add(Type.JSON);
        COMPATIBILITY_MAP.put(Type.STRING, STRING);
        
        Set<Type> HTML = EnumSet.noneOf(Type.class);
        HTML.add(Type.STRING);
        HTML.add(Type.IMAGE_REF);
        HTML.add(Type.JSON);
        COMPATIBILITY_MAP.put(Type.HTML, HTML);

        Set<Type> IMAGE_REF = EnumSet.noneOf(Type.class);
        IMAGE_REF.add(Type.STRING);
        IMAGE_REF.add(Type.HTML);
        IMAGE_REF.add(Type.JSON);
        COMPATIBILITY_MAP.put(Type.IMAGE_REF, IMAGE_REF);

        Set<Type> JSON = EnumSet.noneOf(Type.class);
        JSON.add(Type.STRING);
        JSON.add(Type.HTML);
        JSON.add(Type.IMAGE_REF);
        COMPATIBILITY_MAP.put(Type.JSON, JSON);

        Set<Type> DATE = EnumSet.noneOf(Type.class);
        DATE.add(Type.TIMESTAMP);
        COMPATIBILITY_MAP.put(Type.DATE, DATE);

        Set<Type> TIMESTAMP = EnumSet.noneOf(Type.class);
        TIMESTAMP.add(Type.DATE);
        COMPATIBILITY_MAP.put(Type.TIMESTAMP, TIMESTAMP);
    }
    
    private PropertyTypeDefinition propertyTypeDefinition;
    private Value value;
    private Value[] values;
    
    @Override
    public Value getValue() {
        if (this.propertyTypeDefinition.isMultiple()) {
            throw new IllegalOperationException("Property " + this + " is multi-value"); 
        }
        
        return this.value;
    }

    @Override
    public void setValue(Value value) throws ValueFormatException {
        if (this.propertyTypeDefinition.isMultiple()) {
            throw new ValueFormatException("Property " + this + " is multi-value");
        }
        
        validateValue(value);
        this.value = value;
    }
    
    @Override
    public void setValues(Value[] values) throws ValueFormatException {
        if (! this.propertyTypeDefinition.isMultiple()) {
            throw new ValueFormatException("Property " + this + " is not multi-value");
        }
        
        validateValues(values);
        this.values = values;
    }
    
    @Override
    public Value[] getValues() {
        if (! this.propertyTypeDefinition.isMultiple()) {
            throw new IllegalOperationException("Property " + this + " is not multi-value");
        }
        
        return this.values;
    }

    @Override
    public Date getDateValue() throws IllegalOperationException {
        if (this.value == null || (getType() != PropertyType.Type.TIMESTAMP
                && getType() != PropertyType.Type.DATE)) {
            throw new IllegalOperationException("Property " + this + " not of type Date");
        }
        
        return this.value.getDateValue();
    }

    @Override
    public void setDateValue(Date dateValue) throws ValueFormatException {
        boolean date = false;
        if (getType() == PropertyType.Type.DATE) {
            date = true;
        }
        Value v = new Value(dateValue, date);
        setValue(v);
    }

    @Override
    public String getStringValue() throws IllegalOperationException {
        if (this.value == null) {
            throw new IllegalOperationException("Property " + this + " has a null value");
        }
        
        switch (getType()) {
        case STRING:
        case HTML:
        case IMAGE_REF:
        case JSON:
            return this.value.getStringValue();
        case BOOLEAN:
            return String.valueOf(this.value.getBooleanValue());
         
        default:
            throw new IllegalOperationException("Property " + this + " not a string type");
        }
    }

    @Override
    public void setStringValue(String stringValue) throws ValueFormatException {
        Value v = new Value(stringValue, PropertyType.Type.STRING);
        setValue(v);
    }
    
    @Override
    public void setLongValue(long longValue) throws ValueFormatException {
        Value v = new Value(longValue);
        setValue(v);
    }

    @Override
    public long getLongValue() throws IllegalOperationException {
        if (this.value == null || getType() != PropertyType.Type.LONG) {
            throw new IllegalOperationException("Property " + this + " not of type Long");
        }
        return this.value.getLongValue();
    }

    @Override
    public void setIntValue(int intValue) throws ValueFormatException {
        Value v = new Value(intValue);
        setValue(v);
    }

    @Override
    public int getIntValue() throws IllegalOperationException {
        if (this.value == null || getType() != PropertyType.Type.INT) {
            throw new IllegalOperationException("Property " + this + " not of type Integer");
        }
        return this.value.getIntValue();
    }
        
    @Override
    public boolean getBooleanValue() throws IllegalOperationException {
        if (this.value == null || getType() != PropertyType.Type.BOOLEAN) {
            throw new IllegalOperationException("Property " + this + " not of type Boolean");
        }
        return this.value.getBooleanValue();
    }

    @Override
    public void setBooleanValue(boolean booleanValue) throws ValueFormatException {
        Value v = new Value(booleanValue);
        setValue(v);
    }
    
    @Override
    public JSONObject getJSONValue() throws IllegalOperationException {
        if (this.value == null || getType() != PropertyType.Type.JSON) {
            throw new IllegalOperationException("Property " + this + " not of type JSON");
        }
        return this.value.getJSONValue();
    }
    
    @Override
    public void setJSONValue(JSONObject value) {
        Value v = new Value(value);
        setValue(v);
    }

    @Override
    public Principal getPrincipalValue() throws IllegalOperationException {
        if (this.value == null || getType() != PropertyType.Type.PRINCIPAL) {
            throw new IllegalOperationException("Property " + this + " not of type Principal");
        }
        return this.value.getPrincipalValue();
    }
    
    @Override
    public Type getType() {
        return this.propertyTypeDefinition.getType();
    }
    
    @Override
    public PropertyTypeDefinition getDefinition() {
        return this.propertyTypeDefinition;
    }
    
    @Override
    public void setPrincipalValue(Principal principalValue) throws ValueFormatException {
        Value v = new Value(principalValue);
        setValue(v);
    }
    
    @Override
    public Object clone() {
        PropertyImpl clone = new PropertyImpl();
        
        // "Dumb" clone, avoid all type checks, just copy data structures
        clone.propertyTypeDefinition = this.propertyTypeDefinition;
        
        // Values
        if (this.value != null) 
            clone.value = (Value)this.value.clone();

        if (this.values != null) {
            clone.values = new Value[this.values.length];
            // Need to deep-copy array of values
            for (int i=0; i<this.values.length; i++) {
                clone.values[i] = (Value)this.values[i].clone();
            }
        }
        clone.inherited = this.inherited;
        
        return clone;
    }

    // XXX has equals, but no hashCode
    //     We're probably getting away with that because we always look up these by namespace and name explicitly.
    @Override
    public boolean equals(Object obj) {
        if ((obj == null) || !(obj instanceof Property)) return false;
        
        Property otherProp = (Property) obj;
        
        if (! this.getDefinition().getName().equals(otherProp.getDefinition().getName()) || 
            ! this.getDefinition().getNamespace().equals(otherProp.getDefinition().getNamespace())) {
            return false;
        }
        
        if (this.propertyTypeDefinition.isMultiple()) {
            
            // Other prop must also be multiple, otherwise not equal
            if (otherProp.getDefinition() == null ||
                ! otherProp.getDefinition().isMultiple()) {
                return false;
            }
            
            Value[] otherValues = otherProp.getValues();
            
            // Other prop's value list must be equal
            if (this.values.length != otherValues.length) return false;
            
            for (int i=0; i<this.values.length; i++) {
                if (! this.values[i].equals(otherValues[i])) return false;
            }
            
            return true;
        }
        // This property is not multiple (or lacks def), other prop cannot be multiple
        if (otherProp.getDefinition() != null && otherProp.getDefinition().isMultiple()) {
            return false;
        }
        
        return this.value.equals(otherProp.getValue());
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(this.getClass().getName()).append(": ");
        sb.append("[ ").append(this.getDefinition().getNamespace());
        sb.append(":").append(this.getDefinition().getName());
        if (this.propertyTypeDefinition.isMultiple()) {
            sb.append(" = {");
            for (int i=0; values != null && i<this.values.length; i++) {
                sb.append("'").append(this.values[i]).append("'");
                if (i < this.values.length-1) 
                    sb.append(",");
            }
            sb.append("}");
        } else {
            sb.append(" = '").append(this.value).append("'");
        }
        
        sb.append("]");

        return sb.toString();
    }

    private void validateValues(Value[] values) throws ValueFormatException,
                                                ConstraintViolationException {
        if (values == null || values.length == 0) 
            throw new ValueFormatException("A property must have non null value");
        
        for (int i=0; i<values.length; i++) {
            validateValue(values[i]);
        }
    }

    
    private void validateValue(Value value) throws ValueFormatException,
                                                ConstraintViolationException {
        if (value == null) {
            throw new ValueFormatException("A property cannot have a null value");
        }
        
        
        if (value.getType() != getType()) {
            Set<Type> compatible = COMPATIBILITY_MAP.get(getType());
            if (compatible == null || !compatible.contains(value.getType())) {
                throw new ValueFormatException("Illegal value type " + 
                        value.getType() + 
                        " for value [" + value + "] on property " + this 
                        + ". Should be " +  getType());
            }
        }

        // Check for potential null values
        switch (value.getType()) {
        case PRINCIPAL:
            if (value.getPrincipalValue() == null) {
                throw new ValueFormatException(
                        "Principal value of property '" + this + "' cannot be null");
            }
            break;
        case STRING:
        case IMAGE_REF:
        case HTML:
        case JSON:
            if (value.getStringValue() == null) {
                throw new ValueFormatException(
                        "String value of property '" + this + "' cannot be null");
            }
            break;
        case DATE:
        case TIMESTAMP:
            if (value.getDateValue() == null) {
                throw new ValueFormatException(
                        "Date value of property '" + this + "' cannot be null");
            }
        }
        
        if (getType() == Type.JSON) {
            try {
                JSONObject.fromObject(value.getStringValue());
            } catch (Exception e) {
                throw new ValueFormatException(
                        "Value of property '" + this + "': invalid JSON object: " 
                        + value.getStringValue());
            }
        }
        
        Vocabulary<Value> vocabulary = this.propertyTypeDefinition.getVocabulary();
        if (vocabulary != null && vocabulary.getAllowedValues() != null) {
            List<Value> valuesList = Arrays.asList(vocabulary.getAllowedValues());
            if (!valuesList.contains(value)) {
                ConstraintViolationException e = 
                    new ConstraintViolationException(
                        "Value '" + value + "' not in list of allowed values for property '" + this);
                e.setStatusCode(ConstraintViolationException.NOT_IN_ALLOWED_VALUES);
                throw e;
            }
        }
        
    }
    

    public void setDefinition(PropertyTypeDefinition propertyTypeDefinition) {
        this.propertyTypeDefinition = propertyTypeDefinition;
    }
    
    @Override
    public boolean isValueInitialized() {
        if (this.propertyTypeDefinition.isMultiple()) {
            if (this.values == null) return false;
            for (Value v : this.values) {
                if (v == null) return false;
            }
            return true;
        }
        return this.value != null;
    }

    @Override
    public String getFormattedValue() {
        return getFormattedValue(null, null);
    }
    
    @Override
    public String getFormattedValue(String format, Locale locale) {

        if (!this.propertyTypeDefinition.isMultiple()) {
            return this.propertyTypeDefinition.getValueFormatter().valueToString(this.value, format, locale);
        }

        ValueFormatter formatter = this.propertyTypeDefinition.getValueFormatter();
        ValueSeparator separator = this.propertyTypeDefinition.getValueSeparator(format);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < this.values.length; i++) {
            Value value = this.values[i];
            sb.append(formatter.valueToString(value, format, locale));
            if (i < this.values.length - 2) {
                sb.append(separator.getIntermediateSeparator(value, locale));
            } else if (i == this.values.length - 2) {
                sb.append(separator.getFinalSeparator(value, locale));
            }
        } 
        return sb.toString();
    }
    
    @Override
    public void setBinaryValue(byte[] buffer, String contentType) {
        if (getType() != PropertyType.Type.BINARY) {
            throw new IllegalOperationException("Property " + this + " not of type BINARY");
        }
        setValue(new Value(buffer, contentType));
    }
    
    @Override
    public ContentStream getBinaryStream() throws IllegalOperationException {
    	if (this.value == null || getType() != PropertyType.Type.BINARY) {
            throw new IllegalOperationException("Property " + this + " not of type BINARY or is BINARY multi-value");
        }
        return this.value.getBinaryValue().getContentStream();
    }
    
    @Override
    public String getBinaryContentType() throws IllegalOperationException {
    	if (this.value == null || getType() != PropertyType.Type.BINARY) {
            throw new IllegalOperationException("Property " + this + " not of type BINARY or is BINARY multi-value");
        }
        return this.value.getBinaryValue().getContentType();
    }
    
    private boolean inherited = false;
    public boolean isInherited() {
        return this.inherited;
    }
    public void setInherited(boolean inherited) {
        this.inherited = inherited;
    }

}

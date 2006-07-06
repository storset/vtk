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
package org.vortikal.repositoryimpl;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.vortikal.repository.IllegalOperationException;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Property;
import org.vortikal.repository.resourcetype.Constraint;
import org.vortikal.repository.resourcetype.ConstraintViolationException;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.repository.resourcetype.ValueFormatException;
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
public class PropertyImpl implements java.io.Serializable, Cloneable, Property {

    private static final long serialVersionUID = 3762531209208410417L;
    
    private PropertyTypeDefinition propertyTypeDefinition;
    private Namespace namespace;
    private String name;
    private Value value;
    private Value[] values;
    
    public Namespace getNamespace() {
        return this.namespace;
    }

    public void setNamespace(Namespace namespace) {
        this.namespace = namespace;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Value getValue() {
        if (this.propertyTypeDefinition != null &&
            this.propertyTypeDefinition.isMultiple()) {
            throw new IllegalOperationException("Property " + this + " is multi-value"); 
        }
        
        return this.value;
    }

    public void setValue(Value value) throws ValueFormatException {
        if (this.propertyTypeDefinition != null &&
            this.propertyTypeDefinition.isMultiple()) {
            throw new ValueFormatException("Property " + this + " is multi-value");
        }
        
        validateValue(value);
        this.value = value;
    }
    
    public void setValues(Value[] values) throws ValueFormatException {
        if (this.propertyTypeDefinition == null ||
            ! this.propertyTypeDefinition.isMultiple()) {
            throw new ValueFormatException("Property " + this + " is not multi-value");
        }
        
        validateValues(values);
        this.values = values;
    }
    
    public Value[] getValues() {
        if (this.propertyTypeDefinition == null || 
            ! this.propertyTypeDefinition.isMultiple()) {
            throw new IllegalOperationException("Property " + this + " is not multi-value");
        }
        
        return this.values;
    }

    public Date getDateValue() throws IllegalOperationException {
        if (this.value == null || getType() != PropertyType.TYPE_DATE) {
            throw new IllegalOperationException("Property " + this + " not of type Date");
        }
        
        return this.value.getDateValue();
    }

    public void setDateValue(Date dateValue) throws ValueFormatException {
        Value v = new Value(dateValue);
        setValue(v);
    }

    public String getStringValue() throws IllegalOperationException {
        if (this.value == null || getType() != PropertyType.TYPE_STRING) {
            throw new IllegalOperationException("Property " + this + " not of type String");
        }
        return this.value.getStringValue();
    }

    public void setStringValue(String stringValue) throws ValueFormatException {
        Value v = new Value(stringValue);
        setValue(v);
    }
    
    public void setLongValue(long longValue) throws ValueFormatException {
        Value v = new Value(longValue);
        setValue(v);
    }

    public long getLongValue() throws IllegalOperationException {
        if (this.value == null || getType() != PropertyType.TYPE_LONG) {
            throw new IllegalOperationException("Property " + this + " not of type Long");
        }
        return this.value.getLongValue();
    }

    public void setIntValue(int intValue) throws ValueFormatException {
        Value v = new Value(intValue);
        setValue(v);
    }

    public int getIntValue() throws IllegalOperationException {
        if (this.value == null || getType() != PropertyType.TYPE_INT) {
            throw new IllegalOperationException("Property " + this + " not of type Integer");
        }
        return this.value.getIntValue();
    }
        
    public boolean getBooleanValue() throws IllegalOperationException {
        if (this.value == null || getType() != PropertyType.TYPE_BOOLEAN) {
            throw new IllegalOperationException("Property " + this + " not of type Boolean");
        }
        return this.value.getBooleanValue();
    }

    public void setBooleanValue(boolean booleanValue) throws ValueFormatException {
        Value v = new Value(booleanValue);
        setValue(v);
    }

    public Principal getPrincipalValue() throws IllegalOperationException {
        if (this.value == null || getType() != PropertyType.TYPE_PRINCIPAL) {
            throw new IllegalOperationException("Property " + this + " not of type Principal");
        }
        return this.value.getPrincipalValue();
    }
    
    public void setPrincipalValue(Principal principalValue) throws ValueFormatException {
        Value v = new Value(principalValue);
        setValue(v);
    }
    
    public Object clone() {
        PropertyImpl clone = new PropertyImpl();
        
        // "Dumb" clone, avoid all type checks, just copy data structures
        clone.propertyTypeDefinition = this.propertyTypeDefinition;
        clone.name = this.name;
        clone.namespace = this.namespace; // XXX: Do we need to clone namespace ? 
        
        // Values
        clone.value = (Value)this.value.clone();
        clone.values = new Value[this.values.length];
        
        // Need to deep-copy array of values
        for (int i=0; i<this.values.length; i++) {
            clone.values[i] = (Value)this.values[i].clone();
        }
        
        return clone;
    }

    public boolean equals(Object obj) {
        if ((obj == null) || !(obj instanceof Property)) return false;
        
        Property otherProp = (Property) obj;
        
        if (! this.name.equals(otherProp.getName()) || 
            ! this.namespace.equals(otherProp.getNamespace())) {
            return false;
        }
        
        if (this.propertyTypeDefinition != null &&
            this.propertyTypeDefinition.isMultiple()) {
            
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
    
    public String toString() {
        StringBuffer sb = new StringBuffer();

        sb.append(this.getClass().getName()).append(": ");
        sb.append("[ ").append(this.namespace);
        sb.append(":").append(this.name);
        if (this.propertyTypeDefinition != null && this.propertyTypeDefinition.isMultiple()) {
            sb.append(" = {");
            for (int i=0; i<this.values.length; i++) {
                sb.append("'").append(this.values[i]).append("'");
                if (i < this.values.length-1) sb.append(",");
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
            throw new ValueFormatException("A property must have non null value");
        }
        
        if (value.getType() != getType()) {
            throw new ValueFormatException("Illegal value type " + 
                    PropertyType.PROPERTY_TYPE_NAMES[value.getType()] + 
                    " for property " + this + ". Should be " + 
                    PropertyType.PROPERTY_TYPE_NAMES[getType()]);
        }
        
        // Check for potential null values
        switch (value.getType()) {
        case PropertyType.TYPE_PRINCIPAL:
            if (value.getPrincipalValue() == null) {
                throw new ValueFormatException(
                    "Principal value of property '" + this + "' cannot be null");
            }
            break;
        case PropertyType.TYPE_STRING:
            if (value.getStringValue() == null) {
                throw new ValueFormatException(
                    "String value of property '" + this + "' cannot be null");
            }
            break;
        case PropertyType.TYPE_DATE:
            if (value.getDateValue() == null) {
                throw new ValueFormatException(
                    "Date value of property '" + this + "' cannot be null");
            }
        }
        
        // XXX: Do we want this, or should the client check by itself ?
        if (this.propertyTypeDefinition == null) {
            return;
        }
        
        if (this.propertyTypeDefinition.getConstraint() != null) {
            Constraint constraint = this.propertyTypeDefinition.getConstraint();
            constraint.validate(value);
        }

        if (this.propertyTypeDefinition.getAllowedValues() != null) {
            List valuesList = Arrays.asList(this.propertyTypeDefinition.getAllowedValues());
            if (!valuesList.contains(value)) {
                ConstraintViolationException e = 
                    new ConstraintViolationException(
                        "Value not in list of allowed values for property '" + this);
                e.setStatusCode(ConstraintViolationException.NOT_IN_ALLOWED_VALUES);
                throw e;
            }
        }
        
    }
    
    public int getType() {
        if (this.propertyTypeDefinition == null)
            return PropertyType.TYPE_STRING;
        
        return this.propertyTypeDefinition.getType();
    }
    
    public PropertyTypeDefinition getDefinition() {
        return this.propertyTypeDefinition;
    }

    public void setDefinition(PropertyTypeDefinition propertyTypeDefinition) {
        this.propertyTypeDefinition = propertyTypeDefinition;
    }
    
    public boolean isValueInitialized() {
        if (this.propertyTypeDefinition != null && this.propertyTypeDefinition.isMultiple()) {
            return this.values != null;
        }
        return this.value != null;
    }
    

}

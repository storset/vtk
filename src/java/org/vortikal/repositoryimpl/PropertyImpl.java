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

import java.util.Date;

import org.vortikal.repository.IllegalOperationException;
import org.vortikal.repository.Property;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.repository.resourcetype.ValueFormatException;


/**
 * This class represents meta information about resources. A resource
 * may have several properties set on it, each of which are identified
 * by a namespace and a name. Properties may contain arbitrary string
 * values, such as XML. The application programmer is responsible for
 * the interpretation and processing of properties.
 */
public class PropertyImpl implements java.io.Serializable, Cloneable, Property {

    private static final long serialVersionUID = 3762531209208410417L;
    
    private PropertyTypeDefinition propertyTypeDefinition;
    private String namespaceUri;
    private String name;
    private Value value;

    public PropertyImpl() {
        value = new Value();
    }
    
    public int getType() {
        if (this.propertyTypeDefinition == null)
            return PropertyType.TYPE_STRING;
        return this.propertyTypeDefinition.getType();
    }
    
    public String getNamespace() {
        return this.namespaceUri;
    }

    public void setNamespace(String namespace) {
        this.namespaceUri = namespace;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Value getValue() {
        return this.value;
    }

    private void validateValue(Value value) throws ValueFormatException  {

        if (value == null) return; // XXX: desired behaviour ?
        
        if (value.getType() != getType()) {
            throw new ValueFormatException("Illegal value type " + 
                    PropertyType.PROPERTY_TYPE_NAMES[value.getType()] + 
                    " for property " + this.name + ". Should be " + 
                    PropertyType.PROPERTY_TYPE_NAMES[getType()]);
        }
    }
    
    public void setValue(Value value) {
        validateValue(value);
        this.value = value;
    }

    public Date getDateValue() throws IllegalOperationException {
        if (value == null || getType() != PropertyType.TYPE_DATE) {
            throw new IllegalOperationException();
        }
        
        return value.getDateValue();
    }

    public void setDateValue(Date dateValue) throws ValueFormatException {
        Value v = new Value();
        v.setDateValue(dateValue);
        validateValue(v);
        this.value = v;
    }

    public String getStringValue() throws IllegalOperationException {
        if (value == null || getType() != PropertyType.TYPE_STRING) {
            throw new IllegalOperationException();
        }
        return value.getValue();
    }

    public void setStringValue(String stringValue) throws ValueFormatException {
        Value v = new Value();
        v.setValue(stringValue);
        validateValue(v);
        this.value = v;
    }
    
    public void setLongValue(long longValue) throws ValueFormatException {
        Value v = new Value();
        v.setLongValue(longValue);
        validateValue(v);
        this.value = v;
    }

    public long getLongValue() throws IllegalOperationException {
        if (value == null || (propertyTypeDefinition != null && 
                propertyTypeDefinition.getType() != PropertyType.TYPE_LONG)) {
            throw new IllegalOperationException();
        }
        return value.getLongValue();
    }

    public void setIntValue(int intValue) throws ValueFormatException {
        Value v = new Value();
        v.setIntValue(intValue);
        validateValue(v);
        this.value = v;
    }

    public int getIntValue() throws IllegalOperationException {
        if (value == null || getType() != PropertyType.TYPE_INT) {
            throw new IllegalOperationException();
        }
        return value.getIntValue();
    }
        
    public boolean getBooleanValue() throws IllegalOperationException {
        if (value == null || getType() != PropertyType.TYPE_BOOLEAN) {
            throw new IllegalOperationException();
        }
        return value.getBooleanValue();
    }

    public void setBooleanValue(boolean booleanValue) throws ValueFormatException {
        Value v = new Value();
        v.setBooleanValue(booleanValue);
        validateValue(v);
        this.value = v;
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();

        sb.append(this.getClass().getName()).append(": ");
        sb.append("[ ").append(this.namespaceUri);
        sb.append(":").append(this.name);
        sb.append(" = ").append(this.value);
        sb.append("]");

        return sb.toString();
    }

    public PropertyTypeDefinition getDefinition() {
        return propertyTypeDefinition;
    }

    public void setDefinition(PropertyTypeDefinition propertyTypeDefinition) {
        this.propertyTypeDefinition = propertyTypeDefinition;
    }
    
}

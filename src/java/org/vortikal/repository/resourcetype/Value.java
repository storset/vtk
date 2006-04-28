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
package org.vortikal.repository.resourcetype;

import java.util.Date;

import org.vortikal.security.Principal;


public final class Value implements Cloneable {
    private int type = PropertyType.TYPE_STRING;

    private String value = "";
    private Date dateValue;
    private boolean booleanValue;
    private int intValue;
    private long longValue;
    private Principal principalValue;

    public void setValue(String value) {
        this.type = PropertyType.TYPE_STRING;
        this.value = value;
    }
    
    public void setBooleanValue(boolean booleanValue) {
        this.type = PropertyType.TYPE_BOOLEAN;
        this.booleanValue = booleanValue;
    }

    public void setDateValue(Date dateValue) {
        this.type = PropertyType.TYPE_DATE;
        this.dateValue = dateValue;
    }

    public void setLongValue(long longValue) {
        this.type = PropertyType.TYPE_LONG;
        this.longValue = longValue;
    }
    
    public void setIntValue(int intValue) {
        this.type = PropertyType.TYPE_INT;
        this.intValue = intValue;
    }
    
    public void setPrincipalValue(Principal principalValue) {
        this.type = PropertyType.TYPE_PRINCIPAL;
        this.principalValue = principalValue;
    }

    public String getValue() {
        return value;
    }
    
    public int getType() {
        return type;
    }
    
    public boolean getBooleanValue() {
        return booleanValue;
    }

    public Date getDateValue() {
        return dateValue;
    }
    
    public long getLongValue() {
        return this.longValue;
    }
    
    public int getIntValue() {
        return this.intValue;
    }
    
    public Principal getPrincipalValue() {
        return this.principalValue;
    }

    // XXX: Use hashCode?
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof Value)) {
            return false;
        }
        
        if (obj == this) return true;
        
        Value v = (Value) obj;
        
        if (this.type != v.getType()) 
            return false;
        
        switch (this.type) {
        case PropertyType.TYPE_BOOLEAN:
            return (this.booleanValue == v.getBooleanValue());
        case PropertyType.TYPE_INT:
            return (this.intValue == v.getIntValue());
        case PropertyType.TYPE_LONG:
            return (this.longValue == v.getLongValue());
        case PropertyType.TYPE_DATE:
            return (this.dateValue == null && v.getDateValue() == null) ||
                (this.dateValue != null && this.dateValue.equals(v.getDateValue()));
        case PropertyType.TYPE_PRINCIPAL:
            return (this.principalValue == null && v.getPrincipalValue() == null) ||
                (this.principalValue != null && this.principalValue.equals(v.getPrincipalValue()));
        default:
            return (this.value == null && v.getValue() == null) ||
                (this.value != null && this.value.equals(v.getValue()));
        }
    }
    
    public int hashCode() {
        int hash = 7;
        
        hash  = hash * 31 + this.type;
        switch (this.type) {
        case PropertyType.TYPE_BOOLEAN:
            return hash + (this.booleanValue ? 1231 : 1237);
        case PropertyType.TYPE_INT:
            return hash + this.intValue;   
        case PropertyType.TYPE_LONG:
            return hash + (int)(this.longValue ^ (this.longValue >>> 32));
        case PropertyType.TYPE_DATE:    
            return hash + (this.dateValue == null ? 0 : this.dateValue.hashCode());
        case PropertyType.TYPE_PRINCIPAL:
            return hash + (this.principalValue == null ? 0 : this.principalValue.hashCode());
            
        default:
            return hash + (this.value == null ? 0 : this.value.hashCode());
        }
    }
    
    public Object clone() throws CloneNotSupportedException {
        // XXX: Clone only the field corresponding to the value's type ?
        Value clone = new Value();
        clone.type = this.type;
        clone.dateValue = this.dateValue != null ? (Date)this.dateValue.clone() : null;
        clone.booleanValue = this.booleanValue;
        clone.intValue = this.intValue;
        clone.longValue = this.longValue;
        clone.value = this.value;
        clone.principalValue = this.principalValue;
        
        return clone;
    }
    
    public String toString() {
        StringBuffer sb = new StringBuffer();

        switch (this.type) {
            case PropertyType.TYPE_STRING:
                sb.append(this.value);
                break;
            case PropertyType.TYPE_INT:
                sb.append(this.intValue);
                break;
            case PropertyType.TYPE_LONG:
                sb.append(this.longValue);
                break;
            case PropertyType.TYPE_DATE:
                sb.append(this.dateValue);
                break;
            case PropertyType.TYPE_BOOLEAN:
                sb.append(this.booleanValue);
                break;
            case PropertyType.TYPE_PRINCIPAL:
                sb.append(this.principalValue);
                break;
            default:
                sb.append(this.value);
                break;
        }
        return sb.toString();
    }
    
    public String getNativeStringRepresentation() {
        
        String representation = null;
        switch (this.type) {
        
        case PropertyType.TYPE_BOOLEAN:
            representation = this.booleanValue ? "true" : "false";
            break;
            
        case PropertyType.TYPE_DATE:
            Date date = this.dateValue;
            
            if (date == null) {
                throw new ValueFormatException("Cannot convert date value to string, field was null");
            }
            
            representation = Long.toString(date.getTime());
            break;
            
        case PropertyType.TYPE_INT:
            representation = Integer.toString(intValue);
            break;
            
        case PropertyType.TYPE_LONG:
            representation = Long.toString(longValue);
            break;
            
        case PropertyType.TYPE_STRING:
            representation = value;
            break;
            
        case PropertyType.TYPE_PRINCIPAL:
            Principal principal = principalValue;
            if (principal == null) {
                throw new ValueFormatException("Cannot convert principal value to string, field was null");
            }
            
            representation = principal.getQualifiedName();
        }
        
        return representation;
    }
}

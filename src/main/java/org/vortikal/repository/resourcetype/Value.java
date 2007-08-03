/* Copyright (c) 2006, 2007, University of Oslo, Norway
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


public final class Value implements Cloneable, Comparable<Value> {
    
    private int type = PropertyType.TYPE_STRING;
    private static final long MAX_LENGTH = 2048;

    private String stringValue;
    private Date dateValue;
    private boolean booleanValue;
    private int intValue;
    private long longValue;
    private Principal principalValue;

    public Value(String stringValue) {
        if (stringValue == null)
            throw new IllegalArgumentException("Value object cannot be null");
        if (stringValue.length() > MAX_LENGTH) {
            throw new ValueFormatException(
                "String value too large: " + stringValue.length() + " (max size = "
                + MAX_LENGTH + ")");
        }

        this.type = PropertyType.TYPE_STRING;
        this.stringValue = stringValue;
    }
    
    public Value(boolean booleanValue) {
        this.type = PropertyType.TYPE_BOOLEAN;
        this.booleanValue = booleanValue;
    }

    public Value(Date dateValue) {
        if (dateValue == null)
            throw new IllegalArgumentException("Value object cannot be null");
        this.type = PropertyType.TYPE_DATE;
        this.dateValue = (Date)dateValue.clone();
    }

    public Value(long longValue) {
        this.type = PropertyType.TYPE_LONG;
        this.longValue = longValue;
    }
    
    public Value(int intValue) {
        this.type = PropertyType.TYPE_INT;
        this.intValue = intValue;
    }
    
    public Value(Principal principalValue) {
        if (principalValue == null)
            throw new IllegalArgumentException("Value object cannot be null");
        String qualifiedName = principalValue.getQualifiedName();
        if (qualifiedName.length() > MAX_LENGTH) {
            throw new ValueFormatException(
                "Princpal name too long: " + qualifiedName.length() + " (max size = "
                + MAX_LENGTH + ")");
        }

        this.type = PropertyType.TYPE_PRINCIPAL;
        this.principalValue = principalValue;
    }

    public int getType() {
        return this.type;
    }
    
    public boolean getBooleanValue() {
        return this.booleanValue;
    }

    public Date getDateValue() {
        return (Date)this.dateValue.clone();
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

    public String getStringValue() {
        return this.stringValue;
    }
 
        
   public Object getObjectValue() {
        switch (this.type) {
        
            case PropertyType.TYPE_BOOLEAN:
                return Boolean.valueOf(this.booleanValue);
            
            case PropertyType.TYPE_DATE:
                return this.dateValue.clone();
            
            case PropertyType.TYPE_INT:
                return new Integer(this.intValue);
            
            case PropertyType.TYPE_LONG:
                return new Long(this.longValue);

            case PropertyType.TYPE_STRING:
                return this.stringValue;
            
            case PropertyType.TYPE_PRINCIPAL:
                return this.principalValue;
        }
        
        throw new IllegalStateException(
            "Unable to return value: Illeal type: " + this.type);
    }
    

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
            return (this.stringValue == null && v.getStringValue() == null) ||
                (this.stringValue != null && this.stringValue.equals(v.getStringValue()));
        }
    }
    
    public int hashCode() {
        int hash  = 7 * 31 + this.type;

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
            return hash + (this.stringValue == null ? 0 : this.stringValue.hashCode());
        }
    }
    
    public Object clone() {

        switch (this.type) {
        case PropertyType.TYPE_BOOLEAN:
            return new Value(this.booleanValue);
        case PropertyType.TYPE_INT:
            return new Value(this.intValue);   
        case PropertyType.TYPE_LONG:
            return new Value(this.longValue);
        case PropertyType.TYPE_DATE:    
            return new Value((Date)this.dateValue.clone());
        case PropertyType.TYPE_PRINCIPAL:
            return new Value(this.principalValue);
        default:
            return new Value(this.stringValue);
        }
    }
    
    public int compareTo(Value other) {
        if (this.type != other.type) {
            throw new IllegalArgumentException("Values not of same type");
        }
        switch (this.type) {
        case PropertyType.TYPE_BOOLEAN:
            return Boolean.valueOf(this.booleanValue).compareTo(other.booleanValue);
        case PropertyType.TYPE_INT:
            return Integer.valueOf(this.intValue).compareTo(other.intValue);
        case PropertyType.TYPE_LONG:
            return Long.valueOf(this.longValue).compareTo(other.longValue);
        case PropertyType.TYPE_DATE:    
            return this.dateValue.compareTo(other.dateValue);
        case PropertyType.TYPE_PRINCIPAL:
            return this.principalValue.getQualifiedName().compareTo(
                other.principalValue.getQualifiedName());
        default:
            return this.stringValue.compareTo(other.stringValue);
        }
    }
    

    public String toString() {
        StringBuffer sb = new StringBuffer();

        switch (this.type) {
            case PropertyType.TYPE_STRING:
                sb.append(this.stringValue);
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
                sb.append(this.stringValue);
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
                throw new ValueFormatException(
                    "Cannot convert date value to string, field was null");
            }
            
            representation = Long.toString(date.getTime());
            break;
            
        case PropertyType.TYPE_INT:
            representation = Integer.toString(this.intValue);
            break;
            
        case PropertyType.TYPE_LONG:
            representation = Long.toString(this.longValue);
            break;
            
        case PropertyType.TYPE_STRING:
            representation = this.stringValue;
            break;
            
        case PropertyType.TYPE_PRINCIPAL:
            Principal principal = this.principalValue;
            if (principal == null) {
                throw new ValueFormatException(
                    "Cannot convert principal value to string, field was null");
            }
            
            representation = principal.getQualifiedName();
        }
        
        return representation;
    }
}

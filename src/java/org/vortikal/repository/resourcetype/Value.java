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

import org.vortikal.repository.Property;


public final class Value {

    private int type = PropertyType.TYPE_STRING;

    private String value;
    private Date dateValue;
    private boolean booleanValue;
    private int intValue;
    private long longValue;

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

    public boolean equals(Object obj) {
        if (! (obj instanceof Value)) 
            return false;

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
        default:
            return (this.value == null && v.getValue() == null) ||
                (this.value != null && this.value.equals(v.getValue()));
        }
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
            default:
                sb.append(this.value);
                break;
        }
        return sb.toString();
    }
    
}

/* Copyright (c) 2009, University of Oslo, Norway
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
package org.vortikal.text.tl;

public class Literal implements Argument {

    public enum Type {
        String, Integer, Boolean;
    }
    
    private String rawValue;
    private Type type;
    private Object value;
    
    public Literal(String rawValue) {
        this.rawValue = rawValue;
        String stringVal = getStringValue(rawValue);
        if (stringVal != null) {
            this.type = Type.String;
            this.value = stringVal;
            return;
        }
        Integer intVal = getIntegerValue(rawValue);
        if (intVal != null) {
            this.type = Type.Integer;
            this.value = intVal;
            return;
        }
        Boolean boolVal = getBooleanValue(rawValue);
        if (boolVal != null) {
            this.type = Type.Boolean;
            this.value = boolVal;
            return;
        }
        throw new IllegalArgumentException("Unsupported value type: " + value);
    }
    
    public String getRawValue() {
        return this.rawValue;
    }
    
    public Object getValue(Context ctx) {
        return this.value;
    }
    
    public String getStringValue() {
        if (this.type != Type.String) {
            throw new IllegalStateException("Literal not of type String: " + this.value);
        }
        return (String) this.value;
    }

    public Integer getIntValue() {
        if (this.type != Type.Integer) {
            throw new IllegalStateException("Literal not of type Integer: " + this.value);
        }
        return (Integer) this.value;
    }

    public Boolean getBoolValue() {
        if (this.type != Type.Boolean) {
            throw new IllegalStateException("Literal not of type Boolean: " + this.value);
        }
        return (Boolean) this.value;
    }
    
    public Type getType() {
        return this.type;
    }
    
    private String getStringValue(String token) {
        if (token.startsWith("\"") && token.endsWith("\"")) {
            return token.substring(1, token.length() - 1);

        } else if (token.startsWith("'") && token.endsWith("'")) {
            return token.substring(1, token.length() - 1);
        }
        return null;
    }
    
    private Integer getIntegerValue(String token) {
        try {
            return Integer.valueOf(Integer.parseInt(token));
        } catch (NumberFormatException e) { 
            return null;
        }
    }
    
    private Boolean getBooleanValue(String token) {
        if ("true".equals(token)) {
            return Boolean.TRUE;
        } else if ("false".equals(token)) {
            return Boolean.FALSE;
        }
        return null;
    }
    
    public String toString() {
        return "literal:" + this.value;
    }
}

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

import java.math.BigDecimal;

public class Literal implements Token {

    public enum Type {
        STRING, NUMBER, BOOLEAN;
    }
    
    private String rawValue;
    private Type type;
    private Object value;
    
    /**
     * Literal: "string", 'string', true, false, <number>
     * @param rawValue
     */
    public Literal(String rawValue) {
        if (rawValue == null) {
            throw new IllegalArgumentException("Null");
        }
        if (rawValue.length() == 0) {
            throw new IllegalArgumentException("Empty value");
        }
        this.rawValue = rawValue;
        
        String stringValue = getStringValue(rawValue);
        if (stringValue != null) {
            this.type = Type.STRING;
            this.value = stringValue;
            return;
        }
        
        Boolean booleanValue = getBooleanValue(rawValue);
        if (booleanValue != null) {
            this.type = Type.BOOLEAN;
            this.value = booleanValue;
            return;
        }
        
        Number numberValue = getNumberValue(rawValue);
        if (numberValue != null) {
            this.type = Type.NUMBER;
            this.value = numberValue;
            return;
        }
        
        throw new IllegalArgumentException("Unsupported value type: " + rawValue);
    }
    
    public String getRawValue() {
        return this.rawValue;
    }
    
    public Object getValue(Context ctx) {
        return this.value;
    }
    
    public String getStringValue() {
        if (this.type != Type.STRING) {
            throw new IllegalStateException("Literal not of type String: " + this.value);
        }
        return (String) this.value;
    }

    public Number getNumberValue() {
        if (this.type != Type.NUMBER) {
            throw new IllegalStateException("Literal not of type Integer: " + this.value);
        }
        return (Number) this.value;
    }

    public Boolean getBoolValue() {
        if (this.type != Type.BOOLEAN) {
            throw new IllegalStateException("Literal not of type Boolean: " + this.value);
        }
        return (Boolean) this.value;
    }
    
    public Type getType() {
        return this.type;
    }
    
    private String getStringValue(String token) {
        if (!(token.startsWith("\"") || token.startsWith("'"))) {
            return null;
        }
        if (token.length() == 1) {
            throw new IllegalArgumentException("Unterminated string: " + rawValue);
        }
        StringBuilder sb = new StringBuilder();
        int len = token.length();
        boolean escape = false;
        for (int i = 1; i < len - 1; i++) {
            char c = token.charAt(i);
            if (c == '\\' && !escape) {
                escape = true;
                continue;
            }
            if (escape) {
                if (!(c == '\\' || c == '\'' || c == '"')) {
                    throw new IllegalArgumentException("Illegal escape sequence: \\" + c 
                            + " in string: " + token);
                }
            }
            sb.append(c);
            escape = false;
        }
        if (escape) {
            throw new IllegalArgumentException("Unterminated escape sequence in string: " + token);
        }
        if (token.charAt(0) != token.charAt(len - 1)) {
            throw new IllegalArgumentException("Unterminated string: " + token);
        }
        return sb.toString();
    }
    
    private Number getNumberValue(String token) {
        try {
            BigDecimal d = new BigDecimal(token);
            if (d.scale() == 0) {
                return d.intValue();
            } else {
                return d.floatValue();
            }
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.type.hashCode();
        result = prime * result + this.value.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Literal other = (Literal) obj;
        if (!this.type.equals(other.type)) {
            return false;
        }
        if (!this.value.equals(other.value)) {
            return false;
        }
        return true;
    }
    
}

/* Copyright (c) 2007, University of Oslo, Norway
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

import java.util.Enumeration;
import java.util.Locale;
import java.util.ResourceBundle;

import org.springframework.context.support.ResourceBundleMessageSource;

public class MessageSourceValueFormatter implements ValueFormatter {

    private String baseName;
    private ReversableMessageSource messageSource;
    private String keyPrefix = "value.";
    private String unsetKey = "unset";
    private PropertyType.Type type;

    
    public MessageSourceValueFormatter(String messageSourceBaseName, PropertyType.Type type) {
        this.baseName = messageSourceBaseName;
        ReversableMessageSource messageSource = new ReversableMessageSource();
        messageSource.setBasename(messageSourceBaseName);

        this.messageSource = messageSource;
        this.type = type;
    }

    public String valueToString(Value value, String format, Locale locale)
            throws IllegalValueTypeException {
        if ("localized".equals(format)) {
            if (value == null || "".equals(value.toString())) {
                return this.messageSource.getMessage(this.unsetKey, null, "unset", locale);
            }
            
            return this.messageSource.getMessage(keyPrefix + value.toString(), null, value.toString(), locale);
        }
        return value.toString();
    }

    public Value stringToValue(String string, String format, Locale locale) {
        if (string == null) {
            throw new IllegalArgumentException("Cannot get value for 'null' formatted value");
        }
        
        if (!"localized".equals(format)) {
            return stringToValueInternal(string);
        }
        
        String value = this.messageSource.getKeyFromMessage(string, locale);
        if (value == null) {
            throw new IllegalArgumentException("Unknown formatted value: " + string);
        }
        
        if (this.unsetKey.equals(value)) {
            return null;
        }
        return stringToValueInternal(value);
    }

    private Value stringToValueInternal(String stringValue) {
        
        switch (this.type) {
        case BOOLEAN:
            return new Value(Boolean.parseBoolean(stringValue));
            
        case INT:
            return new Value(Integer.parseInt(stringValue));

        default:
            return new Value(stringValue, PropertyType.Type.STRING);
        }
    }
    
    private class ReversableMessageSource extends ResourceBundleMessageSource {
        
        public String getKeyFromMessage(String value, Locale locale) {
            ResourceBundle resourceBundle = getResourceBundle(baseName, locale);
            Enumeration<String> keys = resourceBundle.getKeys();
            while (keys.hasMoreElements()) {
                String key = keys.nextElement();
                String keyValue = resourceBundle.getString(key);
                if (keyValue.equals(value)) {
                    return key;
                }
            }
            return null;
        }
    }
}

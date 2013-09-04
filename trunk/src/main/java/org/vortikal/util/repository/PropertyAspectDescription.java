/* Copyright (c) 2010, University of Oslo, Norway
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
package org.vortikal.util.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;


public class PropertyAspectDescription {
    private List<PropertyAspectField> fields = new ArrayList<PropertyAspectField>();
    private Map<?,?> config;
    private Throwable error = null;

    public PropertyAspectDescription(List<PropertyAspectField> fields) {
        this.fields = fields;
    }
    
    public PropertyAspectDescription(Map<?, ?> config) {
        this(config, false);
    }

    public PropertyAspectDescription(Map<?, ?> config, boolean deferValidation) {
        if (!deferValidation) {
            init(config);
            return;
        }
        try {
            init(config);
        } catch (Throwable t) {
            this.error = t;
        }
    }
    
    public void reload() {
        try {
            init(this.config);
        } catch (Throwable t) {
            this.error = t;
        }
    }
    
    public Throwable getError() {
        return this.error;
    }
    
    public List<PropertyAspectField> getFields() {
        if (this.error != null) {
            throw new RuntimeException(this.error.getMessage(), this.error);
        }
        return Collections.unmodifiableList(this.fields);
    }
    
    public PropertyAspectField getNamedField(String name) {
        if (this.error != null) {
            throw new RuntimeException(this.error.getMessage(), this.error);
        }
        for (PropertyAspectField field : this.fields) {
            if (field.getIdentifier().equals(name)) {
                return field;
            }
         }
        return null;
    }

    private void init(Map<?, ?> config) {
        if (config == null) {
            throw new IllegalArgumentException("Configuration is NULL");
        }
        this.config = config;
        this.fields = new ArrayList<PropertyAspectField>();
        
        for (Object identifier: config.keySet()) {
            Object o = config.get(identifier);
            if (!(o instanceof Map<?, ?>)) {
                throw new IllegalArgumentException("Invalid configuration: not a map: " + config);
            }
            Map<?, ?> descriptor = (Map<?, ?>) o;
            o = descriptor.get("type");
            if (o == null) {
                throw new IllegalArgumentException("Missing 'type' attribute: " + descriptor);
            }
            String type = o.toString();
            
            o = descriptor.get("inherited");
            if (o != null) {
                if (!(o instanceof Boolean)) {
                    throw new IllegalArgumentException("Attribute 'inherited' must be a boolean");
                }
            }
            Boolean inherited = o != null ? (Boolean) o : false;
            
            o = descriptor.get("values");
            if (o != null) {
                if (!(o instanceof List<?>)) {
                    throw new IllegalArgumentException("Attribute 'values' must be a list: " + o);
                }
            }
            List<?> values = (List<?>) o;
            
            o = descriptor.get("i18n");
            if (o != null) {
                if (!(o instanceof Map<?, ?>)) {
                    throw new IllegalArgumentException("Attribute 'i18n' must be a map: " + o);
                }
            }
            Map<?, ?> i18n = (Map<?, ?>) o;
            PropertyAspectField field = new PropertyAspectField(identifier.toString(), type.toString(), inherited, values, i18n);
            this.fields.add(field);
            
        }
    }
}
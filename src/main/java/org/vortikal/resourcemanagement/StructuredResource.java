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
package org.vortikal.resourcemanagement;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.sf.json.JSONObject;

import org.vortikal.util.io.StreamUtil;
import org.vortikal.util.text.JSON;

public class StructuredResource {

    private StructuredResourceDescription desc;
    private Map<String, Object> properties;

    private StructuredResource(StructuredResourceDescription desc, Map<String, Object> properties) {
        if (desc == null) {
            throw new IllegalArgumentException("Description cannot be null");
        }
        this.desc = desc;
        this.properties = properties;
    }
    
    @SuppressWarnings("unchecked")
    static StructuredResource create(StructuredResourceDescription desc, InputStream source) throws Exception {
        String str = StreamUtil.streamToString(source, "utf-8");
        JSONObject json = JSONObject.fromObject(str);
        ValidationResult validation = validateInternal(desc, json);
        if (!validation.isValid()) {
            throw new RuntimeException("Invalid document: " + validation.getErrors());
        }
        JSONObject object = json.getJSONObject("properties");
        Map<String, Object> properties = new HashMap<String, Object>();
        for (Iterator<String> iter = object.keys(); iter.hasNext();) {
            String name = iter.next();
            properties.put(name, object.get(name));
        }
        return new StructuredResource(desc, properties);
    }

    public boolean isValidDocument(JSONObject document) {
        try {
            ValidationResult validation = validateInternal(this.desc, document);
            return validation.isValid();
        } catch (Exception e){
            return false;
        }
    }

    // XXX: make recursive:
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("resourcetype", this.desc.getName());

        JSONObject props = new JSONObject();
        for (String name : this.properties.keySet()) {
            Object value = this.properties.get(name);
            if (value != null) {
                if (value instanceof String) {
                    String s = (String) value;
                    if (s.trim().equals("")) {
                        continue;
                    }
                }
                props.put(name, this.properties.get(name));
            }
        }
        json.put("properties", props);
        return json;
    }

    private static ValidationResult validateInternal(StructuredResourceDescription desc, JSONObject json) {
        if (json == null) {
            throw new IllegalStateException("Input is NULL");
        }
        String type = json.getString("resourcetype");
        if (type == null) {
            throw new IllegalStateException(
            "Unable to validate: missing 'resourcetype' element");
        }
        JSONObject properties = json.getJSONObject("properties");
        if (properties == null) {
            throw new IllegalStateException(
            "Unable to validate: missing 'properties' element");
        }
        List<ValidationError> errors = new ArrayList<ValidationError>();
        for (PropertyDescription propDesc : desc.getPropertyDescriptions()) {
            if (propDesc instanceof SimplePropertyDescription) {
                if (((SimplePropertyDescription) propDesc).isRequired()) {

                    Object value = JSON.select(json, "properties." + propDesc.getName());
                    if (value == null) {
                        errors.add(new ValidationError(propDesc.getName(), 
                        "property is required"));
                    }
                    // TODO: validate types and multiple properties
                }
            }
        }
        ValidationResult result = new ValidationResult(errors);
        return result;
    }

    public StructuredResourceDescription getType() {
        return this.desc;
    }

    public Object getProperty(String name) {
        return this.properties.get(name);
    }

    public void addProperty(String name, Object value) {
        this.properties.put(name, value);
    }

    public void addProperty(String name, Object[] values) {
        this.properties.put(name, values);
    }

    public void removeProperty(String name) {
        this.properties.remove(name);
    }

    public Collection<String> getPropertyNames() {
        return Collections.unmodifiableCollection(this.properties.keySet());
    }

    public String getLocalizedMsg(String key, Locale locale, Object[] param) {
        return this.desc.getLocalizedMsg(key, locale, param);
    }

    public String getLocalizedTooltip(String key, Locale locale) {
        return this.desc.getLocalizedTooltip(key, locale);
    }

}

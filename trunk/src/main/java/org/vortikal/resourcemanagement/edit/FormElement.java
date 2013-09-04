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
package org.vortikal.resourcemanagement.edit;

import java.util.ArrayList;
import java.util.List;

import org.vortikal.resourcemanagement.EditablePropertyDescription;
import org.vortikal.resourcemanagement.ValidationError;

public class FormElement {

    private EditablePropertyDescription description;
    private ValidationError error;
    private Object value;
    private Object defaultValue;

    public FormElement(EditablePropertyDescription description, ValidationError error, Object value, Object defaultValue) {
        this.description = description;
        this.error = error;
        this.value = value;
        this.defaultValue = defaultValue;
    }

    public String getName() {
        return this.description.getName();
    }

    public void setDescription(EditablePropertyDescription description) {
        this.description = description;
    }

    public EditablePropertyDescription getDescription() {
        return this.description;
    }

    public void setError(ValidationError error) {
        this.error = error;
    }

    public ValidationError getError() {
        return error;
    }

    public void setValue(Object value) throws Exception {
        if (this.description.isMultiple() && (value instanceof String)) {
            String[] splitValue = value.toString().split(",");
            ArrayList<String> valueList = new ArrayList<String>();
            for (String val : splitValue) {
                val = val.trim();
                if (!"".equals(val)) {
                    valueList.add(val);
                }
            }
            setValueInternal(valueList);
        } else {
            setValueInternal(value);
        }
    }

    private void setValueInternal(Object value) {
        // XXX: check for JSON
        this.value = value;
    }

    public Object getValue() {
        return this.value;
    }

    @SuppressWarnings("unchecked")
    public Object getFormatedValue() {
        if (value instanceof List) {
            StringBuilder result = new StringBuilder();
            List<String> l = (List<String>) value;
            for (int i = 0; i < l.size(); i++) {
                if (i > 0) {
                    result.append(", ");
                }
                result.append(l.get(i));
            }
            return result.toString();
        }
        return value;
    }

    public boolean valueIsList() {
        return this.value instanceof List<?>;
    }

    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

}

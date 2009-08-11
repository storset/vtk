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

import org.apache.commons.lang.IllegalClassException;
import org.vortikal.resourcemanagement.PropertyDescription;
import org.vortikal.resourcemanagement.ValidationError;

public class FormElement {

    private PropertyDescription description;
    private ValidationError error;
    private Object value;

    public FormElement(PropertyDescription description, ValidationError error,
            Object value) {
        this.description = description;
        this.error = error;
        this.value = value;

    }
    
    public String getName() {
        return this.description.getName();
    }

    public void setDescription(PropertyDescription description) {
        this.description = description;
    }

    public PropertyDescription getDescription() {
        return description;
    }

    public void setError(ValidationError error) {
        this.error = error;
    }

    public ValidationError getError() {
        return error;
    }

    public void setValue(Object value) throws Exception {
        if (this.description.isMultiple()) {
            if (value instanceof String) {
                String[] a = value.toString().split(",");
                ArrayList<String> b = new ArrayList<String>();
                for (int i = 0; i < a.length; i++) {
                    b.add(a[i].toString());
                }
                this.value = b;
            } else if (value instanceof List<?>) {
                this.value = value;
            } else {
                throw new IllegalClassException("Unknown value type: " + value.getClass()
                        + " for multiple-valued property " + this.description.getName());
            }
        } else {
            this.value = value;
        }
    }

    public Object getValue() {
        return value;
    }

    @SuppressWarnings("unchecked")
    public Object getFormatedValue() {
        if (value instanceof List) {
            String result = "";
            List<String> l = (List<String>) value;
            for (int i = 0; i < l.size(); i++) {
                if (i > 0) {
                    result += ",";
                }
                result += l.get(i);
            }
            return result;
        }
        return value;
    }

}

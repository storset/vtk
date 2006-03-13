/* Copyright (c) 2004, University of Oslo, Norway
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
package org.vortikal.repository;

import java.util.Date;

import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.resourcetype.Value;


/**
 * This class represents meta information about resources. A resource
 * may have several properties set on it, each of which are identified
 * by a namespace and a name. Properties may contain arbitrary string
 * values, such as XML. The application programmer is responsible for
 * the interpretation and processing of properties.
 */
public class Property implements java.io.Serializable, Cloneable {

    public static final String LOCAL_NAMESPACE = "http://www.uio.no/vortex/custom-properties";
    
    private static final long serialVersionUID = 3762531209208410417L;
    
    private String namespaceUri;
    private String name;
    private Value value;

    public String getNamespace() {
        return this.namespaceUri;
    }

    public void setNamespace(String namespace) {
        this.namespaceUri = namespace;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Value getValue() {
        return this.value;
    }

    public void setValue(Value value) {
        this.value = value;
    }

    public Date getDateValue() {
        if (value == null || value.getType() != PropertyType.TYPE_DATE) {
            throw new IllegalOperationException();
        }
        return value.getDateValue();
    }

    public void setDateValue(Date dateValue) {
        value = new Value();
        value.setDateValue(dateValue);
    }

    public String getStringValue() {
        if (value == null || value.getType() != PropertyType.TYPE_STRING) {
            throw new IllegalOperationException();
        }
        return value.getValue();
    }

    public void setStringValue(String stringValue) {
        value = new Value();
        value.setValue(stringValue);
    }

    public boolean getBooleanValue() {
        if (value == null || value.getType() != PropertyType.TYPE_BOOLEAN) {
            throw new IllegalOperationException();
        }
        return value.getBooleanValue();
    }

    public void setBooleanValue(boolean booleanValue) {
        value = new Value();
        value.setBooleanValue(booleanValue);
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();

        sb.append(this.getClass().getName()).append(": ");
        sb.append("[ ").append(this.namespaceUri);
        sb.append(":").append(this.name);
        sb.append(" = ").append(this.value);
        sb.append("]");

        return sb.toString();
    }
}

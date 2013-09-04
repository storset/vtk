/* Copyright (c) 2011, University of Oslo, Norway
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
package org.vortikal.repository.resourcetype.property;

import org.bouncycastle.util.encoders.Base64;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertyEvaluationContext;
import org.vortikal.repository.resourcetype.PropertyEvaluator;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.store.Metadata;

public class JSONPropertyFieldEvaluratorImpl implements PropertyEvaluator {

    private Class<?> clazz;
    private String key;
    private String binaryMimeType = null;

    @Override
    public boolean evaluate(Property property, PropertyEvaluationContext ctx) throws PropertyEvaluationException {

        Metadata obj = null;
        try {
            obj = (Metadata) ctx.getContent().getContentRepresentation(clazz);
        } catch (Exception e) {
        }
        if (obj == null) {
            return false;
        }
        String value = (String) obj.getValue(key);
        if (value != null) {
            if (property.getType() == PropertyType.Type.BINARY) {
                property.setBinaryValue(Base64.decode(value), binaryMimeType);
            } else if (property.getType() == PropertyType.Type.INT) {
                try {
                    int intValue = Integer.parseInt(value);
                    property.setIntValue(intValue);
                } catch (Exception e) {
                    return false;
                }

            } else if (property.getType() == PropertyType.Type.STRING) {
                property.setStringValue(value);
            }
        } else {
            return false;
        }
        return true;
    }

    @Required
    public void setClazz(Class<?> clazz) {
        this.clazz = clazz;
    }

    @Required
    public void setKey(String key) {
        this.key = key;
    }

    public void setBinaryMimeType(String binaryMimeType) {
        this.binaryMimeType = binaryMimeType;
    }

}
/* Copyright (c) 2012, University of Oslo, Norway
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

import java.io.InputStream;
import java.io.InputStreamReader;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertyEvaluationContext;
import org.vortikal.repository.resourcetype.LatePropertyEvaluator;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;

public class LinkStatusEvaluator implements LatePropertyEvaluator {

    private PropertyTypeDefinition linkCheckPropDef;
    private PropertyTypeDefinition linksPropDef;
    
    public void setLinksPropDef(PropertyTypeDefinition linksPropDef) {
        this.linksPropDef = linksPropDef;
    }

    public void setLinkCheckPropDef(PropertyTypeDefinition linkCheckPropDef) {
        this.linkCheckPropDef = linkCheckPropDef;
    }

    @Override
    public boolean evaluate(Property property, PropertyEvaluationContext ctx)
            throws PropertyEvaluationException {
        Property linksProp = ctx.getNewResource().getProperty(this.linksPropDef);

        if (linksProp == null) {
            property.setStringValue("NO_LINKS");
            return true;
        }
        
        if (ctx.getEvaluationType() != PropertyEvaluationContext.Type.SystemPropertiesChange) {
            property.setStringValue("AWAITING_LINKCHECK");
            return true;
        }
        
        Property linkCheckProp = ctx.getNewResource().getProperty(this.linkCheckPropDef);
        
        try {
            JSONObject linkCheck = propValue(linkCheckProp);
            Object brokenLinks = linkCheck.get("brokenLinks");
            String value = "OK";
            if (brokenLinks != null) {
                JSONArray arr = (JSONArray) brokenLinks;
                if (!arr.isEmpty()) {
                    value = "BROKEN_LINKS";
                }
            }
            property.setStringValue(value);
            return true;
        } catch (Throwable t) {
            property.setStringValue("LINKCHECK_ERROR");
            return true;
        }
    }

    
    private JSONObject propValue(Property linkCheck) throws Exception {
        InputStream stream = linkCheck.getBinaryStream().getStream();
        Object o = JSONValue.parse(new InputStreamReader(stream));
        stream.close();
        return (JSONObject) o;
    }
}

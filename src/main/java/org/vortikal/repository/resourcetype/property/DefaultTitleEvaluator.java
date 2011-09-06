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
package org.vortikal.repository.resourcetype.property;

import org.vortikal.repository.Property;
import org.vortikal.repository.PropertyEvaluationContext;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.resourcetype.PropertyEvaluator;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.repository.resourcetype.ValueFormatException;

/**
 * Evaluate name....
 */
public class DefaultTitleEvaluator implements PropertyEvaluator {

    private PropertyTypeDefinition propertyDefinition;
    private PropertyTypeDefinition fallbackTitlePropDef;

    private boolean capitalizeResourceNames = false;
    
    public boolean evaluate(Property property, PropertyEvaluationContext ctx) throws PropertyEvaluationException {

        Property prop = ctx.getNewResource().getProperty(this.propertyDefinition);
        if (prop != null) {
            property.setValue(new Value(prop.getStringValue(), PropertyType.Type.STRING));
        } else {
            property.setValue(new Value(getFallback(ctx.getNewResource()), PropertyType.Type.STRING));
        }
        return true;
    }

    private String getFallback(PropertySet ancestorPropertySet) throws ValueFormatException {
        if (this.fallbackTitlePropDef != null) {
            Property prop = ancestorPropertySet.getProperty(this.fallbackTitlePropDef);

            if (prop != null) {
                return prop.getStringValue();
            }
        }
        if (this.capitalizeResourceNames) {
            StringBuilder result = new StringBuilder();
            String name = ancestorPropertySet.getName(); 
            if (name.length() > 0) {
                result.append(name.substring(0, 1).toUpperCase());
                result.append(name.substring(1));
            }
            return result.toString();
        }
        return ancestorPropertySet.getName();
    }

    public void setPropertyDefinition(PropertyTypeDefinition propertyDefinition) {
        this.propertyDefinition = propertyDefinition;
    }
    
    public void setFallbackTitlePropDef(PropertyTypeDefinition fallbackTitlePropDef) {
        this.fallbackTitlePropDef = fallbackTitlePropDef;
    }

    public void setCapitalizeResourceNames(boolean capitalizeResourceNames) {
        this.capitalizeResourceNames = capitalizeResourceNames;
    }

}

/* Copyright (c) 2006, University of Oslo, Norway
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
package org.vortikal.web.actions.properties;

import org.vortikal.repository.Property;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;


public class PropertyItem {

    private Property property;
    private PropertyTypeDefinition definition;
    private String editURL;
    private String format;
    private String toggleURL;
    private String toggleValue;

    public PropertyItem(Property property, PropertyTypeDefinition definition,
                        String editURL, String format, String toggleURL,
                        String toggleValue) {
        this.property = property;
        this.definition = definition;
        this.editURL = editURL;
        this.format = format;
        this.toggleURL = toggleURL;
        this.toggleValue = toggleValue;
    }

    public Property getProperty() {
        return this.property;
    }

    public void setProperty(Property property) {
        this.property = property;
    }

    public PropertyTypeDefinition getDefinition() {
        return this.definition;
    }

    public void setDefinition(PropertyTypeDefinition definition) {
        this.definition = definition;
    }

    public String getFormat() {
        return this.format;
    }
    

    public String getEditURL() {
        return this.editURL;
    }

    public void setEditURL(String editURL) {
        this.editURL = editURL;
    }

    public String getToggleURL() {
        return this.toggleURL;
    }
    
    public String getToggleValue() {
        return this.toggleValue;
    }
    
}

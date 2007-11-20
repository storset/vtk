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
package org.vortikal.edit.editor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.vortikal.repository.Property;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.text.html.HtmlElement;
import org.vortikal.text.html.HtmlPage;
import org.vortikal.text.html.HtmlSelectUtil;

public class ResourceCommand {

    /* Content */
    
    private HtmlPage content;
    private boolean contentChange = false;

    public HtmlPage getContent() {
        return this.content;
    }

    public void setContent(HtmlPage content) {
        this.content = content;
    }

    public boolean isContentChange() {
        return contentChange;
    }

    public void setContentChange(boolean contentChange) {
        this.contentChange = contentChange;
    }

    public String getBodyAsString() {
        List<HtmlElement> elements = HtmlSelectUtil.select(this.content, "html.body");
        if (elements == null || elements.isEmpty()) {
            return "";
        } 
        return elements.get(0).getContent(); 
    }

    
    /* Resource */
    
    private Resource resource;
    private List<PropertyTypeDefinition> editableProperties;
    private boolean propChange = false;

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public boolean isPropChange() {
        return propChange;
    }

    public void setPropChange(boolean propChange) {
        this.propChange = propChange;
    }

    public List<PropertyTypeDefinition> getEditableProperties() {
        return editableProperties;
    }

    public void setEditableProperties(
            List<PropertyTypeDefinition> editableProperties) {
        this.editableProperties = editableProperties;
    }

    /* Errors */

    private Map<PropertyTypeDefinition, String> errors = new HashMap<PropertyTypeDefinition, String>();

    public void reject(PropertyTypeDefinition propDef, String code) {
        this.errors.put(propDef, code);
    }
    
    public String getError(PropertyTypeDefinition propDef) {
        return this.errors.get(propDef);
    }
    
    public boolean hasErrors() {
        return !this.errors.isEmpty();
    }
    
    public Map<PropertyTypeDefinition, String> getErrors() {
        return this.errors;
    }

    /* Tooltips */
    
    private List<Map<String, String>> tooltips;
    
    public List<Map<String, String>> getTooltips() {
        return tooltips;
    }

    public void setTooltips(List<Map<String, String>> tooltips) {
        this.tooltips = tooltips;
    }


    public String getValue(PropertyTypeDefinition propDef) {
        Property prop = resource.getProperty(propDef);
        if (prop == null) {
            return "";
        }
        return prop.getFormattedValue(null, null);
        
    }

    private boolean save = false;

    public boolean isSave() {
        return this.save;
    }
    
    public void setSave(boolean save) {
        this.save = save;
    }
}

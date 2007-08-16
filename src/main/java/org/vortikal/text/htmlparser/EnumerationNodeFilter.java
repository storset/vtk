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
package org.vortikal.text.htmlparser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.vortikal.text.html.EnclosingHtmlContent;
import org.vortikal.text.html.HtmlAttribute;
import org.vortikal.text.html.HtmlComment;
import org.vortikal.text.html.HtmlContent;
import org.vortikal.text.html.HtmlElement;
import org.vortikal.text.html.HtmlNodeFilter;
import org.vortikal.text.html.HtmlText;


public class EnumerationNodeFilter implements HtmlNodeFilter {

    private boolean includeComments = false;
    private Set<String> illegalElements = new HashSet<String>();
    private Map<String, HtmlElementDescriptor> validElements = new HashMap<String, HtmlElementDescriptor>();
    
    
    public void setIllegalElements(Set<String> illegalElements) {
        for (String elem: illegalElements) {
            this.illegalElements.add(elem);
        }
    }
    
    public void setValidElements(Set<HtmlElementDescriptor> validElements) {
        for (HtmlElementDescriptor desc: validElements) {
            this.validElements.put(desc.getName(), desc);
        }
    }
    
    public void setIncludeComments(boolean includeComments) {
        this.includeComments = includeComments;
    }
    

    public HtmlContent filterNode(HtmlContent node) {
        if (node instanceof HtmlComment) {
            return this.includeComments ? node : null;

        } else if (node instanceof HtmlText) {
            return node;

        } else if (node instanceof HtmlElement) {
            HtmlElement element = (HtmlElement) node;
            String name = element.getName().toLowerCase();

            if (this.illegalElements.contains(name)) {
                return null;
            }

            if (this.validElements.containsKey(name)) {
                HtmlElementDescriptor desc = this.validElements.get(name);
                List<HtmlAttribute> filteredAttributes = new ArrayList<HtmlAttribute>();
                for (String attribute: desc.getAttributes()) {
                    for (HtmlAttribute a: element.getAttributes()) {
                        if (attribute.equals(a.getName().toLowerCase())) {
                            filteredAttributes.add(a);
                        }
                    }
                }
                
                HtmlAttribute[] newAttributes = filteredAttributes.<HtmlAttribute>toArray(
                    new HtmlAttribute[filteredAttributes.size()]);
                element.setAttributes(newAttributes);
                return element;
            }

            // Return text:
            EnclosingHtmlText result = new EnclosingHtmlText();
            for (HtmlContent c: element.getChildNodes()) {
                result.addContent(c);
            }

            return result;
        }
        return node;
    }
    
    private class EnclosingHtmlText implements EnclosingHtmlContent {
        private List<HtmlContent> content = new ArrayList<HtmlContent>();

        public void addContent(HtmlContent c) {
            this.content.add(c);
        }

        public String getContent() {
            StringBuilder sb = new StringBuilder("");
            for (HtmlContent c: this.content) {
                if (c instanceof EnclosingHtmlContent) {
                    sb.append(((EnclosingHtmlContent) c).getEnclosedContent());
                } else {
                    sb.append(c.getContent());
                }
            }
            return sb.toString();
        }

        public String getEnclosedContent() {
            return getContent();
        }
        
        public HtmlContent[] getChildNodes() {
            return this.content.toArray(new HtmlContent[this.content.size()]);
        }

        public String toString() {
            return "enclosing-text: " + this.content.toString();
        }
        
    }
    

}

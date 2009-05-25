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
package org.vortikal.web.decorating;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.text.html.HtmlAttribute;
import org.vortikal.text.html.HtmlContent;
import org.vortikal.text.html.HtmlElement;
import org.vortikal.text.html.HtmlNodeFilter;


public class NodeLabellingFilter implements HtmlNodeFilter, InitializingBean {
    
    private Set<String> labelElements;
    
    public void setLabelElements(Set<String> labelElements) {
        this.labelElements = labelElements;
    }
    

    public void afterPropertiesSet() {
        if (this.labelElements == null) {
            throw new BeanInitializationException(
                "JavaBean property 'labelElements' not specified");
        }
    }
    

    public HtmlContent filterNode(HtmlContent node) {

        if (node instanceof HtmlElement) {
            HtmlElement element = (HtmlElement) node;
            String name = element.getName().toLowerCase();

            if (this.labelElements.contains(name)) {
                HtmlAttribute[] attributes = element.getAttributes();
                List<HtmlAttribute> newAttributes = new ArrayList<HtmlAttribute>();

                boolean haveNameAttribute = false;

                for (HtmlAttribute attribute: attributes) {
                    if ("name".equals(attribute.getName())) {
                        haveNameAttribute = true;
                    }
                    newAttributes.add(attribute);
                }
                if (!haveNameAttribute) {

                    final String value = String.valueOf(System.currentTimeMillis());
                    newAttributes.add(new HtmlAttribute() {
                            public String getName() {
                                return "name";
                            }
                            public String getValue() {
                                return value;
                            }
                            public void setName(String name) {
                            }
                            public void setValue(String value) {
                            }
                            public boolean hasValue() {
                                return true;
                            }
                            public boolean isSingleQuotes() {
                                return false;
                            }
                            public void setSingleQuotes(boolean singleQuotes) {                                
                            }
                        });
                }
                element.setAttributes(
                    newAttributes.toArray(new HtmlAttribute[newAttributes.size()]));
            }
        }
        return node;
    }
}

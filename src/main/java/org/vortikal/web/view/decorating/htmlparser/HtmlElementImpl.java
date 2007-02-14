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
package org.vortikal.web.view.decorating.htmlparser;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.vortikal.web.view.decorating.HtmlAttribute;
import org.vortikal.web.view.decorating.HtmlElement;

public class HtmlElementImpl implements HtmlElement {
    private boolean empty;
    private boolean xhtml;

    private String name;
    private String content;
    private List attributes = new ArrayList();
    private List contentList = new ArrayList();
    private List children = new ArrayList();
    private Map namedChildMap = new HashMap();
    private boolean text;
        
    public HtmlElementImpl(String name, boolean empty, boolean xhtml, boolean text) {
        this.name = name;
        this.text = text;
        this.xhtml = xhtml;
        this.empty = empty;
    }

    public String getName() {
        return this.name;
    }
        
    public HtmlElement[] getChildElements() {
        return (HtmlElement[]) this.children.toArray(new HtmlElement[this.children.size()]);
    }
        
    public HtmlElement[] getChildElements(String name) {
        List list = (List) this.namedChildMap.get(name);
        if (list == null) {
            list = new ArrayList();
        }
        return (HtmlElement[]) list.toArray(new HtmlElement[list.size()]);
    }

    public void addChild(HtmlElementImpl child) {
        this.contentList.add(child);
        if (!child.text) {
            this.children.add(child);
            List childList = (List) this.namedChildMap.get(child.getName());
            if (childList == null) {
                childList = new ArrayList();
                this.namedChildMap.put(child.getName(), childList);
            }
            childList.add(child);
        }
    }

    public HtmlAttribute[] getAttributes() {
        return (HtmlAttribute[]) this.attributes.toArray(new HtmlAttribute[this.attributes.size()]);
    }

    public void addAttribute(HtmlAttributeImpl attribute) {
        this.attributes.add(attribute);
    }
        

    public String getContent() {
        if (this.text) {
            return this.content;
        }
        StringBuffer sb = new StringBuffer();
        for (Iterator i = this.contentList.iterator(); i.hasNext();) {
            HtmlElementImpl child = (HtmlElementImpl) i.next();
            sb.append(child.getEnclosedContent());
        }
        return sb.toString();
    }
        
    public void setContent(String content) {
        this.content = content;
    }

    public String getEnclosedContent() {
        if (this.text) {
            return this.content;
        } else {
            StringBuffer sb = new StringBuffer();
            sb.append("<").append(this.name);
            if (this.attributes.size() > 0) {
                for (Iterator i = this.attributes.iterator(); i.hasNext();) {
                    HtmlAttributeImpl attr = (HtmlAttributeImpl) i.next();
                    if (attr.hasValue()) {
                        sb.append(" ").append(attr.getName()).append("=\"");
                        sb.append(attr.getValue()).append("\"");
                    } else if (this.xhtml) {
                        sb.append(" ").append(attr.getName()).append("=\"\"");
                    } else {
                        sb.append(" ").append(attr.getName());
                    }
                }
            }
            if (this.empty && this.xhtml) {
                sb.append("/>");
            }
            else if (this.empty) {
                sb.append(">");
            } else {
                sb.append(">").append(getContent());
                sb.append("</").append(this.name).append(">");    
            }
            return sb.toString();
        }
    }

    public String toString() {
        return this.name;
    }
        
}

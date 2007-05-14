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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.vortikal.text.html.EnclosingHtmlContent;
import org.vortikal.text.html.HtmlAttribute;
import org.vortikal.text.html.HtmlContent;
import org.vortikal.text.html.HtmlElement;
import org.vortikal.text.html.HtmlNodeFilter;


public class HtmlElementImpl implements HtmlElement {
    private boolean empty;
    private boolean xhtml;

    private String name;
    private List<HtmlAttribute> attributes = new ArrayList<HtmlAttribute>();
    private List<HtmlContent> contentList = new ArrayList<HtmlContent>();
    private List<HtmlElement> childElements = new ArrayList<HtmlElement>();
    private Map<String, List<HtmlElement>> namedChildMap = new HashMap<String, List<HtmlElement>>();
        
    public HtmlElementImpl(String name, boolean empty, boolean xhtml) {
        this.name = name;
        this.xhtml = xhtml;
        this.empty = empty;
    }

    public String getName() {
        return this.name;
    }
        
    public HtmlElement[] getChildElements() {
        return this.childElements.toArray(
            new HtmlElement[this.childElements.size()]);
    }
        
    public HtmlElement[] getChildElements(String name) {
        List<HtmlElement> list = this.namedChildMap.get(name.toLowerCase());
        if (list == null) {
            list = new ArrayList<HtmlElement>();
        }
        return list.toArray(new HtmlElement[list.size()]);
    }

    public HtmlContent[] getChildNodes() {
        return this.contentList.toArray(
            new HtmlContent[this.contentList.size()]);
    }
    
    public void setChildNodes(HtmlContent[] childNodes) {
        this.namedChildMap = new HashMap<String, List<HtmlElement>>();
        this.contentList = new ArrayList<HtmlContent>();
        this.childElements = new ArrayList<HtmlElement>();
        for (int i = 0; i < childNodes.length; i++) {
            addContent(childNodes[i]);
        }
    }

    public HtmlContent[] getChildNodes(HtmlNodeFilter filter) {
        List<HtmlContent> list = new ArrayList<HtmlContent>();
        for (int i = 0; i < this.contentList.size(); i++) {
            HtmlContent content = this.contentList.get(i);
            content = filter.filterNode(content);
            if (content != null) {
                list.add(content);
            }
        }
        return list.toArray(new HtmlContent[list.size()]);
    }
    

    public void addContent(HtmlContent child) {
        this.contentList.add(child);
        if (child instanceof HtmlElement) {
            HtmlElement theChild = (HtmlElement) child;
            this.childElements.add(theChild);
            List<HtmlElement> childList = this.namedChildMap.get(theChild.getName().toLowerCase());
            if (childList == null) {
                childList = new ArrayList<HtmlElement>();
                this.namedChildMap.put(theChild.getName().toLowerCase(), childList);
            }
            childList.add(theChild);
        }
    }

    public HtmlAttribute[] getAttributes() {
        return this.attributes.toArray(
            new HtmlAttribute[this.attributes.size()]);
    }

    public void setAttributes(HtmlAttribute[] attributes) {
        this.attributes = new ArrayList<HtmlAttribute>();
        for (int i = 0; i < attributes.length; i++) {
            this.attributes.add(attributes[i]);
        }
    }
    

    public void addAttribute(HtmlAttributeImpl attribute) {
        this.attributes.add(attribute);
    }
        

    public String getContent() {
        StringBuffer sb = new StringBuffer();
        for (Iterator i = this.contentList.iterator(); i.hasNext();) {
            HtmlContent child = (HtmlContent) i.next();
            if (child instanceof EnclosingHtmlContent) {
                sb.append(((EnclosingHtmlContent) child).getEnclosedContent());
            } else {
                sb.append(child.getContent());
            }
        }
        return sb.toString();
    }
        
    public String getContent(HtmlNodeFilter filter) {
        StringBuffer sb = new StringBuffer();
        for (Iterator i = this.contentList.iterator(); i.hasNext();) {
            HtmlContent child = (HtmlContent) i.next();
            child = filter.filterNode(child);
            if (child != null) {
                if (child instanceof HtmlElement) {
                    sb.append(((HtmlElement) child).getEnclosedContent(filter));
                } else if (child instanceof EnclosingHtmlContent) {
                    sb.append(((EnclosingHtmlContent) child).getEnclosedContent());
                } else {
                    sb.append(child.getContent());
                }
            }
        }
        return sb.toString();
    }

    public String getEnclosedContent() {
        StringBuffer sb = new StringBuffer();
        sb.append("<").append(this.name);
        HtmlAttribute[] attributes = getAttributes();
        if (attributes.length > 0) {
            for (int i = 0; i < attributes.length; i++) {
                HtmlAttribute attr = attributes[i];
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

    public String getEnclosedContent(HtmlNodeFilter filter) {
        StringBuffer sb = new StringBuffer();
        sb.append("<").append(this.name);
        HtmlAttribute[] attributes = getAttributes();
        if (attributes.length > 0) {
            for (int i = 0; i < attributes.length; i++) {
                HtmlAttribute attr = attributes[i];
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
            sb.append(">").append(getContent(filter));
            sb.append("</").append(this.name).append(">");    
        }
        return sb.toString();
    }

    public String toString() {
        return this.name;
    }
        
}

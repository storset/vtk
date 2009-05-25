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
package org.vortikal.text.html;

import java.util.ArrayList;
import java.util.List;



public class HtmlElementImpl implements HtmlElement {
    private boolean xhtml;

    private String name;
    private boolean emptyTag = false;
    private List<HtmlAttribute> attributes = new ArrayList<HtmlAttribute>();
    private List<HtmlContent> contentList = new ArrayList<HtmlContent>();
        
    public HtmlElementImpl(String name, boolean xhtml, boolean emptyTag) {
        this.name = name;
        this.xhtml = xhtml;
        this.emptyTag = emptyTag;
    }

    public String getName() {
        return this.name;
    }
        
    public HtmlElement[] getChildElements() {
        List<HtmlElement> childElements = getChildElementsInternal();
        return childElements.toArray(
            new HtmlElement[childElements.size()]);
    }
    
    private List<HtmlElement> getChildElementsInternal() {
        List<HtmlElement> result = new ArrayList<HtmlElement>();
        for (HtmlContent c : this.contentList) {
            if (c instanceof HtmlElement) {
                result.add((HtmlElement)c);
            }
        }
        return result;
    }
        
    public HtmlElement[] getChildElements(String name) {
        List<HtmlElement> list = new ArrayList<HtmlElement>();
        for (HtmlContent c : this.contentList) {
            if (c instanceof HtmlElement) {
                HtmlElement e = (HtmlElement) c;
                if (e.getName().toLowerCase().equals(name)) {
                    list.add((HtmlElement) c);
                }
            }
        }
        return list.toArray(new HtmlElement[list.size()]);
    }

    public HtmlContent[] getChildNodes() {
        return this.contentList.toArray(
            new HtmlContent[this.contentList.size()]);
    }
    
    public void setChildNodes(HtmlContent[] childNodes) {
        this.contentList = new ArrayList<HtmlContent>();
        for (HtmlContent c : childNodes) {
            addContent(c);
        }
    }

    public HtmlContent[] getChildNodes(HtmlNodeFilter filter) {
        List<HtmlContent> list = new ArrayList<HtmlContent>();
        for (HtmlContent content : this.contentList) {
            content = filter.filterNode(content);
            if (content != null) {
                list.add(content);
            }
        }
        return list.toArray(new HtmlContent[list.size()]);
    }
    

    public void addContent(HtmlContent child) {
        addContent(this.contentList.size(), child);
    }
    
    public void addContent(int pos, HtmlContent child) {
        this.contentList.add(pos, child);
    }

    public void removeContent(HtmlContent child) {
        this.contentList.remove(child);
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
    
    public HtmlAttribute getAttribute(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Argument cannot be NULL");
        }
        for (HtmlAttribute attr: this.attributes) {
            if (name.equals(attr.getName())) {
                return attr;
            }
        }
        return null;
    }

    public void addAttribute(HtmlAttributeImpl attribute) {
        this.attributes.add(attribute);
    }
        

    public String getContent() {
        StringBuilder sb = new StringBuilder();
        for (HtmlContent child : this.contentList) {
            if (child instanceof EnclosingHtmlContent) {
                sb.append(((EnclosingHtmlContent) child).getEnclosedContent());
            } else {
                sb.append(child.getContent());
            }
        }
        return sb.toString();
    }
        
    public String getContent(HtmlNodeFilter filter) {
        StringBuilder sb = new StringBuilder();
        for (HtmlContent child : this.contentList) {
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
        StringBuilder sb = new StringBuilder();
        sb.append("<").append(this.name);
        HtmlAttribute[] attributes = getAttributes();
        if (attributes.length > 0) {
            for (HtmlAttribute attr : attributes) {
                if (attr.hasValue()) {
                    sb.append(" ").append(attr.getName()).append("=");
                    sb.append(attr.isSingleQuotes() ? "'" : "\"");
                    sb.append(attr.getValue());
                    sb.append(attr.isSingleQuotes() ? "'" : "\"");
                } else if (this.xhtml) {
                    sb.append(" ").append(attr.getName()).append("=");
                    sb.append(attr.isSingleQuotes() ? "''" : "\"\"");
                } else {
                    sb.append(" ").append(attr.getName());
                }
            }
        }
        if (this.contentList.isEmpty() && this.emptyTag && this.xhtml) {
            sb.append("/>");
        }
        else if (this.contentList.isEmpty() && this.emptyTag) {
            sb.append(">");
        } else {
            sb.append(">").append(getContent());
            sb.append("</").append(this.name).append(">");    
        }
        return sb.toString();
    }

    public String getEnclosedContent(HtmlNodeFilter filter) {
        StringBuilder sb = new StringBuilder();
        sb.append("<").append(this.name);
        HtmlAttribute[] attributes = getAttributes();
        if (attributes.length > 0) {
            for (HtmlAttribute attr : attributes) {
                if (attr.hasValue()) {
                    sb.append(" ").append(attr.getName()).append("=");
                    sb.append(attr.isSingleQuotes() ? "'" : "\"");
                    sb.append(attr.getValue());
                    sb.append(attr.isSingleQuotes() ? "'" : "\"");
                } else if (this.xhtml) {
                    sb.append(" ").append(attr.getName()).append("=");
                    sb.append(attr.isSingleQuotes() ? "''" : "\"\"");
                } else {
                    sb.append(" ").append(attr.getName());
                }
            }
        }
        if (this.contentList.isEmpty() && this.emptyTag && this.xhtml) {
            sb.append("/>");
        }
        else if (this.contentList.isEmpty() && this.emptyTag) {
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

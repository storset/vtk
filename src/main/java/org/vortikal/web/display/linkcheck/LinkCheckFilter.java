/* Copyright (c) 2010, University of Oslo, Norway
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
package org.vortikal.web.display.linkcheck;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.security.SecurityContext;
import org.vortikal.text.html.HtmlAttribute;
import org.vortikal.text.html.HtmlContent;
import org.vortikal.text.html.HtmlElement;
import org.vortikal.text.html.HtmlNodeFilter;
import org.vortikal.text.html.HtmlPage;
import org.vortikal.text.html.HtmlPageFilter;
import org.vortikal.web.RequestContext;

public class LinkCheckFilter implements HtmlPageFilter, HtmlNodeFilter {

    private String elementClass = null;
    
    @Required
    public void setElementClass(String elementClass) {
        this.elementClass = elementClass;
    }

    @Override
    public boolean match(HtmlPage page) {
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        if (securityContext.getPrincipal() == null) {
            return false;
        }
        RequestContext requestContext = RequestContext.getRequestContext();
        HttpServletRequest request = requestContext.getServletRequest();
        return "true".equals(request.getParameter("link-check"));
    }
    
    @Override
    public NodeResult filter(HtmlContent node) {
        if (!(node instanceof HtmlElement)) {
            return NodeResult.keep;
        }
        HtmlElement element = (HtmlElement) node;
        String name = element.getName().toLowerCase();
        if (!"a".equals(name)) {
            return NodeResult.keep;
        }
        HtmlAttribute href = element.getAttribute("href");
        if (href == null) {
            return NodeResult.keep;
        }
        HtmlAttribute clazz = element.getAttribute("class");
        if (clazz == null) {
            clazz = new SimpleAttr("class", this.elementClass);
        } else {
            clazz = new SimpleAttr("class", clazz.getValue() + " " + this.elementClass);
        }
        element.setAttribute(clazz);
        return NodeResult.keep;
    }
    
    @Override
    public HtmlContent filterNode(HtmlContent content) {
        filter(content);
        return content;
    }
    
    private class SimpleAttr implements HtmlAttribute {
        private String name, value;
        private boolean singleQuotes = false;
        public SimpleAttr(String name, String value) {
            this.name = name;
            this.value = value;
        }
        public String getName() {
            return this.name;
        }
        public String getValue() {
            return this.value;
        }
        public void setName(String name) {
            this.name = name;
        }
        public void setValue(String value) {
            this.value = value;
        }
        public boolean hasValue() {
            return true;
        }
        public boolean isSingleQuotes() {
            return this.singleQuotes;
        }
        public void setSingleQuotes(boolean singleQuotes) {
            this.singleQuotes = singleQuotes;
        }
    }
}

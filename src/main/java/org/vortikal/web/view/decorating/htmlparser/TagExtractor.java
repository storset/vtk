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

import com.opensymphony.module.sitemesh.html.BasicRule;
import com.opensymphony.module.sitemesh.html.Tag;
import com.opensymphony.module.sitemesh.html.util.CharArray;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;


public class TagExtractor extends BasicRule {
    private HtmlElementImpl html, head, title, body;

    private Stack stack = new Stack();

    public boolean shouldProcess(String name) {
        return true;
    }

    public void process(Tag tag) {
        if (tag.getType() == Tag.OPEN) {
            start(tag);
        } else if (tag.getType() == Tag.CLOSE) {
            end(tag);
        } else if (tag.getType() == Tag.EMPTY) {
            empty(tag);
        }
    }

    public HtmlPageImpl getPage() {
        if (!this.stack.isEmpty()) {
            HtmlElementImpl top = (HtmlElementImpl) this.stack.peek();
            throw new IllegalStateException("Unable to parse HTML: "
                                            + "element '" + top.getName()
                                            + "' not closed");
        }
        return new HtmlPageImpl(this.html, this.head, this.title, this.body);
    }
        
        
    private void start(Tag tag) {
        String name = tag.getName();
        CharArray completeContent = new CharArray(64);
        tag.writeTo(completeContent);
        CharArray content = new CharArray(64);
        context.pushBuffer(new CharArray(64));
        HtmlAttributeImpl[] attrs = getAttributes(tag);
        HtmlElementImpl elem = new HtmlElementImpl(name, content,
                                                   completeContent, attrs);
        this.stack.push(elem);

        if ("html".equals(name) && this.html == null) {
            this.html = elem;
        } else if ("head".equals(name) && this.head == null) {
            this.head = elem;
        } else if ("title".equals(name) && this.title == null) {
            this.title = elem;
        } else if ("body".equals(name) && this.body == null) {
            this.body = elem;
        }
    }


    private void end(Tag tag) {

        HtmlElementImpl elem = (HtmlElementImpl) this.stack.pop();
        HtmlElementImpl parent = null;
        if (!stack.isEmpty()) {
            parent = (HtmlElementImpl) stack.peek();
            parent.addChildElement(elem);
        }
            
        CharArray buffer = context.popBuffer();

        elem.getContentBuffer().append(buffer);
        elem.getCompleteContentBuffer().append(buffer);
        tag.writeTo(elem.getCompleteContentBuffer());
        context.currentBuffer().append(elem.getCompleteContent());

    }

    private void empty(Tag tag) {
        String name = tag.getName();
        HtmlAttributeImpl[] attrs = getAttributes(tag);
        CharArray completeContent = new CharArray(64);
        tag.writeTo(completeContent);
        context.currentBuffer().append(completeContent);

        HtmlElementImpl elem = new HtmlElementImpl(name, new CharArray(0), 
                                                   completeContent, attrs);
        HtmlElementImpl parent = null;
        if (!stack.isEmpty()) {
            parent = (HtmlElementImpl) stack.peek();
            parent.addChildElement(elem);
        }

        if ("html".equals(name) && this.html == null) {
            this.html = elem;
        } else if ("head".equals(name) && this.head == null) {
            this.head = elem;
        } else if ("title".equals(name) && this.title == null) {
            this.title = elem;
        } else if ("body".equals(name) && this.body == null) {
            this.body = elem;
        }

    }

    private HtmlAttributeImpl[] getAttributes(Tag tag) {
        List attrs = new ArrayList();
        for (int i = 0; i < tag.getAttributeCount(); i++) {
            HtmlAttributeImpl attribute = new HtmlAttributeImpl(
                tag.getAttributeName(i), tag.getAttributeValue(i));
            attrs.add(attribute);
        }
        return (HtmlAttributeImpl[]) attrs.toArray(new HtmlAttributeImpl[attrs.size()]);
    }
}

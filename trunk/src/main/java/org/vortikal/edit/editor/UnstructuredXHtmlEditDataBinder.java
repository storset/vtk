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

import org.vortikal.text.html.HtmlContent;
import org.vortikal.text.html.HtmlElement;
import org.vortikal.text.html.HtmlPage;
import org.vortikal.text.html.HtmlPageFilter;
import org.vortikal.text.html.HtmlPageParser;

public class UnstructuredXHtmlEditDataBinder extends ResourceEditDataBinder {

    public UnstructuredXHtmlEditDataBinder(Object target, String objectName, HtmlPageParser htmlParser,
            HtmlPageFilter htmlPropsFilter) {
        super(target, objectName, htmlParser, htmlPropsFilter, null);
    }


    protected void parseContent(ResourceEditWrapper command, String suppliedContent) {

        super.parseContent(command, suppliedContent);

        try {
            HtmlPage page = command.getContent();

            String inputTitle = command.getValueByName("htmlTitle");
            if (inputTitle != null) {
                HtmlElement head = page.selectSingleElement("html.head");
                HtmlElement title = page.selectSingleElement("html.head.title");
                HtmlElement body = page.selectSingleElement("html.body");

                if (head != null && body != null) {
                    if (title == null) {
                        title = page.createElement("title");
                        head.addContent(title);
                    }
                    title.setChildNodes(new HtmlContent[] { page.createTextNode(inputTitle) });
                }
            }
            command.setContent(page);
            command.setContentChange(true);
            command.setPropChange(false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

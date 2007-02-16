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
package org.vortikal.web.view.decorating.components;

import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

import org.vortikal.web.view.decorating.DecoratorRequest;
import org.vortikal.web.view.decorating.DecoratorResponse;
import org.vortikal.web.view.decorating.HtmlElement;

public class HtmlElementComponent extends AbstractHtmlSelectComponent {

    private static final String ENCODING = "utf-8";
    private String exclude;
    
    public void setExclude(String exclude) {
        this.exclude = exclude;
    }
    

    public void processElements(HtmlElement[] elements, DecoratorRequest request,
                                DecoratorResponse response) throws Exception {
        String exclude = (this.exclude != null) ?
            this.exclude : request.getStringParameter("exclude");

        Set excludedElements = new HashSet();
        if (exclude != null) {
            String[] splitValues = exclude.split(",");
            for (int i = 0; i < splitValues.length; i++) {
                excludedElements.add(splitValues[i]);
            }
        }

        response.setCharacterEncoding(ENCODING);
        OutputStream out = response.getOutputStream();
        if (exclude == null) {
            for (int i = 0; i < elements.length; i++) {
                out.write(elements[i].getEnclosedContent().getBytes(ENCODING));    
            }

        } else {
            //HtmlElement[] children = elements.getChildElements();
            for (int i = 0; i < elements.length; i++) {
                if (excludedElements.contains(elements[i].getName())) {
                    continue;
                }
                out.write(elements[i].getEnclosedContent().getBytes(ENCODING));
                out.write("\n".getBytes(ENCODING));
            }
        }
        out.flush();
        out.close();
    }

}


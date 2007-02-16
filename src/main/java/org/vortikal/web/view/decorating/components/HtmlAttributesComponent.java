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
import org.vortikal.web.view.decorating.HtmlAttribute;
import org.vortikal.web.view.decorating.HtmlElement;

public class HtmlAttributesComponent extends AbstractHtmlSelectComponent {

    private String exclude;
    
    public void setExclude(String exclude) {
        this.exclude = exclude;
    }
    
    public void processElements(HtmlElement[] elements, DecoratorRequest request,
                                DecoratorResponse response) throws Exception {
        if (elements.length == 0) {
            return;
        }
        HtmlElement element = elements[0];
        
        String exclude = (this.exclude != null) ?
            this.exclude : request.getStringParameter("exclude");

        Set excludedAttributes = new HashSet();
        if (exclude != null) {
            String[] splitValues = exclude.split(",");
            for (int i = 0; i < splitValues.length; i++) {
                excludedAttributes.add(splitValues[i]);
            }
        }
        response.setCharacterEncoding("utf-8");
        OutputStream out = response.getOutputStream();
        StringBuffer sb = new StringBuffer();
        HtmlAttribute[] attributes = element.getAttributes();
        for (int i = 0; i < attributes.length; i++) {
            if (exclude != null && excludedAttributes.contains(attributes[i].getName())) {
                continue;
            }
            sb.append(" ").append(attributes[i].getName()).append("=\"");
            sb.append(attributes[i].getValue()).append("\"");
        }

        out.write(sb.toString().getBytes("utf-8"));
        out.flush();
        out.close();
    }

}


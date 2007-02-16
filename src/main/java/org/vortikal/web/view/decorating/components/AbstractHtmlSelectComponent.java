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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.vortikal.web.view.decorating.DecoratorRequest;
import org.vortikal.web.view.decorating.DecoratorResponse;
import org.vortikal.web.view.decorating.HtmlElement;
import org.vortikal.web.view.decorating.HtmlPage;

public abstract class AbstractHtmlSelectComponent extends AbstractDecoratorComponent {

    private String elementPath;

    public void setSelect(String select) {
        this.elementPath = select;
    }
    
    public void render(DecoratorRequest request, DecoratorResponse response) throws Exception {

        String expression = (this.elementPath != null) ?
            this.elementPath : request.getStringParameter("select");
        if (expression == null) {
            throw new IllegalArgumentException("Missing parameter 'select'");
        }

        String[] path = expression.split("\\.");        

        HtmlPage page = request.getHtmlPage();
        HtmlElement current = page.getRootElement();

        if (current == null) {
            return;
        }
        
        HtmlElement[] elements = new HtmlElement[] {current} ;

        int i = 0;
        while (i < path.length && elements.length > 0) {
            String name = path[i];
            List list = new ArrayList();
            
            for (int j = 0; j < elements.length; j++) {
                HtmlElement e = elements[j];
                HtmlElement[] m = e.getChildElements(name);
                list.addAll(Arrays.asList(m));
            }
        
            elements = (HtmlElement[])list.toArray(new HtmlElement[list.size()]);
            i++;
        }
        
        if (i < path.length - 1 || elements.length == 0) {
            return;
        }

        handleElements(elements, request, response);
    }


    protected abstract void handleElements(HtmlElement[] elements,
                                          DecoratorRequest request,
                                          DecoratorResponse response) throws Exception;

}


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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.vortikal.web.view.decorating.DecoratorRequest;
import org.vortikal.web.view.decorating.DecoratorResponse;
import org.vortikal.web.view.decorating.html.HtmlElement;
import org.vortikal.web.view.decorating.html.HtmlPage;


public abstract class AbstractHtmlSelectComponent extends AbstractDecoratorComponent {

    protected static final String PARAMETER_SELECT = "select";

    private static Log logger = LogFactory.getLog(AbstractDecoratorComponent.class);

    private String elementPath;

    public void setSelect(String select) {
        this.elementPath = select;
    }
    
    public void render(DecoratorRequest request, DecoratorResponse response) throws Exception {

        String expression = (this.elementPath != null) ?
            this.elementPath : request.getStringParameter(PARAMETER_SELECT);
        if (expression == null) {
            throw new DecoratorComponentException("Missing parameter 'select'");
        }

        String[] path = expression.split("\\.");

        HtmlPage page = request.getHtmlPage();
        HtmlElement current = page.getRootElement();

        if (current == null || !current.getName().equals(path[0])) {
            if (logger.isDebugEnabled()) {
                logger.debug("No node match for expression '" + expression + "'");
            }
            return;
        }

        List elements = new ArrayList();
        elements.add(current) ;

        int i = 1;
        while (i < path.length && elements.size() > 0) {
            String name = path[i];
            List list = new ArrayList();
            
            for (int j = 0; j < elements.size(); j++) {
                HtmlElement e = (HtmlElement)elements.get(j);
                HtmlElement[] m = e.getChildElements(name);
                list.addAll(Arrays.asList(m));
            }
        
            elements = list;
            i++;
        }
        
        if (i < path.length - 1 || elements.size() == 0) {
            return;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Processing elements: " + elements);
        }

        processElements(elements, request, response);
    }

    protected abstract void processElements(List elements,
                                            DecoratorRequest request,
                                            DecoratorResponse response) throws Exception;

}


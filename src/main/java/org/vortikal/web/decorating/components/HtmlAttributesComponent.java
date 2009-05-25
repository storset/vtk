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
package org.vortikal.web.decorating.components;

import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.vortikal.text.html.HtmlAttribute;
import org.vortikal.text.html.HtmlElement;
import org.vortikal.text.html.HtmlPage;
import org.vortikal.web.decorating.DecoratorRequest;
import org.vortikal.web.decorating.DecoratorResponse;

public class HtmlAttributesComponent extends AbstractDecoratorComponent {

    protected static final String PARAMETER_SELECT = "select";
    private static final String PARAMETER_EXCLUDE = "exclude";
    private static final String PARAMETER_EXCLUDE_DESC = "Comma-separated list of attribute names to exclude";
    private static final String PARAMETER_SELECT_DESC = "The element for which to select attributes";
    
    private String elementPath;
    private String exclude;
    
    public void setSelect(String select) {
        this.elementPath = select;
    }
    
    public void setExclude(String exclude) {
        this.exclude = exclude;
    }

    protected String getDescriptionInternal() {
        return "Outputs the attributes of a specified element";
    }

    protected Map<String, String> getParameterDescriptionsInternal() {
        Map<String, String> map = new HashMap<String, String>();
        map.put(PARAMETER_SELECT, PARAMETER_SELECT_DESC);
        map.put(PARAMETER_EXCLUDE, PARAMETER_EXCLUDE_DESC);
        return map;
    }

    public void render(DecoratorRequest request, DecoratorResponse response)
            throws Exception {
        String expression = (this.elementPath != null) ?
                this.elementPath : request.getStringParameter(PARAMETER_SELECT);
        if (expression == null) {
            throw new DecoratorComponentException("Missing parameter 'select'");
        }

        HtmlPage page = request.getHtmlPage();
        HtmlElement element = page.selectSingleElement(expression);
        if (element == null) {
            throw new DecoratorComponentException(
                "Unable to resolve element from expression: '" + expression + "'");
        }
        String exclude = (this.exclude != null) ?
                this.exclude : request.getStringParameter(PARAMETER_EXCLUDE);

        Set<String> excludedAttributes = new HashSet<String>();
        if (exclude != null && !exclude.trim().equals("")) {
            String[] splitValues = exclude.split(",");
            for (int i = 0; i < splitValues.length; i++) {
                excludedAttributes.add(splitValues[i]);
            }
        }
        Writer out = response.getWriter();
        StringBuilder sb = new StringBuilder();
        for (HtmlAttribute htmlAttribute: element.getAttributes()) {
            if (exclude != null && excludedAttributes.contains(htmlAttribute.getName())) {
                continue;
            }
            if (htmlAttribute.hasValue()) {
                sb.append(" ").append(htmlAttribute.getName()).append("=");
                sb.append(htmlAttribute.isSingleQuotes() ? "'" : "\"");
                sb.append(htmlAttribute.getValue());
                sb.append(htmlAttribute.isSingleQuotes() ? "'" : "\"");
            }
        }
        out.write(sb.toString());
        out.flush();
        out.close();
    }
}

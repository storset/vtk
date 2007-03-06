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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.vortikal.web.view.decorating.DecoratorRequest;
import org.vortikal.web.view.decorating.DecoratorResponse;
import org.vortikal.web.view.decorating.html.HtmlElement;

public class HtmlElementComponent extends AbstractHtmlSelectComponent {

    private static final String PARAMETER_EXCLUDE = "exclude";
    private static final String PARAMETER_EXCLUDE_DESC = 
        "Comma-separated list of element names to exclude (takes presedence over includes)";
    private static final String PARAMETER_INCLUDE = "include";
    private static final String PARAMETER_INCLUDE_DESC = 
        "Comma-separated list of element names to include";
    private static final String PARAMETER_ENCLOSED = "enclosed";
    private static final String PARAMETER_ENCLOSED_DESC = "If the selected element should enclose the content, set this to 'true'";
    
    private static final String PARAMETER_SELECT_DESC = "The element to select";

    private static final String DESCRIPTION = "Outputs the contents of the element(s) identified by select";
    
    private static final String ENCODING = "utf-8";
    private String include;
    private String exclude;
    private boolean enclosed = true;
    
    
    public void setExclude(String exclude) {
        this.exclude = exclude;
    }

    public void setEnclosed(boolean enclosed) {
        this.enclosed = enclosed;
    }
    
    
    public void processElements(HtmlElement[] elements, DecoratorRequest request,
                                DecoratorResponse response) throws Exception {

        boolean enclosed = (request.getStringParameter(PARAMETER_ENCLOSED) != null) ?
            "true".equals(request.getStringParameter(PARAMETER_ENCLOSED)) :
            this.enclosed;

        String include = (this.include != null) ?
            this.include : request.getStringParameter(PARAMETER_INCLUDE);

        Set includedElements = new HashSet();
        if (include != null) {
            String[] splitValues = exclude.split(",");
            for (int i = 0; i < splitValues.length; i++) {
                includedElements.add(splitValues[i]);
            }
        }

        String exclude = (this.exclude != null) ?
            this.exclude : request.getStringParameter(PARAMETER_EXCLUDE);

        Set excludedElements = new HashSet();
        if (exclude != null) {
            String[] splitValues = exclude.split(",");
            for (int i = 0; i < splitValues.length; i++) {
                excludedElements.add(splitValues[i]);
            }
        }

        response.setCharacterEncoding(ENCODING);
        OutputStream out = response.getOutputStream();

        for (int i = 0; i < elements.length; i++) {
            boolean included = true;
            if (excludedElements.contains(elements[i].getName())) {
                included = false;
            } else if (include != null && !include.contains(elements[i].getName())) {
                included = false;
            } 
            if (included) {
                if (enclosed) {
                    out.write(elements[i].getEnclosedContent().getBytes(ENCODING));
                } else {
                    out.write(elements[i].getContent().getBytes(ENCODING));
                }
            }
            
        }
        out.flush();
        out.close();
    }

    protected String getDescriptionInternal() {
        return DESCRIPTION;
    }

    protected Map getParameterDescriptionsInternal() {
        Map map = new HashMap();
        map.put(PARAMETER_SELECT, PARAMETER_SELECT_DESC);
        map.put(PARAMETER_INCLUDE, PARAMETER_INCLUDE_DESC);
        map.put(PARAMETER_EXCLUDE, PARAMETER_EXCLUDE_DESC);
        map.put(PARAMETER_ENCLOSED, PARAMETER_ENCLOSED_DESC);
        return map;
    }

}


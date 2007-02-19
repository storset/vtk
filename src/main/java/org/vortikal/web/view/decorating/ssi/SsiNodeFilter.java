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
package org.vortikal.web.view.decorating.ssi;


import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import org.vortikal.web.RequestContext;
import org.vortikal.web.view.decorating.DecoratorComponent;
import org.vortikal.web.view.decorating.DecoratorRequest;
import org.vortikal.web.view.decorating.DecoratorRequestImpl;
import org.vortikal.web.view.decorating.DecoratorResponseImpl;
import org.vortikal.web.view.decorating.HtmlComment;
import org.vortikal.web.view.decorating.HtmlContent;
import org.vortikal.web.view.decorating.HtmlElement;
import org.vortikal.web.view.decorating.HtmlNodeFilter;
import org.vortikal.web.view.decorating.HtmlPage;
import org.vortikal.web.view.decorating.HtmlText;


public class SsiNodeFilter implements HtmlNodeFilter {
    
    private static final Pattern SSI_DIRECTIVE_REGEXP = Pattern.compile(
            "#([a-zA-Z]+)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern SSI_PARAMETERS_REGEXP = Pattern.compile(
            "\\s*([a-zA-Z]*)\\s*=\\s*\"([^\"]+)\"",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private Map directiveComponentMap;
    

    public void setDirectiveComponentMap(Map directiveComponentMap) {
        this.directiveComponentMap = directiveComponentMap;
    }
    

    public HtmlContent filterNode(HtmlContent node) {
        if (node instanceof HtmlComment) {
            String content = node.getContent();
            if (content == null || "".equals(content.trim())) {
                return node;
            }
            
            String directive = null;
            Matcher directiveMatcher = SSI_DIRECTIVE_REGEXP.matcher(content);
            if (!directiveMatcher.find()) {
                return node;
            }
            directive = directiveMatcher.group(1);
            
            Matcher paramMatcher = SSI_PARAMETERS_REGEXP.matcher(
                content.substring(directiveMatcher.end()));

            Map parameters = new HashMap();

            while (paramMatcher.find()) {
                String name = paramMatcher.group(1);
                String value = paramMatcher.group(2);
                if (name != null && value != null) {
                    parameters.put(name, value);
                }
            }
            HtmlContent filteredNode = invokeComponent(node, directive, parameters);
            return filteredNode;
            //System.out.println("_________directive: " + directive + ", params: " + parameters);
        }
        return node;
    }
    
    private HtmlContent invokeComponent(HtmlContent node, String directive, Map parameters) {
        DecoratorComponent component = (DecoratorComponent)
            this.directiveComponentMap.get(directive);
        if (component == null) {
            return node;
        }

        RequestContext requestContext = requestContext = RequestContext.getRequestContext();
        HttpServletRequest servletRequest = requestContext.getServletRequest();
        
        org.springframework.web.servlet.support.RequestContext ctx =
            new org.springframework.web.servlet.support.RequestContext(servletRequest);
        Locale locale = ctx.getLocale();

        final String doctype = "";
        HtmlPage dummyPage = new HtmlPage() {
           public String getDoctype() {
               return doctype;
           }
           public HtmlElement getRootElement() {
               return null;
           }
        };
        
        DecoratorRequest decoratorRequest = new DecoratorRequestImpl(
            new HashMap(), dummyPage, servletRequest, parameters, doctype, locale);

        DecoratorResponseImpl response = new DecoratorResponseImpl(doctype, locale, "utf-8");
        
        String result = null;
        try {
            component.render(decoratorRequest, response);
            result = response.getContentAsString();
        } catch (Throwable t) {
            result = "SSI: " + t.getMessage();
        }
        final String text = result;
        return new HtmlText() {
            public String getContent() {
                return text;
            }
        };
    }
    

}

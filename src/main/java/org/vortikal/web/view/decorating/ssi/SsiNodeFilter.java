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

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;

import org.vortikal.web.RequestContext;
import org.vortikal.web.view.decorating.ComponentInvocation;
import org.vortikal.web.view.decorating.DecoratorComponent;
import org.vortikal.web.view.decorating.DecoratorRequest;
import org.vortikal.web.view.decorating.DecoratorRequestImpl;
import org.vortikal.web.view.decorating.DecoratorResponseImpl;
import org.vortikal.web.view.decorating.TemplateParser;
import org.vortikal.web.view.decorating.TemplateSource;
import org.vortikal.web.view.decorating.html.HtmlComment;
import org.vortikal.web.view.decorating.html.HtmlContent;
import org.vortikal.web.view.decorating.html.HtmlElement;
import org.vortikal.web.view.decorating.html.HtmlNodeFilter;
import org.vortikal.web.view.decorating.html.HtmlPage;
import org.vortikal.web.view.decorating.html.HtmlText;


public class SsiNodeFilter implements HtmlNodeFilter, InitializingBean {
    
    private static final Pattern SSI_DIRECTIVE_REGEXP = Pattern.compile(
            "#([a-zA-Z]+)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern SSI_PARAMETERS_REGEXP = Pattern.compile(
            "\\s*([a-zA-Z]*)\\s*=\\s*\"([^\"]+)\"",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private Map directiveComponentMap;
    
    private TemplateParser contentComponentParser;
    

    public void setDirectiveComponentMap(Map directiveComponentMap) {
        this.directiveComponentMap = directiveComponentMap;
    }
    
    public void setContentComponentParser(TemplateParser contentComponentParser) {
        this.contentComponentParser = contentComponentParser;
    }
    
    public void afterPropertiesSet() {
        if (this.directiveComponentMap == null) {
            throw new BeanInitializationException(
                "JavaBean property 'directiveComponentMap' not specified");
        }
    }
    

    public HtmlContent filterNode(HtmlContent node) {

        if (node instanceof HtmlComment) {
            ComponentInvocation ssiInvocation = buildSsiComponentInvocation(node);
            if (ssiInvocation == null) {
                return node;
            }
            HtmlContent filteredNode = invokeComponents(new ComponentInvocation[] {ssiInvocation});
            return filteredNode;

        } else if (node instanceof HtmlText) {
            if (this.contentComponentParser == null) {
                return node;
            }
            String content = ((HtmlText) node).getContent();
            try {
                ComponentInvocation[] parsedContent =
                    this.contentComponentParser.parseTemplate(new java.io.StringReader(content));
                HtmlContent filteredNode = invokeComponents(parsedContent);
                return filteredNode;
            } catch (Exception e) {
                return node;
            }
        }
        return node;
    }

    
    private ComponentInvocation buildSsiComponentInvocation(HtmlContent node) {
        String content = node.getContent();
        if (content == null || "".equals(content.trim())) {
            return null;
        }
            
        String directive = null;
        Matcher directiveMatcher = SSI_DIRECTIVE_REGEXP.matcher(content);
        if (!directiveMatcher.find()) {
            return null;
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

        final DecoratorComponent component = (DecoratorComponent)
            this.directiveComponentMap.get(directive);
        if (component == null) {
            return null;
        }
        final Map invocationParams = parameters;
        return new ComponentInvocation() {
            public Map getParameters() {
                return invocationParams;
            }
            public DecoratorComponent getComponent() {
                return component;
            }
        };
    }
    

    private HtmlContent invokeComponents(ComponentInvocation[] components) {
        HttpServletRequest servletRequest = RequestContext.getRequestContext().getServletRequest();
        
        org.springframework.web.servlet.support.RequestContext ctx =
            new org.springframework.web.servlet.support.RequestContext(servletRequest);
        Locale locale = ctx.getLocale();

        final String doctype = "";
        
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < components.length; i++) {
            Map parameters = components[i].getParameters();
            DecoratorRequest decoratorRequest = new DecoratorRequestImpl(
                new HashMap(), null, servletRequest, parameters, doctype, locale);
            DecoratorResponseImpl response = new DecoratorResponseImpl(doctype, locale, "utf-8");
            String result = null;
            try {
                DecoratorComponent component = components[i].getComponent();
                component.render(decoratorRequest, response);
                result = response.getContentAsString();
            } catch (Throwable t) {
                result = "Error: " + t.getMessage();
            }
            sb.append(result);
        }
        final String text = sb.toString();
        return new HtmlText() {
            public String getContent() {
                return text;
            }
        };
    }
    

}

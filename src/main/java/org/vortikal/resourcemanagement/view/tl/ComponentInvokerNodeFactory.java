/* Copyright (c) 2009, University of Oslo, Norway
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
package org.vortikal.resourcemanagement.view.tl;

import java.io.Writer;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;

import javax.servlet.http.HttpServletRequest;

import org.vortikal.resourcemanagement.view.StructuredResourceDisplayController;
import org.vortikal.text.html.HtmlPageParser;
import org.vortikal.text.tl.Argument;
import org.vortikal.text.tl.Context;
import org.vortikal.text.tl.DirectiveNodeFactory;
import org.vortikal.text.tl.DirectiveParseContext;
import org.vortikal.text.tl.Node;
import org.vortikal.web.RequestContext;
import org.vortikal.web.decorating.ComponentResolver;
import org.vortikal.web.decorating.DecoratorComponent;
import org.vortikal.web.decorating.DecoratorRequest;
import org.vortikal.web.decorating.DecoratorRequestImpl;
import org.vortikal.web.decorating.DecoratorResponseImpl;
import org.vortikal.web.decorating.TemplateExecution;

public class ComponentInvokerNodeFactory implements DirectiveNodeFactory {

    private static final String COMPONENT_STACK_REQ_ATTR = 
        ComponentInvokerNodeFactory.class.getName() + ".ComponentStack";
    
    private HtmlPageParser htmlParser;
    
    public ComponentInvokerNodeFactory(HtmlPageParser htmlParser) {
        this.htmlParser = htmlParser;
    }

    public Node create(DirectiveParseContext ctx) throws Exception {
        List<Argument> args = ctx.getArguments();
        if (args.size() == 0 || args.size() > 2) {
            throw new RuntimeException(
                    "Wrong number of arguments: expected <component-reference> <params>");
        }
        final Argument arg1 = args.get(0);
        final Argument arg2 = args.size() == 2 ? args.get(1) : null;
        return new Node() {
            @SuppressWarnings("unchecked")
            public void render(Context ctx, Writer out) throws Exception {
                Object componentRef = arg1.getValue(ctx);
                Object parameterMap = arg2 != null ? arg2.getValue(ctx) : null;

                if (!(componentRef instanceof String)) {
                    throw new RuntimeException("First argument must be a string");
                }
                if (parameterMap != null) {
                    if (!(parameterMap instanceof Map<?, ?>)) {
                        throw new RuntimeException("Second argument must be a map");
                    }
                    for (Map.Entry<?, ?> entry : ((Map<?, ?>) parameterMap).entrySet()) {
                        if (entry.getKey() == null || entry.getValue() == null) {
                            throw new RuntimeException("NULL values not allowed in parametermap");
                        }
                        
                        if (!(entry.getKey() instanceof String)
                                || !(entry.getValue() instanceof String)) {
                            throw new RuntimeException("Parameter map values must be strings");
                        }
                    }
                }
                String namespace = null;
                String name = (String) componentRef;
                if (name.contains(":")) {
                    namespace = name.substring(0, name.indexOf(":"));
                    name = name.substring(namespace.length() + 1);
                }
                
                RequestContext requestContext = RequestContext.getRequestContext();
                HttpServletRequest servletRequest = requestContext.getServletRequest();
                TemplateExecution execution = (TemplateExecution) 
                    servletRequest.getAttribute(StructuredResourceDisplayController.TEMPLATE_EXECUTION_REQ_ATTR);
                ComponentResolver componentResolver = execution.getComponentResolver();
                DecoratorComponent component = componentResolver.resolveComponent(namespace, name);
                if (component == null) {
                    throw new RuntimeException("Unable to resolve component '" + componentRef + "'");
                }
                
                Stack<DecoratorComponent> componentStack = 
                    (Stack<DecoratorComponent>) servletRequest.getAttribute(COMPONENT_STACK_REQ_ATTR);
                if (componentStack == null) {
                    componentStack = new Stack<DecoratorComponent>();
                    servletRequest.setAttribute(COMPONENT_STACK_REQ_ATTR, componentStack);
                }
                
                for (DecoratorComponent c : componentStack) {
                    if (c == component) {
                        throw new RuntimeException("Component invocation loop detected: " 
                                + c.getNamespace() + ":" + c.getName());
                    }
                }
                componentStack.push(component);
                try {
                    Locale locale = ctx.getLocale();
                    final String doctype = "";

                    Map<Object, Object> mvcModel = (Map<Object, Object>) ctx.get(StructuredResourceDisplayController.MVC_MODEL_KEY);
                    DecoratorRequest decoratorRequest = new DecoratorRequestImpl(
                            null, servletRequest, mvcModel, 
                            (Map<String, Object>) parameterMap, doctype, locale);
                    DecoratorResponseImpl decoratorResponse = new DecoratorResponseImpl(
                            doctype, locale, "utf-8");
                    component.render(decoratorRequest, decoratorResponse);
                    out.write(decoratorResponse.getContentAsString());
                } finally {
                    componentStack.pop();
                }
            }
        };
    }
}    

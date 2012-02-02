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
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import javax.servlet.http.HttpServletRequest;

import org.vortikal.resourcemanagement.view.StructuredResourceDisplayController;
import org.vortikal.text.html.HtmlPage;
import org.vortikal.text.tl.Context;
import org.vortikal.text.tl.DirectiveNodeFactory;
import org.vortikal.text.tl.DirectiveParseContext;
import org.vortikal.text.tl.Node;
import org.vortikal.text.tl.Token;
import org.vortikal.text.tl.expr.Expression;
import org.vortikal.text.tl.expr.Function;
import org.vortikal.web.RequestContext;
import org.vortikal.web.decorating.ComponentResolver;
import org.vortikal.web.decorating.DecoratorComponent;
import org.vortikal.web.decorating.DecoratorRequest;
import org.vortikal.web.decorating.DecoratorRequestImpl;
import org.vortikal.web.decorating.DecoratorResponseImpl;

public class ComponentInvokerNodeFactory implements DirectiveNodeFactory {

    private static final String COMPONENT_STACK_REQ_ATTR = 
        ComponentInvokerNodeFactory.class.getName() + ".ComponentStack";
    
    private ComponentSupport componentSupport;
    private Set<Function> functions;
    
    public ComponentInvokerNodeFactory(ComponentSupport componentSupport, Set<Function> functions) {
        if (componentSupport == null) {
            throw new IllegalArgumentException("Constructor argument is NULL");
        }
        this.componentSupport = componentSupport;
        this.functions = functions;
    }
    

    public interface ComponentSupport {
        public ComponentResolver getComponentResolver(Context context);
        public HtmlPage getHtmlPage(Context context);
    }
    
    protected DecoratorComponent resolveComponent(Context context, String namespace, String name) {
        ComponentResolver componentResolver = this.componentSupport.getComponentResolver(context);
        
        DecoratorComponent component = componentResolver.resolveComponent(namespace, name);
        return component;
    }
    
    protected HtmlPage getHtmlPage(Context context) {
        return this.componentSupport.getHtmlPage(context);
    }
    
    public Node create(DirectiveParseContext ctx) throws Exception {
        List<Token> args = ctx.getArguments();
        
        if (args.size() == 0) {
            throw new RuntimeException(
                    "Wrong number of arguments:" + ctx.getNodeText() 
                    + ": expected <component-reference> <params>");
        }
        
        final Token arg1 = args.remove(0);
        final Expression expression = args.size() > 0 ? new Expression(this.functions, args) : null;

        return new Node() {
            @SuppressWarnings("unchecked")
            public boolean render(Context ctx, Writer out) throws Exception {
                Object componentRef = arg1.getValue(ctx);
                
                if (!(componentRef instanceof String)) {
                    throw new RuntimeException("First argument must be a string");
                }

                Object parameterMap = Collections.<String, Object>emptyMap();
                if (expression != null) {
                    try {
                        parameterMap = expression.evaluate(ctx);
                    } catch (Throwable t) {
                        out.write(componentRef + ":" + t.getMessage());
                        return true;
                    }
                }
                if (!(parameterMap instanceof Map<?, ?>)) {
                    out.write(componentRef + ": second argument must be a map: " + parameterMap);
                    return true;
                }
                for (Map.Entry<?, ?> entry : ((Map<?, ?>) parameterMap).entrySet()) {
//                    if (entry.getKey() == null || entry.getValue() == null) {
//                        throw new RuntimeException("NULL values not allowed in parameter map: " + entry.getKey() + " = " + entry.getValue());
//                    }
                    if (!(entry.getKey() instanceof String)) {
                        out.write(componentRef + ": parameter name must be string: " + entry.getKey());
                    }
                }

                String namespace = null;
                String name = (String) componentRef;
                if (name.contains(":")) {
                    namespace = name.substring(0, name.indexOf(":"));
                    name = name.substring(namespace.length() + 1);
                }

                DecoratorComponent component = resolveComponent(ctx, namespace, name);
                if (component == null) {
                    out.write("Unable to resolve component '" + namespace + ":" + name + "'");
                    return true;
                }
                
                RequestContext requestContext = RequestContext.getRequestContext();
                HttpServletRequest servletRequest = requestContext.getServletRequest();
                Stack<DecoratorComponent> componentStack = 
                    (Stack<DecoratorComponent>) servletRequest.getAttribute(COMPONENT_STACK_REQ_ATTR);
                if (componentStack == null) {
                    componentStack = new Stack<DecoratorComponent>();
                    servletRequest.setAttribute(COMPONENT_STACK_REQ_ATTR, componentStack);
                }
                
                for (DecoratorComponent c : componentStack) {
                    if (c == component) {
                        out.write("Component invocation loop detected: '" + c.getNamespace() + ":" + c.getName()+ "'");
                        return true;
                    }
                }
                componentStack.push(component);
                try {
                    Locale locale = ctx.getLocale();
                    final String doctype = "";
                    
                    Map<String, Object> mvcModel = (Map<String, Object>) servletRequest.getAttribute(StructuredResourceDisplayController.MVC_MODEL_REQ_ATTR);
                    DecoratorRequest decoratorRequest = new DecoratorRequestImpl(
                            getHtmlPage(ctx), servletRequest, mvcModel, 
                            (Map<String, Object>) parameterMap, doctype, locale);
                    DecoratorResponseImpl decoratorResponse = new DecoratorResponseImpl(
                            doctype, locale, "utf-8");
                    component.render(decoratorRequest, decoratorResponse);
                    out.write(decoratorResponse.getContentAsString());
                } catch (Throwable t) {
                    out.write(component.getNamespace() + ":" + component.getName()+ ": " + t.getMessage());
                    
                } finally {
                    componentStack.pop();
                }
                return true;
            }
        };
    }
}    

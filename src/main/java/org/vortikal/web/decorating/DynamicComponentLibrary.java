/* Copyright (c) 2014, University of Oslo, Norway
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
package org.vortikal.web.decorating;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.vortikal.text.tl.Context;
import org.vortikal.text.tl.DirectiveHandler;
import org.vortikal.text.tl.DirectiveState;
import org.vortikal.text.tl.DirectiveValidator;
import org.vortikal.text.tl.Literal;
import org.vortikal.text.tl.Node;
import org.vortikal.text.tl.NodeList;
import org.vortikal.text.tl.Parser.Directive;
import org.vortikal.text.tl.TemplateContext;
import org.vortikal.text.tl.TemplateHandler;
import org.vortikal.text.tl.TemplateParser;
import org.vortikal.text.tl.Token;
import org.vortikal.util.io.InputSource;
import org.vortikal.web.decorating.components.DynamicDecoratorComponent;

public class DynamicComponentLibrary {
    private static Log logger = LogFactory.getLog(DynamicComponentLibrary.class);

    private String namespace = null;
    private List<DecoratorComponent> components = new ArrayList<DecoratorComponent>();
    private List<String> errors = null;
    List<DirectiveHandler> directiveHandlers;
    
    private InputSource inputSource;

    public DynamicComponentLibrary(String namespace, List<DirectiveHandler> directiveHandlers, 
            InputSource inputSource) throws IOException {
        this.namespace = namespace;
        this.directiveHandlers = new ArrayList<DirectiveHandler>(directiveHandlers);
        this.inputSource = inputSource;
        try {
            compile();
        } catch (Exception e) {
            logger.warn("Failed to compile component(s) in " + inputSource, e);
        }
    }

    public List<String> errors() {
        return errors;
    }
    
    public List<DecoratorComponent> components() {
        return components;
    }
    
    public String getNamespace() {
        return namespace;
    }

    public void compile() throws IOException {
        InputStream inputStream = inputSource.getInputStream();
        Reader reader = new InputStreamReader(inputStream);

        final List<DynamicDecoratorComponent.Builder> builders = 
                new ArrayList<DynamicDecoratorComponent.Builder>();
        final Map<String, Object> messages = new HashMap<String, Object>();
        
        List<DirectiveHandler> handlers = new ArrayList<DirectiveHandler>(this.directiveHandlers);

        
        DirectiveHandler componentHandler = new DirectiveHandler() {
            private DynamicDecoratorComponent.Builder cur = null;
            @Override
            public String[] tokens() {
                return new String[] { 
                        "messages", "/messages", 
                        "component", "/component", 
                        "description", "/description", 
                        "parameter",
                        "error"
                        };
            }
            
            @Override
            public void directive(Directive directive, TemplateContext context) {
                String name = directive.name();
                if (cur != null) {
                    cur.context(directive);
                }
                if ("component".equals(name)) {
                    cur = DynamicDecoratorComponent.newBuilder();
                    componentStart(directive, context, cur);
                } else if ("/component".equals(name)) {
                    componentEnd(context, cur);
                    builders.add(cur);
                } else if ("description".equals(name)) {
                    descriptionStart(directive, context);
                } else if ("/description".equals(name)) {
                    descriptionEnd(context, cur);
                } else if ("parameter".equals(name)) {
                    parameter(directive, context, cur);
                } else if ("error".equals(name)) {
                    error(directive, context);
                } else if ("messages".equals(name)) {
                    messagesStart(directive, context);
                } else if ("/messages".equals(name)) {
                    messagesEnd(context);
                } else {
                    unknown(directive, context);
                }
            }
            
            private void componentStart(Directive directive, TemplateContext context, DynamicDecoratorComponent.Builder builder) {
                if (context.level() != 0) {
                    context.error("[component] cannot be a child of "
                            + "[" + context.top().directive().name() + "]");
                    return;
                }
                context.push(new DirectiveState(directive));
                if (directive.args().size() > 0)
                    builder.name(directive.args().get(0).getRawValue());
                builder.namespace(namespace);
            }
            
            private void componentEnd(TemplateContext context, DynamicDecoratorComponent.Builder builder) {
                DirectiveState state = context.pop();
                if (state == null || !"component".equals(state.directive().name())) {
                    context.error("Misplaced [/component]");
                    return;
                }
                builder.body(state.nodes());
            }
            
            private void descriptionStart(Directive directive, TemplateContext context) {
                DirectiveState state = context.top();
                if (state == null || !"component".equals(state.directive().name())) {
                    context.error("Misplaced [description]");
                    return;
                }
                context.push(new DirectiveState(directive));
            }
            
            private void descriptionEnd(TemplateContext context, DynamicDecoratorComponent.Builder builder) {
                DirectiveState state = context.pop();
                if (state == null || !"description".equals(state.directive().name())) {
                    context.error("Misplaced [/description]");
                    return;
                }
                NodeList nodes = state.nodes();
                Context ctx = new Context(Locale.getDefault());
                StringWriter out = new StringWriter();
                for (Node node: nodes) {
                    try {
                        node.render(ctx, out);
                    } catch (Exception e) { 
                        context.error(e.getMessage()); 
                        return; 
                    }
                }
                builder.description(out.toString());
            }
            
            private void parameter(Directive directive, TemplateContext context, DynamicDecoratorComponent.Builder builder) {
                DirectiveState state = context.top();
                if (state == null || !"component".equals(state.directive().name())) {
                    context.error("Misplaced [parameter]");
                    return;
                }
                List<Token> args = directive.args();
                if (args.size() != 2) {
                    context.error("Malformed parameter directive. "
                            + "Should be: [parameter name description]");
                    return;
                }
                String arg1 = args.get(0).getRawValue();
                Token arg2 = args.get(1);
                if (!(arg2 instanceof Literal) || ((Literal) arg2).getType() != Literal.Type.STRING) {
                    context.error("[parameter] description must be a string");
                    return;
                }
                builder.parameter(arg1, ((Literal) arg2).getStringValue());
            }
            
            private void error(Directive directive, TemplateContext context) {
                List<Token> args = directive.args();
                if (args.size() != 1) {
                    context.error("[error]: missing argument");
                    return;
                }
                final Token arg = args.get(0);
                context.add(new Node() {
                    @Override
                    public boolean render(Context ctx, Writer out)
                            throws Exception {
                        Object msg = arg.getValue(ctx); 
                        out.write(msg != null ? msg.toString() : "null");
                        return false;
                    }});
            }
            
            private void messagesStart(Directive directive, TemplateContext context) {
                if (context.level() != 0) {
                    context.error("[messages] must be a top-level directive");
                    return;
                }
                context.push(new DirectiveState(directive));
            }            
            
            private void messagesEnd(TemplateContext context) {
                DirectiveState state = context.pop();
                if (state == null || !"messages".equals(state.directive().name())) {
                    context.error("Misplaced [/messages]");
                    return;
                }
                NodeList nodes = state.nodes();
                Context ctx = new Context(Locale.getDefault());
                StringWriter out = new StringWriter();
                for (Node node: nodes) {
                    try {
                        node.render(ctx, out);
                    } catch (Exception e) { 
                        context.error(e.getMessage()); 
                        return; 
                    }
                }
                addMessages(out.toString(), messages);
            }
            
            private void unknown(Directive directive, TemplateContext context) {
                final String msg = inputSource + ":" 
                        + directive.line() + ": Unknown directive: [" 
                        + directive.name() + "]";
                context.add(new Node() {
                    @Override
                    public boolean render(Context ctx, Writer out)
                            throws Exception {
                        out.write(msg);
                        return true;
                    }
                });
            }
            
        };
        
        handlers.add(componentHandler);
        
        DirectiveValidator validator = new DirectiveValidator() {
            @Override
            public void validate(Directive directive, TemplateContext context) {
                // Ensure no directives inside [description]
                // Rest of validation is performed in handleXXX() methods
                DirectiveState state = context.top();
                if (state != null && "description".equals(state.directive().name())) {
                    if (!"/description".equals(directive.name()))
                        context.error("Directive [" + directive.name() 
                                + "] not allowed inside [description]");
                }
            }
        };
        
        final List<String> errors = new ArrayList<String>();
        TemplateParser parser = new TemplateParser(reader, handlers, componentHandler, validator, new TemplateHandler() {
            @Override
            public void success(NodeList nodeList) { }
            @Override
            public void error(String message, int line) {
                errors.add(inputSource + ":" + line + ": " + message);
            }
        });

        parser.parse();
        
        if (errors.isEmpty()) {
            List<DecoratorComponent> list = new ArrayList<DecoratorComponent>();
            for (DynamicDecoratorComponent.Builder builder: builders) {
                try {
                    builder.messages(messages);
                    list.add(builder.build());
                } catch (Exception e) {
                    String name = builder.name();
                    if (name != null) {
                        String message = e.getMessage();
                        Directive context = builder.context();
                        if (context != null) {
                            message = inputSource + ":" + context.line() + ": " + message;
                        }
                        message = "Error compiling component " + name + ": " + message;
                        list.add(errorComponent(name, message));
                    } else {
                        if (builder.context() != null)
                            errors.add(inputSource + ":" + builder.context().line() + ": " + e.getMessage());
                        else errors.add(inputSource + ": " + e.getMessage());
                    }
                }
            }
            this.components = Collections.unmodifiableList(list);
        }
        if (!errors.isEmpty()) {
            this.errors = Collections.unmodifiableList(errors);
            logger.info("Failed to compile component(s) in " + inputSource + ": " + errors);
        }
        else {            
            this.errors = null;
            logger.info("Compiled " + components.size() + " components in " + inputSource);
        }
    }
    
    private DecoratorComponent errorComponent(String name, final String message) {
        DynamicDecoratorComponent.Builder builder = DynamicDecoratorComponent.newBuilder();
        builder.name(name);
        builder.namespace(namespace);
        builder.description("");
        builder.body(new NodeList(new Node() {
            @Override
            public boolean render(Context ctx, Writer out) throws Exception {
                out.write(message);
                return true;
            }}));
        return builder.build();
    }

    private void addMessages(String string, Map<String, Object> map) {
        try {
            BufferedReader reader = new BufferedReader(new StringReader(string));
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                int eqPos = line.indexOf('=');
                if (eqPos < 1) continue;
                String key = line.substring(0, eqPos).trim();
                String val = line.substring(eqPos + 1).replace("\\=", "=").trim();
                
                String[] elems = key.split("\\.");
                Map<String, Object> cur = map;
                for (int i = 0; i < elems.length - 1; i++) {
                    String elem = elems[i];
                    Map<String, Object> newMap = null;
                    if (!cur.containsKey(elem)) {
                        newMap = new HashMap<String, Object>();
                        cur.put(elem, newMap);
                    } else {
                        Object existing = cur.get(elem);
                        if (existing instanceof Map<?,?>) {
                            newMap = (Map<String, Object>) existing;
                        } else {
                            newMap = new HashMap<String, Object>();
                            cur.put(elem, newMap);
                        }
                    }
                    cur = newMap;
                }
                cur.put(elems[elems.length -1], val);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
    
}

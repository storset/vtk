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
package org.vortikal.text.tl;

import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public final class TemplateParser implements Parser.Handler, TemplateContext {
    private Parser parser = null;
    private Map<String, DirectiveHandler> handlers = 
            new HashMap<String, DirectiveHandler>();
    private DirectiveHandler unknownHandler = null;
    private DirectiveValidator validator = null;

    private Stack<DirectiveState> nestedHandlers = new Stack<DirectiveState>();
    private NodeList nodes = new NodeList();
    private TemplateHandler consumer = null;

    public TemplateParser(Reader reader, List<DirectiveHandler> handlers, 
            TemplateHandler consumer) {
        this(reader, handlers, null, null, consumer);
    }
    
    public TemplateParser(Reader reader, List<DirectiveHandler> handlers, 
            DirectiveHandler unknownHandler, DirectiveValidator validator, 
            TemplateHandler consumer) {
        for (DirectiveHandler handler: handlers)
            if (handler.tokens() == null || handler.tokens().length == 0)
                throw new IllegalArgumentException(
                        "Directive handler " + handler 
                        + " does not specify any tokens");
            else for (String token: handler.tokens())
                if (this.handlers.containsKey(token))
                    throw new IllegalArgumentException(
                            "Cannot register handler " + handler + ": token '"
                            + token 
                            + "' is already specified by another directive handler");
                else this.handlers.put(token, handler);
        this.unknownHandler = unknownHandler;
        this.validator = validator;
        this.consumer = consumer;
        parser = new Parser(reader, this);
    }

    public void parse() {
        parser.parse();
    }
    
    @Override
    public void push(DirectiveState state) { 
        nestedHandlers.push(state); 
    }
    
    @Override
    public DirectiveState top() { 
        if (nestedHandlers.size() == 0) return null;
        return nestedHandlers.peek();
    }
    
    @Override
    public DirectiveState pop() { 
        if (nestedHandlers.size() == 0) return null;
        return nestedHandlers.pop();
    }        

    @Override
    public int level() {
        return nestedHandlers.size();
    }
    
    @Override
    public void add(Node node) {
        if (nestedHandlers.size() == 0) nodes.add(node);
        else top().nodes().add(node);
    }
    
    @Override
    public void start() {
    }
    
    @Override
    public void directive(Parser.Directive directive) {
        DirectiveHandler handler = handlers.get(directive.name());
        if (validator != null)
            validator.validate(directive, this);
        if (parser.isStopped()) return;
        if (handler == null && unknownHandler != null)
            unknownHandler.directive(directive, this);
        else if (handler == null)
            consumer.error(
                    "Unknown directive '" + directive.name() + "'",
                    parser.getLineNumber());
        else handler.directive(directive, this);
    }

    @Override
    public void text(String text) {
        add(new TextNode(text));
    }

    @Override
    public void raw(String text) {
        add(new RawNode(text));
    }

    @Override
    public void comment(String text) {
        add(new CommentNode(text));
    }

    @Override
    public void error(String message, int line) {
        consumer.error(message, line);
        parser.stop();
    }

    @Override
    public void error(String message) {
        error(message, parser.getLineNumber());
    }

    @Override
    public void end() {
        if (!nestedHandlers.isEmpty()) 
            error("Unterminated directive: " 
                    + nestedHandlers.peek().directive());
        else consumer.success(nodes);
    }
}
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
package org.vortikal.web.decorating.components;

import java.io.Writer;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.vortikal.text.tl.Context;
import org.vortikal.text.tl.NodeList;
import org.vortikal.text.tl.Parser.Directive;
import org.vortikal.web.decorating.DecoratorComponent;
import org.vortikal.web.decorating.DecoratorRequest;
import org.vortikal.web.decorating.DecoratorResponse;

public class DynamicDecoratorComponent implements DecoratorComponent {

    private String namespace = null;
    private String name = null;
    private String description = null;
    private Map<String, String> parameters = null;
    private Map<String, Object> messages = null;
    private NodeList body = null;
    
    public static Builder newBuilder() { return new Builder(); }
    
    private DynamicDecoratorComponent(String namespace, String name, String description, 
            Map<String, String> parameters, Map<String, Object> messages, NodeList body) {
        this.namespace = namespace;
        this.name = name;
        this.description = description;
        this.parameters = parameters;
        this.messages = messages;
        this.body = body;
    }

    
    @Override
    public String getNamespace() {
        return namespace;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public Map<String, String> getParameterDescriptions() {
        return parameters;
    }

    @Override
    public Collection<UsageExample> getUsageExamples() {
        return Collections.emptyList();
    }

    @Override
    public void render(DecoratorRequest request, DecoratorResponse response)
            throws Exception {
        Context ctx = new Context(request.getLocale());
        
        Map<String, Object> req = new HashMap<String, Object>();
        req.put("lang", request.getLocale().getLanguage());
        req.put("messages", messages);

        Map<String, Object> params = new HashMap<String, Object>();
        for (String param: parameters.keySet()) {
            params.put(param, request.getRawParameter(param));
        }
        req.put("parameters", params);
        ctx.define("request", req, true);

        Writer writer = response.getWriter();
        body.render(ctx, writer);
        writer.flush();
    }

    @Override
    public String toString() {
        return namespace + ":" + name + " " + body;
    }
    
    public static final class Builder {
        private String name = null;
        private String namespace = null;
        private String description = null;
        private Directive context = null;
        private NodeList body = null;
        private Map<String, String> parameters = new LinkedHashMap<String, String>();
        private Map<String, Object> messages = null;
        private Builder() {}
        public Builder name(String name) 
            { this.name = name; return this; }
        public Builder namespace(String namespace) 
        { this.namespace = namespace; return this; }
        public Builder description(String description) 
            { this.description = description; return this; }
        public Builder parameter(String name, String value) 
            { parameters.put(name, value); return this; }
        public Builder messages(Map<String,Object> messages)
            { this.messages = messages; return this; }
        public Builder body(NodeList body) 
            { this.body = body; return this; }
        public Builder context(Directive context)
            { this.context = context; return this; }
        public DynamicDecoratorComponent build() {
            if (name == null) throw new IllegalArgumentException("Missing component name");
            if (namespace == null) throw new IllegalArgumentException("Missing component namespace");
            if (description == null) throw new IllegalArgumentException("Missing component description");
            if (body == null) throw new IllegalArgumentException("Missing component body");
            if (parameters == null) parameters = new LinkedHashMap<String, String>();
            if (messages == null) messages = new LinkedHashMap<String, Object>();
            return new DynamicDecoratorComponent(
                    namespace, name, description, parameters, messages, body);
        }
        public Directive context() { return context; }
        public String name() { return name; }
        @Override
        public String toString() {
            return "name=" + name + ",namespace=" + namespace + ",description="
                    + description + ",context=" + context + ",body=" + body 
                    + ",parameters=" + parameters + ",messages=" + messages;
        }
    }
}

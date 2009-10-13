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
package org.vortikal.text.tl;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.Writer;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DefineNodeFactory implements DirectiveNodeFactory {

    private Map<String, ValueProvider> valueProviderMap 
        = new HashMap<String, ValueProvider>();

    public DefineNodeFactory() {
        addValueProvider("expr", new ExpressionEvaluator());
        addValueProvider("field", new JavaBeanFieldRetriever());
        addValueProvider("concat", new ConcatHandler());
    }

    public void addValueProvider(String identifier, ValueProvider valueProvider) {
        this.valueProviderMap.put(identifier, valueProvider);
    }
    
    public interface ValueProvider {
        public Object create(List<Argument> tokens, Context ctx) throws Exception;
    }
    
    public Node create(DirectiveParseContext ctx) throws Exception {
        List<Argument> args = ctx.getArguments();
        if (args.size() < 2) {
            throw new RuntimeException("Too few arguments: " + ctx.getNodeText());
        }
        Argument arg1 = args.remove(0);
        if (!(arg1 instanceof Symbol)) {
            throw new RuntimeException("Expected symbol: " + arg1.getRawValue());
        }
        final String variable = ((Symbol) arg1).getSymbol();
        
        Argument firstToken = args.remove(0);
        if (firstToken instanceof Literal) {
            if (!args.isEmpty()) {
                throw new RuntimeException("Extra arguments after value: " + ctx.getNodeText());
            }
            return new DefNode(variable, firstToken);
        }
        
        String identifier = ((Symbol) firstToken).getSymbol();
        final ValueProvider handler = this.valueProviderMap.get(identifier);
        if (handler == null) {
            throw new RuntimeException("Unknown definition type: " + identifier);
        }
        final List<Argument> tokens = args;
        return new Node() {
            public void render(Context ctx, Writer out) throws Exception {
                Object val = handler.create(tokens, ctx);
                ctx.define(variable, val, true);
            }
        };
    }

    private class DefNode extends Node {
        private String variable;
        private Argument value;

        public DefNode(String variable, Argument value) {
            this.variable = variable;
            this.value = value;
        }

        public void render(Context ctx, Writer out) throws Exception {
            Object result;
            if (this.value instanceof Symbol) {
                result = ((Symbol) this.value).resolve(ctx);
            } else {
                result = ((Literal) this.value).getValue();
            }
            ctx.define(this.variable, result, true);
        }

        public String toString() {
            return "[def: " + this.variable + "=" + this.value + "]";
        }
    }

    private class ExpressionEvaluator implements ValueProvider {

        public Object create(List<Argument> tokens, Context ctx)
                throws Exception {
            Expression expr = new Expression(tokens);
            return expr.evaluate(ctx);
        }
    }
    
    private class ConcatHandler implements ValueProvider {

        public Object create(List<Argument> tokens, Context ctx) {
            StringBuilder result = new StringBuilder();
            for (Argument arg: tokens) {
                Object o;
                if (arg instanceof Symbol) {
                    o = ((Symbol) arg).resolve(ctx);
                } else {
                    o = ((Literal) arg).getValue();
                }
                result.append(o);
            }
            return result.toString();
        }
    }
    
    private class JavaBeanFieldRetriever implements ValueProvider {

        public Object create(List<Argument> tokens, Context ctx) throws Exception {
            if (tokens.size() != 2) {
                throw new RuntimeException("Wrong number of arguments: expected <object> <fieldname>");
            }
            
            Object target;
            Argument arg1 = tokens.get(0);
            if (arg1 instanceof Symbol) {
                target = ((Symbol) arg1).resolve(ctx);
            } else {
                target = ((Literal) arg1).getValue();
            }
            if (target == null) {
                throw new RuntimeException("Object " + arg1.getRawValue() + " not found");
            }
            
            Object o;
            Argument arg2 = tokens.get(1);
            if (arg2 instanceof Symbol) {
                o = ((Symbol) arg2).resolve(ctx);
            } else {
                o = ((Literal) arg2).getValue();
            }
            if (o == null) {
                throw new RuntimeException("Object " + arg2.getRawValue() + " not found");
            }
            String field = o.toString();
            BeanInfo beanInfo = Introspector.getBeanInfo(target.getClass());
            PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
            Method getter = null;
            for (PropertyDescriptor desc: propertyDescriptors) {
                if (desc.getName().equals(field)) {
                    getter = desc.getReadMethod();
                    break;
                }
            }
            if (getter == null) {
                throw new RuntimeException("Property not found: " + field);
            }
            return getter.invoke(target);
        }
    }
}

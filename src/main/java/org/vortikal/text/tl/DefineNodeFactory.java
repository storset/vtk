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
        //addValueProvider("field", new JavaBeanFieldRetriever());
        addValueProvider("concat", new ConcatHandler());
    }

    public void addValueProvider(String identifier, ValueProvider valueProvider) {
        this.valueProviderMap.put(identifier, valueProvider);
    }
    
    public interface ValueProvider {
        public Object create(List<Argument> tokens, Context ctx) throws Exception;
    }

    private static final Symbol LB = new Symbol("{");
    private static final Symbol RB = new Symbol("}");
    private static final Symbol COLON = new Symbol(":");
    private static final Symbol COMMA = new Symbol(",");
    
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

        // Simple literal definition (e.g. "def x 22")
        if (args.get(0) instanceof Literal) {
            if (args.size() > 1) {
                throw new RuntimeException("Extra arguments after value: " + ctx.getNodeText());
            }
            final Argument val = args.get(0);
            return new Node() {
                public void render(Context ctx, Writer out) throws Exception {
                    Object result;
                    result = val.getValue(ctx);
                    ctx.define(variable, result, true);
                }
            };
        }
        
        // Map definition (e.g. "def x {a:b, c:d}")
        if (LB.equals(args.get(0)) && RB.equals(args.get(args.size() - 1))) {

            final Map<Argument, Argument> argMap = getMapDefinition(args);
            return new Node() {
                public void render(Context ctx, Writer out) throws Exception {
                    Map<Object, Object> result = new HashMap<Object, Object>();
                    for (Argument keyArg: argMap.keySet()) {
                        Argument valArg = argMap.get(keyArg);
                        Object key = keyArg.getValue(ctx);
                        Object val = valArg.getValue(ctx);
                        result.put(key, val);
                    }
                    ctx.define(variable, result, true);
                }
            };
        }

        // Definition handled by value provider:
        Argument firstToken = args.remove(0);
        String identifier = ((Symbol) firstToken).getSymbol();

        final ValueProvider handler = this.valueProviderMap.get(identifier);
        if (handler == null) {
            throw new RuntimeException("Unknown definition type: " + identifier + ": " + args);
        }
        final List<Argument> tokens = args;
        return new Node() {
            public void render(Context ctx, Writer out) throws Exception {
                Object val = handler.create(tokens, ctx);
                ctx.define(variable, val, true);
            }
        };
    }

    private Map<Argument, Argument> getMapDefinition(List<Argument> args) {
        
        if (args.size() < 2) {
            throw new IllegalArgumentException("Malformed map definition");
        }
        if (!LB.equals(args.get(0)) && !RB.equals(args.get(args.size() - 1))) {
            throw new IllegalArgumentException("Malformed map definition");
        }
        args.remove(0);
        args.remove(args.size() - 1);

        Map<Argument, Argument> map = new HashMap<Argument, Argument>();
        while (true) {
            if (args.isEmpty()) {
                break;
            }
            Argument key = args.remove(0);
            if (args.isEmpty()) {
                throw new RuntimeException("Malformed map definition");
            }
            Argument colon = args.remove(0);
            if (!COLON.equals(colon)) {
                throw new RuntimeException("Expected ':' in map definition");
            }
            if (args.isEmpty()) {
                throw new RuntimeException("Malformed map definition");
            }
            Argument value = args.remove(0);
            map.put(key, value);
            if (args.isEmpty()) {
                break;
            }
            Argument comma = args.remove(0);
            if (!COMMA.equals(comma)) {
                throw new RuntimeException("Entries must be comma-separated");
            }
        }
        return map;
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
                Object o = arg.getValue(ctx);
                result.append(o);
            }
            return result.toString();
        }
    }
    
    @SuppressWarnings("unused")
    private class JavaBeanFieldRetriever implements ValueProvider {

        public Object create(List<Argument> tokens, Context ctx) throws Exception {
            if (tokens.size() != 2) {
                throw new RuntimeException("Wrong number of arguments: expected <object> <fieldname>");
            }
            Argument arg1 = tokens.get(0);
            Object target = arg1.getValue(ctx);
            
            Argument arg2 = tokens.get(1);
            Object o = arg2.getValue(ctx);
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

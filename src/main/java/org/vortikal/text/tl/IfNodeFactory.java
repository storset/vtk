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

import java.io.Writer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import org.vortikal.text.tl.expr.Expression;
import org.vortikal.text.tl.expr.Function;

public class IfNodeFactory implements DirectiveNodeFactory {

    private static final Set<String> TERMS = 
        new HashSet<String>(Arrays.asList("elseif", "else", "endif"));

    private Set<Function> functions = new HashSet<Function>();
    
    public void setFunctions(Set<Function> functions) {
        if (functions != null) {
            for (Function function: functions) {
                this.functions.add(function);
            }
        }
    }
    
    public Node create(DirectiveParseContext ctx) throws Exception {
        LinkedHashMap<Expression, NodeList> expressions = new LinkedHashMap<Expression, NodeList>();

        Parser parser = ctx.getParser();
        int line = parser.getLineNumber();
        String cur = ctx.getName();
        List<Argument> curArgs = ctx.getArguments();
        while (true) {
            ParseResult parsed = parser.parse(TERMS);
            DirectiveParseContext info = parsed.getTerminator();
            if (info == null) {
                throw new RuntimeException("Unterminated directive at line " 
                        + line + ": " + "[" + ctx.getNodeText() + "...");
            }
            String terminator = info.getName();

            if (curArgs.isEmpty()) {
                curArgs.add(new Literal("true"));
            }
            Expression expression = new Expression(this.functions, curArgs);
            expressions.put(expression, parsed.getNodeList());

            if ("endif".equals(terminator)) {
                break;
            }

            if ("elseif".equals(terminator)) {
                if (!("if".equals(cur) || "elseif".equals(cur))) {
                    throw new RuntimeException("elseif can only follow if or elseif");
                }
                cur = "elseif";
            } else if ("else".equals(terminator)) {
                cur = "else";
            }
            curArgs = info.getArguments();
        }
        return new IfNode(expressions);
    }

    private class IfNode extends Node {
        private LinkedHashMap<Expression, NodeList> branches;

        public IfNode(LinkedHashMap<Expression, NodeList> branches) {
            this.branches = branches;
        }

        public void render(Context ctx, Writer out) throws Exception {
            NodeList branch = null;
            for (Expression exp : this.branches.keySet()) {
                if (eval(exp, ctx)) {
                    branch = this.branches.get(exp);
                    break;
                }
            }
            if (branch != null) {
                ctx.push();
                branch.render(ctx, out);
                ctx.pop();
            }
        }

        private boolean eval(Expression expression, Context ctx) {
            Object o = expression.evaluate(ctx);
            if (o == null) {
                return false;
            } else if (o instanceof Boolean) {
                return ((Boolean) o).booleanValue();
            } else {
                return true;
            }
        }
        
        public String toString() {
            return "[if-node]";
        }
    }
}

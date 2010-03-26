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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.vortikal.text.tl.expr.Expression;
import org.vortikal.text.tl.expr.Function;

public class ListNodeFactory implements DirectiveNodeFactory {

    private static final Set<String> LIST_TERM = new HashSet<String>(Arrays.asList("endlist"));

    private Set<Function> functions = new HashSet<Function>();
    
    public void setFunctions(Set<Function> functions) {
        if (functions != null) {
            for (Function function: functions) {
                this.functions.add(function);
            }
        }
    }
    
    /**
     * [list x varname] 
     *    .. do stuff with varname: [val varname] 
     *    [if _first] first element [endif]
     *    [if _last] last element [endif]
     *    index: [val _index]
     *    size: [val _size]
     * [endlist]
     * 
     */
    public Node create(DirectiveParseContext ctx) throws Exception {
        List<Argument> args = ctx.getArguments();
        if (args.size() < 2) {
            throw new RuntimeException("List directive: " + ctx.getNodeText() 
                        + ": wrong number of arguments");
        }
        Argument last = args.remove(args.size() - 1);
        if (!(last instanceof Symbol)) {
            throw new RuntimeException("Expected symbol: " + last.getRawValue());
        }

        Expression expression = new Expression(this.functions, args);
        ParseResult listBlock = ctx.getParser().parse(LIST_TERM);

        String terminator = listBlock.getTerminator();
        if (terminator == null) {
            throw new RuntimeException("Unterminated directive: " + ctx.getNodeText());
        }
        NodeList nodeList = listBlock.getNodeList();
        return new ListNode(expression, (Symbol) last, nodeList);
    }

    private class ListNode extends Node {
        private Expression expression;
        private Symbol defVar;
        private NodeList nodeList;

        public ListNode(Expression expression, Symbol defVar, NodeList nodeList) {
            this.expression = expression;
            this.defVar = defVar;
            this.nodeList = nodeList;
        }

        public void render(Context ctx, Writer out) throws Exception {
            Object evaluated = this.expression.evaluate(ctx);
            List<Object> elements = new ArrayList<Object>();
            if (evaluated instanceof Iterable<?>) {
            	Iterable<?> iterable = (Iterable<?>) evaluated;
            	for (Object o : iterable) {
            		elements.add(o);
            	}
            } else if (evaluated instanceof Iterator<?>) {
            	Iterator<?> iter = (Iterator<?>) evaluated;
            	while (iter.hasNext()) {
            		elements.add(iter.next());
            	}
            } else if (evaluated instanceof Object[]) {
                for (Object o : (Object[]) evaluated) {
            		elements.add(o);
                }
            } else {
                throw new RuntimeException(
                		"List: Cannot iterate expression: " 
                		+ this.expression + ": result is not a list: " 
                		+ evaluated);
            }
            execute(elements, ctx, out);
        }
        
        private void execute(List<Object> elements, Context ctx, Writer out) throws Exception {
        	int size = elements.size();

        	for (int i = 0; i < size; i++) {
				Object object = elements.get(i);
                ctx.push();
                ctx.define(this.defVar.getSymbol(), object, false);
                ctx.define("_size", size, false);
                ctx.define("_index", i, false);
                ctx.define("_first", (i == 0), false);
                ctx.define("_last", (i == size - 1), false);
                this.nodeList.render(ctx, out);
                ctx.pop();
			}
        }
        
        public String toString() {
            return "[list]";
        }
    }
}

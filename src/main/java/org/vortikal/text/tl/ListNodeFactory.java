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
import java.util.Iterator;
import java.util.List;
import java.util.Set;


public class ListNodeFactory implements DirectiveNodeFactory {

    private static final Set<String> LIST_TERM =
        new HashSet<String>(Arrays.asList("endlist"));


    /**
     * [list x varname]
     *   .. do stuff with varname ..
     * [endlist]
     *
     */
    public Node create(DirectiveParseContext ctx) throws Exception {
        List<Argument> args = ctx.getArguments();
        if (args.size() != 2) {
            throw new RuntimeException("List directive: " 
                    + ctx.getNodeText() 
                    + ": wrong number of arguments");
        }

        Argument arg1 = args.remove(0);
        Argument arg2 = args.remove(0);
        if (!(arg1 instanceof Symbol)) {
            throw new RuntimeException("Expected symbol: " 
                    + arg1.getRawValue());
        }
        if (!(arg2 instanceof Symbol)) {
            throw new RuntimeException("Expected symbol: " 
                    + arg2.getRawValue());
        }
        ParseResult listBlock = ctx.getParser().parse(LIST_TERM);

        String terminator = listBlock.getTerminator();
        if (terminator == null) {
            throw new RuntimeException("Unterminated directive: " 
                    + ctx.getNodeText());
        } 
        NodeList nodeList = listBlock.getNodeList();
        return new ListNode((Symbol) arg1, (Symbol) arg2, nodeList);
    }


    private class ListNode extends Node {
        private Symbol listVar;
        private Symbol defVar;
        private NodeList nodeList;

        public ListNode(Symbol listVar, Symbol defVar, NodeList nodeList) {
            this.listVar = listVar;
            this.defVar = defVar;
            this.nodeList = nodeList;
        }

        public void render(Context ctx, Writer out) throws Exception {
            Object var = this.listVar.resolve(ctx);
            if (var == null) {
                throw new RuntimeException("List: No such variable: " + this.listVar);
            }
            
            if (!(var instanceof List<?>) && !(var instanceof Iterator<?>)) {
                throw new RuntimeException("List: Cannot iterate variable: "
                                           + this.listVar + ": not a list");
            }
            if (var instanceof List<?>) {
                List<?> list = (List<?>) var;
                for (Object o: list) {
                	executeBody(o, ctx, out);
                }
            } else {
                Iterator<?> iter = (Iterator<?>) var;
                while (iter.hasNext()) {
                	Object o = iter.next();
                	executeBody(o, ctx, out);
                }
            }
        }

        private void executeBody(Object o, Context ctx, Writer out) throws Exception {
            ctx.push();
            ctx.define(this.defVar.getSymbol(), o, false);
            this.nodeList.render(ctx, out);
            ctx.pop();
        }
        
        public String toString() {
            return "[list-node]";
        }
    }
}

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
import java.util.List;
import java.util.Set;

import org.vortikal.text.tl.expr.Expression;

public class IfNodeFactory implements DirectiveNodeFactory {

    private static final Set<String> IF_ELSE_TERM =
        new HashSet<String>(Arrays.asList("else", "endif"));

    private static final Set<String> IF_TERM =
        new HashSet<String>(Arrays.asList("endif"));

    public Node create(DirectiveParseContext ctx) throws Exception {

        ParseResult trueBranch = ctx.getParser().parse(IF_ELSE_TERM);
        ParseResult falseBranch = null;

        String terminator = trueBranch.getTerminator();
        if (terminator == null) {
            throw new RuntimeException("Unterminated directive: " + ctx.getName());
        }
        if (terminator.equals("else")) {
            falseBranch = ctx.getParser().parse(IF_TERM);
            terminator = falseBranch.getTerminator();
            if (terminator == null) {
                throw new RuntimeException("Unterminated directive: " + ctx.getNodeText());
            }
        }
        NodeList trueNodeList = trueBranch.getNodeList();
        NodeList falseNodeList = falseBranch != null ? falseBranch.getNodeList() : null;

        List<Argument> args = ctx.getArguments();
        if (args.isEmpty()) {
            throw new RuntimeException("Missing condition: "
                    + ctx.getNodeText());
        }
        
        Expression expression = new Expression(args);
        return new IfNode(expression, trueNodeList, falseNodeList);
    }


    private class IfNode extends Node {
        private Expression expression = null;
        private NodeList trueBranch;
        private NodeList falseBranch;

        public IfNode(Expression expression, NodeList trueBranch, NodeList falseBranch) {
            this.expression = expression;
            this.trueBranch = trueBranch;
            this.falseBranch = falseBranch;
        }

        public void render(Context ctx, Writer out) throws Exception {

            boolean result;
            Object o = this.expression.evaluate(ctx);
            if (o == null) {
                result = false;
            } else if (o instanceof Boolean) {
                result = ((Boolean) o).booleanValue();
            } else {
                result = true;
            }
            
            ctx.push();
            if (result) {
                this.trueBranch.render(ctx, out);
            } else if (this.falseBranch != null) {
                this.falseBranch.render(ctx, out);
            }
            ctx.pop();
        }
        
        public String toString() {
            return "[if-node]";
        }
    }
}

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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.vortikal.text.tl.expr.Expression;
import org.vortikal.text.tl.expr.Function;

public final class DefineNodeFactory implements DirectiveNodeFactory {

    private Set<Function> functions = new HashSet<Function>();

    public DefineNodeFactory() {
    }
    
    public DefineNodeFactory(Set<Function> functions) {
        setFunctions(functions);
    }
    
    public void setFunctions(Set<Function> functions) {
        if (functions != null) {
            for (Function function: functions) {
                this.functions.add(function);
            }
        }
    }

    public Node create(DirectiveParseContext ctx) throws Exception {
        List<Token> args = ctx.getArguments();
        if (args.size() < 2) {
            throw new RuntimeException("Too few arguments: " + ctx.getNodeText());
        }
        Token arg1 = args.remove(0);
        if (!(arg1 instanceof Symbol)) {
            throw new RuntimeException("Expected symbol: " + arg1.getRawValue());
        }
        final String variable = ((Symbol) arg1).getSymbol();

        final Expression expression = new Expression(this.functions, args);
        return new Node() {
            public boolean render(Context ctx, Writer out) throws Exception {
                Object val = expression.evaluate(ctx);
                ctx.define(variable, val, true);
                return true;
            }
        };
    }
}

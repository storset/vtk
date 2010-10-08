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
package org.vortikal.text.tl.expr;

import org.vortikal.text.tl.Context;
import org.vortikal.text.tl.Symbol;

public class Gt extends Operator {

    public Gt(Symbol symbol, Notation notation, Precedence precedence) {
        super(symbol, notation, precedence);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object eval(Context ctx, EvalStack stack) {
        Object o2 = stack.pop();
        Object o1 = stack.pop();
        if (o1 == null) {
            throw new IllegalArgumentException("Compare:" + getSymbol() + " first argument is NULL");
        }
        if (o2 == null) {
            throw new IllegalArgumentException("Compare:" + getSymbol() + " second argument is NULL");
        }
        if (o1.getClass() != o2.getClass()) {
            throw new IllegalArgumentException("Compare: " + o1 + " " + getSymbol() + " " + o2 + ": not comparable");
        }
        if (!(o1 instanceof Comparable<?>)) {
            throw new IllegalArgumentException("Compare: " + o1 + " " + getSymbol() + " " + o2 + ": not comparable");
        }
        Comparable<Object> c1 = (Comparable<Object>) o1;
        Comparable<Object> c2 = (Comparable<Object>) o2;
        return Boolean.valueOf(c1.compareTo(c2) > 0);
    }
}

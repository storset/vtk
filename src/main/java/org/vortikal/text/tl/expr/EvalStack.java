/* Copyright (c) 2010, University of Oslo, Norway
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

import java.util.Stack;

import org.vortikal.text.tl.Context;
import org.vortikal.text.tl.Symbol;

/**
 * Evaluation stack for expressions.
 */
public class EvalStack {
    private Stack<Object> stack = new Stack<Object>();
    private Context ctx;
    
    public EvalStack(Context ctx) {
        this.ctx = ctx;
    }
    
    /**
     * Gets the size of the stack.
     * @return the size of the stack
     */
    public int size() {
        return this.stack.size();
    }
    
    /**
     * Tests if the stack is empty.
     * @return a boolean indicating whether 
     * or not the stack is empty
     */
    public boolean isEmpty() {
        return this.stack.isEmpty();
    }
    
    /**
     * Pushes an object onto the stack
     * @param o the object to push
     */
    public void push(Object o) {
        this.stack.push(o);
    }

    /**
     * Pops the stack
     * @return the popped object
     */
    public Object pop() {
        return pop(true);
    }

    /**
     * Pops the stack and optionally evaluates the 
     * returned object in the context (only if the object is a symbol).
     * @param evaluate whether to evaluate the returned object
     * @return the popped object
     */
    public Object pop(boolean evaluate) {
        Object o = this.stack.pop();
        if (evaluate && o instanceof Symbol) {
            return ((Symbol) o).getValue(this.ctx);
        }
        return o;
    }
}
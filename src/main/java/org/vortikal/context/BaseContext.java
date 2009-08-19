/* Copyright (c) 2006, University of Oslo, Norway
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
package org.vortikal.context;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public final class BaseContext {

    private static ThreadLocal<Stack<BaseContext>> threadLocal = new ThreadLocal<Stack<BaseContext>>();

    private Map<Object, Object> map = new HashMap<Object, Object>();


    private BaseContext() {
    }


    public void setAttribute(Object key, Object value) {
        this.map.put(key, value);
    }


    public Object getAttribute(Object key) {
        return this.map.get(key);
    }


    public static BaseContext getContext() {
        Stack<BaseContext> s = threadLocal.get();
        if (s == null || s.isEmpty()) {
            throw new IllegalStateException("Cannot call getContext(): no context exists");
        }
        return s.peek();
    }


    public static void pushContext() {
        BaseContext ctx = new BaseContext();
        Stack<BaseContext> s = threadLocal.get();
        if (s == null) {
            s = new Stack<BaseContext>();
            threadLocal.set(s);
        }
        s.push(ctx);
    }


    public static void popContext() {
        Stack<BaseContext> s = threadLocal.get();
        if (s == null) {
            throw new IllegalStateException("Cannot call popContext(): no context exists");
        }
        s.pop();
        if (s.isEmpty()) {
            threadLocal.set(null);
        }
    }

}

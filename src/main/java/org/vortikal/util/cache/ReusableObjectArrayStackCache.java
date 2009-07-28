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
package org.vortikal.util.cache;


/**
 * Abstract implementation of {@link ReusableObjectCache}. Uses an array-based
 * stack internally for minimum overhead in {@link #getInstance()} and 
 * {@link #putInstance(Object)}. It has a maximum capacity which can
 * optionally be set using constructor {@link #AbstractReusableObjectStackCache(int)}.  
 *  
 * @see org.vortikal.util.cache.ReusableObjectCache
 * 
 * @author oyviste
 */
public class ReusableObjectArrayStackCache<T> implements ReusableObjectCache<T> {

    public static final int DEFAULT_CAPACITY = 10;
    
    private int top = -1;
    private T[] stack;
    
    /**
     * Construct an instance with a default maximum capacity
     *
     */
    @SuppressWarnings("unchecked")
    public ReusableObjectArrayStackCache() {
        this.stack = (T[]) new Object[DEFAULT_CAPACITY];
    }

    /**
     * Construct an instance with a configurable maximum capacity
     * @param capacity
     */
    @SuppressWarnings("unchecked")
    public ReusableObjectArrayStackCache(int capacity) {
        this.stack = (T[]) new Object[capacity > 0 ? capacity : DEFAULT_CAPACITY];
    }
    
    /**
     * @see org.vortikal.util.cache.ReusableObjectCache#getInstance()
     */
    public final T getInstance() {
        return pop();
    }
    
    /**
     * @see org.vortikal.util.cache.ReusableObjectCache#putInstance(Object)
     */
    public final boolean putInstance(T object) {
        return push(object);
    }
    
    /**
     * @see org.vortikal.util.cache.ReusableObjectCache#size()
     */
    public final synchronized int size() {
        return this.top + 1;
    }
    
    private final synchronized T pop() {
        if (this.top == -1) {
            return null; // Cache empty
        }
        // Return instance at the top
        T object = this.stack[this.top];
        this.stack[this.top--] = null;
        return object;
    }

    private final synchronized boolean push(T object) {
        if (this.top == this.stack.length-1) {
            // Cache full
            return false;
        }
        // Add at the top
        this.stack[++this.top] = object;
        return true;
    }
 
}

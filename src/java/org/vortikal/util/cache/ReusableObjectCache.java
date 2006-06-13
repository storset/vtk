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
 * Defines an interface to a cache that creates instances of reusable objects
 * as necessary and allows them to be put back for later re-use.
 * 
 * The motivation behind this type of cache is to efficiently re-use instances
 * of types that are not thread safe, but frequently needed in multithreaded code
 * and that have expensive constructors, such as {@link java.text.SimpleDateFormat}.
 * 
 * @author oyviste
 *
 */
public interface ReusableObjectCache {

    /**
     * Get an object instance from the cache. If the cache
     * is empty, a new instance will be created.
     * 
     * It should never return the same instance twice, if instances haven't 
     * been put back with {@link #putInstance(Object)}. Therefore, it should be
     * safe to use from multithreaded code. 
     * 
     * @return An object instance
     */
    public Object getInstance();
    
    /**
     * Put an instance back into the cache after usage. This allows it to be returned later
     * by {@link #getInstance()}, possibly avoiding the need to create a new object.
     * 
     * @param o The instance to put back. Only instances obtained by 
     *          {@link #getInstance()} should be used here.
     */
    public void putInstance(Object o);
    
    /**
     * Get current number of instances in cache.
     */
    public int size();
}

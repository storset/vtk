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
package org.vortikal.util.text;

import java.text.SimpleDateFormat;

import org.vortikal.util.cache.AbstractReusableObjectArrayStackCache;
import org.vortikal.util.cache.ReusableObjectCache;

/**
 * A class that creates and caches {@link java.text.DateFormat} instances
 * with a common format-specifier.
 * 
 * The motivation behind this is that construction of {@link SimpleDateFormat}
 * objects is expensive while the instances themselves are not thread safe, 
 * so this should help in avoiding some of that overhead.
 * 
 * @see java.text.DateFormat
 * @see org.vortikal.util.cache.AbstractReusableObjectArrayStackCache
 * @see org.vortikal.util.cache.ReusableObjectCache
 * 
 * @author oyviste
 *
 */
public final class SimpleDateFormatCache extends AbstractReusableObjectArrayStackCache 
    implements ReusableObjectCache {

    private final String pattern;

    public SimpleDateFormatCache(String pattern) {
        super();
        this.pattern = pattern;
    }
    
    public SimpleDateFormatCache(String pattern, int maxSize) {
        super(maxSize);
        this.pattern = pattern;
    }

    protected Object createNewInstance() {
        return new SimpleDateFormat(this.pattern);
    }
}

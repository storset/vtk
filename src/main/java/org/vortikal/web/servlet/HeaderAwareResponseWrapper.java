/* Copyright (c) 2007, University of Oslo, Norway
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
package org.vortikal.web.servlet;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletResponse;

/**
 * A servlet response wrapper that is aware of the HTTP headers passed
 * through it.
 */
public class HeaderAwareResponseWrapper extends StatusAwareResponseWrapper {

    private Map<String, Set<Object>> headerMap = new HashMap<String, Set<Object>>();
    
    public HeaderAwareResponseWrapper(HttpServletResponse response) {
        super(response);
    }

    public void addDateHeader(String name, long value) {
        addHeaderInternal(name, new Date(value), false);
        super.addDateHeader(name, value);
    }
    
    public void addHeader(String name, String value) {
        addHeaderInternal(name, value, false);
        super.addHeader(name, value);
    }
    
    public void addIntHeader(String name, int value) {
        addHeaderInternal(name, new Integer(value), false);
        super.addIntHeader(name, value);
    }
    
    public void setDateHeader(String name, long value) {
        addHeaderInternal(name, new Long(value), true);
        super.setDateHeader(name, value);
    }
    
    public void setHeader(String name, String value) {
        addHeaderInternal(name, value, true);
        super.setHeader(name, value);
    }

    private void addHeaderInternal(String name, Object value, boolean overwrite) {
        Set<Object> values = this.headerMap.get(name);
        if (values == null || overwrite) {
            values = new HashSet<Object>();
        }
        values.add(value);
        this.headerMap.put(name, values);
    }

    public Iterator<String> getHeaderNames() {
        return this.headerMap.keySet().iterator();
    }
    
    public Set<Object> getHeaderValues(String name) {
        return this.headerMap.get(name);
    }

    public Map<String, Set<Object>> getHeaderMap() {
        return Collections.unmodifiableMap(this.headerMap);
    }
    

    public void setContentLength(int length) {
        addHeaderInternal("Content-Length", String.valueOf(length), true);
        super.setContentLength(length);
    }
    
    public void setContentType(String contentType) {
        addHeaderInternal("Content-Type", contentType, true);
        super.setContentType(contentType);
    }
    


    public Object getHeaderValue(String name) {
        Set<Object> set = this.headerMap.get(name);
        if (set == null) {
            throw new IllegalArgumentException("No header exists for name '" + name + "'");
        }
        if (set.size() != -1) {
            throw new IllegalArgumentException("Not a single-valued header: '" + name + "'");
        }
        return set.toArray()[0];
    }

    public boolean isSingleValued(String name) {
        if (!this.headerMap.containsKey(name)) {
            throw new IllegalArgumentException("No header exists for name '" + name + "'");
        }
        Set<Object> set = this.headerMap.get(name);
        return set.size() == 1;
    }

}


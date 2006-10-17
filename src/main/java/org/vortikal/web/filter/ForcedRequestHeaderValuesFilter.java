/* Copyright (c) 2005, University of Oslo, Norway
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
package org.vortikal.web.filter;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;


/**
 * Request URI processor that translates the URI from one encoding to
 * another.
 *
 * <p>Configurable JavaBean properties:
 * <ul>
 *   <li><code>fromEncoding</code> - the encoding to translate from
 *   <li><code>toEncoding</code> - the encoding to translate to
 * </ul>
 */
public class ForcedRequestHeaderValuesFilter implements RequestFilter, InitializingBean {

    private int order = Integer.MAX_VALUE;

    private Map headers;
    
    public void setOrder(int order) {
        this.order = order;
    }
    
    public int getOrder() {
        return this.order;
    }
    

    public void setHeaders(Map headers) {
        this.headers = headers;
    }

    public void afterPropertiesSet() throws Exception {
        if (this.headers == null) {
            throw new BeanInitializationException(
                "JavaBean property 'headers' not specified");
        }
    }
    
    public HttpServletRequest filterRequest(HttpServletRequest request) {
        return new ForcedHeaderValuesRequestWrapper(request);
    }
    
    private class ForcedHeaderValuesRequestWrapper extends HttpServletRequestWrapper {

        private HttpServletRequest request;

        public ForcedHeaderValuesRequestWrapper(HttpServletRequest request) {
            super(request);
            this.request = request;
        }
        
        public String getHeader(String name) {
            if (ForcedRequestHeaderValuesFilter.this.headers.containsKey(name)) {
                return (String) ForcedRequestHeaderValuesFilter.this.headers.get(name);
            }
            return this.request.getHeader(name);
        }

        public String getContentType() {
            if (ForcedRequestHeaderValuesFilter.this.headers.containsKey("Content-Type")) {
                return (String) ForcedRequestHeaderValuesFilter.this.headers.get("Content-Type");
            }
            return this.request.getContentType();
        }

        public String getCharacterEncoding() {
            String contentType = (String) ForcedRequestHeaderValuesFilter.this.headers.get("Content-Type");
            if (contentType != null && contentType.indexOf("charset=") != -1) {
                String characterEncoding = contentType.substring(
                    contentType.indexOf("=") + 1).trim();
                return characterEncoding;
            }
            return this.request.getCharacterEncoding();
        }
        
    }
}


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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;


/**
 * Request URI processor that strips away certain prefixes from URIs.
 *
 * <p>Configurable JavaBean properties:
 * <ul>
 *   <li><code>uriPrefixes</code> - a list of prefixes to match and strip away
 * </ul>
 */
public class URIPrefixRequestFilter implements RequestFilter, InitializingBean {

    private String[] uriPrefixes;

    private int order = Integer.MAX_VALUE;
    

    public void setUriPrefixes(String[] uriPrefixes) {
        this.uriPrefixes = uriPrefixes;
    }

    public void setOrder(int order) {
        this.order = order;
    }
    
    public int getOrder() {
        return this.order;
    }
    

    public void afterPropertiesSet() throws Exception {
        if (this.uriPrefixes == null) {
            throw new BeanInitializationException(
                "JavaBean property 'uriPrefixes' not specified");
        }
    }
    
    public HttpServletRequest filterRequest(HttpServletRequest request) {
        return new URIPrefixRequestWrapper(request, uriPrefixes);
    }
    
    private class URIPrefixRequestWrapper extends HttpServletRequestWrapper {

        private String uri;
        
        public URIPrefixRequestWrapper(HttpServletRequest request,
                                         String[] uriPrefixes) {
            super(request);
            String uri = request.getRequestURI();
            for (int i = 0; i < uriPrefixes.length; i++) {
                if (uri.startsWith(uriPrefixes[i])) {
                    uri = uri.substring(uriPrefixes[i].length());
                    break;
                }
            }
            
            this.uri = uri;
        }
        
        public String getRequestURI() {
            return this.uri;
        }
    }
        
}
    



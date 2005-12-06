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
import javax.servlet.http.HttpSession;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.security.SecurityContext;
import org.vortikal.util.web.URLUtil;


/**
 */
public class RequestParameterSecurityTokenFilter implements RequestFilter, InitializingBean {

    private String requestParameter;
    private int order = Integer.MAX_VALUE;
    

    public void setRequestParameter(String requestParameter) {
        this.requestParameter = requestParameter;
    }

    public void setOrder(int order) {
        this.order = order;
    }
    
    public int getOrder() {
        return this.order;
    }
    

    public void afterPropertiesSet() throws Exception {
        if (this.requestParameter == null) {
            throw new BeanInitializationException(
                "JavaBean property 'requestParameter' not specified");
        }
    }
    
    public HttpServletRequest filterRequest(HttpServletRequest request) {
        return new RequestWrapper(request, this.requestParameter);
    }
    
    private class RequestWrapper extends HttpServletRequestWrapper {

        private String token;
        
        public RequestWrapper(HttpServletRequest request,
                                         String requestParameter) {
            super(request);
            
            this.token = request.getParameter(requestParameter);
        }
        
        public HttpSession getSession() {
            HttpSession session = super.getSession();
            if (this.token != null && session != null) {
                session.setAttribute(
                    SecurityContext.SECURITY_TOKEN_ATTRIBUTE,
                    token);
                
            }
            return session;
        }
        
        public HttpSession getSession(boolean create) {
            HttpSession session = super.getSession(create);
            if (this.token != null && session != null) {
                session.setAttribute(
                    SecurityContext.SECURITY_TOKEN_ATTRIBUTE,
                    token);
                
            }
            return session;
        }
        
    }
        
}

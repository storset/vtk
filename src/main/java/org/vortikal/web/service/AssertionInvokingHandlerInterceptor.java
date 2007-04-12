/* Copyright (c) 2004, University of Oslo, Norway
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
package org.vortikal.web.service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.web.servlet.ModelAndView;
import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;
import org.vortikal.security.SecurityContext;



/**
 * Interceptor invokes a list of {@link Assertion assertions} in the
 * <code>preHandle()</code> method, and does not allow the request to
 * proceed unless unless all the assertions match.
 *
 * <p>Configurable JavaBean properties:
 * <ul>
 *   <li><code>assertions</code> - a list of {@link Assertion
 *       assertions} that are to be invoked on every request.
 * </ul>
 *
 */
public class AssertionInvokingHandlerInterceptor extends ResourceRetrievingHandlerInterceptor
  {
    
    private Assertion[] assertions;

    public void setAssertions(Assertion[] assertions) {
        this.assertions = assertions;
    }
    
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();

        if (this.assertions == null) {
            throw new BeanInitializationException(
                "JavaBean property 'assersions' not specified");
        }                
    }

    
    @Override
    protected boolean handleInternal(Resource resource, HttpServletRequest request) {
        Principal principal = SecurityContext.getSecurityContext().getPrincipal();

        boolean proceed = true;
        
        for (int i = 0; i < this.assertions.length; i++) {
            proceed = this.assertions[i].matches(request, resource, principal);
            if (!proceed) {
                break;
            }
        }
        return proceed;
    }

    public void postHandle(HttpServletRequest request, HttpServletResponse response,
                           Object object, ModelAndView modelAndView) throws Exception {

    }

    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object object, Exception exception) throws Exception {

    }

}

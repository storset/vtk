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
package org.vortikal.web.service;

import javax.servlet.http.HttpServletRequest;

import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;
import org.vortikal.security.SecurityContext;



/**
 * Assertion that matches when a security token is present in request
 * parameters.
 *
 * <p>Configurable JavaBean properties:
 * <ul>
 *   <li><code>requestParameter</code> - the name of the request
 *   parameter to check.
 * </ul>
 *
 */
public class SecurityTokenRequestParameterAssertion implements Assertion {

    private String requestParameter;

    public void setRequestParameter(String requestParameter) {
        this.requestParameter = requestParameter;
    }
    
    public boolean matches(HttpServletRequest request, Resource resource, Principal principal) {
        String value = request.getParameter(this.requestParameter);
        if (value != null) {
            SecurityContext securityContext = SecurityContext.getSecurityContext();
            String token = securityContext.getToken();
            if (token != null) {
                return token.equals(value);
            }
        }
        return false;
    }


    public boolean processURL(URL url, Resource resource, Principal principal, boolean match) {

        SecurityContext securityContext = SecurityContext.getSecurityContext();
        String token = securityContext.getToken();
        if (token != null) {
            url.addParameter(this.requestParameter, token);
            return true;
        }
        return false;
    }

    public boolean conflicts(Assertion assertion) {
        if (assertion instanceof RequestParameterAssertion) {

            if (this.requestParameter.equals(
                    ((RequestParameterAssertion)assertion).getParameterName())) {
                return true;
            }
        }
        return false;
    }

}

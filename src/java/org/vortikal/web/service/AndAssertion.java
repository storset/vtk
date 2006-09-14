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
package org.vortikal.web.service;


import java.util.Arrays;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;
import org.vortikal.web.RequestContext;


public class AndAssertion implements Assertion {

    private Log logger = LogFactory.getLog(this.getClass());
    private Assertion[] assertions;


    public void setAssertions(Assertion[] assertions) {
        this.assertions = assertions;
    }
    
    public boolean processURL(URL url, Resource resource, Principal principal, boolean match) {
        HttpServletRequest request = null;
        if (match) {
            RequestContext context = RequestContext.getRequestContext();
            if (context != null) {
                request = context.getServletRequest();
            }
        }

        for (int i = 0; i < this.assertions.length; i++) {
//             if (match) {
//                 if (!this.assertions[i].matches(request, resource, principal)) {
//                     if (logger.isDebugEnabled()) {
//                         logger.debug("Assertion " + this.assertions[i]
//                                      + " did not match, will not continue to construct URL");
//                     }
//                     return false;
//                 }

//         }
            this.assertions[i].processURL(url, resource, principal, match);
        }
        return true;
    }
    

    public boolean matches(HttpServletRequest request, Resource resource, Principal principal) {
        for (int i = 0; i < this.assertions.length; i++) {
            if (!this.assertions[i].matches(request, resource, principal)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Assertion " + this.assertions[i]
                                 + " did not match, returning false");
                }
                return false;
            }
        }
        return true;
    }
    

    public boolean conflicts(Assertion assertion) {
        for (int i = 0; i < this.assertions.length; i++) {
            if (this.assertions[i].conflicts(assertion)) {
                return true;
            }
        }
        return false;
    }
    
    public String toString() {
        StringBuffer sb = new StringBuffer(this.getClass().getName());
        sb.append(": assertions = [" + java.util.Arrays.asList(this.assertions) + "]");
        return sb.toString();
    }
    

}

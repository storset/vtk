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

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;

import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;


/**
 * Assertion that wraps another assertion, delegating the matching to
 * the wrapped assertion when the request URI matches a regular
 * expression.
 *
 * <p>Configurable properties:
 * <ul>
 *   <li><code>wrappedAssertion</code> - a {@link Assertion}
 *   that is invoked when the <code>uriPattern</code> regexp matches
 *   the current request.
 *   <li><code>uriPattern</code> - a regular expression constraining
 *   the set of resources that the wrapped assertion applies to. If
 *   set, this regexp has to match in order for this assertion to even
 *   <emph>attempt</emph> to match the request. This constraint also
 *   applies to the URL construction phase.
 * </ul>
 */
public class RequestURIRegexpWrappingAssertion 
  implements Assertion, InitializingBean {
	
    private Pattern uriPattern = null;
    private Assertion wrappedAssertion = null;


    public void setUriPattern(String uriPattern) {
        if (uriPattern == null) throw new IllegalArgumentException(
            "Property 'uriPattern' cannot be null");
    
        this.uriPattern = Pattern.compile(uriPattern);
    }
    

    public void setWrappedAssertion(Assertion wrappedAssertion) {
        this.wrappedAssertion = wrappedAssertion;
    }
    

    public void afterPropertiesSet() {
        if (this.uriPattern == null) {
            throw new BeanInitializationException(
                "Bean property 'uriPattern' not set");
        }
        if (this.wrappedAssertion == null) {
            throw new BeanInitializationException(
                "Bean property 'wrappedAssertion' not set");
        }
    }
    

    public boolean conflicts(Assertion assertion) {
        return this.wrappedAssertion.conflicts(assertion);
    }


    public String toString() {
        StringBuffer sb = new StringBuffer();
		
        sb.append(super.toString());
        sb.append("; uriPattern = " + this.uriPattern.pattern());
        sb.append("; [wrappedAssertion = " + this.wrappedAssertion);
        sb.append("]");
        return sb.toString();
    }


    public boolean processURL(URL url, Resource resource,
                              Principal principal, boolean match) {
        if (this.uriPattern != null) {
            Matcher m = this.uriPattern.matcher(url.getPath());
            if (!m.find()) {
                // According to the regular expression, this URL
                // should not be affected by the assertion:
                return true;
            }
        }

        return this.wrappedAssertion.processURL(url, resource, principal, match);
    }


    public boolean matches(HttpServletRequest request, Resource resource,
                           Principal principal) {
        if (this.uriPattern != null) {
            Matcher m = this.uriPattern.matcher(request.getRequestURI());
            if (!m.find()) {
                // According to the regular expression, this request
                // should not be affected by the assertion:
                return true;
            }
        }

        return wrappedAssertion.matches(request, resource, principal);
    }

}

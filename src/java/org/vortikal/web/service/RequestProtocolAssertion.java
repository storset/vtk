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

import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;



/**
 * Assertion on the URL protocol fragment.
 * Legal values are <code>http</code> and <code>https</code>.
 *
 * <p>In addition to only matching on a given protocol, this assertion
 * can be configured to require a match only on the set of URIs
 * matching a given regular expression. This is useful for requiring
 * that certain subtrees are constricted to using the https protocol,
 * for example.
 *
 * <p>Configurable properties:
 * <ul>
 *   <lI><code>protocol</code> - the name of the protocol. Legal
 *   values are <code>http</code> and <code>https</code>.
 *   <li><code>uriPattern</code> - a regexp constraining the set of
 *   resources that this assertion applies to. If set, this regexp has
 *   to match in order for this assertion to even <emph>attempt</emph>
 *   to match the request protocol. Default value for this property is
 *   <code>null</code>.
 * </ul>
 */
public class RequestProtocolAssertion extends AssertionSupport
  implements RequestAssertion {
	
    private String protocol = null;
    private Pattern uriPattern = null;
	

    public String getProtocol() {
        return this.protocol;
    }
    

    public void setProtocol(String protocol) {
        if ("http".equals(protocol) || "https".equals(protocol)) {
            this.protocol = protocol;
        } else {
            throw new IllegalArgumentException(
                "Only http and https are supported protocols");
        }
    }


    public void setUriPattern(String uriPattern) {
        if (uriPattern == null) throw new IllegalArgumentException(
            "Property 'uriPattern' cannot be null");
    
        this.uriPattern = Pattern.compile(uriPattern);
    }
    

    public boolean matches(HttpServletRequest request) {

        if (this.uriPattern != null) {
            Matcher m = this.uriPattern.matcher(request.getRequestURI());
            if (!m.find()) {
                // According to the regular expression, this request
                // should not be affected by the assertion:
                return true;
            }
        }

        if ("http".equals(protocol))
            return !request.isSecure();
		
        return request.isSecure();
    }


    public boolean conflicts(Assertion assertion) {
        if (assertion instanceof RequestProtocolAssertion) {
            return ! (this.protocol.equals(
                          ((RequestProtocolAssertion)assertion).getProtocol()));
        }
        return false;
    }


    public void processURL(URL url, Resource resource, Principal principal) {
        if (this.uriPattern != null) {
            String uri = resource.getURI();
            if (resource.isCollection()) {
                uri += "/";
            }
            Matcher m = this.uriPattern.matcher(uri);
            if (!m.find()) {
                // According to the regular expression, this resource
                // should not be affected by the assertion:
                return;
            }
        }
        url.setProtocol(this.protocol);
    }
    

    public String toString() {
        StringBuffer sb = new StringBuffer();
		
        sb.append(super.toString());
        sb.append("; protocol = ").append(this.protocol);
        if (this.uriPattern != null) {
            sb.append("; uriPattern = " + this.uriPattern.pattern());
        }

        return sb.toString();
    }

}

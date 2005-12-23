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

import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;
import org.vortikal.web.RequestContext;


/**
 * Assertion on the URL protocol fragment.
 *
 * <p>Configurable JavaBean properties:
 * <ul>
 *   <lI><code>protocol</code> - the name of the protocol. Legal
 *   values are <code>*</code> (match any), <code>http</code> and
 *   <code>https</code>.
 *   <li><code>additionalProtocol</code> - an additional protocol to
 *   check. This protocol influences URL generation only if it matches
 *   the port of the servlet request.
 * </ul>
 */
public class RequestProtocolAssertion implements Assertion {
	
    private final static String PROTO_HTTP = "http";
    private final static String PROTO_HTTPS = "https";
    private final static String PROTO_ANY = "*";
    

    private String protocol;
	
    private String additionalProtocol;
    

    public String getProtocol() {
        return this.protocol;
    }
    

    public void setProtocol(String protocol) {
        if (PROTO_HTTP.equals(protocol) || PROTO_HTTPS.equals(protocol)
                                        || PROTO_ANY.equals(protocol)) {
            this.protocol = protocol;

        } else {
            throw new IllegalArgumentException(
                "Values '" + PROTO_ANY + "', '" + PROTO_HTTP
                + "' and '" + PROTO_HTTPS + "' are supported for the 'protocol' property");
        }
    }

    public void setAdditionalProtocol(String protocol) {
        if (PROTO_HTTP.equals(protocol) || PROTO_HTTPS.equals(protocol)) {
            this.additionalProtocol = protocol;
        } else {
            throw new IllegalArgumentException(
                "Values '" + PROTO_HTTP + "' and '" + PROTO_HTTPS
                + "' are supported for the 'additionalProtocol' property");
        }
    }

    public boolean conflicts(Assertion assertion) {
        if (assertion instanceof RequestProtocolAssertion) {
            if (PROTO_ANY.equals(this.protocol) ||
                PROTO_ANY.equals(((RequestProtocolAssertion) assertion).getProtocol())) {
                return false;
            }

            return ! (this.protocol.equals(
                          ((RequestProtocolAssertion)assertion).getProtocol()));
        }
        return false;
    }


    public boolean processURL(URL url, Resource resource,
                              Principal principal, boolean match) {
        if (!PROTO_ANY.equals(this.protocol)) {
            url.setProtocol(this.protocol);
        }
        
        if (this.additionalProtocol != null && !PROTO_ANY.equals(this.protocol)) {

            RequestContext requestContext = RequestContext.getRequestContext();
            String requestProtocol = getProtocol(requestContext.getServletRequest());
            
            if (this.additionalProtocol.equals(requestProtocol)) {
                url.setProtocol(requestProtocol);
            }
        }

        return true;
    }


    public boolean matches(HttpServletRequest request, Resource resource,
                           Principal principal) {
        if (PROTO_ANY.equals(this.protocol)) {
            return true;
        }

        String requestProtocol = getProtocol(request);
        if (this.protocol.equals(requestProtocol)) {
            return true;
        }

        if (this.additionalProtocol != null
            && this.additionalProtocol.equals(requestProtocol)) {
            return true;
        }

        return false;
    }


    public String toString() {
        StringBuffer sb = new StringBuffer();
		
        sb.append(super.toString());
        sb.append("; protocol = ").append(this.protocol);

        return sb.toString();
    }

    private String getProtocol(HttpServletRequest request) {
        return request.isSecure() ? PROTO_HTTPS : PROTO_HTTP;
    }
    

}

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


/**
 * Assertion matching on request port numbers.
 *
 * <p>Configurable JavaBean properties:
 * <ul>
 *   <li><code>port</code> - either the string <code>*</code> meaning
 *   'match all ports', or a port number (a positive integer)
 * </ul>
 */
public class RequestPortAssertion
  implements Assertion {

    private int port = -1;
	
    public void setPort(String port) {
		
        if (!"*".equals(port)) {
            this.port = Integer.parseInt(port);
            if (this.port <= 0) throw new IllegalArgumentException(
                "Server port number must be a positive integer");
        }
    }
	

    public int getPortNumber() {
        return this.port;
    }
	

    public boolean conflicts(Assertion assertion) {

        if (assertion instanceof RequestPortAssertion) {
            if (this.port == -1 ||
                ((RequestPortAssertion)assertion).getPortNumber() == -1) {
                return false;
            }
            return (this.port  != ((RequestPortAssertion)assertion).getPortNumber());
        }
        return false;
    }


    public String toString() {
        StringBuffer sb = new StringBuffer();
		
        sb.append(super.toString());
        if (this.port == -1) {
            sb.append("; port = *");
        } else {
            sb.append("; port = ").append(this.port);
        }
        return sb.toString();
    }


    public boolean processURL(URL url, Resource resource, Principal principal,
                              boolean match) {
        if (this.port != -1) {
            url.setPort(new Integer(this.port));
        }
        return true;
    }


    public boolean matches(HttpServletRequest request, Resource resource, Principal principal) {
        if (this.port == -1) {
            return true;
        }
        return port == request.getServerPort();
    }

}

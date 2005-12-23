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

import java.util.Set;
import javax.servlet.http.HttpServletRequest;

import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;
import org.vortikal.util.web.URLUtil;
import org.vortikal.web.RequestContext;

/**
 * Assertion that matches on the request hostname.
 *
 * <p>Configurable properties
 * <ul>
 *   <li><code>hostName</code> - the hostname to match. A value of
 *   <code>*</code> means that any hostname matches.
 *   <li><code>additionalHostNames</code> - a {@link Set} of
 *   additional host names to match for. When generating URLs, a
 *   hostname specified here has effect only if it matches that of the
 *   request
 * </ul>
 */
public class RequestHostNameAssertion implements Assertion {

    private String hostName;
    private Set additionalHostNames;
    
	
    public void setHostName(String hostName) {
        this.hostName = hostName;
    }


    public void setAdditionalHostNames(Set additionalHostNames) {
        this.additionalHostNames = additionalHostNames;
    }
    

    /**
     * Gets the host name. If the configuration parameter
     * <code>hostName</code> has a value of <code>*</code>, the
     * current host's default host name is returned.
     *
     */
    public String getHostName() {
        if ("*".equals(this.hostName)) {
            return org.vortikal.util.net.NetUtils.guessHostName();
        }
        return hostName;
    }


    public boolean conflicts(Assertion assertion) {
        if (assertion instanceof RequestHostNameAssertion) {
            String otherHostName = ((RequestHostNameAssertion)assertion).getHostName();
            if ("*".equals(this.hostName) || "*".equals(otherHostName)) {
                return false;
            }
            return ! (this.hostName.equals(otherHostName));
        }
        return false;
    }


    public boolean processURL(URL url, Resource resource, Principal principal, boolean match) {

        url.setHost(this.getHostName());

        if (this.additionalHostNames != null) {
            
            RequestContext requestContext = RequestContext.getRequestContext();

            String requestHostName = requestContext.getServletRequest().getServerName();
            if (this.additionalHostNames.contains(requestHostName)) {
                url.setHost(requestHostName);
            }
        }
        return true;
    }


    public boolean matches(HttpServletRequest request, Resource resource, Principal principal) {
        if ("*".equals(this.hostName)) {
            return true;
        }

        String requestHostName = request.getServerName();
        if (this.hostName.equals(requestHostName)) {
            return true;
        }

        if (this.additionalHostNames != null &&
            this.additionalHostNames.contains(requestHostName)) {
            return true;
        }

        return false;
    }


    public String toString() {
        StringBuffer sb = new StringBuffer();
		
        sb.append(super.toString());
        sb.append("; hostName = ").append(this.hostName);

        return sb.toString();
    }


}

/* Copyright (c) 2011, University of Oslo, Norway
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
import org.vortikal.util.net.IPv4Matcher;
import org.vortikal.util.net.IPv4SimpleMatcher;
import org.vortikal.util.net.IPv4SubnetMatcher;
import org.vortikal.util.net.IPv4WildcardMatcher;
import org.vortikal.web.RequestContext;

/**
 * An assertion that matches on or more remote IP-addresses.
 * 
 * IP-addresses can be matched one-to-one, by subnet or by using wildcard.
 * 
 * <p>Properties:</p>
 * <ul>
 *  <li>address - A single IP-address, IP-address wildcard or subnet specification</li>
 *  <li>addresses - A list of IP-addresses, wildcards and/or subnet specifications</li>
 *  <li>invert - Invert the matching</li>
 * </ul>
 * 
 * <p>Examples of valid address values</p>
 * <ul>
 *  <li>'192.168.0.1'</li>
 *  <li>'192.168.*'</li>
 *  <li>'192.*'</li>
 *  
 *  <li>'129.240.4.0/255.255.254.0' (subnet/network match)</li>
 *  <li>'129.240.4.0/23' (subnet/network match)</li>
 *  
 *  <li>'*' (matches any remote address)</li>
 * </ul>
 * 
 * TODO: Might consider remote host <em>name</em> assertions too, but DNS resolving
 *       can be time consuming if done for every match/request, so we should
 *       be careful with that.
 * TODO: Permit host-names to be configured and resolve them once to IP-addresses
 *       during bean initialization, then use the resolved IP-address 
 *       from that point on.
 * TODO: Only IPv4 supported for now. Consider supporting IPv6 as well.
 * 
 * @author oyviste
 *
 */
public class RequestRemoteAddressAssertion implements Assertion {

    private IPv4Matcher[] matchers;
    private IPv4Matcher matcher;
    private boolean invert = false;
    
    public boolean matches(HttpServletRequest request, Resource resource,
            Principal principal) {
        
        if (this.matcher != null && this.matcher.matches(request.getRemoteAddr())) {
            return !invert;
        }
        
        if (this.matchers != null) {
            for (int i=0; i<this.matchers.length; i++) {
                if (this.matchers[i].matches(request.getRemoteAddr())) {
                    return !invert;
                }
            }
        }
        
        return invert;
    }

    public boolean processURL(URL url, Resource resource, Principal principal,
            boolean match) {

        RequestContext requestContext = RequestContext.getRequestContext();
        HttpServletRequest request = requestContext.getServletRequest();

        if (match && request != null) {
            return matches(requestContext.getServletRequest(), resource, principal); 
        }
        
        return true;
    }

    public void processURL(URL url) {
        // Empty.
    }

    public boolean conflicts(Assertion assertion) {
        // Hard to determine ..
        return false;
    }
    
    private IPv4Matcher getMatcher(String input) {
        if (input.indexOf(IPv4WildcardMatcher.WILDCARD) != -1) {
            // Use an IPv4WildcardMatcher instance
            return new IPv4WildcardMatcher(input);
        } else if (input.indexOf('/') != -1) {
            // Subnet-matcher
            // Two supported notations (subnet-mask or CIDR):
            // 1) 10.0.0.0/255.0.0.0
            // 2) 10.0.0.0/8
            String parts[] = input.split("/");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid IPv4 subnet specification: '" 
                        + input + "'");
            }
            
            String networkAddress = parts[0];
            if (parts[1].indexOf('.') != -1) {
                // Plain netmask notation
                return new IPv4SubnetMatcher(networkAddress, parts[1]);
            } else {
                // CIDR notation
                return new IPv4SubnetMatcher(networkAddress, Integer.parseInt(parts[1]));
            }
            
        } else {
            // Use plain matcher
            return new IPv4SimpleMatcher(input);
        }
    }

    public String toString() {
        StringBuilder buffer = new StringBuilder("RequestRemoteAddressAssertion[");
        if (this.invert) {
            buffer.append("(INVERTED) ");
        }
        
        if (this.matcher != null) {
            buffer.append(this.matcher);
            if (this.matchers != null) buffer.append(", ");
        }
        
        if (this.matchers != null) {
            for (int i=0; i<this.matchers.length; i++) {
                buffer.append(this.matchers[i]);
                if (i < this.matchers.length-1) buffer.append(", ");
            }
        }
        buffer.append(']');
        return buffer.toString();
    }
    
    public void setAddress(String address) {
        this.matcher = getMatcher(address);
    }
    
    public void setAddresses(String[] addresses) {
        IPv4Matcher[] matchers = new IPv4Matcher[addresses.length];
        for (int i=0; i<addresses.length; i++) {
            matchers[i] = getMatcher(addresses[i]);
        }
        this.matchers = matchers;
    }
    
    public void setInvert(boolean invert) {
        this.invert = invert;
    }

}

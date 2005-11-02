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



import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;
import org.vortikal.web.RequestContext;

/**
 * Assertion that matches a regular expression (or a list of regular
 * expression) against the value of a configurable request header.
 *
 * <p>Configurable JavaBean properties:
 * <ul>
 *   <li><code>header</code> - the name of the header to check
 *   <li><code>pattern</code> - the regular expression to match
 *   <li><code>patterns</code> - a list of regular expressions to
 *   match. If one of these matches, the assertion also matches. This
 *   setting is incompatible with the <code>pattern</code> setting
 *   (i.e. only one can be specified).
 * </ul>
 */
public class RequestHeaderRegexpAssertion implements Assertion, InitializingBean {

    private Pattern pattern;
    private Pattern[] patterns;
    private String header;


    public void setPattern(String pattern) {
        if (pattern != null) {
            this.pattern = Pattern.compile(pattern);
        }
    }
    
    
    public void setPatterns(String[] patterns) {
        if (patterns != null) {
            this.patterns = new Pattern[patterns.length];
            for (int i = 0; i < patterns.length; i++) {
                this.patterns[i] = Pattern.compile(patterns[i]);
            }
        }
    }
    

    public void setHeader(String header) {
        if (header == null) throw new IllegalArgumentException(
            "Property 'header' cannot be null");
    
        this.header = header;
    }
    

    public void afterPropertiesSet() {
        if (this.pattern == null && this.patterns == null) {
            throw new BeanInitializationException(
                "One of JavaBean properties 'pattern' or 'patterns' must be specified");
        }
        if (this.pattern != null && this.patterns != null) {
            throw new BeanInitializationException(
                "Only one of JavaBean properties 'pattern' and 'patterns' can be specified");
        }
        if (this.header == null) {
            throw new BeanInitializationException(
                "JavaBean property 'header' must be specified");
        }
    }
    

    public boolean conflicts(Assertion assertion) {
        return false;
    }


    public String toString() {
        StringBuffer sb = new StringBuffer();
		
        sb.append(super.toString());
        sb.append("; header = ").append(this.header);
        if (this.pattern != null) {
            sb.append("; pattern = ").append(this.pattern);
        }
        if (this.patterns != null) {
            sb.append("; patterns = ").append(java.util.Arrays.asList(this.patterns));
        }
		
        return sb.toString();
    }


    public boolean matches(HttpServletRequest request, Resource resource,
                           Principal principal) {
        String headerValue = request.getHeader(this.header);
        if (headerValue == null) {
            return false;
        }
        if (this.patterns != null) {
            for (int i = 0; i < this.patterns.length; i++) {
                Matcher m = this.patterns[i].matcher(headerValue);
                if (m.matches()) {
                    return true;
                }
            }
            return false;
        }

        Matcher m = pattern.matcher(headerValue);
        return m.matches();
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
    

}

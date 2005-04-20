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

import java.util.Iterator;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;

/**
 * Assertion that matches on HTTP method(s).
 *
 * <p>Configurable properties:
 * <ul>
 *   <li><code>method</code> - the HTTP method to match
 *   <li><code>methods</code> - alternatively, a {@link Set set} of
 *   HTTP methods can be specified. Note that these configuration
 *   properties are "mutually exclusive", they cannot both be
 *   specified.
 * </ul>
 *
 */
public class RequestMethodAssertion extends AbstractRequestAssertion
  implements InitializingBean {

    private String method = null;
    private Set methods = null;
    
	
    public void setMethod(String method) {
        this.method = method;
    }
	
    public void setMethods(Set methods) {
        this.methods = methods;
    }
    
    public String getMethod() {
        return this.method;
    }

    public Set getMethods() {
        return this.methods;
    }

    public void afterPropertiesSet() {
        if (this.method != null && this.methods != null) {
            throw new BeanInitializationException(
                "Bean properties 'method' and 'methods' cannot both be set");
        }

        if (this.method == null && this.methods == null) {
            throw new BeanInitializationException(
                "Either bean property 'method' or 'methods' must be set");
        }
    }
    

    public boolean matches(HttpServletRequest request) {
        String reqMethod = request.getMethod();

        if (this.methods != null) {
            return this.methods.contains(reqMethod);
        }
        return reqMethod.equals(this.method);
    }


    public boolean conflicts(Assertion assertion) {
        if (!(assertion instanceof RequestMethodAssertion)) {
            return false;
        }

        RequestMethodAssertion other = (RequestMethodAssertion) assertion;

        if (this.method != null) {
            if (other.getMethods() != null) {
                return other.getMethods().contains(this.method);
            } 
            return !this.method.equals(other.getMethod());
        }

        if (other.getMethod() != null) {
            return !this.methods.contains(other.getMethod());
        }

        boolean intersect = false;
        for (Iterator i = other.getMethods().iterator(); i.hasNext();) {
            String method = (String) i.next();
            if (this.methods.contains(method)) {
                intersect = true;
                break;
            }
        }
        return !intersect;
    }


    public String toString() {
        StringBuffer sb = new StringBuffer();
		
        sb.append(super.toString());
        if (this.methods != null) {
            sb.append("; methods = ").append(this.methods);
        } else {
            sb.append("; method = ").append(this.method);
        }
        return sb.toString();
    }

}

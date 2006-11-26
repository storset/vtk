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

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;


/**
 * Class that takes an Asserion of any type as "parameter" and inverts its result
 * XXX: Cannot be used on request matching, impossible to invert url processing!
 * Properties:
 * 
 * <ul>
 *   <li>assertion - the assertion to test the inverted value of</li>
 *   <li>repository (should usually be set in the bean using this assertion)</li>
 * </ul>
 */

public class InvertAssertion extends AbstractAssertion implements InitializingBean {
    
    private Assertion assertion;
    private Repository repository;
    
  
    public boolean matches(HttpServletRequest request, Resource resource, Principal principal) {
        
        return ! this.assertion.matches(request, resource, principal);
    }
    
    
    public boolean conflicts(Assertion assertion) {
        if (assertion instanceof InvertAssertion)
            return true;

        return false;
    }
        
    public void processURL(URL url) {
        // Empty
        
    }

    public boolean processURL(URL url, Resource resource, Principal principal, boolean match) {
        return true;
    }
    
    
    public void afterPropertiesSet() throws Exception {
        if (this.assertion == null) 
            throw new BeanInitializationException("Property 'assertion' required");
        else if (this.repository == null) 
            throw new BeanInitializationException("Property 'repository' required");
    }
    
    
    
    /*
     * Public setters for configurable parameters
     */
    public void setAssertion(Assertion assertion) {
        this.assertion = assertion;
    }
    
    public void setRepository(Repository repository) {
        this.repository = repository;
    }

} // end class InvertAssertion


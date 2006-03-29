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

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;

/**
 * Assertion for matching on whether the {@link Repository content
 * repository} is currently operating in read-only mode.  
 *
 * <p>Configurable properties:
 * <ul>
 *   <li><code>repository</code> - the {@link Repository content
 *   repository} (required)
 *   <li><code>invert</code> - a boolean telling whether to return the
 *   inverted result when matching. Default is <code>false</code>.
 * </ul>
 *
 * <p>FIXME: this should be a temporary class, until we get JMX
 * support for configuring backend components.
 *
 * @see Repository#getConfiguration
 */
public class RepositoryReadOnlyAssertion
    extends AbstractRepositoryAssertion implements InitializingBean {
    
    private Repository repository = null;
    private boolean invert = false;

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public void setInvert(boolean invert) {
        this.invert = invert;
    }
    
     public void afterPropertiesSet() {
         if (this.repository == null) {
             throw new BeanInitializationException(
                 "Property 'repository' not set");
         }
     }
     

    public boolean conflicts(Assertion assertion) {
        if (assertion instanceof RepositoryReadOnlyAssertion) {
            RepositoryReadOnlyAssertion theAsserion
                = (RepositoryReadOnlyAssertion) assertion;
            return (this.repository == theAsserion.repository
                    && this.invert != theAsserion.invert);
        }
        return false;
    }
    

    public boolean matches(Resource resource, Principal principal) {
        if (this.invert) {
            return !this.repository.isReadOnly();
        }
        return this.repository.isReadOnly();
    }
    
    public String toString() {
        StringBuffer sb = new StringBuffer();
		
        sb.append(super.toString());
        sb.append("; repository = ").append(this.repository);
        sb.append("; read-only = ").append(!this.invert);

        return sb.toString();
    }

  
}

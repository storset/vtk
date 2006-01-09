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
 * Assertion checking the existence of a child resource with name <code>childName</code>.
 * In addition an optional <code>Assertion</code> may be specified to be evaluated against the child
 * if the child exists. If an assertion is specified, the repository must be supplied as well.
 *
 * <p>Configurable properties:
 * <ul>
 *   <li><code>childName</code> - the name of the child resource.
 *   <li><code>childResourceAssertion</code> - optional assertion to evaluate against the child resource. Note that
 *   this assertion only should assert about the resource, the request and principal parameters to match will be null! 
 *   <li><code>repository</code> - required if childAssertion is set
 *   <li><code>trustedToken</code> - required if childAssertion is set.
 * </ul>
 */
public class ResourceChildAssertion extends AbstractRepositoryAssertion implements InitializingBean  {

    private String childName;
    private Assertion childResourceAssertion;
    private Repository repository;
    private String trustedToken;
    
    public boolean matches(Resource resource, Principal principal) {
        if (!resource.isCollection()) {
            return false;
        }


        String[] childURIs = resource.getChildURIs();
        
        for (int i = 0; i < childURIs.length; i++) {
            String childURI = childURIs[i];
            if (childURI.substring(childURI.lastIndexOf("/") + 1).equals(childName)) {
                if (childResourceAssertion == null) return true;
                
                try {
                    Resource child = repository.retrieve(trustedToken, childURI, true);
                    return childResourceAssertion.matches(null, child, null);
                } catch (Exception e) {
                    return false;
                        //FIXME: catch IO/Auth/Repos instead?
                }
            }
        }
        
        return false;
    }

    public void setChildName(String childName) {
        if (childName == null) throw new IllegalArgumentException(
            "Property 'childName' cannot be null");
        this.childName = childName;
    }
    
    public boolean conflicts(Assertion assertion) {
        return false;
    }


    public String toString() {
        StringBuffer sb = new StringBuffer();
        
        sb.append(super.toString());
        sb.append("; childName = ").append(this.childName);

        return sb.toString();
    }

    public void setChildResourceAssertion(Assertion childResourceAssertion) {
        this.childResourceAssertion = childResourceAssertion;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public void afterPropertiesSet() throws Exception {
        if (childName == null) 
            throw new BeanInitializationException("Required property 'childName' not set");
        if (childResourceAssertion != null && repository == null) 
            throw new BeanInitializationException("Property 'repository' required when property 'childResourceAssertion' is set");
        if (childResourceAssertion != null && trustedToken == null) 
            throw new BeanInitializationException("Property 'trustedToken' required when property 'childResourceAssertion' is set");
    }

    public void setTrustedToken(String trustedToken) {
        this.trustedToken = trustedToken;
    }
    
    
    

}

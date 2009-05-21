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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repository.Path;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;

/**
 * Assertion checking the existence of a child resource with name
 * <code>childName</code>.  In addition an optional list of {@link
 * Assertion assertions} may be specified to be evaluated against the
 * child if the child exists. If the list of assertions is specified,
 * the repository must be supplied as well.
 *
 * <p>Configurable properties:
 * <ul>
 *   <li><code>childName</code> - the name of the child resource.
 *   <li><code>childResourceAssertions</code> - optional list of
 *   assertions to evaluate against the child resource. Note that this
 *   assertions should only assert about the resource, the request and
 *   principal parameters to match will be null!
 *   <li><code>repository</code> - required if childAssertion is set
 *   <li><code>trustedToken</code> - required if childAssertion is set.
 * </ul>
 */
public class ResourceChildAssertion extends AbstractRepositoryAssertion
  implements InitializingBean  {

    private String childName;
    private String[] childNames;
    private Set<String> childNameSet = new HashSet<String>();
    private Assertion[] childResourceAssertions;
    private Repository repository;
    private String trustedToken;
    
    public void setChildName(String childName) {
        if (childName == null) throw new IllegalArgumentException(
            "Property 'childName' cannot be null");
        this.childName = childName;
    }
    
    public void setChildNames(String[] childNames) {
        if (childNames == null) throw new IllegalArgumentException(
            "Property 'childNames' cannot be null");
        this.childNames = childNames;
    }
    
    public boolean conflicts(Assertion assertion) {
        return false;
    }

    public void setChildResourceAssertions(Assertion[] childResourceAssertions) {
        this.childResourceAssertions = childResourceAssertions;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public void setTrustedToken(String trustedToken) {
        this.trustedToken = trustedToken;
    }


    public void afterPropertiesSet() throws Exception {
        if (this.childName == null && this.childNames == null) 
            throw new BeanInitializationException(
                "One of JavaBean properties 'childName' or 'childNames' must be specified");
        if (this.childResourceAssertions != null && this.repository == null) 
            throw new BeanInitializationException(
                "JavaBean Property 'repository' required when property 'childResourceAssertions' is set");
        if (this.childResourceAssertions != null && this.trustedToken == null) 
            throw new BeanInitializationException(
                "JavaBean Property 'trustedToken' required when property 'childResourceAssertions' is set");
        if (this.childName != null) {
            this.childNameSet.add(this.childName);
        }
        if (this.childNames != null) {
            for (int i = 0; i < this.childNames.length; i++) {
                this.childNameSet.add(this.childNames[i]);    
            }
        }
    }


    public boolean matches(Resource resource, Principal principal) {
        if (resource == null || !resource.isCollection()) {
            return false;
        }
        List<Path> childURIs = resource.getChildURIs();
        for (Path childURI: childURIs) {
            if (this.childNameSet.contains(childURI.getName())) {
                if (this.childResourceAssertions == null) {
                    return true;
                }
                try {
                    Resource child = this.repository.retrieve(this.trustedToken, childURI, true);
                    for (Assertion assertion: this.childResourceAssertions) {
                        if (assertion.matches(null, child, null)) {
                            return true;
                        }
                    }
                    return false;
                } catch (Exception e) {
                    return false;
                }
            }
        }
        return false;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString());
        sb.append("; childNames = ").append(this.childNameSet);
        if (this.childResourceAssertions != null) {
            sb.append("; childResourceAssertions = ");
            sb.append(java.util.Arrays.asList(this.childResourceAssertions));
        }
        return sb.toString();
    }
}

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
package org.vortikal.repositoryimpl.index.observation;

import java.io.IOException;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repository.Repository;
import org.vortikal.repository.RepositoryException;
import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;
import org.vortikal.security.token.TokenManager;
import org.vortikal.web.service.RepositoryAssertion;



/**
 * Filter criterion that uses a set of {@link RepositoryAssertion
 * assertions} for determining if a resource should be filtered out of
 * an index.
 */
public class RepositoryAssertionFilterCriterion implements FilterCriterion, 
    InitializingBean {
    
    private Repository repository;
    private String token;
    private TokenManager tokenManager;
    
    private RepositoryAssertion[] assertions;
    
    
    public void afterPropertiesSet() throws BeanInitializationException {
        if (this.repository == null) {
            throw new BeanInitializationException(
                "JavaBean Property 'repository' not specified.");
        }
        if (this.token == null) {
            throw new BeanInitializationException(
                "JavaBean Property 'token' not specified.");
        }
        if (this.tokenManager == null) {
            throw new BeanInitializationException(
                "JavaBean Property 'tokenManager' not specified.");
        }
        if (this.assertions == null) {
            throw new BeanInitializationException(
                "JavaBean Property 'assertions' not specified.");
        }
    }
    
    public boolean isFiltered(String uri) {
        try {
            Resource resource = repository.retrieve(token, uri, false);
            Principal principal = this.tokenManager.getPrincipal(this.token);
            for (int i = 0; i < this.assertions.length; i++) {
                if (!this.assertions[i].matches(resource, principal)) {
                    return true;
                }
            }
        } catch (IOException io) {
        } catch (RepositoryException repex) {
        }
       return false;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setTokenManager(TokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }

    public void setAssertions(RepositoryAssertion[] assertions) {
        this.assertions = assertions;
    }
    
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("Assertions: ");
        buffer.append(java.util.Arrays.asList(this.assertions));

        return buffer.toString();
        
    }
}

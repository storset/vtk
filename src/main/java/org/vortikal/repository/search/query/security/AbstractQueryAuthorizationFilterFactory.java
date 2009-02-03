/* Copyright (c) 2009, University of Oslo, Norway
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
package org.vortikal.repository.search.query.security;

import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Filter;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.security.Principal;
import org.vortikal.security.PrincipalManager;
import org.vortikal.security.roles.RoleManager;
import org.vortikal.security.token.TokenManager;

/**
 * Does role-based checks and has methods for resolving <code>Principal</code>
 * from token.
 * 
 *
 */
public abstract class AbstractQueryAuthorizationFilterFactory implements 
                                                QueryAuthorizationFilterFactory {

    Log logger = LogFactory.getLog(AbstractQueryAuthorizationFilterFactory.class);
    
    private PrincipalManager principalManager;
    private TokenManager tokenManager;
    private RoleManager roleManager;
    
    
    public abstract Filter authorizationQueryFilter(String token, IndexReader reader)
        throws QueryAuthorizationException;
    
    protected Principal getPrincipal(String token) {
        return this.tokenManager.getPrincipal(token);
    }
    
    protected Set<Principal> getPrincipalMemberGroups(Principal principal) {
        return this.principalManager.getMemberGroups(principal);
    }
    
    protected boolean isAuthorizedByRole(Principal principal) {
        return (this.roleManager.hasRole(principal, RoleManager.READ_EVERYTHING)
             || this.roleManager.hasRole(principal, RoleManager.ROOT));
    }

    @Required
    public void setPrincipalManager(PrincipalManager principalManager) {
        this.principalManager = principalManager;
    }


    @Required
    public void setTokenManager(TokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }


    @Required
    public void setRoleManager(RoleManager roleManager) {
        this.roleManager = roleManager;
    }

    
    
}

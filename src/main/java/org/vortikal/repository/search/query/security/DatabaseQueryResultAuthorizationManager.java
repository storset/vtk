/* Copyright (c) 2006, 2007, University of Oslo, Norway
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repository.store.IndexDao;
import org.vortikal.security.Principal;
import org.vortikal.security.PrincipalManager;
import org.vortikal.security.roles.RoleManager;
import org.vortikal.security.token.TokenManager;

/**
 * Authorize result lists using {@link org.vortikal.repository.store.IndexDao}.
 * @deprecated
 */
public final class DatabaseQueryResultAuthorizationManager implements 
    QueryResultAuthorizationManager, InitializingBean {

    Log logger = LogFactory.getLog(DatabaseQueryResultAuthorizationManager.class);
    
    private PrincipalManager principalManager;
    private TokenManager tokenManager;
    private IndexDao indexDao;
    private RoleManager roleManager;
    
    /**
     * Set of principal names for which all hits are automatically
     * authorized without checking database.
     */
    private Set<String> noAuthorizationCheckForPrincipals;
    
    public void afterPropertiesSet() throws BeanInitializationException {
        
        if (this.principalManager == null) {
            throw new BeanInitializationException(
                    "Bean property 'principalManager' not set (null).");
        } else if (this.indexDao == null) {
            throw new BeanInitializationException(
                    "Bean property 'indexDataAccessor' not set (null).");
        } else if (this.tokenManager == null) {
            throw new BeanInitializationException("Bean property 'tokenManager' not set.");
        } else if (this.roleManager == null) {
            throw new BeanInitializationException("Bean property 'roleManager' not set.");
        }
        
    }

    public void authorizeQueryResults(String token, List<ResultSecurityInfo> rsiList) 
        throws QueryAuthorizationException {
        
        Principal principal = this.tokenManager.getPrincipal(token);
        
        if (principal != null) {
            // Check if auto-authorization is configured for principal 
            if (isAuthorizedByConfig(principal)) {
                this.logger.info("Unconditionally authorizing all results for principal '" + 
                        principal + "'");
                
                for (ResultSecurityInfo rsi: rsiList) {
                    rsi.setAuthorized(true);
                }
            
                return;
            }
            
            // Check if principal is authorized to view all results by its role
            // (ROOT role or READ_EVERYTHING role)
            if (isAuthorizedByRole(principal)) {
                if (this.logger.isDebugEnabled()) {
                    this.logger.debug("Authorizing all results for principal '" 
                            + principal.getQualifiedName() + "' by role");
                }
                
                for (ResultSecurityInfo rsi: rsiList) {
                    rsi.setAuthorized(true);
                }
                
                return;
            }
        }

        
        Set<String> principalNames = new HashSet<String>();
        if (principal != null) {
            principalNames.add(principal.getQualifiedName());
            
            for (Principal group: this.principalManager.getMemberGroups(principal)) {
                principalNames.add(group.getQualifiedName());
            }
        } 
        
        if (logger.isDebugEnabled()) {
            logger.debug("Qualified principal names: " + principalNames);
        }
        
        try {
            this.indexDao.processQueryResultsAuthorization(principalNames, 
                                                                    rsiList);
        } catch (Exception e) {
            this.logger.warn("Exception while authorizing query result list: ", e);
            throw new QueryAuthorizationException(e);
        }
        
    }
    
    private boolean isAuthorizedByConfig(Principal principal) {
        if (this.noAuthorizationCheckForPrincipals != null) {
            return (this.noAuthorizationCheckForPrincipals.contains(
                                                    principal.getQualifiedName()));
        }

        return false;
    }
    
    private boolean isAuthorizedByRole(Principal principal) {
        return (this.roleManager.hasRole(principal, RoleManager.READ_EVERYTHING)
             || this.roleManager.hasRole(principal, RoleManager.ROOT));
    }

    public void setIndexDao(IndexDao indexDao) {
        this.indexDao = indexDao;
    }

    public void setPrincipalManager(PrincipalManager principalManager) {
        this.principalManager = principalManager;
    }

    public void setTokenManager(TokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }

    public void setNoAuthorizationCheckForPrincipals(Set<String> noAuthorizationCheckForPrincipals) {
        this.noAuthorizationCheckForPrincipals = noAuthorizationCheckForPrincipals;
    }

    public void setRoleManager(RoleManager roleManager) {
        this.roleManager = roleManager;
    }

}

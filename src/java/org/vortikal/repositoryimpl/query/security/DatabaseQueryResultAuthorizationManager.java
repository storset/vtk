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
package org.vortikal.repositoryimpl.query.security;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repositoryimpl.dao.IndexDataAccessor;
import org.vortikal.security.PrincipalManager;
import org.vortikal.security.token.TokenManager;

/**
 * Authorize result lists using <code>IndexDataAccessor</code> (database).
 * 
 * @author oyviste
 *
 */
public final class DatabaseQueryResultAuthorizationManager implements 
    QueryResultAuthorizationManager, InitializingBean {

    private PrincipalManager principalManager;
    private TokenManager tokenManager;
    private IndexDataAccessor indexDataAccessor;
    
    public void afterPropertiesSet() throws BeanInitializationException {
        
        if (this.principalManager == null) {
            throw new BeanInitializationException(
                    "Bean property 'principalManager' not set (null).");
        } else if (this.indexDataAccessor == null) {
            throw new BeanInitializationException(
                    "Bean property 'indexDataAccessor' not set (null).");
        } else if (this.tokenManager == null) {
            throw new BeanInitializationException("Bean property 'tokenManager' not set.");
        }
        
    }

    public void authorizeQueryResults(String token, List resultSecurityInfo) 
        throws QueryAuthorizationException {
        //Principal principal = tokenManager.getPrincipal(token);
        
        // XXX: resolve groups, etc.
        // XXX: only filter on read-for-all, right now .. (FIXME)
        try {
            
            indexDataAccessor.processQueryResultsAuthorization(resultSecurityInfo);
            
        } catch (IOException io) {
            // XXX: log + proper message
            throw new QueryAuthorizationException(io.getMessage());
        }
        
    }


    
}

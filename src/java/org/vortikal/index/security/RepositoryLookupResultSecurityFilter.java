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

package org.vortikal.index.security;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.index.ModifiableResults;
import org.vortikal.repository.AuthorizationException;
import org.vortikal.repository.Repository;
import org.vortikal.security.AuthenticationException;

/**
 * Simple, quick and dirty filtering of a result set based on a given
 * security token. Does brute-force repository lookups.
 * NOTE: The filter introduces _significant_ overhead in the process of index
 *       querying.
 *
 * @author oyviste
 */
public class RepositoryLookupResultSecurityFilter implements ResultSecurityFilter,
    InitializingBean {
    
    Log logger = LogFactory.getLog(RepositoryLookupResultSecurityFilter.class);
    
    private Repository repository;
    
    public void afterPropertiesSet() {
        if (repository == null) {
            throw new BeanInitializationException("Property 'repository' not set.");
        }
    }
    
    
    
    /**
     * Filters result set in accordance to what resources the given
     * security token has access to.
     * 
     * @param results Result set to filter (this will be modified).
     * @param token The security token to use as filtering criteria.
     */
    public void filterResults(ModifiableResults results, String token) {
        String uri = null;
        for (int i=0; i<results.getSize(); i++) {
            try {
                uri = results.getResultMetadata(i).getUri();
                // Try and see if we can get resource without causing an exception to be
                // thrown.
                repository.retrieve(token, uri, true);
                continue;
            } catch (AuthorizationException authorizationEx) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Removing result at URI '" + uri + "', reason: " +
                            " AuthorizationException using given security token.");
                }
            } catch (AuthenticationException authenticationEx) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Removing result at URI '" + uri + "', reason: " +
                            " AuthenticationException using given security token.");
                }
            } catch (Exception e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Removing result at URI: '" + uri + "', reason: " +
                            " Exception caught trying to retrieve resource.");
                }
            }
            results.removeResult(i--);
        }
    }
    
    public void setRepository(Repository repository) {
        this.repository = repository;
    }
}

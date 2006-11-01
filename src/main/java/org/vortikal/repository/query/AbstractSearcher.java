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
package org.vortikal.repository.query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repositoryimpl.index.ModifiableResults;
import org.vortikal.repositoryimpl.index.Results;
import org.vortikal.repositoryimpl.index.security.ResultSecurityFilter;
import org.vortikal.repositoryimpl.index.security.ResultSecurityFilterException;

/**
 * Security features.
 * 
 * @deprecated
 * @author oyviste
 */
public abstract class AbstractSearcher implements Searcher, InitializingBean {

    private static Log logger = LogFactory.getLog(AbstractSearcher.class);
    
    private ResultSecurityFilter securityFilter;
    private boolean applySecurityFilter = true;
    
    protected void applySecurityFilter(ModifiableResults results, String token) 
        throws ResultSecurityFilterException {
        if (this.applySecurityFilter && this.securityFilter != null) {
            this.securityFilter.filterResults(results, token);
        }
    }
    
    public void afterPropertiesSet() {
        if (this.securityFilter == null) {
            logger.warn("No result security filter configured.");
        }
    }
    
    public void setApplySecurityFilter(boolean applySecurityFilter) {
        this.applySecurityFilter = applySecurityFilter;
    }

    public void setSecurityFilter(ResultSecurityFilter securityFilter) {
        this.securityFilter = securityFilter;
    }

    public abstract Results execute(String token, Query query) throws QueryException;
    
    public abstract Results execute(String token, Query query, int maxResults);
    
    public abstract Results execute(String token, Query query, int maxResults, int cursor);

}

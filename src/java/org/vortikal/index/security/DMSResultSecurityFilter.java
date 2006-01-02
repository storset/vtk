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

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.vortikal.index.ModifiableResults;
import org.vortikal.security.Principal;
import org.vortikal.security.SecurityContext;

/**
 * DMS queries should allow searches by root@localhost principal
 * to go through unfiltered. In addition, if there are
 * more than 1000 hits, normal users won't get any results, bec
 * 
 * @author oyviste 
 *
 */
public class DMSResultSecurityFilter extends
        RepositoryLookupResultSecurityFilter {

    private static Log logger = LogFactory.getLog(DMSResultSecurityFilter.class);
    
    /** Set of principals (fully qualified principal names)
     *  where the filter will not be applied.
     *  Typically, only 'root@localhost' should be put here.
     */
    private Set noFilterPrincipals = new HashSet();
    private int maxFilteredPrincipalsResults = 1000;
    
    public void filterResults(ModifiableResults results, String token) 
        throws TooManyResultsException {
        
        Principal p = null;
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        if (securityContext != null) {
            p = securityContext.getPrincipal();
        }

        if (p != null) {
            if (noFilterPrincipals.contains(p.getQualifiedName())) {
                // Don't filter results for this principal.
                if (logger.isDebugEnabled()) {
                    logger.debug("Not filtering results for principal '" + 
                                 p.getQualifiedName() + "'");
                }
                return;
            }
        }
        
        // Results should be filtered according to given security token
        // Make sure we don't run the [slow] security filter on more than 
        // 'maxFilteredPrincipalResults ' results.
        if (results.getSize() > maxFilteredPrincipalsResults) {
            throw new TooManyResultsException(
                    "Result size exceeded maximum of " + maxFilteredPrincipalsResults, 
                    token);
        }
        
        // Just filter results normally.
        super.filterResults(results, token);
    }
    
    public void setNoFilterPrincipals(Set noFilterPrincipals) {
        this.noFilterPrincipals = noFilterPrincipals;
    }
    
    public void setMaxFilteredPrincipalsResults(int maxFilteredPrincipalsResults) {
        this.maxFilteredPrincipalsResults = maxFilteredPrincipalsResults;
    }
    
}

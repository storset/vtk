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

import java.io.IOException;
import java.util.BitSet;
import java.util.Set;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.TermsFilter;
import org.apache.lucene.util.OpenBitSet;
import org.vortikal.repository.index.mapping.FieldNameMapping;
import org.vortikal.security.Principal;
import org.vortikal.security.PrincipalFactory;

/**
 * Simple query authorization filter factory which generates filters, but does
 * no caching at all.
 */
public class SimpleQueryAuthorizationFilterFactory extends
        AbstractQueryAuthorizationFilterFactory {

    protected final static Term READ_FOR_ALL_TERM = 
            new Term(FieldNameMapping.ACL_READ_PRINCIPALS_FIELD_NAME, 
                        PrincipalFactory.NAME_ALL);
    
    protected final static Filter ACL_READ_FOR_ALL_FILTER = 
            new ACLReadForAllFilter();
    
    @Override
    public Filter authorizationQueryFilter(String token, IndexReader reader) {

        if (token == null) {
            // Generate a filter which only allows read-for-all documents
            return ACL_READ_FOR_ALL_FILTER;
        }

        Principal principal = getPrincipal(token);

        if (principal == null) {
            // Invalid token (no mapping to principal), return read-for-all only filter
            return ACL_READ_FOR_ALL_FILTER;
        }

        if (isAuthorizedByRole(principal)) {
            return null; // No filter (root-level user)
        }
        
        // Get member groups
        Set<Principal> memberGroups = getPrincipalMemberGroups(principal);
        
        // Build filter for principal and member groups
        return buildACLReadFilter(principal, memberGroups);
    }
    
    private Filter buildACLReadFilter(Principal principal, Set<Principal> memberGroups) {
    
        TermsFilter termsFilter = new TermsFilter();
        for (Principal group: memberGroups) {
            termsFilter.addTerm(new Term(FieldNameMapping.ACL_READ_PRINCIPALS_FIELD_NAME, 
                                                    group.getQualifiedName()));
        }

        // Add ALL principal
        termsFilter.addTerm(new Term(FieldNameMapping.ACL_READ_PRINCIPALS_FIELD_NAME, PrincipalFactory.ALL.getQualifiedName()));
        
        // Add principal executing the query
        termsFilter.addTerm(new Term(FieldNameMapping.ACL_READ_PRINCIPALS_FIELD_NAME, principal.getQualifiedName()));
        
        return termsFilter;
    }
    
    /**
     * Just a quick and easy term filter for the common case of anonymous queries,
     * which require that the document is read-for-all.
     * This should be a lot faster than wrapping
     * a TermQuery with <code>QueryWrapperFilter</code>.
     */
    private static class ACLReadForAllFilter extends Filter {
        
        private static final long serialVersionUID = -1927640174374225525L;

        @Override
        public DocIdSet getDocIdSet(IndexReader reader) throws IOException {
         
            OpenBitSet docIdSet = new OpenBitSet(reader.maxDoc());
            TermDocs tdocs = reader.termDocs(READ_FOR_ALL_TERM);
            try {
                while (tdocs.next()) {
                    docIdSet.fastSet(tdocs.doc());
                }
            } finally {
                tdocs.close();
            }
            
            return docIdSet;
        }

    }
    
}

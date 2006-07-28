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
package org.vortikal.repositoryimpl.query.builders;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.vortikal.repositoryimpl.query.DocumentMapper;
import org.vortikal.repositoryimpl.query.QueryBuilder;
import org.vortikal.repositoryimpl.query.QueryBuilderException;

/**
 * 
 * @author oyviste
 */
public class UriPrefixQueryBuilder implements QueryBuilder {

    private Term idTerm;
    private String uri;
    
    /**
     * 
     * @param idTerm The <code>Term</code> containing the special id of the property set
     *        that represents the URI prefix (the ancestor).
     */
    public UriPrefixQueryBuilder(String uri, Term idTerm) {
        this.idTerm = idTerm;
        this.uri = uri;
    }
    
    public Query buildQuery() throws QueryBuilderException {
        // Use ancestor ids field from index to get all descendants
        TermQuery uriDescendants = 
            new TermQuery(
                    new Term(DocumentMapper.ANCESTORIDS_FIELD_NAME, this.idTerm.text()));

        if (this.uri.endsWith("/")) {
            // Don't include parent
            // XXX: Note that the root URI '/' is a special case, it will not be included
            //      as part of URI prefix query results (only the children).
            //      If we need to differentiate between the "include-self or not"-case
            //      for the root resource, this info has to be explicitly available in query class.
            return uriDescendants;
        }
        // Include the parent URI as well
        BooleanQuery bq = new BooleanQuery();
        TermQuery uriTermq = new TermQuery(this.idTerm);
        bq.add(uriTermq, BooleanClause.Occur.SHOULD);
        bq.add(uriDescendants, BooleanClause.Occur.SHOULD);
        
        return bq;
    }

}

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
package org.vortikal.repository.search.query.builders;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.PrefixFilter;
import org.apache.lucene.search.Query;
import org.vortikal.repository.index.mapping.FieldNames;
import org.vortikal.repository.search.query.NamePrefixQuery;
import org.vortikal.repository.search.query.QueryBuilder;
import org.vortikal.repository.search.query.QueryBuilderException;
import org.vortikal.repository.search.query.filter.InversionFilter;

/**
 * XXX: Somewhat experimental, as it uses a constant-score query with a filter
 *      to do the actual prefix query. It has no upper limitation on the number
 *      of matches (we don't risk the BooleanQuery.TooManyClauses-exception).
 * 
 * @author oyviste
 */
public class NamePrefixQueryBuilder implements QueryBuilder {

    private NamePrefixQuery query;
    private Filter deletedDocsFilter;

    public NamePrefixQueryBuilder(NamePrefixQuery query) {
        this.query = query;
    }
    
    public NamePrefixQueryBuilder(NamePrefixQuery query, Filter deletedDocs) {
        this(query);
        this.deletedDocsFilter = deletedDocs;
    }

    /* (non-Javadoc)
     * @see org.vortikal.repository.query.QueryBuilder#buildQuery()
     */
    @Override
    public Query buildQuery() throws QueryBuilderException {
        
        Term prefixTerm = new Term(FieldNames.NAME_FIELD_NAME, 
                                                        this.query.getTerm());
        
        Filter filter = new PrefixFilter(prefixTerm);
        
        if (query.isInverted()) {
            filter = new InversionFilter(filter, this.deletedDocsFilter);
        }

        ConstantScoreQuery csq = new ConstantScoreQuery(filter);
        
        return csq;
    }

}

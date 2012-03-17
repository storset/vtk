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
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.TermQuery;
import org.vortikal.repository.index.mapping.FieldNames;
import org.vortikal.repository.search.query.QueryBuilder;
import org.vortikal.repository.search.query.QueryBuilderException;
import org.vortikal.repository.search.query.UriPrefixQuery;
import org.vortikal.repository.search.query.filter.InversionFilter;

/**
 * 
 * @author oyviste
 */
public class UriPrefixQueryBuilder implements QueryBuilder {

    private final UriPrefixQuery upQuery;
    private Filter deletedDocsFilter;
    
    public UriPrefixQueryBuilder(UriPrefixQuery upQuery) {
        this.upQuery = upQuery;
    }
    
    public UriPrefixQueryBuilder(UriPrefixQuery upQuery, Filter deletedDocs) {
        this.upQuery = upQuery;
        this.deletedDocsFilter = deletedDocs;
    }

    @Override
    public Query buildQuery() throws QueryBuilderException {
        Query query = new TermQuery(new Term(FieldNames.URI_ANCESTORS_FIELD_NAME, upQuery.getUri()));

        if (this.upQuery.isIncludeSelf()) {
            BooleanQuery bq = new BooleanQuery();
            TermQuery uriTermq = new TermQuery(new Term(FieldNames.URI_FIELD_NAME, upQuery.getUri()));
            bq.add(uriTermq, BooleanClause.Occur.SHOULD);
            bq.add(query, BooleanClause.Occur.SHOULD);
            query = bq;
        }

        if (this.upQuery.isInverted()) {
            Filter filter = new InversionFilter(new QueryWrapperFilter(query), this.deletedDocsFilter);
            return new ConstantScoreQuery(filter);
        }

        return query;
    }
}

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

import java.util.List;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.vortikal.repository.search.query.AbstractMultipleQuery;
import org.vortikal.repository.search.query.AndQuery;
import org.vortikal.repository.search.query.LuceneQueryBuilder;
import org.vortikal.repository.search.query.OrQuery;
import org.vortikal.repository.search.query.Query;
import org.vortikal.repository.search.query.QueryBuilder;


public class QueryTreeBuilder implements QueryBuilder {

    private AbstractMultipleQuery query;
    private LuceneQueryBuilder factory;
    private IndexReader reader;
    
    public QueryTreeBuilder(LuceneQueryBuilder factory, 
            IndexReader reader, AbstractMultipleQuery query) {
        this.query = query;
        this.factory = factory;
        this.reader = reader;
    }

    @Override
    public org.apache.lucene.search.Query buildQuery() {
        return buildInternal(this.query);
    }
    
    private org.apache.lucene.search.Query buildInternal(Query query) {
        Occur occur = null;
        
        if (query instanceof AndQuery) {
            occur = BooleanClause.Occur.MUST;
        } else if (query instanceof OrQuery) {
            occur = BooleanClause.Occur.SHOULD;
        } else {
            return this.factory.buildQuery(query, reader);
        }

        AbstractMultipleQuery multipleQuery = (AbstractMultipleQuery)query;
        List<Query> subQueries = multipleQuery.getQueries();
        
        BooleanQuery bq = new BooleanQuery(true);
        for (Query q: subQueries) {
            bq.add(buildInternal(q), occur);
        }
        
        return bq;
    }
}

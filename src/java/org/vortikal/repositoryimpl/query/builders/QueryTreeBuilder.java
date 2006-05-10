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

import java.util.Iterator;
import java.util.List;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.vortikal.repositoryimpl.query.QueryBuilder;
import org.vortikal.repositoryimpl.query.QueryBuilderFactory;
import org.vortikal.repositoryimpl.query.query.AbstractMultipleQuery;
import org.vortikal.repositoryimpl.query.query.AndQuery;
import org.vortikal.repositoryimpl.query.query.OrQuery;
import org.vortikal.repositoryimpl.query.query.Query;


/**
 * 
 * @author oyviste
 *
 */
public class QueryTreeBuilder implements QueryBuilder {

    AbstractMultipleQuery query;
    QueryBuilderFactory factory;
    
    public QueryTreeBuilder(QueryBuilderFactory factory, AbstractMultipleQuery query) {
        this.query = query;
        this.factory = factory;
    }

    public org.apache.lucene.search.Query buildQuery() {
        return buildInternal(this.query);
    }
    
    private org.apache.lucene.search.Query buildInternal(Query query) {
        
        if (query instanceof AndQuery) {
            AndQuery andQ = (AndQuery)query;
            List subQueries = andQ.getQueries();
            
            BooleanQuery bq = new BooleanQuery(true);
            for (Iterator i = subQueries.iterator(); i.hasNext();) {
                bq.add(buildInternal((Query)i.next()), BooleanClause.Occur.MUST);
            }

            return bq;
        } else if (query instanceof OrQuery) {
            OrQuery orQ = (OrQuery)query;
            List subQueries = orQ.getQueries();
            
            BooleanQuery bq = new BooleanQuery(true);
            for (Iterator i = subQueries.iterator(); i.hasNext();) {
                bq.add(buildInternal((Query)i.next()), BooleanClause.Occur.SHOULD);
            }

            return bq;
            
        } else {
            
            return this.factory.getBuilder(query).buildQuery();
        }
    }
}

/* Copyright (c) 2008, University of Oslo, Norway
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
package org.vortikal.web.search;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Resource;
import org.vortikal.repository.search.QueryParser;
import org.vortikal.repository.search.query.AndQuery;
import org.vortikal.repository.search.query.OrQuery;
import org.vortikal.repository.search.query.Query;
import org.vortikal.repository.search.query.UriDepthQuery;

public class QueryStringSearchComponent extends QuerySearchComponent {

    private String query;
    private QueryParser queryParser;

    @Override
    protected Query getQuery(Resource collection, HttpServletRequest request, boolean recursive) {

        Query query = this.queryParser.parse(this.query);

        Query aggregationQuery = null;
        boolean aggregate = false;
        if (this.aggregationResolver != null) {
            aggregationQuery = this.aggregationResolver.getAggregationQuery(query, collection);
            if (!query.equals(aggregationQuery)) {
                aggregate = true;
            }
        }

        if (!recursive) {
            AndQuery andQuery = new AndQuery();
            andQuery.add(query);
            andQuery.add(new UriDepthQuery(collection.getURI().getDepth() + 1));
            query = andQuery;
        }

        if (aggregate) {
            OrQuery orQuery = new OrQuery();
            orQuery.add(query);
            orQuery.add(aggregationQuery);
            return orQuery;
        }

        return query;
    }

    @Required
    public void setQuery(String query) {
        this.query = query;
    }

    @Required
    public void setQueryParser(QueryParser queryParser) {
        this.queryParser = queryParser;
    }

}

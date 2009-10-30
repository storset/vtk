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
package org.vortikal.web.display.collection.aggregation;

import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.repository.search.query.AbstractMultipleQuery;
import org.vortikal.repository.search.query.AndQuery;
import org.vortikal.repository.search.query.OrQuery;
import org.vortikal.repository.search.query.Query;
import org.vortikal.repository.search.query.TermOperator;
import org.vortikal.repository.search.query.UriPrefixQuery;

public class CollectionListingAggregationResolver implements AggregationReslover {

    private Repository repository;
    private PropertyTypeDefinition aggregationPropDef;

    public Query extend(Query query, Resource collection) throws IllegalArgumentException {

        // Only extend simple UriPrefixQuery or instances of
        // AbstractMultipleQuery (AndQuery or OrQuery) containing UriPrefixQuery
        if (!(query instanceof AbstractMultipleQuery || query instanceof UriPrefixQuery)) {
            IllegalArgumentException up = new IllegalArgumentException("Unsupported query type: " + query);
            throw up;
        }

        Property aggregationProp = collection.getProperty(this.aggregationPropDef);
        if (aggregationProp != null) {

            OrQuery aggregatedFoldersQuery = new OrQuery();
            for (Value value : aggregationProp.getValues()) {

                // XXX handle recursive aggregation, limit, max depth etc...

                aggregatedFoldersQuery.add(new UriPrefixQuery(value.toString(), TermOperator.EQ, false));
            }

            if (aggregatedFoldersQuery.getQueries().size() > 0) {
                // A simple UriPrefixQuery -> just extend and return
                if (isExtendableUriPrefixQuery(query)) {
                    aggregatedFoldersQuery.add(query);
                    return aggregatedFoldersQuery;
                } else {
                    // Iterate the query and build a new one, extending any
                    // UriPrefixQuery you encounter
                    AbstractMultipleQuery extendableQuery = getExtendableQuery(query);
                    for (Query q : ((AbstractMultipleQuery) query).getQueries()) {
                        q = extend(q, aggregatedFoldersQuery, null);
                        extendableQuery.add(q);
                    }
                    return extendableQuery;
                }
            }

        }

        return query;

    }

    private Query extend(Query query, OrQuery aggregatedFoldersQuery, AbstractMultipleQuery extendable) {

        if (query instanceof AbstractMultipleQuery) {
            if (extendable == null) {
                extendable = getExtendableQuery(query);
            }
            for (Query q : ((AbstractMultipleQuery) query).getQueries()) {
                if (isExtendableUriPrefixQuery(q)) {
                    q = getAggregatedUriQuery(q, aggregatedFoldersQuery);
                } else if (q instanceof AbstractMultipleQuery) {
                    q = extend(q, aggregatedFoldersQuery, getExtendableQuery(q));
                }
                extendable.add(q);
            }
            return extendable;
        } else if (isExtendableUriPrefixQuery(query)) {
            query = getAggregatedUriQuery(query, aggregatedFoldersQuery);
        }

        return query;

    }

    private AbstractMultipleQuery getExtendableQuery(Query query) {
        if (query instanceof AndQuery) {
            return new AndQuery();
        }
        return new OrQuery();
    }

    private Query getAggregatedUriQuery(Query query, OrQuery aggregatedFoldersQuery) {
        OrQuery aggregatedUriQuery = new OrQuery();
        aggregatedUriQuery.add(query);
        for (Query q : aggregatedFoldersQuery.getQueries()) {
            aggregatedUriQuery.add(q);
        }
        return aggregatedUriQuery;
    }

    private boolean isExtendableUriPrefixQuery(Query query) {
        if (query instanceof UriPrefixQuery) {
            UriPrefixQuery uriPrefixQuery = (UriPrefixQuery) query;
            if (uriPrefixQuery.getOperator() == null || uriPrefixQuery.getOperator() == TermOperator.EQ) {
                return true;
            }
        }
        return false;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public void setAggregationPropDef(PropertyTypeDefinition aggregationPropDef) {
        this.aggregationPropDef = aggregationPropDef;
    }

}

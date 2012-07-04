/* Copyright (c) 2012, University of Oslo, Norway
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
package org.vortikal.web.search.collectionlisting;

import java.util.List;

import org.vortikal.repository.search.ConfigurablePropertySelect;
import org.vortikal.repository.search.Sorting;
import org.vortikal.repository.search.query.Query;
import org.vortikal.web.display.collection.aggregation.CollectionListingAggregatedResources;
import org.vortikal.web.service.URL;

public class CollectionListingConditions {

    private String token;
    private Query uriQuery;
    private List<Query> additionalQueries;
    private CollectionListingAggregatedResources collectionListingAggregatedResources;
    private int limit;
    private int offset;
    private Sorting sorting;
    private URL url;
    private ConfigurablePropertySelect propertySelect;

    public CollectionListingConditions(String token, Query uriQuery, List<Query> additionalQueries,
            CollectionListingAggregatedResources collectionListingAggregatedResources, int limit, int offset,
            Sorting sorting, URL url, ConfigurablePropertySelect propertySelect) {
        this.token = token;
        this.uriQuery = uriQuery;
        this.additionalQueries = additionalQueries;
        this.collectionListingAggregatedResources = collectionListingAggregatedResources;
        this.limit = limit;
        this.offset = offset;
        this.sorting = sorting;
        this.url = url;
        this.propertySelect = propertySelect;
    }

    public String getToken() {
        return this.token;
    }

    public Query getUriQuery() {
        return this.uriQuery;
    }

    public List<Query> getAdditionalQueries() {
        return this.additionalQueries;
    }

    public CollectionListingAggregatedResources getCollectionListingAggregatedResources() {
        return this.collectionListingAggregatedResources;
    }

    public int getLimit() {
        return this.limit;
    }

    public int getOffset() {
        return this.offset;
    }

    public Sorting getSorting() {
        return this.sorting;
    }

    public URL getUrl() {
        return this.url;
    }

    public ConfigurablePropertySelect getPropertySelect() {
        return propertySelect;
    }

}

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
 *      * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
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

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.MultiHostSearcher;
import org.vortikal.repository.Path;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.search.ConfigurablePropertySelect;
import org.vortikal.repository.search.ResultSet;
import org.vortikal.repository.search.Search;
import org.vortikal.repository.search.Sorting;
import org.vortikal.repository.search.query.AndQuery;
import org.vortikal.repository.search.query.OrQuery;
import org.vortikal.repository.search.query.Query;
import org.vortikal.repository.search.query.UriPrefixQuery;
import org.vortikal.repository.search.query.UriSetQuery;
import org.vortikal.web.RequestContext;
import org.vortikal.web.display.collection.aggregation.AggregationResolver;
import org.vortikal.web.display.collection.aggregation.CollectionListingAggregatedResources;
import org.vortikal.web.search.ListingUriQueryBuilder;
import org.vortikal.web.search.QueryPartsSearchComponent;
import org.vortikal.web.search.SearchComponentQueryBuilder;
import org.vortikal.web.search.VHostScopeQueryRestricter;
import org.vortikal.web.service.URL;

public class CollectionListingSearchComponent extends QueryPartsSearchComponent {

    private static Log logger = LogFactory.getLog(CollectionListingSearchComponent.class.getName());

    private AggregationResolver aggregationResolver;
    private MultiHostSearcher multiHostSearcher;
    private ListingUriQueryBuilder listingUriQueryBuilder;
    private Ehcache cache;

    @Override
    protected ResultSet getResultSet(HttpServletRequest request, Resource collection, String token, Sorting sorting,
            int searchLimit, int offset, ConfigurablePropertySelect propertySelect) {

        // Check cache for aggregation set containing ref to other hosts
        CollectionListingCacheKey cacheKey = getCacheKey(request, collection, token);
        Element cached = cache.get(cacheKey);
        Object cachedObj = cached != null ? cached.getObjectValue() : null;

        URL localHostBaseURL = viewService.constructURL(Path.ROOT);

        boolean isMultiHostSearch = false;
        CollectionListingAggregatedResources clar = null;
        if (cachedObj != null) {
            clar = (CollectionListingAggregatedResources) cachedObj;
            isMultiHostSearch = true;
        } else {
            clar = aggregationResolver.getAggregatedResources(collection);
            isMultiHostSearch = multiHostSearcher.isMultiHostSearchEnabled()
                    && (clar != null && clar.includesResourcesFromOtherHosts(localHostBaseURL));
        }

        ResultSet result = null;

        Query query = generateQuery(request, collection, clar, localHostBaseURL, isMultiHostSearch);

        Search search = new Search();
        search.setQuery(query);
        search.setLimit(searchLimit);
        search.setCursor(offset);
        search.setSorting(sorting);
        if (propertySelect != null) {
            search.setPropertySelect(propertySelect);
        }

        boolean successfulMultiHostSearch = false;
        if (isMultiHostSearch) {

            // Keep aggregation set in cache
            cache.put(new Element(cacheKey, clar));

            try {
                result = multiHostSearcher.search(token, search);
                if (result != null) {
                    successfulMultiHostSearch = true;
                }
            } catch (Exception e) {
                logger.error("An error occured while searching multiple hosts: " + e.getMessage());
            }
        }

        if (!successfulMultiHostSearch) {

            Repository repository = RequestContext.getRequestContext().getRepository();
            result = repository.search(token, search);

        }

        return result;
    }

    private Query generateQuery(HttpServletRequest request, Resource collection,
            CollectionListingAggregatedResources clar, URL localHostBaseURL, boolean isMultiHostSearch) {

        AndQuery and = new AndQuery();

        Query uriQuery = listingUriQueryBuilder.build(collection);
        if (isMultiHostSearch) {
            uriQuery = VHostScopeQueryRestricter.vhostRestrictedQuery(uriQuery, localHostBaseURL);
        }

        Query aggregationQuery = clar.getAggregationQuery(localHostBaseURL, isMultiHostSearch);
        OrQuery uriOr = null;
        if (aggregationQuery == null) {
            and.add(uriQuery);
        } else {
            uriOr = new OrQuery();
            uriOr.add(uriQuery);
            uriOr.add(aggregationQuery);
        }

        List<Query> additionalQueries = getAdditionalQueries(collection, request);
        if (additionalQueries != null) {
            for (Query q : additionalQueries) {
                if (isMultiHostSearch && (q instanceof UriPrefixQuery || q instanceof UriSetQuery)) {
                    q = VHostScopeQueryRestricter.vhostRestrictedQuery(q, localHostBaseURL);
                    if (uriOr == null) {
                        uriOr = new OrQuery();
                    }
                    uriOr.add(q);
                } else {
                    and.add(q);
                }
            }
        }

        if (uriOr != null) {
            and.add(uriOr);
        }

        return and;

    }

    private CollectionListingCacheKey getCacheKey(HttpServletRequest request, Resource collection, String token) {
        String lastModified = collection.getPropertiesLastModified().toString();
        String url = request.getRequestURL().toString();
        CollectionListingCacheKey cacheKey = new CollectionListingCacheKey(lastModified, token, getName(), url);
        return cacheKey;
    }

    private List<Query> getAdditionalQueries(Resource collection, HttpServletRequest request) {
        if (queryBuilders != null) {
            List<Query> result = new ArrayList<Query>();
            for (SearchComponentQueryBuilder queryBuilder : queryBuilders) {
                Query q = queryBuilder.build(collection, request);
                if (q != null) {
                    result.add(q);
                }
            }
            return result;
        }
        return null;
    }

    @Required
    public void setAggregationResolver(AggregationResolver aggregationResolver) {
        this.aggregationResolver = aggregationResolver;
    }

    @Required
    public void setMultiHostSearcher(MultiHostSearcher multiHostSearcher) {
        this.multiHostSearcher = multiHostSearcher;
    }

    @Required
    public void setListingUriQueryBuilder(ListingUriQueryBuilder listingUriQueryBuilder) {
        this.listingUriQueryBuilder = listingUriQueryBuilder;
    }

    @Required
    public void setCache(Ehcache cache) {
        this.cache = cache;
    }

}
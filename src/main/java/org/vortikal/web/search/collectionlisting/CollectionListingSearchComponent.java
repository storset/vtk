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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.MultiHostSearcher;
import org.vortikal.repository.Path;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
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
import org.vortikal.web.search.QueryBuilder;
import org.vortikal.web.search.QueryPartsSearchComponent;
import org.vortikal.web.service.URL;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

public class CollectionListingSearchComponent extends QueryPartsSearchComponent {

    private static Log logger = LogFactory.getLog(CollectionListingSearchComponent.class.getName());

    private AggregationResolver aggregationResolver;
    private MultiHostSearcher multiHostSearcher;
    private ListingUriQueryBuilder listingUriQueryBuilder;
    private Ehcache cache;

    @Override
    protected ResultSet getResultSet(HttpServletRequest request, Resource collection, String token, Sorting sorting,
            int searchLimit, int offset) {

        // Check cache for aggregation set containing ref to other hosts
        CollectionListingCacheKey cacheKey = this.getCacheKey(request, collection, token);
        Element cached = this.cache.get(cacheKey);
        Object cachedObj = cached != null ? cached.getObjectValue() : null;

        boolean isMultiHostSearch = false;
        CollectionListingAggregatedResources clar = null;
        if (cachedObj != null) {
            clar = (CollectionListingAggregatedResources) cachedObj;
            isMultiHostSearch = true;
        } else {
            clar = this.aggregationResolver.getAggregatedResources(collection);
            isMultiHostSearch = this.isMultiHostSearch(clar);
        }

        ResultSet result = null;

        Query uriQuery = this.listingUriQueryBuilder.build(collection);
        List<Query> additionalQueries = this.getAdditionalQueries(collection, request);

        boolean successfulMultiHostSearch = false;
        if (isMultiHostSearch) {

            // Keep aggregation set in cache
            cache.put(new Element(cacheKey, clar));

            CollectionListingConditions clc = new CollectionListingConditions(token, uriQuery, additionalQueries, clar,
                    searchLimit, offset, sorting, null);
            try {
                result = this.multiHostSearcher.collectionListing(clc);
                if (result != null) {
                    successfulMultiHostSearch = true;
                }
            } catch (Exception e) {
                logger.error("An error occured while searching multiple hosts: " + e.getMessage());
            }
        }

        if (!successfulMultiHostSearch) {

            Query query = generateLocalQuery(uriQuery, additionalQueries, clar);

            Search search = new Search();
            search.setQuery(query);
            search.setLimit(searchLimit);
            search.setCursor(offset);
            search.setSorting(sorting);

            Repository repository = RequestContext.getRequestContext().getRepository();
            result = repository.search(token, search);

        }

        return result;
    }

    private CollectionListingCacheKey getCacheKey(HttpServletRequest request, Resource collection, String token) {
        String lastModified = collection.getPropertiesLastModified().toString();
        String url = request.getRequestURL().toString();
        CollectionListingCacheKey cacheKey = new CollectionListingCacheKey(lastModified, token, this.getName(), url);
        return cacheKey;
    }

    private List<Query> getAdditionalQueries(Resource collection, HttpServletRequest request) {
        if (this.queryBuilders != null) {
            List<Query> result = new ArrayList<Query>();
            for (QueryBuilder queryBuilder : this.queryBuilders) {
                Query q = queryBuilder.build(collection, request);
                if (q != null) {
                    result.add(q);
                }
            }
            return result;
        }
        return null;
    }

    private boolean isMultiHostSearch(CollectionListingAggregatedResources clar) {

        Map<URL, Set<Path>> aggregationSet = clar.getAggregationSet();
        Set<URL> manuallyApprovedSet = clar.getManuallyApproved();

        if (!this.multiHostSearcher.isMultiHosSearchEnabled()) {
            return false;
        }
        if ((aggregationSet == null || aggregationSet.isEmpty())
                && (manuallyApprovedSet == null || manuallyApprovedSet.isEmpty())) {
            return false;
        }
        URL currentHost = RequestContext.getRequestContext().getRequestURL().relativeURL("/");
        for (URL url : aggregationSet.keySet()) {
            if (!url.equals(currentHost)) {
                return true;
            }
        }
        for (URL url : manuallyApprovedSet) {
            url = url.relativeURL("/");
            if (!url.equals(currentHost)) {
                return true;
            }
        }
        return false;
    }

    public static Query generateLocalQuery(Query uriQuery, List<Query> additionalQueries,
            CollectionListingAggregatedResources clar) {

        URL localHost = RequestContext.getRequestContext().getRequestURL().relativeURL("/");

        Set<Path> aggregationSet = null;
        Set<Path> manuallyApprovedSet = null;
        if (clar != null) {
            aggregationSet = clar.getHostAggregationSet(localHost);
            manuallyApprovedSet = clar.getHostManuallyApprovedSet(localHost);
        }

        AndQuery and = new AndQuery();
        if (aggregationSet == null && manuallyApprovedSet == null) {
            and.add(uriQuery);
        } else {
            OrQuery or = new OrQuery();
            or.add(uriQuery);
            if (aggregationSet != null) {
                for (Path aggregationPath : aggregationSet) {
                    or.add(new UriPrefixQuery(aggregationPath.toString()));
                }
            }
            if (manuallyApprovedSet != null) {
                Set<String> uriSet = new HashSet<String>();
                for (Path p : manuallyApprovedSet) {
                    uriSet.add(p.toString());
                }
                or.add(new UriSetQuery(uriSet));
            }
            and.add(or);
        }
        if (additionalQueries != null) {
            for (Query q : additionalQueries) {
                and.add(q);
            }
        }
        return and;
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
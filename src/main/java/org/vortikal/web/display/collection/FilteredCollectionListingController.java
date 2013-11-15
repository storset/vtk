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
package org.vortikal.web.display.collection;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.servlet.ModelAndView;
import org.vortikal.repository.MultiHostSearcher;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Path;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.ResourceTypeTree;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.search.ConfigurablePropertySelect;
import org.vortikal.repository.search.ResultSet;
import org.vortikal.repository.search.Search;
import org.vortikal.repository.search.Sorting;
import org.vortikal.repository.search.SortingImpl;
import org.vortikal.repository.search.query.AndQuery;
import org.vortikal.repository.search.query.OrQuery;
import org.vortikal.repository.search.query.Query;
import org.vortikal.repository.search.query.UriPrefixQuery;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;
import org.vortikal.web.display.collection.aggregation.AggregationResolver;
import org.vortikal.web.display.collection.aggregation.CollectionListingAggregatedResources;
import org.vortikal.web.display.listing.ListingPager;
import org.vortikal.web.search.SearchSorting;
import org.vortikal.web.service.Service;
import org.vortikal.web.service.URL;

public abstract class FilteredCollectionListingController implements ListingController {

    private static final String filterNamespace = "filter.";

    private String viewName;
    private Map<String, List<String>> filters;
    private int pageLimit = 25;
    protected ResourceTypeTree resourceTypeTree;

    // XXX NO. These shoudn't be here. SearchComponent, more specifically
    // CollectionListingSearchComponent -> Reuse!!!
    protected MultiHostSearcher multiHostSearcher;
    protected AggregationResolver aggregationResolver;
    private SearchSorting defaultSearchSorting;
    private List<String> configurablePropertySelectPointers;
    protected Service viewService;
    private List<String> filterWhitelistExceptions;

    /**
     * Sets up, prepares and runs search. Puts results on model for view.
     */
    abstract public void runSearch(HttpServletRequest request, Resource collection, Map<String, Object> model,
            int pageLimit) throws Exception;

    /**
     * A filtered collection listing has one or more filter resolvers that set
     * configured filters on search.
     */
    protected Map<String, List<String>> explicitlySetFilters(Resource collection, Map<String, List<String>> filters) {
        // By default this does nothing. Can be overridden to handle special
        // filters that cannot be created with beans.
        return filters;
    }

    /**
     * Run the actual search, handling sorting, offset, limit and potential
     * aggregation.
     */
    protected ResultSet search(Resource collection, Query query, int offset) {

        AndQuery and = query instanceof AndQuery ? (AndQuery) query : new AndQuery();
        Search search = new Search();

        if (RequestContext.getRequestContext().isPreviewUnpublished()) {
            search.removeFilterFlag(Search.FilterFlag.UNPUBLISHED_COLLECTIONS);
        }

        UriPrefixQuery uriQuery = new UriPrefixQuery(collection.getURI().toString(), false);

        // Initially no multi host search
        boolean isMultiHostSearch = false;

        // Initially no aggregation
        OrQuery aggregationQuery = null;

        // Resolve aggregation
        CollectionListingAggregatedResources aggregateResources = aggregationResolver
                .getAggregatedResources(collection);

        // Check if any reference to resources from another host
        if (aggregateResources != null) {
            URL localHostBaseURL = viewService.constructURL(Path.ROOT);

            isMultiHostSearch = multiHostSearcher.isMultiHostSearchEnabled()
                    && aggregateResources.includesResourcesFromOtherHosts(localHostBaseURL);

            Query resolvedAggregationquery = aggregateResources
                    .getAggregationQuery(localHostBaseURL, isMultiHostSearch);
            if (resolvedAggregationquery != null) {
                aggregationQuery = new OrQuery();
                aggregationQuery.add(uriQuery);
                aggregationQuery.add(resolvedAggregationquery);
            }

        }

        if (aggregationQuery != null) {
            and.add(aggregationQuery);
        } else {
            and.add(uriQuery);
        }
        search.setQuery(and);
        search.setSorting(getDefaultSearchSorting(collection));

        ConfigurablePropertySelect propertySelect = getPropertySelect();
        if (propertySelect != null) {
            search.setPropertySelect(propertySelect);
        }

        search.setCursor(offset);
        search.setLimit(pageLimit);

        ResultSet rs = null;
        String token = SecurityContext.getSecurityContext().getToken();
        // Most common case
        if (!isMultiHostSearch) {
            rs = RequestContext.getRequestContext().getRepository().search(token, search);
        } else {
            rs = multiHostSearcher.search(token, search);
        }
        return rs;
    }

    @Override
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Map<String, Object> model = new HashMap<String, Object>();

        Repository repository = RequestContext.getRequestContext().getRepository();
        String token = RequestContext.getRequestContext().getSecurityToken();
        Resource collection = repository.retrieve(token, URL.toPath(request), false);

        Map<String, List<String>> currentFilters = new LinkedHashMap<String, List<String>>(filters);
        currentFilters = explicitlySetFilters(collection, currentFilters);

        model.put("collection", collection);

        if (currentFilters != null) {
            Map<String, Map<String, FilterURL>> urlFilters = new LinkedHashMap<String, Map<String, FilterURL>>();
            Map<String, FilterURL> urlList;
            FilterURL filterUrl;

            String parameterKey;
            for (String filter : currentFilters.keySet()) {
                List<String> parameterValues = currentFilters.get(filter);
                urlList = new LinkedHashMap<String, FilterURL>();
                parameterKey = filterNamespace + filter;

                URL url = ListingPager.removePagerParms(URL.create(request));
                filterUrl = new FilterURL(!request.getParameterMap().containsKey(parameterKey),
                        url.removeParameter(parameterKey));
                urlList.put("all", filterUrl);

                for (String parameter : parameterValues) {
                    url = ListingPager.removePagerParms(URL.create(request));
                    List<String> oldParameterValues = url.getParameters(parameterKey);
                    if (oldParameterValues != null && oldParameterValues.contains(parameter)) {
                        url.removeParameter(parameterKey);

                        for (String value : oldParameterValues) {
                            if (!value.equals(parameter)) {
                                url.addParameter(parameterKey, value);
                            }
                        }

                        filterUrl = new FilterURL(true, url);
                    } else {
                        url.addParameter(parameterKey, parameter);

                        filterUrl = new FilterURL(false, url);
                    }
                    urlList.put(parameter, filterUrl);
                }

                urlFilters.put(filter, urlList);
            }

            model.put("filters", urlFilters);
        }

        runSearch(request, collection, model, pageLimit);

        return new ModelAndView(viewName, model);
    }

    public class FilterURL {
        private boolean marked;
        private URL url;

        public FilterURL(boolean marked, URL url) {
            this.marked = marked;
            this.url = url;
        }

        public boolean getMarked() {
            return marked;
        }

        public URL getUrl() {
            return url;
        }
    }

    protected boolean valueExistsInFilters(Resource collection, String parameterKey, String parameterValue)
            throws Exception {
        Map<String, List<String>> currentFilters = new LinkedHashMap<String, List<String>>(filters);
        currentFilters = explicitlySetFilters(collection, currentFilters);

        if (parameterKey != null && parameterKey.startsWith(filterNamespace)) {
            parameterKey = parameterKey.substring(filterNamespace.length());
        }

        if (filterWhitelistExceptions != null && filterWhitelistExceptions.contains(parameterKey)) {
            return true;
        }

        List<String> filter = currentFilters.get(parameterKey);
        if (filter != null) {
            return filter.contains(parameterValue);
        }

        return false;
    }

    protected PropertyTypeDefinition getPropDef(String propertyName) {
        if (propertyName != null && propertyName.startsWith(filterNamespace)) {
            propertyName = propertyName.substring(filterNamespace.length());
        }

        Namespace ns = Namespace.STRUCTURED_RESOURCE_NAMESPACE;
        return resourceTypeTree.getPropertyTypeDefinition(ns, propertyName);
    }

    protected ConfigurablePropertySelect getPropertySelect() {
        ConfigurablePropertySelect propertySelect = null;
        if (configurablePropertySelectPointers != null && resourceTypeTree != null) {
            for (String propPointer : configurablePropertySelectPointers) {
                PropertyTypeDefinition ptd = resourceTypeTree.getPropertyDefinitionByPointer(propPointer);
                if (ptd != null) {
                    if (propertySelect == null) {
                        propertySelect = new ConfigurablePropertySelect();
                    }
                    propertySelect.addPropertyDefinition(ptd);
                }
            }
        }
        return propertySelect;
    }

    protected Sorting getDefaultSearchSorting(Resource collection) {
        return new SortingImpl(defaultSearchSorting.getSortFields(collection));
    }

    @Required
    public void setViewName(String viewName) {
        this.viewName = viewName;
    }

    public void setFilters(Map<String, List<String>> filters) {
        this.filters = filters;
    }

    public void setPageLimit(int pageLimit) {
        if (pageLimit <= 0)
            throw new IllegalArgumentException("Limit must be a positive integer");
        this.pageLimit = pageLimit;
    }

    protected int getPageLimit() {
        return pageLimit;
    }

    @Required
    public void setResourceTypeTree(ResourceTypeTree resourceTypeTree) {
        this.resourceTypeTree = resourceTypeTree;
    }

    @Required
    public void setMultiHostSearcher(MultiHostSearcher multiHostSearcher) {
        this.multiHostSearcher = multiHostSearcher;
    }

    @Required
    public void setAggregationResolver(AggregationResolver aggregationResolver) {
        this.aggregationResolver = aggregationResolver;
    }

    @Required
    public void setDefaultSearchSorting(SearchSorting defaultSearchSorting) {
        this.defaultSearchSorting = defaultSearchSorting;
    }

    @Required
    public void setViewService(Service viewService) {
        this.viewService = viewService;
    }

    public void setConfigurablePropertySelectPointers(List<String> configurablePropertySelectPointers) {
        this.configurablePropertySelectPointers = configurablePropertySelectPointers;
    }

    public void setFilterWhitelistExceptions(List<String> filterWhitelistExceptions) {
        this.filterWhitelistExceptions = filterWhitelistExceptions;
    }

}

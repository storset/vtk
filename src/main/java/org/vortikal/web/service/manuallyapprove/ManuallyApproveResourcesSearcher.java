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
package org.vortikal.web.service.manuallyapprove;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.MultiHostSearcher;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.ResourceNotFoundException;
import org.vortikal.repository.ResourceTypeTree;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.search.ConfigurablePropertySelect;
import org.vortikal.repository.search.PropertySortField;
import org.vortikal.repository.search.ResultSet;
import org.vortikal.repository.search.Search;
import org.vortikal.repository.search.SortFieldDirection;
import org.vortikal.repository.search.SortingImpl;
import org.vortikal.repository.search.query.AndQuery;
import org.vortikal.repository.search.query.OrQuery;
import org.vortikal.repository.search.query.Query;
import org.vortikal.repository.search.query.TermOperator;
import org.vortikal.repository.search.query.TypeTermQuery;
import org.vortikal.repository.search.query.UriPrefixQuery;
import org.vortikal.repository.search.query.UriSetQuery;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;
import org.vortikal.web.display.collection.aggregation.AggregationResolver;
import org.vortikal.web.display.collection.aggregation.CollectionListingAggregatedResources;
import org.vortikal.web.search.VHostScopeQueryRestricter;
import org.vortikal.web.service.Service;
import org.vortikal.web.service.URL;

public class ManuallyApproveResourcesSearcher {

    public static final int SEARCH_LIMIT = 1000;

    private Service viewService;
    private AggregationResolver aggregationResolver;
    private MultiHostSearcher multiHostSearcher;
    private Map<String, String> listingResourceTypeMappingPointers;
    private List<String> configurablePropertySelectPointers;
    private ResourceTypeTree resourceTypeTree;

    private PropertyTypeDefinition titlePropDef;
    private PropertyTypeDefinition publishDatePropDef;
    private PropertyTypeDefinition creationTimePropDef;

    public List<ManuallyApproveResource> getManuallyApproveResources(Resource collection, Set<String> locations,
            Set<String> alreadyApproved) throws Exception {

        // The final product. Will be populated with search results.
        List<ManuallyApproveResource> result = new ArrayList<ManuallyApproveResource>();

        Repository repository = RequestContext.getRequestContext().getRepository();
        String token = SecurityContext.getSecurityContext().getToken();
        URL localHostURL = viewService.constructURL(Path.ROOT);

        ConfigurablePropertySelect propertySelect = null;
        if (this.configurablePropertySelectPointers != null && this.resourceTypeTree != null) {
            for (String propPointer : this.configurablePropertySelectPointers) {
                PropertyTypeDefinition ptd = this.resourceTypeTree.getPropertyDefinitionByPointer(propPointer);
                if (ptd != null) {
                    if (propertySelect == null) {
                        propertySelect = new ConfigurablePropertySelect();
                    }
                    propertySelect.addPropertyDefinition(ptd);
                }
            }
        }

        // Sort on publish date
        SortingImpl sorting = new SortingImpl();
        sorting.addSortField(new PropertySortField(this.publishDatePropDef, SortFieldDirection.DESC));

        String resourceTypePointer = this.listingResourceTypeMappingPointers.get(collection.getResourceType());
        Query resourceTypeQuery = new TypeTermQuery("file", TermOperator.IN);
        if (resourceTypePointer != null) {
            resourceTypePointer = resourceTypePointer.startsWith("structured.") ? resourceTypePointer.replace(".", "-")
                    : resourceTypePointer;
            resourceTypeQuery = new TypeTermQuery(resourceTypePointer, TermOperator.IN);
        }

        // Get all resources that are eligible for manual approval, all
        // separated on origin (location)
        Map<String, List<PropertySet>> resourceSet = new HashMap<String, List<PropertySet>>();

        // Get resources to manually approve
        for (String location : locations) {

            URL locationURL = getLocaltionAsURL(location, localHostURL);
            PropertySet resource = getResource(repository, token, locationURL, localHostURL);
            if (resource == null) {
                // Nothing found
                continue;
            }

            CollectionListingAggregatedResources clar = aggregationResolver.getAggregatedResources(resource);

            boolean isOtherHostLocation = isOtherHostLocation(location, localHostURL);
            boolean isMultiHostSearch = multiHostSearcher.isMultiHostSearchEnabled()
                    && ((clar != null && clar.includesResourcesFromOtherHosts(localHostURL)) || isOtherHostLocation);

            Query query = generateQuery(locationURL, resourceTypeQuery, clar, localHostURL, isMultiHostSearch);

            Search search = new Search();
            search.setPreviewUnpublished(RequestContext.getRequestContext().isPreviewUnpublished());
            search.setQuery(query);
            search.setLimit(SEARCH_LIMIT);
            search.setSorting(sorting);
            if (propertySelect != null) {
                search.setPropertySelect(propertySelect);
            }

            ResultSet searchResults = null;
            if (isMultiHostSearch) {
                searchResults = multiHostSearcher.search(token, search);
            } else {
                searchResults = repository.search(token, search);
            }

            resourceSet.put(location, searchResults.getAllResults());

        }

        // Map search results to objects for view
        for (Entry<String, List<PropertySet>> entry : resourceSet.entrySet()) {
            String source = entry.getKey();
            List<PropertySet> resources = entry.getValue();
            for (PropertySet ps : resources) {
                URL url = getPropertySetURL(ps, localHostURL);
                boolean approved = alreadyApproved.contains(url.toString());
                ManuallyApproveResource m = mapPropertySetToManuallyApprovedResource(ps, localHostURL, source, approved);
                result.add(m);
            }
        }

        // Get any already approved resource where the source might be gone
        // (e.g. removed)
        Set<PropertySet> alreadyApprovedMissingSource = getAlreadyApprovedMissingSource(alreadyApproved, result,
                repository, token, localHostURL);
        for (PropertySet ps : alreadyApprovedMissingSource) {
            URL url = getPropertySetURL(ps, localHostURL);
            String source = url.relativeURL(url.getPath().getParent().toString()).toString();
            ManuallyApproveResource m = mapPropertySetToManuallyApprovedResource(ps, localHostURL, source, true);
            result.add(m);
        }

        // Sort and return
        Collections.sort(result, new ManuallyApproveResourceComparator());
        // Handle limit
        if (result.size() > SEARCH_LIMIT) {
            result = result.subList(0, SEARCH_LIMIT);
        }
        return result;
    }

    private URL getLocaltionAsURL(String location, URL localHostURL) {
        URL url = getAsURL(location);
        if (url == null) {
            try {
                Path localPath = Path.fromStringWithTrailingSlash(location);
                url = URL.parse(localHostURL.toString());
                url.setPath(localPath);
            } catch (IllegalArgumentException iae) {
                return null;
            }
        }
        return url;
    }

    private Query generateQuery(URL locationURL, Query resourceTypeQuery, CollectionListingAggregatedResources clar,
            URL localHostBaseURL, boolean isMultiHostSearch) {

        AndQuery and = new AndQuery();
        and.add(resourceTypeQuery);

        Query uriQuery = new UriPrefixQuery(locationURL.getPath().toString());
        if (isMultiHostSearch) {
            uriQuery = VHostScopeQueryRestricter.vhostRestrictedQuery(uriQuery, locationURL);
        }

        Query aggregationQuery = clar.getAggregationQuery(localHostBaseURL, isMultiHostSearch);
        if (aggregationQuery == null) {
            and.add(uriQuery);
        } else {
            OrQuery uriOr = new OrQuery();
            uriOr.add(uriQuery);
            uriOr.add(aggregationQuery);
            and.add(uriOr);

        }

        return and;

    }

    private PropertySet getResource(Repository repository, String token, URL url, URL localHostURL) {
        try {

            PropertySet resource = null;
            Path path = null;
            if (localHostURL.getHost().equals(url.getHost())) {
                path = url.getPath();
            }
            if (path != null) {
                resource = repository.retrieve(token, path, false);
            } else if (this.multiHostSearcher.isMultiHostSearchEnabled()) {
                resource = multiHostSearcher.retrieve(token, url);
            }

            if (resource == null) {
                return null;
            }
            return resource;
        } catch (ResourceNotFoundException rnfe) {
            // resource doesn'n exist, ignore
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }

    private Set<PropertySet> getAlreadyApprovedMissingSource(Set<String> alreadyApproved,
            List<ManuallyApproveResource> result, Repository repository, String token, URL localHostURL) {

        Set<String> missingAlreadyApproved = new HashSet<String>();
        for (String s : alreadyApproved) {
            URL url = getAsURL(s);
            boolean found = false;
            for (ManuallyApproveResource m : result) {
                if (m.getUrl().equals(url)) {
                    found = true;
                }
            }
            if (!found) {
                missingAlreadyApproved.add(s);
            }
        }

        Set<PropertySet> alreadyApprovedResources = new HashSet<PropertySet>();
        if (missingAlreadyApproved.size() > 0) {

            Set<String> localPathsAsStringSet = new HashSet<String>();
            Set<URL> urls = new HashSet<URL>();

            for (String approved : missingAlreadyApproved) {

                Path localPath = getLocalPath(approved, localHostURL);
                if (localPath != null) {
                    localPathsAsStringSet.add(localPath.toString());
                } else {
                    URL url = getAsURL(approved);
                    if (url != null) {
                        urls.add(url);
                    }
                }

            }

            if (!localPathsAsStringSet.isEmpty()) {
                UriSetQuery uriSetQuery = new UriSetQuery(localPathsAsStringSet, TermOperator.IN);
                Search search = new Search();
                search.setPreviewUnpublished(RequestContext.getRequestContext().isPreviewUnpublished());
                search.setQuery(uriSetQuery);
                ResultSet rs = repository.search(token, search);
                alreadyApprovedResources.addAll(rs.getAllResults());
            }

            if (!urls.isEmpty() && multiHostSearcher.isMultiHostSearchEnabled()) {

                Set<PropertySet> rs = multiHostSearcher.retrieve(token, urls);
                if (rs != null && rs.size() > 0) {
                    alreadyApprovedResources.addAll(rs);
                }
            }

        }
        return alreadyApprovedResources;
    }

    private boolean isOtherHostLocation(String location, URL localHostURL) {
        URL url = getAsURL(location);
        return url == null || url.getHost().equals(localHostURL.getHost()) ? false : true;
    }

    private Path getLocalPath(String location, URL localHostURL) {

        try {
            URL url = URL.parse(location);
            if (url.getHost().equals(localHostURL.getHost())) {
                // Is an url ref to a resource on local host
                return url.getPath();
            } else {
                // Is an url to resource on some other host
                return null;
            }
        } catch (Exception e) {
            // Not an url, continue and assume a path
        }

        try {
            return Path.fromStringWithTrailingSlash(location);
        } catch (IllegalArgumentException iae) {
            return null;
        }
    }

    private URL getAsURL(String location) {
        try {
            return URL.parse(location);
        } catch (Exception e) {
            return null;
        }
    }

    private ManuallyApproveResource mapPropertySetToManuallyApprovedResource(PropertySet ps, URL localURL,
            String source, boolean approved) {
        String title = ps.getName();
        Property titleProp = ps.getProperty(this.titlePropDef);
        if (titleProp != null) {
            title = titleProp.getStringValue();
        }
        URL url = this.getPropertySetURL(ps, localURL);
        Property dateProp = ps.getProperty(this.publishDatePropDef);
        if (dateProp == null) {
            dateProp = ps.getProperty(this.creationTimePropDef);
        }
        Date publishDate = dateProp != null ? dateProp.getDateValue() : Calendar.getInstance().getTime();
        ManuallyApproveResource m = new ManuallyApproveResource(title, url, source, publishDate, approved);
        return m;
    }

    private URL getPropertySetURL(PropertySet ps, URL localURL) {
        URL url = null;
        Property urlProp = ps.getProperty(Namespace.DEFAULT_NAMESPACE, MultiHostSearcher.URL_PROP_NAME);
        if (urlProp != null) {
            url = URL.parse(urlProp.getStringValue());
        } else {
            url = new URL(localURL).relativeURL("/");
            url.setPath(ps.getURI());
        }
        return url;
    }

    @Required
    public void setListingResourceTypeMappingPointers(Map<String, String> listingResourceTypeMappingPointers) {
        this.listingResourceTypeMappingPointers = listingResourceTypeMappingPointers;
    }

    @Required
    public void setTitlePropDef(PropertyTypeDefinition titlePropDef) {
        this.titlePropDef = titlePropDef;
    }

    @Required
    public void setPublishDatePropDef(PropertyTypeDefinition publishDatePropDef) {
        this.publishDatePropDef = publishDatePropDef;
    }

    @Required
    public void setCreationTimePropDef(PropertyTypeDefinition creationTimePropDef) {
        this.creationTimePropDef = creationTimePropDef;
    }

    @Required
    public void setMultiHostSearcher(MultiHostSearcher multiHostSearcher) {
        this.multiHostSearcher = multiHostSearcher;
    }

    @Required
    public void setViewService(Service viewService) {
        this.viewService = viewService;
    }

    @Required
    public void setAggregationResolver(AggregationResolver aggregationResolver) {
        this.aggregationResolver = aggregationResolver;
    }

    public void setConfigurablePropertySelectPointers(List<String> configurablePropertySelectPointers) {
        this.configurablePropertySelectPointers = configurablePropertySelectPointers;
    }

    public void setResourceTypeTree(ResourceTypeTree resourceTypeTree) {
        this.resourceTypeTree = resourceTypeTree;
    }

}
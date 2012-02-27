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
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.search.PropertySortField;
import org.vortikal.repository.search.ResultSet;
import org.vortikal.repository.search.Search;
import org.vortikal.repository.search.SortFieldDirection;
import org.vortikal.repository.search.SortingImpl;
import org.vortikal.repository.search.query.Query;
import org.vortikal.repository.search.query.TermOperator;
import org.vortikal.repository.search.query.TypeTermQuery;
import org.vortikal.repository.search.query.UriPrefixQuery;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;
import org.vortikal.web.display.collection.aggregation.AggregationResolver;
import org.vortikal.web.display.collection.aggregation.CollectionListingAggregatedResources;
import org.vortikal.web.search.collectionlisting.CollectionListingConditions;
import org.vortikal.web.search.collectionlisting.CollectionListingSearchComponent;
import org.vortikal.web.service.URL;

public class ManuallyApproveResourcesSearcher {

    private static final int RESOURCE_LIST_LIMIT = 1000;

    private Map<String, String> listingResourceTypeMappingPointers;
    private AggregationResolver aggregationResolver;
    private PropertyTypeDefinition titlePropDef;
    private PropertyTypeDefinition publishDatePropDef;
    private PropertyTypeDefinition creationTimePropDef;
    private MultiHostSearcher multiHostSearcher;

    public List<ManuallyApproveResource> getManuallyApproveResources(Resource collection, Set<String> locations,
            Set<String> alreadyApproved) throws Exception {

        // The final product. Will be populated with search results.
        List<ManuallyApproveResource> result = new ArrayList<ManuallyApproveResource>();

        Repository repository = RequestContext.getRequestContext().getRepository();
        String token = SecurityContext.getSecurityContext().getToken();

        // Resource type
        String resourceTypePointer = this.listingResourceTypeMappingPointers.get(collection.getResourceType());
        Query resourceTypeQuery = new TypeTermQuery("file", TermOperator.IN);
        if (resourceTypePointer != null) {
            resourceTypePointer = resourceTypePointer.startsWith("structured.") ? resourceTypePointer.replace(".", "-")
                    : resourceTypePointer;
            resourceTypeQuery = new TypeTermQuery(resourceTypePointer, TermOperator.IN);
        }
        List<Query> queries = new ArrayList<Query>();
        queries.add(resourceTypeQuery);

        // Sort on publish date
        SortingImpl sorting = new SortingImpl();
        sorting.addSortField(new PropertySortField(this.publishDatePropDef, SortFieldDirection.DESC));

        URL localURL = RequestContext.getRequestContext().getRequestURL().relativeURL("/");

        // Get the already approved resources.
        Set<PropertySet> alreadyApprovedResources = new HashSet<PropertySet>();
        if (alreadyApproved.size() > 0) {
            for (String approved : alreadyApproved) {

                PropertySet ps = null;

                Path localPath = this.getLocalPath(approved, localURL);
                if (localPath != null) {
                    ps = repository.retrieve(token, localPath, false);
                } else {
                    URL url = this.getAsURL(approved);
                    if (url != null) {
                        ps = this.multiHostSearcher.retrieve(token, url);
                    }
                }

                if (ps != null) {
                    alreadyApprovedResources.add(ps);
                }

            }
        }

        // We display a maximum of 1000 resources
        int searchLimit = RESOURCE_LIST_LIMIT - alreadyApprovedResources.size();

        // Get all resources that are eligible for manual approval, all
        // separated on origin (location)
        Map<String, List<PropertySet>> resourceSet = new HashMap<String, List<PropertySet>>();
        for (String location : locations) {

            // Is it a local resource ref?
            Path localPath = this.getLocalPath(location, localURL);

            CollectionListingAggregatedResources clar = null;
            URL locationURL = this.getAsURL(location);
            if (localPath != null) {
                Resource resource = repository.retrieve(token, localPath, false);
                clar = this.aggregationResolver.getAggregatedResources(resource);
            } else if (this.multiHostSearcher.isMultiHosSearchEnabled() && locationURL != null) {
                clar = this.aggregationResolver.getAggregatedResources(locationURL);
            }

            ResultSet rs = null;

            if (this.multiHostSearcher.isMultiHosSearchEnabled() && clar != null
                    && clar.includesResourcesFromOtherHosts(localURL)) {

                // Resolved aggregation indicates resources from other hosts,
                // and we have proper configuration to meet demands -> search
                // accordingly

                CollectionListingConditions clc = new CollectionListingConditions(token, null, queries, clar,
                        searchLimit, 0, sorting, locationURL);
                rs = this.multiHostSearcher.collectionListing(clc);

                if (rs != null && rs.getSize() > 0) {
                    resourceSet.put(location, rs.getAllResults());
                }

            } else if (localPath != null) {

                // We either don't need any multihost search, or we lack the
                // configuration to do so -> keep it local

                Query uriQuery = new UriPrefixQuery(localPath.toString());
                List<Query> additionalQueries = new ArrayList<Query>();
                additionalQueries.add(resourceTypeQuery);
                Query query = CollectionListingSearchComponent.generateLocalQuery(uriQuery, additionalQueries, clar);

                Search search = new Search();
                search.setSorting(sorting);
                search.setLimit(searchLimit);
                search.setQuery(query);

                rs = repository.search(token, search);

                if (rs != null && rs.getSize() > 0) {
                    resourceSet.put(location, rs.getAllResults());
                }

            }

        }

        // Map search results to objects for view
        for (Entry<String, List<PropertySet>> entry : resourceSet.entrySet()) {
            String source = entry.getKey();
            List<PropertySet> resources = entry.getValue();
            for (PropertySet ps : resources) {
                ManuallyApproveResource m = this.mapPropertySetToManuallyApprovedResource(ps, localURL, source, false);
                result.add(m);
            }
        }

        // Mark as approved all already approved resources, also those where
        // the source might have been removed
        for (PropertySet ps : alreadyApprovedResources) {
            URL url = this.getPropertySetURL(ps, localURL);
            String source = url.relativeURL(url.getPath().getParent().toString()).toString();
            ManuallyApproveResource mx = this.getResource(result, url);
            if (mx != null) {
                mx.setApproved(true);
            } else {
                ManuallyApproveResource m = this.mapPropertySetToManuallyApprovedResource(ps, localURL, source, true);
                result.add(m);
            }
        }

        // Sort and return
        Collections.sort(result, new ManuallyApproveResourceComparator());
        // Handle limit
        if (result.size() > RESOURCE_LIST_LIMIT) {
            result = result.subList(0, RESOURCE_LIST_LIMIT);
        }
        return result;
    }

    private Path getLocalPath(String location, URL localURL) {

        try {
            URL url = URL.parse(location);
            if (url.relativeURL("/").equals(localURL)) {
                // Is a url ref to a resource on local host
                return url.getPath();
            }
        } catch (Exception e) {
            // Not a url, continue and assume a path
        }

        try {
            return Path.fromString(location);
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

    private ManuallyApproveResource getResource(List<ManuallyApproveResource> result, URL url) {
        for (ManuallyApproveResource m : result) {
            if (m.getUrl().equals(url)) {
                return m;
            }
        }
        return null;
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
    public void setAggregationResolver(AggregationResolver aggregationResolver) {
        this.aggregationResolver = aggregationResolver;
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

}

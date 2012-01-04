/* Copyright (c) 2010, University of Oslo, Norway
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.Value;
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
import org.vortikal.web.search.MultiHostSearch;
import org.vortikal.web.search.MultiHostSearchComponent;
import org.vortikal.web.search.MultiHostSearchImpl;
import org.vortikal.web.service.URL;

public class ManuallyApproveResourcesSearcher {

    private static final int RESOURCE_LIST_LIMIT = 1000;

    private static final Pattern PROTOCOL_PATTERN = Pattern.compile("https?://");

    private Map<String, String> listingResourceTypeMappingPointers;
    private AggregationResolver aggregationResolver;
    private PropertyTypeDefinition collectionPropDef;
    private PropertyTypeDefinition titlePropDef;
    private PropertyTypeDefinition publishDatePropDef;
    private PropertyTypeDefinition creationTimePropDef;
    private PropertyTypeDefinition manuallyApprovedResourcesPropDef;

    private MultiHostSearchComponent multiHostSearchComponent;
    private boolean searchMultiHosts;

    public List<ManuallyApproveResource> getManuallyApproveResources(Resource collection, Set<String> folders,
            Set<String> alreadyApproved) throws Exception {

        Repository repository = RequestContext.getRequestContext().getRepository();
        String repositoryId = repository.getId();
        String token = SecurityContext.getSecurityContext().getToken();

        String resourceTypePointer = this.listingResourceTypeMappingPointers.get(collection.getResourceType());
        Query resourceTypeQuery = null;
        if (resourceTypePointer != null) {
            resourceTypePointer = resourceTypePointer.startsWith("structured.") ? resourceTypePointer.replace(".", "-")
                    : resourceTypePointer;
            resourceTypeQuery = new TypeTermQuery(resourceTypePointer, TermOperator.IN);
        }

        List<ManuallyApproveResource> result = new ArrayList<ManuallyApproveResource>();
        List<PropertySet> alreadyApprovedResources = new ArrayList<PropertySet>();

        boolean performMultiHostSearch = this.searchMultiHosts(folders, alreadyApproved, repositoryId);
        if (performMultiHostSearch) {

            if (alreadyApproved.size() > 0) {
                for (String approved : alreadyApproved) {
                    PropertySet ps = this.multiHostSearchComponent.retrieve(token, approved);
                    if (ps != null) {
                        alreadyApprovedResources.add(ps);
                    }
                }
            }

            for (String uri : folders) {
                if (!uri.startsWith("http")) {
                    uri = "http://".concat(repositoryId).concat(uri);
                }
                MultiHostSearchImpl multiHostSearch = new MultiHostSearchImpl(token, uri, resourceTypePointer);
                ResultSet rs = this.multiHostSearchComponent.search(multiHostSearch);
                for (PropertySet ps : rs.getAllResults()) {
                    Property collectionProp = ps.getProperty(this.collectionPropDef);
                    if (!collectionProp.getBooleanValue()) {
                        boolean approved = alreadyApprovedResources.contains(ps);
                        ManuallyApproveResource m = this.mapPropertySetToManuallyApprovedResource(repositoryId, ps,
                                uri, approved);
                        result.add(m);
                    }
                }
                PropertySet ps = this.multiHostSearchComponent.retrieve(token, uri);
                if (ps != null) {
                    Set<String> rSet = this.getManuallyApprovedUris(ps, false);
                    if (rSet != null && rSet.size() > 0) {
                        for (String s : rSet) {
                            PropertySet r2 = null;
                            try {
                                r2 = this.multiHostSearchComponent.retrieve(token, s);
                            } catch (Exception e) {
                                // XXX log?
                                continue;
                            }
                            ManuallyApproveResource m = this.mapPropertySetToManuallyApprovedResource(repositoryId, r2,
                                    uri, false);
                            result.add(m);
                        }
                    }
                }
            }

        } else {

            Set<String> localFolders = this.getLocalPaths(folders, repositoryId);
            Set<String> localAlreadyApproved = this.getLocalPaths(alreadyApproved, repositoryId);

            // Get already approved resources on local host
            if (localAlreadyApproved.size() > 0) {
                Search search = this.getSearch(new UriSetQuery(localAlreadyApproved));
                ResultSet rs = repository.search(token, search);
                alreadyApprovedResources.addAll(rs.getAllResults());
            }

            // Get list of resources to manually approve from on local host
            if (localFolders.size() > 0) {
                for (String folder : localFolders) {
                    Query query = this.getUriPrefixesQuery(localFolders, folder);
                    if (resourceTypeQuery != null) {
                        AndQuery and = new AndQuery();
                        and.add(resourceTypeQuery);
                        and.add(query);
                        query = and;
                    }
                    ResultSet rs = repository.search(token, this.getSearch(query));
                    for (PropertySet ps : rs.getAllResults()) {
                        Property collectionProp = ps.getProperty(this.collectionPropDef);
                        if (!collectionProp.getBooleanValue()) {
                            boolean approved = alreadyApprovedResources.contains(ps);
                            ManuallyApproveResource m = this.mapPropertySetToManuallyApprovedResource(repositoryId, ps,
                                    folder, approved);
                            result.add(m);
                        }
                    }
                    Resource r = repository.retrieve(token, Path.fromString(folder), false);
                    Set<String> rSet = this.getManuallyApprovedUris(r, true);
                    if (rSet != null && rSet.size() > 0) {
                        for (String s : rSet) {
                            Resource r2 = null;
                            try {
                                r2 = repository.retrieve(token, Path.fromString(s), false);
                            } catch (Exception e) {
                                // XXX log?
                                continue;
                            }
                            ManuallyApproveResource m = this.mapPropertySetToManuallyApprovedResource(repositoryId, r2,
                                    folder, false);
                            result.add(m);
                        }
                    }
                }
            }

        }

        // Mark as approved all already approved resources, also those where
        // the source might have been removed
        for (PropertySet ps : alreadyApprovedResources) {
            String resourceUri = "http://".concat(repositoryId).concat(ps.getURI().toString());
            String source = ps.getURI().getParent().toString();
            if (searchMultiHosts) {
                Property urlProp = ps.getProperty(Namespace.DEFAULT_NAMESPACE, MultiHostSearch.SOLR_URL_PROP_NAME);
                if (urlProp != null) {
                    String uri = URL.parse(urlProp.getStringValue()).toString();
                    // Resource is from another host. Set complete source
                    if (!uri.contains(repositoryId)) {
                        resourceUri = uri;
                        source = uri.substring(0, uri.lastIndexOf("/"));
                    }
                }
            }
            ManuallyApproveResource m = this.mapPropertySetToManuallyApprovedResource(repositoryId, ps, source, true);
            ManuallyApproveResource mx = this.getResource(result, resourceUri);
            if (mx != null) {
                mx.setApproved(true);
            } else {
                result.add(m);
            }
        }

        // Handle limit
        if (result.size() > RESOURCE_LIST_LIMIT) {
            result = result.subList(0, RESOURCE_LIST_LIMIT);
        }
        // Sort and return
        Collections.sort(result, new ManuallyApproveResourceComparator());
        return result;
    }

    private boolean searchMultiHosts(Set<String> folders, Set<String> alreadyApproved, String repositoryId) {
        if (this.multiHostSearchComponent == null || !this.searchMultiHosts) {
            return false;
        }
        for (String folder : folders) {
            if (folder.startsWith("http") && !folder.contains(repositoryId)) {
                return true;
            }
        }
        for (String approved : alreadyApproved) {
            if (!approved.contains(repositoryId)) {
                return true;
            }
        }
        return false;
    }

    private ManuallyApproveResource getResource(List<ManuallyApproveResource> result, String uri) {
        for (ManuallyApproveResource m : result) {
            if (m.getUri().equals(uri)) {
                return m;
            }
        }
        return null;
    }

    private Set<String> getLocalPaths(Set<String> uris, String repositoryID) {
        Set<String> uriSet = new HashSet<String>();
        for (String uri : uris) {
            boolean isUri = uri.startsWith("http");
            if (isUri) {
                uri = PROTOCOL_PATTERN.matcher(uri).replaceAll("");
            }
            if (uri.startsWith(repositoryID)) {
                uri = uri.replace(repositoryID, "");
            }
            try {
                Path.fromString(uri);
                uriSet.add(uri);
            } catch (IllegalArgumentException e) {
            }
        }
        return uriSet;
    }

    private Search getSearch(Query query) {
        Search search = new Search();
        SortingImpl sorting = new SortingImpl();
        sorting.addSortField(new PropertySortField(this.publishDatePropDef, SortFieldDirection.DESC));
        search.setSorting(sorting);
        search.setLimit(1000);
        search.setQuery(query);
        return search;
    }

    private ManuallyApproveResource mapPropertySetToManuallyApprovedResource(String reposirotyId, PropertySet ps,
            String source, boolean approved) {
        String title = ps.getProperty(this.titlePropDef).getStringValue();
        String uri = "http://".concat(reposirotyId).concat(ps.getURI().toString());
        Property urlProp = ps.getProperty(Namespace.DEFAULT_NAMESPACE, MultiHostSearch.SOLR_URL_PROP_NAME);
        if (urlProp != null) {
            uri = URL.parse(urlProp.getStringValue()).toString();
        }
        Property dateProp = ps.getProperty(this.publishDatePropDef);
        if (dateProp == null) {
            dateProp = ps.getProperty(this.creationTimePropDef);
        }
        Date publishDate = dateProp != null ? dateProp.getDateValue() : Calendar.getInstance().getTime();
        ManuallyApproveResource m = new ManuallyApproveResource(title, uri, source, publishDate, approved);
        return m;
    }

    private Query getUriPrefixesQuery(Set<String> validatedFolders, String folder) {
        Query uriPrefixQuery = new UriPrefixQuery(folder);
        List<Path> aggregatedPaths = this.aggregationResolver.getAggregationPaths(Path.fromString(folder));
        if (aggregatedPaths == null || aggregatedPaths.size() == 0) {
            return uriPrefixQuery;
        } else {
            OrQuery query = new OrQuery();
            query.add(uriPrefixQuery);
            for (Path aggregatedPath : aggregatedPaths) {
                String s = aggregatedPath.toString();
                // Don't include aggregated folder if it already is part of
                // validated list of folders to manually approve from
                if (!validatedFolders.contains(s)) {
                    query.add(new UriPrefixQuery(s));
                }
            }
            return query;
        }
    }

    public Set<String> getManuallyApprovedUris(PropertySet collection, boolean onlyLocal) {
        Set<String> uriSet = new HashSet<String>();
        Property manuallyApprovedProp = collection.getProperty(this.manuallyApprovedResourcesPropDef);
        if (manuallyApprovedProp != null) {
            Value[] values = manuallyApprovedProp.getValues();
            for (Value val : values) {
                uriSet.add(val.getStringValue());
            }
        }
        if (onlyLocal) {
            Repository repository = RequestContext.getRequestContext().getRepository();
            return this.getLocalPaths(uriSet, repository.getId());
        }
        return uriSet;
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
    public void setCollectionPropDef(PropertyTypeDefinition collectionPropDef) {
        this.collectionPropDef = collectionPropDef;
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
    public void setManuallyApprovedResourcesPropDef(PropertyTypeDefinition manuallyApprovedResourcesPropDef) {
        this.manuallyApprovedResourcesPropDef = manuallyApprovedResourcesPropDef;
    }

    public void setMultiHostSearchComponent(MultiHostSearchComponent multiHostSearchComponent) {
        this.multiHostSearchComponent = multiHostSearchComponent;
    }

    public void setSearchMultiHosts(boolean searchMultiHosts) {
        this.searchMultiHosts = searchMultiHosts;
    }

}

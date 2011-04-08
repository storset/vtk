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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Required;
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
import org.vortikal.repository.search.query.UriSetQuery;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;
import org.vortikal.web.display.collection.aggregation.AggregationResolver;

public class ManuallyApproveResourcesSearcher {

    // Used only if configured and hooked up in config
    private ExternalSearcher externalSearcher;

    private Map<String, String> listingResourceTypeMappingPointers;
    private AggregationResolver aggregationResolver;
    private PropertyTypeDefinition collectionPropDef;
    private PropertyTypeDefinition titlePropDef;
    private PropertyTypeDefinition publishDatePropDef;
    private PropertyTypeDefinition creationTimePropDef;

    public List<ManuallyApproveResource> getManuallyApproveResources(Resource collection, Set<String> folders,
            Set<String> alreadyApproved) {

        Repository repository = RequestContext.getRequestContext().getRepository();
        String token = SecurityContext.getSecurityContext().getToken();

        List<ManuallyApproveResource> result = new ArrayList<ManuallyApproveResource>();

        // Get already approved resources on local host
        List<PropertySet> alreadyApprovedResources = new ArrayList<PropertySet>();
        if (alreadyApproved.size() > 0) {
            Query localAlreadyApprovedUriSetQuery = this.getUriSetQuery(alreadyApproved, repository.getId(), true);
            Search search = this.getSearch(localAlreadyApprovedUriSetQuery, token);
            ResultSet rs = repository.search(token, search);
            alreadyApprovedResources.addAll(rs.getAllResults());
        }

        // Perform searches, map property sets to manually approved resource
        // object containers and join results
        if (externalSearcher != null) {
            // Get already approved resources from other hosts
            if (alreadyApproved.size() > 0) {
                Query alreadyApprovedUriSetQuery = this.getUriSetQuery(alreadyApproved, repository.getId(), false);
                Search search = this.getSearch(alreadyApprovedUriSetQuery, token);
                List<PropertySet> l = this.externalSearcher.search(token, search);
                alreadyApprovedResources.addAll(l);
            }
        }

        // Sort and return
        Collections.sort(result, new ManuallyApproveResourceComparator());
        return result;
    }

    private Query getUriSetQuery(Set<String> alreadyApproved, String repositoryId, boolean local) {
        Set<String> uriSet = new HashSet<String>();
        for (String s : alreadyApproved) {
            if (local) {
                if (s.startsWith(repositoryId)) {
                    uriSet.add(s.replace(repositoryId, ""));
                }
            } else if (!s.startsWith(repositoryId)) {
                uriSet.add(s);
            }
        }
        return new UriSetQuery(uriSet);
    }

    private Search getSearch(Query query, String token) {
        Search search = new Search();
        SortingImpl sorting = new SortingImpl();
        sorting.addSortField(new PropertySortField(this.publishDatePropDef, SortFieldDirection.DESC));
        search.setSorting(sorting);
        search.setLimit(1000);
        search.setQuery(query);
        return search;
    }

    public void setExternalSearcher(ExternalSearcher externalSearcher) {
        this.externalSearcher = externalSearcher;
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

}

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

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
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
import org.vortikal.web.service.Service;

public class ManuallyApproveResourcesHandler implements Controller {

    private static final String FOLDERS_PARAM = "folders";
    private static final String AGGREGATE_PARAM = "aggregate";

    private Repository repository;
    private PropertyTypeDefinition manuallyApproveFromPropDef;
    private PropertyTypeDefinition manuallyApprovedResourcesPropDef;
    private PropertyTypeDefinition collectionPropDef;
    private PropertyTypeDefinition titlePropDef;
    private PropertyTypeDefinition publishDatePropDef;
    private PropertyTypeDefinition creationTimePropDef;
    private PropertyTypeDefinition aggregationPropDef;
    private PropertyTypeDefinition recursivePropDef;
    private Map<String, String> listingResourceTypeMappingPointers;
    private AggregationResolver aggregationResolver;
    private Service viewService;

    @Override
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {

        Path currentCollectionPath = RequestContext.getRequestContext().getCurrentCollection();
        String token = SecurityContext.getSecurityContext().getToken();
        Resource currentCollection = this.repository.retrieve(token, currentCollectionPath, false);
        Property manuallyApproveFromProp = currentCollection.getProperty(this.manuallyApproveFromPropDef);
        Property manuallyApprovedResourcesProp = currentCollection.getProperty(this.manuallyApprovedResourcesPropDef);
        Property aggregationProp = currentCollection.getProperty(this.aggregationPropDef);
        Property recursiveProp = currentCollection.getProperty(this.recursivePropDef);
        String[] folders = request.getParameterValues(FOLDERS_PARAM);
        String[] aggregate = request.getParameterValues(AGGREGATE_PARAM);

        // Nothing to work with, need at least one of these
        if (manuallyApproveFromProp == null && manuallyApprovedResourcesProp == null && folders == null) {
            return null;
        }

        Set<String> validatedFolders = new HashSet<String>();
        // Parameter "folders" overrides what's already stored, because user
        // might change content and update service before storing resource
        if (folders != null) {
            for (String folder : folders) {
                if (this.isValidFolder(currentCollectionPath, folder, aggregationProp, recursiveProp)) {
                    validatedFolders.add(folder);
                }
            }
        } else if (manuallyApproveFromProp != null) {
            Value[] manuallyApproveFromValues = manuallyApproveFromProp.getValues();
            for (Value manuallyApproveFromValue : manuallyApproveFromValues) {
                String folder = manuallyApproveFromValue.getStringValue();
                if (this.isValidFolder(currentCollectionPath, folder, aggregationProp, recursiveProp)) {
                    validatedFolders.add(manuallyApproveFromValue.getStringValue());
                }
            }
        }

        Set<String> alreadyApproved = new HashSet<String>();
        if (manuallyApprovedResourcesProp != null) {
            String value = manuallyApprovedResourcesProp.getFormattedValue();
            JSONArray arr = JSONArray.fromObject(value);
            for (int i = 0; i < arr.size(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                alreadyApproved.add(obj.getString("uri"));
            }
        }

        // No valid folders to display contents from and no already manually
        // approved resources -> finish
        if (validatedFolders.size() == 0 && alreadyApproved.size() == 0) {
            return null;
        }

        // Step two of headache: resolve already manually approved resource from
        // folders to manually approve from
        for (String validatedFolder : validatedFolders) {
            Set<String> s = this.getManuallyApprovedResources(validatedFolder, token);
            if (s != null) {
                alreadyApproved.addAll(s);
            }
        }

        List<PropertySet> approvedResources = new ArrayList<PropertySet>();
        if (alreadyApproved.size() > 0) {
            ResultSet rs = this.search(new UriSetQuery(alreadyApproved), token);
            approvedResources.addAll(rs.getAllResults());
        }

        String resourceTypePointer = this.listingResourceTypeMappingPointers.get(currentCollection.getResourceType());
        Query resourceTypeQuery = null;
        if (resourceTypePointer != null) {
            resourceTypePointer = resourceTypePointer.startsWith("structured.") ? resourceTypePointer.replace(".", "-")
                    : resourceTypePointer;
            resourceTypeQuery = new TypeTermQuery(resourceTypePointer, TermOperator.IN);
        }

        List<ManuallyApproveResource> result = new ArrayList<ManuallyApproveResource>();
        List<PropertySet> psList = new ArrayList<PropertySet>();
        for (String folder : validatedFolders) {
            Query query = this.getUriPrefixesQuery(validatedFolders, folder);
            if (resourceTypeQuery != null) {
                AndQuery and = new AndQuery();
                and.add(resourceTypeQuery);
                and.add(query);
                query = and;
            }
            ResultSet rs = this.search(query, token);
            for (PropertySet ps : rs.getAllResults()) {
                Property collectionProp = ps.getProperty(this.collectionPropDef);
                if (!collectionProp.getBooleanValue()) {
                    boolean approved = approvedResources.contains(ps);
                    if (approved) {
                        psList.add(ps);
                    }
                    ManuallyApproveResource m = this.mapPropertySetToManuallyApprovedResource(ps, folder, approved);
                    result.add(m);
                }
            }
        }
        for (PropertySet ps : approvedResources) {
            if (!psList.contains(ps)) {
                ManuallyApproveResource m = this.mapPropertySetToManuallyApprovedResource(ps, ps.getURI().getParent()
                        .toString(), true);
                result.add(m);
            }
        }
        Collections.sort(result, new ManuallyApproveResourceComparator());

        JSONArray arr = new JSONArray();
        for (ManuallyApproveResource m : result) {
            JSONObject obj = new JSONObject();
            obj.put("title", m.getTitle());
            obj.put("uri", m.getUri());
            obj.put("source", m.getSource());
            obj.put("published", m.getPublishDateAsString());
            obj.put("approved", m.isApproved());
            arr.add(obj);
        }
        response.setContentType("text/plain;charset=utf-8");
        PrintWriter writer = response.getWriter();
        writer.print(arr);
        writer.flush();
        writer.close();

        return null;
    }

    private ManuallyApproveResource mapPropertySetToManuallyApprovedResource(PropertySet ps, String source,
            boolean approved) {
        String title = ps.getProperty(this.titlePropDef).getStringValue();
        Path resourcePath = ps.getURI();
        String uri = resourcePath.toString();
        Property dateProp = ps.getProperty(this.publishDatePropDef);
        if (dateProp == null) {
            dateProp = ps.getProperty(this.creationTimePropDef);
        }
        Date publishDate = dateProp != null ? dateProp.getDateValue() : Calendar.getInstance().getTime();
        ManuallyApproveResource m = new ManuallyApproveResource(title, uri, source, publishDate, approved);
        return m;
    }

    private ResultSet search(Query query, String token) {
        Search search = new Search();
        SortingImpl sorting = new SortingImpl();
        sorting.addSortField(new PropertySortField(this.publishDatePropDef, SortFieldDirection.DESC));
        search.setSorting(sorting);
        search.setLimit(1000);
        search.setQuery(query);
        return this.repository.search(token, search);
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

    private Set<String> getManuallyApprovedResources(String validatedFolder, String token) {
        try {
            Resource collection = this.repository.retrieve(token, Path.fromString(validatedFolder), false);
            Property prop = collection.getProperty(this.manuallyApprovedResourcesPropDef);
            if (prop == null) {
                return null;
            }
            Set<String> s = new HashSet<String>();
            Value[] manuallyApprovedValues = prop.getValues();
            for (Value manuallyApprovedResource : manuallyApprovedValues) {
                s.add(manuallyApprovedResource.getStringValue());
            }
            return s;
        } catch (Exception e) {
            // XXX: log, but don't break everything
        }
        return null;
    }

    private boolean isValidFolder(Path currentCollectionPath, String folder, Property aggregationProp,
            Property recursiveProp) {
        if (StringUtils.isBlank(folder)) {
            return false;
        }
        // XXX Validate absolute uris (http/www)
        Path folderPath = this.getPath(folder);
        if (folderPath == null || folderPath.equals(currentCollectionPath)) {
            return false;
        }
        if ((recursiveProp != null && recursiveProp.getBooleanValue())
                || currentCollectionPath.isAncestorOf(folderPath)) {
            return false;
        }
        if (aggregationProp != null) {
            Value[] aggregationValues = aggregationProp.getValues();
            for (Value aggregationValue : aggregationValues) {
                Path aggregationPath = this.getPath(aggregationValue.toString());
                if (aggregationPath == null) {
                    continue;
                }
                if (folderPath.equals(aggregationPath) || aggregationPath.isAncestorOf(folderPath)) {
                    return false;
                }
            }
        }
        // XXX Ignore if is child of current collection and recursive
        // search is already selected
        return true;
    }

    private Path getPath(String folder) {
        try {
            return Path.fromString(folder);
        } catch (IllegalArgumentException iae) {
            return null;
        }
    }

    @Required
    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    @Required
    public void setManuallyApproveFromPropDef(PropertyTypeDefinition manuallyApproveFromPropDef) {
        this.manuallyApproveFromPropDef = manuallyApproveFromPropDef;
    }

    @Required
    public void setManuallyApprovedResourcesPropDef(PropertyTypeDefinition manuallyApprovedResourcesPropDef) {
        this.manuallyApprovedResourcesPropDef = manuallyApprovedResourcesPropDef;
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
    public void setAggregationPropDef(PropertyTypeDefinition aggregationPropDef) {
        this.aggregationPropDef = aggregationPropDef;
    }

    @Required
    public void setRecursivePropDef(PropertyTypeDefinition recursivePropDef) {
        this.recursivePropDef = recursivePropDef;
    }

    @Required
    public void setAggregationResolver(AggregationResolver aggregationResolver) {
        this.aggregationResolver = aggregationResolver;
    }

    @Required
    public void setViewService(Service viewService) {
        this.viewService = viewService;
    }

    @Required
    public void setListingResourceTypeMappingPointers(Map<String, String> listingResourceTypeMappingPointers) {
        this.listingResourceTypeMappingPointers = listingResourceTypeMappingPointers;
    }

}

/* Copyright (c) 2008, University of Oslo, Norway
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.RequestContext;
import org.vortikal.edit.editor.ResourceWrapperManager;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.search.ResultSet;
import org.vortikal.repository.search.Search;
import org.vortikal.repository.search.query.AndQuery;
import org.vortikal.repository.search.query.PropertyExistsQuery;
import org.vortikal.repository.search.query.TermOperator;
import org.vortikal.repository.search.query.TypeTermQuery;
import org.vortikal.repository.search.query.UriDepthQuery;
import org.vortikal.repository.search.query.UriPrefixQuery;
import org.vortikal.security.Principal;
import org.vortikal.security.SecurityContext;
import org.vortikal.util.repository.ResourcePropertyComparator;
import org.vortikal.web.service.Service;
import org.vortikal.web.service.URL;

public abstract class AbstractCollectionListingController implements ListingController {

    protected final static String MODEL_KEY_SEARCH_COMPONENTS = "searchComponents";
    protected final static String MODEL_KEY_PAGE = "page";
    protected final static String MODEL_KEY_PAGE_THROUGH_URLS = "pageThroughUrls";
    protected final static String MODEL_KEY_HIDE_ALTERNATIVE_REP = "hideAlternativeRepresentation";

    protected Repository repository;
    protected ResourceWrapperManager resourceManager;
    protected PropertyTypeDefinition hiddenPropDef;
    protected int defaultPageLimit = 20;
    protected int collectionDisplayLimit = 1000;
    protected PropertyTypeDefinition pageLimitPropDef;
    protected PropertyTypeDefinition hideNumberOfComments;
    protected String viewName;
    protected Map<String, Service> alternativeRepresentations;
    private boolean includeRequestParametersInAlternativeRepresentation;

    // A list of properties used when sorting the list of collections
    // @see ResourcePropertyComparator
    protected List<PropertyTypeDefinition> sortPropDefs;
    protected Map<PropertyTypeDefinition, List<PropertyTypeDefinition>> overridingSortPropDefs;

    /**
     * Container class for (resource, URL) for subcollections
     */
    public class CollectionItem {
        private PropertySet resource;
        private URL url;

        public CollectionItem(PropertySet resource, URL url) {
            this.resource = resource;
            this.url = url;
        }

        public PropertySet getResource() {
            return this.resource;
        }

        public URL getURL() {
            return this.url;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {

        Path uri = org.vortikal.web.RequestContext.getRequestContext().getResourceURI();
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        String token = securityContext.getToken();
        Principal principal = securityContext.getPrincipal();

        Resource collection = this.repository.retrieve(token, uri, true);
        List<PropertySet> subCollections = listCollections(uri, token);

        Locale locale = new RequestContext(request).getLocale();
        Collections.sort(subCollections, new ResourcePropertyComparator(this.sortPropDefs, this.overridingSortPropDefs,
                false, locale));

        Map<String, Object> model = new HashMap<String, Object>();
        model.put("collection", this.resourceManager.createResourceWrapper(collection));
        model.put("subCollections", createSubCollections(request, subCollections));

        int pageLimit = getPageLimit(collection);
        if (pageLimit > 0) {
            /* Run the actual search (done in subclasses) */
            runSearch(request, collection, model, pageLimit);
        }

        if (this.alternativeRepresentations != null) {
            Set<Object> alt = new HashSet<Object>();
            for (String contentType : this.alternativeRepresentations.keySet()) {
                try {
                    Map<String, Object> m = new HashMap<String, Object>();
                    Service service = this.alternativeRepresentations.get(contentType);

                    URL url = service.constructURL(collection, principal);
                    if (this.includeRequestParametersInAlternativeRepresentation) {
                        Enumeration<String> requestParameters = request.getParameterNames();
                        while (requestParameters.hasMoreElements()) {
                            String requestParameter = requestParameters.nextElement();
                            String parameterValue = request.getParameter(requestParameter);
                            url.addParameter(requestParameter, parameterValue);
                        }
                    }

                    String title = service.getName();
                    RequestContext rc = new RequestContext(request);
                    title = rc.getMessage(service.getName(), new Object[] { collection.getTitle() }, service.getName());

                    m.put("title", title);
                    m.put("url", url);
                    m.put("contentType", contentType);

                    alt.add(m);
                } catch (Throwable t) {
                }
            }
            if (pageLimit > 0) {
                model.put("alternativeRepresentations", alt);
            }
        }
        return new ModelAndView(this.viewName, model);
    }

    protected List<CollectionItem> createSubCollections(HttpServletRequest request, List<PropertySet> subCollections) {
        List<CollectionItem> result = new ArrayList<CollectionItem>();
        for (PropertySet propertySet : subCollections) {
            URL url = URL.create(request);
            url.clearParameters();
            url.setRef(null);
            url.setPath(propertySet.getURI());
            CollectionItem c = new CollectionItem(propertySet, url);
            result.add(c);
        }
        return result;
    }

    protected List<PropertySet> listCollections(Path uri, String token) {

        AndQuery query = new AndQuery();
        query.add(new UriPrefixQuery(uri.toString()));
        query.add(new UriDepthQuery(uri.getDepth() + 1));
        query.add(new TypeTermQuery("collection", TermOperator.IN));
        query.add(new PropertyExistsQuery(this.hiddenPropDef, true));

        Search search = new Search();
        search.setLimit(this.collectionDisplayLimit);
        search.setQuery(query);

        ResultSet result = this.repository.search(token, search);
        return result.getAllResults();
    }

    protected URL createURL(HttpServletRequest request, String... removeableParams) {
        URL url = URL.create(request);
        for (String removableParam : removeableParams) {
            url.removeParameter(removableParam);
        }
        return url;
    }

    protected int getPageLimit(Resource collection) {
        int pageLimit = this.defaultPageLimit;
        Property pageLimitProp = collection.getProperty(this.pageLimitPropDef);
        if (pageLimitProp != null) {
            pageLimit = pageLimitProp.getIntValue();
        }
        return pageLimit;
    }

    protected boolean getHideNumberOfComments(Resource collection) {
        Property p = collection.getProperty(this.hideNumberOfComments);
        if (p == null) {
            return false;
        }
        return p.getBooleanValue();
    }

    protected int getIntParameter(HttpServletRequest request, String name, int defaultValue) {
        String param = request.getParameter(name);
        if (param == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(param);
        } catch (Throwable t) {
            return defaultValue;
        }
    }

    @Required
    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    @Required
    public void setResourceManager(ResourceWrapperManager resourceManager) {
        this.resourceManager = resourceManager;
    }

    @Required
    public void setHiddenPropDef(PropertyTypeDefinition hiddenPropDef) {
        this.hiddenPropDef = hiddenPropDef;
    }

    @Required
    public void setSortPropDefs(List<PropertyTypeDefinition> sortPropDefs) {
        this.sortPropDefs = sortPropDefs;
    }

    public void setOverridingSortPropDefs(
            Map<PropertyTypeDefinition, List<PropertyTypeDefinition>> overridingSortPropDefs) {
        this.overridingSortPropDefs = overridingSortPropDefs;
    }

    @Required
    public void setPageLimitPropDef(PropertyTypeDefinition pageLimitPropDef) {
        this.pageLimitPropDef = pageLimitPropDef;
    }

    public void setDefaultPageLimit(int defaultPageLimit) {
        if (defaultPageLimit <= 0)
            throw new IllegalArgumentException("Argument must be a positive integer");
        this.defaultPageLimit = defaultPageLimit;
    }

    @Required
    public void setViewName(String viewName) {
        this.viewName = viewName;
    }

    public void setAlternativeRepresentations(Map<String, Service> alternativeRepresentations) {
        this.alternativeRepresentations = alternativeRepresentations;
    }

    public void setIncludeRequestParametersInAlternativeRepresentation(
            boolean includeRequestParametersInAlternativeRepresentation) {
        this.includeRequestParametersInAlternativeRepresentation = includeRequestParametersInAlternativeRepresentation;
    }

    public void setHideNumberOfComments(PropertyTypeDefinition hideNumberOfComments) {
        this.hideNumberOfComments = hideNumberOfComments;
    }

    public void setCollectionDisplayLimit(int collectionDisplayLimit) {
        this.collectionDisplayLimit = collectionDisplayLimit;
    }
}

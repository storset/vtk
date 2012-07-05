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
package org.vortikal.web.display.tags;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.vortikal.repository.Path;
import org.vortikal.repository.Resource;
import org.vortikal.repository.ResourceTypeTree;
import org.vortikal.repository.resourcetype.ResourceTypeDefinition;
import org.vortikal.web.RequestContext;
import org.vortikal.web.display.listing.ListingPager;
import org.vortikal.web.display.listing.ListingPagingLink;
import org.vortikal.web.search.Listing;
import org.vortikal.web.search.SearchComponent;
import org.vortikal.web.service.Service;
import org.vortikal.web.service.URL;
import org.vortikal.web.tags.RepositoryTagElementsDataProvider;
import org.vortikal.web.tags.TagElement;
import org.vortikal.web.tags.TagsHelper;

public class TagsController implements Controller {

    private int defaultPageLimit = 20;
    private String viewName;
    private SearchComponent searchComponent;
    private Map<String, Service> alternativeRepresentations;
    private RepositoryTagElementsDataProvider tagElementsProvider;
    private ResourceTypeTree resourceTypeTree;
    private TagsHelper tagsHelper;
    private boolean servesWebRoot;
    private String hostName;

    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        RequestContext requestContext = RequestContext.getRequestContext();
        String token = requestContext.getSecurityToken();

        String tag = request.getParameter(TagsHelper.TAG_PARAMETER);
        Resource scope = this.tagsHelper.getScope(token, request);

        Map<String, Object> model = new HashMap<String, Object>();

        List<ResourceTypeDefinition> resourceTypes = getResourceTypes(request);

        /* List all known tags for the current collection */
        if (!StringUtils.isBlank(tag)) {
            handleSingleTag(request, tag, scope, model, resourceTypes);
        } else {
            handleAllTags(token, request, scope, model, resourceTypes);
        }

        // Add scope up url if scope is not root
        if (this.servesWebRoot && !scope.getURI().equals(Path.ROOT)) {
            Map<String, String> scopeUp = new HashMap<String, String>();
            Service service = org.vortikal.web.RequestContext.getRequestContext().getService();
            URL url = service.constructURL(scope.getURI());
            url.setPath(Path.ROOT);
            List<String> sortFieldParams = null;
            if (!StringUtils.isBlank(tag)) {
                Object searchResult = model.get("listing");
                if (searchResult != null && searchResult instanceof Listing) {
                    Listing listing = (Listing) searchResult;
                    sortFieldParams = listing.getSortFieldParams();
                }
            }
            this.processUrl(url, tag, resourceTypes, sortFieldParams);
            String title = this.tagsHelper.getTitle(request, scope, tag, this.hostName, true);
            scopeUp.put("url", url.toString());
            scopeUp.put("title", title);
            model.put(TagsHelper.SCOPE_UP_MODEL_KEY, scopeUp);
        }

        return new ModelAndView(this.viewName, model);
    }

    private void handleSingleTag(HttpServletRequest request, String tag, Resource scope, Map<String, Object> model,
            List<ResourceTypeDefinition> resourceTypes) throws Exception {

        model.put(TagsHelper.SCOPE_PARAMETER, scope);
        model.put(TagsHelper.TAG_PARAMETER, tag);

        // Setting the default page limit
        int pageLimit = this.defaultPageLimit;

        int page = ListingPager.getPage(request, ListingPager.UPCOMING_PAGE_PARAM);
        int limit = pageLimit;
        int totalHits = 0;

        Listing listing = this.searchComponent.execute(request, scope, page, limit, 0);
        List<String> sortFieldParams = null;
        if (listing != null) {
            totalHits = listing.getTotalHits();
            sortFieldParams = listing.getSortFieldParams();
        }

        if (resourceTypes != null && resourceTypes.size() == 1) {
            model.put(TagsHelper.RESOURCE_TYPE_MODEL_KEY, resourceTypes.get(0).getName());
        }

        Service service = org.vortikal.web.RequestContext.getRequestContext().getService();
        URL baseURL = service.constructURL(scope.getURI());
        this.processUrl(baseURL, tag, resourceTypes, sortFieldParams);

        List<ListingPagingLink> urls = ListingPager.generatePageThroughUrls(totalHits, pageLimit, baseURL, page);

        model.put("listing", listing);
        model.put("page", page);
        model.put("pageThroughUrls", urls);

        if (this.alternativeRepresentations != null) {
            Set<Object> alt = new HashSet<Object>();
            for (String contentType : this.alternativeRepresentations.keySet()) {
                try {
                    Service altService = this.alternativeRepresentations.get(contentType);
                    URL url = altService.constructURL(scope.getURI());
                    this.processUrl(url, tag, resourceTypes, sortFieldParams);
                    String title = altService.getName();
                    org.springframework.web.servlet.support.RequestContext rc = 
                        new org.springframework.web.servlet.support.RequestContext(request);
                    title = rc
                            .getMessage(altService.getName(), new Object[] { scope.getTitle() }, altService.getName());

                    Map<String, Object> m = new HashMap<String, Object>();
                    m.put("title", title);
                    m.put("url", url);
                    m.put("contentType", contentType);

                    alt.add(m);
                    model.put("alternativeRepresentations", alt);
                } catch (Throwable t) {
                }
            }
        }

        String title = this.tagsHelper.getTitle(request, scope, tag);
        model.put("title", title);

    }

    private void processUrl(URL url, String tag, List<ResourceTypeDefinition> resourceTypes,
            List<String> sortFieldParams) {
        if (!StringUtils.isBlank(tag)) {
            url.addParameter(TagsHelper.TAG_PARAMETER, tag);
        }
        if (sortFieldParams != null && sortFieldParams.size() > 0) {
            for (String param : sortFieldParams) {
                url.addParameter(Listing.SORTING_PARAM, param);
            }
        }
        if (resourceTypes != null && !url.getParameterNames().contains(TagsHelper.RESOURCE_TYPE_PARAMETER)) {
            for (ResourceTypeDefinition resourceTypeDefinition : resourceTypes) {
                url.addParameter(TagsHelper.RESOURCE_TYPE_PARAMETER, resourceTypeDefinition.getName());
            }
        }
    }

    private void handleAllTags(String token, HttpServletRequest request, Resource scope, Map<String, Object> model,
            List<ResourceTypeDefinition> resourceTypes) {

        model.put(TagsHelper.SCOPE_PARAMETER, scope);

        Path scopeUri = scope.getURI();

        List<TagElement> tagElements = this.tagElementsProvider.getTagElements(scopeUri, resourceTypes, token, 1, 1,
                Integer.MAX_VALUE, 1);

        model.put("tagElements", tagElements);
        String title = this.tagsHelper.getTitle(request, scope, null);
        model.put("title", title);
    }

    private List<ResourceTypeDefinition> getResourceTypes(HttpServletRequest request) {
        String[] resourcePrams = request.getParameterValues(TagsHelper.RESOURCE_TYPE_PARAMETER);
        if (resourcePrams != null) {
            List<ResourceTypeDefinition> resourceTypes = new ArrayList<ResourceTypeDefinition>();
            for (String resourceType : resourcePrams) {
                try {
                    ResourceTypeDefinition resourceTypeDef = this.resourceTypeTree
                            .getResourceTypeDefinitionByName(resourceType);
                    resourceTypes.add(resourceTypeDef);
                } catch (IllegalArgumentException iae) {
                    // invalid resource type name, ignore it
                }
            }
            return resourceTypes;
        }
        return null;
    }

    public void setDefaultPageLimit(int defaultPageLimit) {
        if (defaultPageLimit <= 0)
            throw new IllegalArgumentException("Argument must be a positive integer");
        this.defaultPageLimit = defaultPageLimit;
    }

    @Required
    public void setSearchComponent(SearchComponent searchComponent) {
        this.searchComponent = searchComponent;
    }

    @Required
    public void setViewName(String viewName) {
        this.viewName = viewName;
    }

    public void setAlternativeRepresentations(Map<String, Service> alternativeRepresentations) {
        this.alternativeRepresentations = alternativeRepresentations;
    }

    public void setTagElementsProvider(RepositoryTagElementsDataProvider tagElementsProvider) {
        this.tagElementsProvider = tagElementsProvider;
    }

    public void setResourceTypeTree(ResourceTypeTree resourceTypeTree) {
        this.resourceTypeTree = resourceTypeTree;
    }

    @Required
    public void setTagsHelper(TagsHelper tagsHelper) {
        this.tagsHelper = tagsHelper;
    }

    public void setServesWebRoot(boolean servesWebRoot) {
        this.servesWebRoot = servesWebRoot;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

}

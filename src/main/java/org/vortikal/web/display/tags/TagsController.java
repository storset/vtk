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

import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.servlet.support.RequestContext;
import org.vortikal.repository.Path;
import org.vortikal.repository.Resource;
import org.vortikal.repository.ResourceTypeTree;
import org.vortikal.repository.resourcetype.ResourceTypeDefinition;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.display.listing.AbstractListingController;
import org.vortikal.web.search.Listing;
import org.vortikal.web.search.SearchComponent;
import org.vortikal.web.service.Service;
import org.vortikal.web.service.URL;
import org.vortikal.web.tags.RepositoryTagElementsDataProvider;
import org.vortikal.web.tags.TagElement;
import org.vortikal.web.tags.TagsHelper;

public class TagsController extends AbstractListingController implements Controller {

    private boolean defaultRecursive = true;
    private int defaultPageLimit = 20;
    private String viewName;
    private SearchComponent searchComponent;
    private Map<String, Service> alternativeRepresentations;
    private RepositoryTagElementsDataProvider tagElementsProvider;
    private ResourceTypeTree resourceTypeTree;
    private TagsHelper tagsHelper;

    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {

        SecurityContext securityContext = SecurityContext.getSecurityContext();
        String token = securityContext.getToken();

        String tag = request.getParameter(TagsHelper.TAG_PARAMETER);
        Resource scope = this.tagsHelper.getScope(token, request);

        /* List all known tags for the current collection */
        if (tag == null || tag.trim().equals("")) {
            return handleAllTags(token, request, scope);
        }

        return handleSingleTag(request, tag, scope);
    }

    private ModelAndView handleSingleTag(HttpServletRequest request, String tag, Resource scope) throws Exception {
        Map<String, Object> model = new HashMap<String, Object>();

        model.put(TagsHelper.SCOPE_PARAMETER, scope);
        model.put(TagsHelper.TAG_PARAMETER, tag);

        // Setting the default page limit
        int pageLimit = this.defaultPageLimit;

        int page = getPage(request, UPCOMING_PAGE_PARAM);
        int limit = pageLimit;
        int totalHits = 0;

        Listing listing = this.searchComponent.execute(request, scope, page, limit, 0, defaultRecursive);
        if (listing != null) {
            totalHits = listing.getTotalHits();
        }

        List<URL> urls = generatePageThroughUrls(totalHits, pageLimit, URL.create(request));

        model.put("listing", listing);
        model.put("page", page);
        model.put("pageThroughUrls", urls);

        Set<Object> alt = new HashSet<Object>();
        for (String contentType : this.alternativeRepresentations.keySet()) {
            try {
                Map<String, Object> m = new HashMap<String, Object>();
                Service service = this.alternativeRepresentations.get(contentType);
                Map<String, String> parameters = new HashMap<String, String>();
                parameters.put(TagsHelper.TAG_PARAMETER, tag);
                URL url = service.constructURL(scope.getURI(), parameters);
                List<String> sortFieldParams = listing.getSortFieldParams();
                if (sortFieldParams.size() > 0) {
                    for (String param : sortFieldParams) {
                        url.addParameter(Listing.SORTING_PARAM, param);
                    }
                }
                String title = service.getName();
                RequestContext rc = new RequestContext(request);
                title = rc.getMessage(service.getName(), new Object[] { scope.getTitle() }, service.getName());

                m.put("title", title);
                m.put("url", url);
                m.put("contentType", contentType);

                alt.add(m);
            } catch (Throwable t) {
            }
        }
        model.put("alternativeRepresentations", alt);

        List<ResourceTypeDefinition> resourceTypes = getResourceTypes(request);
        if (resourceTypes != null) {
            model.put(TagsHelper.RESOURCE_TYPES_MODEL_KEY, resourceTypes);
            if (resourceTypes.size() == 1) {
                model.put(TagsHelper.SINGLE_RESOURCE_TYPE_MODEL_KEY, resourceTypes.get(0).getName());
            }
        }

        String title = this.tagsHelper.getTitle(request, scope, tag);
        model.put("title", title);

        return new ModelAndView(this.viewName, model);
    }

    private ModelAndView handleAllTags(String token, HttpServletRequest request, Resource scope) {

        Map<String, Object> model = new HashMap<String, Object>();
        model.put(TagsHelper.SCOPE_PARAMETER, scope);

        Path scopeUri = scope.getURI();

        List<ResourceTypeDefinition> resourceTypes = getResourceTypes(request);
        List<TagElement> tagElements = this.tagElementsProvider.getTagElements(scopeUri, resourceTypes, token, 1, 1,
                Integer.MAX_VALUE, 1);

        model.put("tagElements", tagElements);
        String title = this.tagsHelper.getTitle(request, scope, null);
        model.put("title", title);

        return new ModelAndView(this.viewName, model);
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

}

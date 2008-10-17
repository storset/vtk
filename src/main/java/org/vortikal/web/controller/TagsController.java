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
package org.vortikal.web.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.vortikal.edit.editor.ResourceWrapperManager;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.security.Principal;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;
import org.vortikal.web.controller.search.SearchComponent;
import org.vortikal.web.controller.search.SearchComponent.Listing;
import org.vortikal.web.service.Service;
import org.vortikal.web.service.URL;

/**
 * 
 */
public class TagsController implements Controller {

    private Repository repository;
    private ResourceWrapperManager resourceManager;
    private int defaultPageLimit = 20;
    private PropertyTypeDefinition pageLimitPropDef;
    private String viewName;
    private List<SearchComponent> searchComponents;
    private Map<String, Service> alternativeRepresentations;
    private Log logger = LogFactory.getLog(this.getClass());


    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Path uri = RequestContext.getRequestContext().getResourceURI();
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        String token = securityContext.getToken();
        Principal principal = securityContext.getPrincipal();
        Resource collection = this.repository.retrieve(token, uri, true);

        Locale locale = new org.springframework.web.servlet.support.RequestContext(request).getLocale();

        Map<String, Object> model = new HashMap<String, Object>();
        model.put("collection", this.resourceManager.createResourceWrapper(collection.getURI()));

        String tag = request.getParameter("tag");

        if (tag == null || tag.trim().equals("")) {
            model.put("error", "No tags specified");
            return new ModelAndView(this.viewName, model);
        }
        model.put("tag", tag);

        // Setting the default pagelimit
        int pageLimit = this.defaultPageLimit;
        Property pageLimitProp = collection.getProperty(this.pageLimitPropDef);
        if (pageLimitProp != null) {
            pageLimit = pageLimitProp.getIntValue();
        }

        int page = 0;
        if (request.getParameter("page") != null) {
            try {
                page = Integer.parseInt(request.getParameter("page"));
                if (page < 1) {
                    page = 1;
                }
            } catch (Throwable t) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Error reading parameter page: " + t.getMessage(), t);
                }
            }
        }

        if (page == 0) {
            page = 1;
        }

        int offset = (page - 1) * pageLimit;
        int limit = pageLimit;

        List<Listing> listings = new ArrayList<Listing>();

        if (tag != null) {
            boolean recursive = true;

            String scopeForSearchComponent = getScope(token, request);
            String[] searchParameters = new String[] { scopeForSearchComponent, tag };
            for (SearchComponent component : this.searchComponents) {
                Listing listing = component.execute(request, collection, page, limit, 0, searchParameters, recursive);
                // Add the listing to the results
                if (listing.getFiles().size() > 0) {
                    listings.add(listing);
                }

                // Check previous result (by redoing the previous search),
                // to see if we need to adjust the offset.
                // XXX: is there a better way?
                if (listing.getFiles().size() == 0 && offset > 0) {
                    Listing prevListing = component.execute(request, collection, page - 1, limit, 0, searchParameters,
                            recursive);
                    if (prevListing.getFiles().size() > 0 && !prevListing.hasMoreResults()) {
                        offset -= prevListing.getFiles().size();
                    }
                }

                // We have more results to display for this listing
                if (listing.hasMoreResults()) {
                    break;
                }

                // Only include enough results to fill the page:
                if (listing.getFiles().size() > 0) {
                    limit -= listing.getFiles().size();
                }
            }
        } else {

        }

        model.put("searchComponents", listings);
        model.put("page", page);

        URL nextURL = null;
        URL prevURL = null;
        if (listings.size() > 0) {
            Listing last = listings.get(listings.size() - 1);
            if (last.hasMoreResults()) {
                nextURL = URL.create(request);
                nextURL.setParameter("page", String.valueOf(page + 1));
            }
            if (page > 1) {
                prevURL = URL.create(request);
                if (page == 1) {
                    prevURL.removeParameter("page");
                } else {
                    prevURL.setParameter("page", String.valueOf(page - 1));
                }
            }
        }

        model.put("nextURL", nextURL);
        model.put("prevURL", prevURL);

        Set<Object> alt = new HashSet<Object>();
        for (String contentType : this.alternativeRepresentations.keySet()) {
            try {
                Map<String, Object> m = new HashMap<String, Object>();
                Service service = this.alternativeRepresentations.get(contentType);
                URL url = service.constructURL(collection, principal);
                String title = service.getName();
                org.springframework.web.servlet.support.RequestContext rc = new org.springframework.web.servlet.support.RequestContext(
                        request);
                title = rc.getMessage(service.getName(), new Object[] { collection.getTitle() }, service.getName());

                m.put("title", title);
                m.put("url", url);
                m.put("contentType", contentType);

                alt.add(m);
            } catch (Throwable t) {

            }
        }
        model.put("alternativeRepresentations", alt);

        return new ModelAndView(this.viewName, model);
    }


    protected String getScope(String token, HttpServletRequest request) throws Exception {
        String scope;
        String scopeFromRequest = request.getParameter("scope");
        if (scopeFromRequest == null || scopeFromRequest.equals("")) {
            return "/*";
        } else {
            if (".".equals(scopeFromRequest)) {
                return RequestContext.getRequestContext().getCurrentCollection().toString() + "/*";
            } else if (scopeFromRequest.startsWith("/")) {
                Resource scopedResource = this.repository.retrieve(token, Path.fromString(scopeFromRequest), true);
                if (!scopedResource.isCollection()) {
                    throw new IllegalArgumentException("scope must be a collection");
                }
                return scopedResource.getURI().toString() + "/*";
            } else {
                throw new IllegalArgumentException("scope must be '.' or start with a '/'");
            }
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
    public void setPageLimitPropDef(PropertyTypeDefinition pageLimitPropDef) {
        this.pageLimitPropDef = pageLimitPropDef;
    }


    public void setDefaultPageLimit(int defaultPageLimit) {
        if (defaultPageLimit <= 0)
            throw new IllegalArgumentException("Argument must be a positive integer");
        this.defaultPageLimit = defaultPageLimit;
    }


    @Required
    public void setSearchComponents(List<SearchComponent> searchComponents) {
        this.searchComponents = searchComponents;
    }


    @Required
    public void setViewName(String viewName) {
        this.viewName = viewName;
    }


    public void setAlternativeRepresentations(Map<String, Service> alternativeRepresentations) {
        this.alternativeRepresentations = alternativeRepresentations;
    }

}

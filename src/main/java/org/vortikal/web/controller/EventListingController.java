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
import java.util.Collections;
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
import org.springframework.web.servlet.mvc.Controller;
import org.vortikal.edit.editor.ResourceWrapperManager;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.security.Principal;
import org.vortikal.security.SecurityContext;
import org.vortikal.util.repository.ResourcePropertyComparator;
import org.vortikal.web.RequestContext;
import org.vortikal.web.controller.search.SearchComponent;
import org.vortikal.web.controller.search.SearchComponent.Listing;
import org.vortikal.web.service.Service;
import org.vortikal.web.service.URL;

/**
 * A controller for displaying event listings (a 
 * collection subtype).
 * 
 * XXX: Refactor this class. Should have a "front page" 
 * displaying a number of upcoming and previous events,
 * and a separate paging mode for each category. The way it 
 * is done now (a single paging mode) is just painful.
 */
public class EventListingController implements Controller {

    private Repository repository;
    private ResourceWrapperManager resourceManager;    
    private PropertyTypeDefinition hiddenPropDef;
    private int defaultPageLimit = 20;
    private PropertyTypeDefinition pageLimitPropDef;
    private List<PropertyTypeDefinition> sortPropDefs;

    private String viewName;
    private SearchComponent upcomingEventsSearch;
    private SearchComponent previousEventsSearch;
    private Map<String, Service> alternativeRepresentations;


    private static final String UPCOMING_PAGE_PARAM = "page";
    private static final String PREVIOUS_PAGE_PARAM = "p-page";
    private static final String PREV_BASE_OFFSET_PARAM = "p-offset";
    
    
    public ModelAndView handleRequest(HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        Path uri = RequestContext.getRequestContext().getResourceURI();
        SecurityContext securityContext = SecurityContext.getSecurityContext(); 
        String token = securityContext.getToken();
        Principal principal = securityContext.getPrincipal();
        Resource collection = this.repository.retrieve(token, uri, true);

        Resource[] children = this.repository.listChildren(token, uri, true);
        List<Resource> subCollections = new ArrayList<Resource>();
        for (Resource r : children) {
            if (r.isCollection() && r.getProperty(this.hiddenPropDef) == null) {
                subCollections.add(r);
            }
        }

        Locale locale = new org.springframework.web.servlet.support.RequestContext(request).getLocale();
        Collections.sort(subCollections, new ResourcePropertyComparator(this.sortPropDefs, false, locale));
        
        Map<String, Object> model = new HashMap<String, Object>();
        model.put("collection", this.resourceManager
                .createResourceWrapper(collection.getURI()));
        model.put("subCollections", subCollections);

        // Setting the default pagelimit
        int pageLimit = this.defaultPageLimit;
        Property pageLimitProp = collection.getProperty(this.pageLimitPropDef);
        if (pageLimitProp != null) {
            pageLimit = pageLimitProp.getIntValue();
        }

        int upcomingEventPage = getPage(request, UPCOMING_PAGE_PARAM);
        int prevEventPage = getPage(request, PREVIOUS_PAGE_PARAM);

        int userDisplayPage = upcomingEventPage;

        URL nextURL = null;
        URL prevURL = null;

        boolean atLeastOneUpcoming = 
            this.upcomingEventsSearch.execute(request, collection, 1, 1, 0).size() > 0;

        List<Listing> searchComponents = new ArrayList<Listing>();
        Listing upcoming = null;
        if (request.getParameter(PREVIOUS_PAGE_PARAM) == null) {
            // Search upcoming events
            upcoming = this.upcomingEventsSearch.execute(
                    request, collection, upcomingEventPage, pageLimit, 0);
            if (upcoming.size() > 0) {
                searchComponents.add(upcoming);
                if (upcomingEventPage > 1) {
                    prevURL = URL.create(request);
                    prevURL.removeParameter(PREVIOUS_PAGE_PARAM);
                    prevURL.removeParameter(PREV_BASE_OFFSET_PARAM);
                    prevURL.setParameter(UPCOMING_PAGE_PARAM, String.valueOf(upcomingEventPage - 1));
                }
            }
            if (upcoming.hasMoreResults()) {
                nextURL = URL.create(request);
                nextURL.removeParameter(PREVIOUS_PAGE_PARAM);
                nextURL.removeParameter(PREV_BASE_OFFSET_PARAM);
                nextURL.setParameter(UPCOMING_PAGE_PARAM, String.valueOf(upcomingEventPage + 1));
            }
        }

        
        if (upcoming == null || upcoming.size() == 0) {
            // Searching only in previous events
            int upcomingOffset = getIntParameter(request, PREV_BASE_OFFSET_PARAM, 0);
            if (upcomingOffset > pageLimit) upcomingOffset = 0;
            Listing previous = this.previousEventsSearch.execute(
                    request, collection, prevEventPage, pageLimit, upcomingOffset);
            if (previous.size() > 0) {
                searchComponents.add(previous);
            }
            
            if (prevEventPage > 1) {
                prevURL = URL.create(request);
                prevURL.setParameter(PREV_BASE_OFFSET_PARAM, String.valueOf(upcomingOffset));
                prevURL.setParameter(PREVIOUS_PAGE_PARAM, String.valueOf(prevEventPage - 1));

            } else if (prevEventPage == 1 && atLeastOneUpcoming) {
                prevURL = URL.create(request);
                prevURL.removeParameter(PREVIOUS_PAGE_PARAM);
                prevURL.removeParameter(PREV_BASE_OFFSET_PARAM);
            }

            if (previous.hasMoreResults()) {
                nextURL = URL.create(request);
                nextURL.setParameter(PREV_BASE_OFFSET_PARAM, String.valueOf(upcomingOffset));
                nextURL.setParameter(PREVIOUS_PAGE_PARAM, String.valueOf(prevEventPage + 1));
            }

            if (atLeastOneUpcoming) {
                userDisplayPage += prevEventPage;
            } else {
                userDisplayPage = prevEventPage;
            }

        } else if (upcoming.size() < pageLimit) {
            // Fill up the rest of the page with previous events
            int upcomingOffset = pageLimit - upcoming.size();
            Listing previous = this.previousEventsSearch.execute(
                    request, collection, 1, upcomingOffset, 0);
            if (previous.size() > 0) {
                searchComponents.add(previous);
            }
            
            if (upcomingEventPage > 1) {
                prevURL = URL.create(request);
                prevURL.removeParameter(PREVIOUS_PAGE_PARAM);
                prevURL.removeParameter(PREV_BASE_OFFSET_PARAM);
                prevURL.setParameter(UPCOMING_PAGE_PARAM, String.valueOf(upcomingEventPage - 1));
            }
            
            if (previous.hasMoreResults()) {
                nextURL = URL.create(request);
                //nextURL.removeParameter(UPCOMING_PAGE_PARAM);
                nextURL.setParameter(PREV_BASE_OFFSET_PARAM, String.valueOf(upcomingOffset));
                nextURL.setParameter(PREVIOUS_PAGE_PARAM, String.valueOf(prevEventPage));
            }
        }
        
        model.put("searchComponents", searchComponents);
        model.put("page", userDisplayPage);

        cleanURL(nextURL);
        cleanURL(prevURL);

        model.put("nextURL", nextURL);
        model.put("prevURL", prevURL);

        Set<Object> alt = new HashSet<Object>();
        for (String contentType: this.alternativeRepresentations.keySet()) {
            try {
                Map<String, Object> m = new HashMap<String, Object>();
                Service service = this.alternativeRepresentations.get(contentType);
                URL url = service.constructURL(collection, principal);
                String title = service.getName();
                org.springframework.web.servlet.support.RequestContext rc = 
                new org.springframework.web.servlet.support.RequestContext(request);
                title = rc.getMessage(service.getName(), new Object[]{collection.getTitle()}, service.getName());
                
                m.put("title", title);
                m.put("url", url);
                m.put("contentType", contentType);
                
                alt.add(m);
            } catch (Throwable t) { }
        }
        model.put("alternativeRepresentations", alt);
        
        return new ModelAndView(this.viewName, model);
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
    public void setUpcomingEventsSearch(SearchComponent upcomingEventsSearch) {
        this.upcomingEventsSearch = upcomingEventsSearch;
    }

    @Required
    public void setPreviousEventsSearch(SearchComponent previousEventsSearch) {
        this.previousEventsSearch = previousEventsSearch;
    }

    @Required
    public void setViewName(String viewName) {
        this.viewName = viewName;
    }
    
    public void setAlternativeRepresentations(Map<String, Service> alternativeRepresentations) {
        this.alternativeRepresentations = alternativeRepresentations;
    }

    private int getPage(HttpServletRequest request, String parameter) {
        int page = 0;
        if (request.getParameter(parameter) != null) {
            try {
                page = Integer.parseInt(request.getParameter(parameter));
                if (page < 1) {
                    page = 1;
                }
            } catch (Throwable t) { }
        }

        if (page == 0) {
            page = 1;
        }
        return page;
    }
    
    private int getIntParameter(HttpServletRequest request, String name, int defaultValue) {
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

    private void cleanURL(URL url) {
        if (url != null) {
            url.setCollection(true);
            String param = url.getParameter(UPCOMING_PAGE_PARAM);
            if ("1".equals(param)) {
                url.removeParameter(UPCOMING_PAGE_PARAM);
            }
            param = url.getParameter(PREV_BASE_OFFSET_PARAM);
            if ("0".equals(param)) {
                url.removeParameter(PREV_BASE_OFFSET_PARAM);
            }
        }
    }

}

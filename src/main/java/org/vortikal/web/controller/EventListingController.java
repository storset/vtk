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
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Resource;
import org.vortikal.web.search.Listing;
import org.vortikal.web.search.SearchComponent;
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
public class EventListingController extends AbstractCollectionListingController {

    private SearchComponent upcomingEventsSearch;
    private SearchComponent previousEventsSearch;

    protected void runSearch(HttpServletRequest request, Resource collection,
    		Map<String, Object> model, int pageLimit) throws Exception {

    	int upcomingEventPage = getPage(request, UPCOMING_PAGE_PARAM);
        int prevEventPage = getPage(request, PREVIOUS_PAGE_PARAM);

        int userDisplayPage = upcomingEventPage;
        
        URL nextURL = null;
        URL prevURL = null;

        boolean atLeastOneUpcoming = this.upcomingEventsSearch.execute(request, collection, 1, 1, 0).size() > 0;

        List<Listing> results = new ArrayList<Listing>();
        Listing upcoming = null;
        if (request.getParameter(PREVIOUS_PAGE_PARAM) == null) {
            // Search upcoming events
            upcoming = this.upcomingEventsSearch.execute(
                    request, collection, upcomingEventPage, pageLimit, 0);
            if (upcoming.size() > 0) {
            	results.add(upcoming);
                if (upcomingEventPage > 1) {
                    prevURL = createURL(request, PREVIOUS_PAGE_PARAM, PREV_BASE_OFFSET_PARAM);
                    prevURL.setParameter(UPCOMING_PAGE_PARAM, String.valueOf(upcomingEventPage - 1));
                }
            }
            if (upcoming.hasMoreResults()) {
                nextURL = createURL(request, PREVIOUS_PAGE_PARAM, PREV_BASE_OFFSET_PARAM);
                nextURL.setParameter(UPCOMING_PAGE_PARAM, String.valueOf(upcomingEventPage + 1));
            } else if (upcoming.size() == pageLimit) {
                nextURL = URL.create(request);
                nextURL.setParameter(PREVIOUS_PAGE_PARAM, String.valueOf(upcomingEventPage));
            }
        }

        
        if (upcoming == null || upcoming.size() == 0) {
            // Searching only in previous events
            int upcomingOffset = getIntParameter(request, PREV_BASE_OFFSET_PARAM, 0);
            if (upcomingOffset > pageLimit) upcomingOffset = 0;
            Listing previous = this.previousEventsSearch.execute(
                    request, collection, prevEventPage, pageLimit, upcomingOffset);
            if (previous.size() > 0) {
            	results.add(previous);
            }
            
            if (prevEventPage > 1) {
                prevURL = URL.create(request);
                prevURL.setParameter(PREV_BASE_OFFSET_PARAM, String.valueOf(upcomingOffset));
                prevURL.setParameter(PREVIOUS_PAGE_PARAM, String.valueOf(prevEventPage - 1));

            } else if (prevEventPage == 1 && atLeastOneUpcoming) {
                prevURL = createURL(request, PREVIOUS_PAGE_PARAM, PREV_BASE_OFFSET_PARAM);
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
            	results.add(previous);
            }
            
            if (upcomingEventPage > 1) {
                prevURL = createURL(request, PREVIOUS_PAGE_PARAM, PREV_BASE_OFFSET_PARAM);
                prevURL.setParameter(UPCOMING_PAGE_PARAM, String.valueOf(upcomingEventPage - 1));
            }
            
            if (previous.hasMoreResults()) {
                nextURL = URL.create(request);
                nextURL.setParameter(PREV_BASE_OFFSET_PARAM, String.valueOf(upcomingOffset));
                nextURL.setParameter(PREVIOUS_PAGE_PARAM, String.valueOf(prevEventPage));
            }
        }
        
        model.put("searchComponents", results);
        model.put("page", userDisplayPage);
        model.put("hideNumberOfComments", getHideNumberOfComments(collection));

        cleanURL(nextURL);
        cleanURL(prevURL);
        
        if (nextURL != null) {
            nextURL.setParameter(USER_DISPLAY_PAGE, String.valueOf(userDisplayPage + 1));
        }
        if (prevURL != null && userDisplayPage > 2) {
            prevURL.setParameter(USER_DISPLAY_PAGE, String.valueOf(userDisplayPage -1));
        }

        model.put("nextURL", nextURL);
        model.put("prevURL", prevURL);
        model.put("currentDate", Calendar.getInstance().getTime());
        
    }
    
    @Required
    public void setUpcomingEventsSearch(SearchComponent upcomingEventsSearch) {
        this.upcomingEventsSearch = upcomingEventsSearch;
    }

    @Required
    public void setPreviousEventsSearch(SearchComponent previousEventsSearch) {
        this.previousEventsSearch = previousEventsSearch;
    }



}

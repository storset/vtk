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
package org.vortikal.web.display.collection.event;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Resource;
import org.vortikal.web.RequestContext;
import org.vortikal.web.display.collection.AbstractCollectionListingController;
import org.vortikal.web.display.listing.ListingPager;
import org.vortikal.web.display.listing.ListingPagingLink;
import org.vortikal.web.search.Listing;
import org.vortikal.web.service.Service;
import org.vortikal.web.service.URL;

/**
 * A controller for displaying event listings (a collection subtype).
 * 
 * XXX: Refactor this class. Should have a "front page" displaying a number of
 * upcoming and previous events, and a separate paging mode for each category.
 * The way it is done now (a single paging mode) is just painful.
 */
public class EventListingController extends AbstractCollectionListingController {

    protected EventListingSearcher searcher;

    @Override
    public void runSearch(HttpServletRequest request, Resource collection, Map<String, Object> model, int pageLimit)
            throws Exception {

        int upcomingEventPage = ListingPager.getPage(request, ListingPager.UPCOMING_PAGE_PARAM);
        int prevEventPage = ListingPager.getPage(request, ListingPager.PREVIOUS_PAGE_PARAM);

        int userDisplayPage = upcomingEventPage;

        int totalHits = 0;
        int totalUpcomingHits = 0;

        boolean atLeastOneUpcoming = this.searcher.searchUpcoming(request, collection, 1, 1, 0).size() > 0;

        List<Listing> results = new ArrayList<Listing>();
        Listing upcoming = null;
        if (request.getParameter(ListingPager.PREVIOUS_PAGE_PARAM) == null) {
            // Search upcoming events
            upcoming = this.searcher.searchUpcoming(request, collection, upcomingEventPage, pageLimit, 0);
            totalHits += upcoming.getTotalHits();
            totalUpcomingHits = upcoming.getTotalHits();
            if (upcoming.size() > 0) {
                results.add(upcoming);
            }
        } else {
            upcoming = this.searcher.searchUpcoming(request, collection, upcomingEventPage, 0, 0);
            totalHits += upcoming.getTotalHits();
            totalUpcomingHits = upcoming.getTotalHits();
            upcoming = null;
        }

        if (upcoming == null || upcoming.size() == 0) {
            // Searching only in previous events
            int upcomingOffset = getIntParameter(request, ListingPager.PREV_BASE_OFFSET_PARAM, 0);
            if (upcomingOffset > pageLimit)
                upcomingOffset = 0;
            Listing previous = this.searcher.searchPrevious(request, collection, prevEventPage, pageLimit,
                    upcomingOffset);
            totalHits += previous.getTotalHits();
            if (previous.size() > 0) {
                results.add(previous);
            }
            if (atLeastOneUpcoming) {
                userDisplayPage += prevEventPage;
            } else {
                userDisplayPage = prevEventPage;
            }
        } else if (upcoming.size() < pageLimit) {
            // Fill up the rest of the page with previous events
            int upcomingOffset = pageLimit - upcoming.size();
            Listing previous = this.searcher.searchPrevious(request, collection, 1, upcomingOffset, 0);
            totalHits += previous.getTotalHits();
            if (previous.size() > 0) {
                results.add(previous);
            }
        } else {
            Listing previous = this.searcher.searchPrevious(request, collection, 1, 0, 0);
            totalHits += previous.getTotalHits();
            previous = null;
        }
        Service service = RequestContext.getRequestContext().getService();
        URL baseURL = service.constructURL(RequestContext.getRequestContext().getResourceURI());

        List<ListingPagingLink> urls = ListingPager.generatePageThroughUrls(totalHits, pageLimit, totalUpcomingHits,
                baseURL, true, userDisplayPage);
        model.put(MODEL_KEY_SEARCH_COMPONENTS, results);
        model.put(MODEL_KEY_PAGE, userDisplayPage);
        model.put(MODEL_KEY_PAGE_THROUGH_URLS, urls);
        model.put(MODEL_KEY_HIDE_NUMBER_OF_COMMENTS, getHideNumberOfComments(collection));
        model.put("currentDate", Calendar.getInstance().getTime());
        model.put(EventListingHelper.DISPLAY_LISTING_ICAL_LINK, atLeastOneUpcoming);

    }

    @Required
    public void setSearcher(EventListingSearcher searcher) {
        this.searcher = searcher;
    }

}

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
package org.vortikal.web.display.collection.event;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Resource;
import org.vortikal.web.display.collection.event.EventListingSearcher.GroupedEvents;
import org.vortikal.web.display.listing.ListingPager;
import org.vortikal.web.search.Listing;

public class EventCalendarListingController extends EventListingController {

    protected EventListingHelper helper;

    private int daysAhead;
    // max 5 further upcoming events on 1 page
    private final int furtherUpcomingLimit = 5;

    @Override
    public void runSearch(HttpServletRequest request, Resource collection, Map<String, Object> model, int pageLimit)
            throws Exception {

        int page = ListingPager.getPage(request, ListingPager.UPCOMING_PAGE_PARAM);
        model.put(MODEL_KEY_PAGE, page);

        List<GroupedEvents> groupedByDayEvents = this.searcher.searchGroupedByDayEvents(request, collection);
        model.put("groupedByDayEvents", groupedByDayEvents);
        String groupedByDayTitle = this.helper.getLocalizedTitle(request, "eventListing.groupedEvents",
                new Object[] { this.daysAhead });
        model.put("groupedEventsTitle", groupedByDayTitle);

        Listing furtherUpcoming = this.searcher.searchFurtherUpcoming(request, collection, this.furtherUpcomingLimit);
        model.put("furtherUpcoming", furtherUpcoming);
        String titleKey = groupedByDayEvents.size() == 0 ? "eventListing.allupcoming"
                : "eventListing.furtherUpcomingEvents";
        String furtherUpcomingTitle = this.helper.getEventTypeTitle(request, collection, titleKey, false);
        model.put("furtherUpcomingTitle", furtherUpcomingTitle);

        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DATE, 1);

        model.put("tomorrow", tomorrow.getTime());
        model.put("today", new Date());

    }

    @Required
    public void setHelper(EventListingHelper helper) {
        this.helper = helper;
    }

    @Required
    public void setDaysAhead(int daysAhead) {
        this.daysAhead = daysAhead;
    }

}

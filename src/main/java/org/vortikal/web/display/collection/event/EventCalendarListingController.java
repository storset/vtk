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

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Resource;
import org.vortikal.web.display.collection.event.EventListingHelper.SpecificDateSearchType;
import org.vortikal.web.display.collection.event.EventListingSearcher.GroupedEvents;
import org.vortikal.web.search.Listing;
import org.vortikal.web.service.Service;
import org.vortikal.web.service.URL;

public class EventCalendarListingController extends EventListingController {

    private EventListingHelper helper;

    private final int daysAhead = 5; // 5 days ahead
    private final int furtherUpcomingPageLimit = 3; // 3 events on 1 page

    @Override
    public void runSearch(HttpServletRequest request, Resource collection, Map<String, Object> model, int pageLimit)
            throws Exception {

        if (!this.searchSpecificDate(request, collection, model)) {

            String viewType = request.getParameter(EventListingHelper.REQUEST_PARAMETER_VIEW);
            if (viewType != null && !"".equals(viewType.trim())) {

                if (EventListingHelper.VIEW_TYPE_ALL_UPCOMING.equals(viewType)) {
                    Listing upcoming = this.searcher.searchUpcoming(request, collection, 1, this.defaultPageLimit, 0);
                    model.put("allUpcoming", upcoming);
                    String overrideDefaultTitle = this.helper.getEventTypeTitle(request, collection,
                            "eventListing.allupcoming", false);
                    model.put("overrideDefaultTitle", overrideDefaultTitle);
                } else if (EventListingHelper.VIEW_TYPE_ALL_PREVIOUS.equals(viewType)) {
                    Listing previuos = this.searcher.searchPrevious(request, collection, 1, this.defaultPageLimit, 0);
                    model.put("allPrevious", previuos);
                    String overrideDefaultTitle = this.helper.getEventTypeTitle(request, collection,
                            "eventListing.allprevious", false);
                    model.put("overrideDefaultTitle", overrideDefaultTitle);
                }

            } else {

                List<GroupedEvents> groupedByDayEvents = this.searcher.searchGroupedByDayEvents(request, collection,
                        this.daysAhead);
                model.put("groupedByDayEvents", groupedByDayEvents);
                String groupedByDayTitle = this.helper.getLocalizedTitle(request, "eventListing.groupedEvents",
                        new Object[] { this.daysAhead });
                model.put("groupedEventsTitle", groupedByDayTitle);

                Listing furtherUpcoming = this.searcher.searchFurtherUpcoming(request, collection, this.daysAhead,
                        this.furtherUpcomingPageLimit);
                model.put("furtherUpcoming", furtherUpcoming);
                String furtherUpcomingTitle = this.helper.getEventTypeTitle(request, collection,
                        "eventListing.furtherUpcomingEvents", false);
                model.put("furtherUpcomingTitle", furtherUpcomingTitle);

            }
        }

        URL viewAllUpcomingURL = createURL(collection, EventListingHelper.REQUEST_PARAMETER_VIEW,
                EventListingHelper.VIEW_TYPE_ALL_UPCOMING);
        model.put("viewAllUpcomingURL", viewAllUpcomingURL);
        model.put("viewAllUpcomingTitle", this.helper.getEventTypeTitle(request, collection,
                "eventListing.viewAllUpcoming", false));
        URL viewAllPreviousURL = createURL(collection, EventListingHelper.REQUEST_PARAMETER_VIEW,
                EventListingHelper.VIEW_TYPE_ALL_PREVIOUS);
        model.put("viewAllPreviousURL", viewAllPreviousURL);
        model.put("viewAllPreviousTitle", this.helper.getEventTypeTitle(request, collection,
                "eventListing.viewAllPrevious", false));
        model.put("today", new Date());

    }

    private boolean searchSpecificDate(HttpServletRequest request, Resource collection, Map<String, Object> model)
            throws Exception {

        Date date = this.helper.getSpecificSearchDate(request);
        if (date != null) {
            SpecificDateSearchType searchType = this.helper.getSpecificDateSearchType(request);
            Listing specificDateEvents = this.searcher.searchSpecificDate(request, collection, date, searchType);

            model.put("specificDate", Boolean.TRUE);
            model.put("specificDateEvents", specificDateEvents);
            String messageKey = searchType == SpecificDateSearchType.Day ? "eventListing.specificDayEvent"
                    : "eventListing.specificDateEvent";
            String specificDateEventsTitle = this.helper.getEventTypeTitle(request, collection, searchType, date,
                    messageKey, true);
            model.put("specificDateEventsTitle", specificDateEventsTitle);
            model.put("noPlannedEventsMsg", this.helper.getEventTypeTitle(request, collection, searchType, date,
                    "eventListing.noPlannedEvents", false));
        }

        return false;
    }

    private URL createURL(Resource collection, String parameterKey, String parameterValue) {
        Service service = org.vortikal.web.RequestContext.getRequestContext().getService();
        URL baseULR = service.constructURL(collection.getURI());
        URL url = URL.create(baseULR);
        url.addParameter(parameterKey, parameterValue);
        return url;
    }

    @Required
    public void setHelper(EventListingHelper helper) {
        this.helper = helper;
    }

}

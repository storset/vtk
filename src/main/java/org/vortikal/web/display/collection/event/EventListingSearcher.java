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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Resource;
import org.vortikal.web.search.Listing;
import org.vortikal.web.search.ProcessedQuerySearchComponenet;
import org.vortikal.web.search.SearchComponent;

public class EventListingSearcher {

    private SearchComponent upcomingEventsSearch;
    private SearchComponent previousEventsSearch;
    private ProcessedQuerySearchComponenet processedQuerySearchComponent;
    private String groupedEventSearchString;
    private String furtherUpcomingSearchString;
    private String specificDateEventSearchString;

    public enum SpecificDateSearchType {
        Day, Month, Year;
    }

    public Listing searchUpcoming(HttpServletRequest request, Resource collection, int upcomingEventPage,
            int pageLimit, int offset) throws Exception {
        return this.upcomingEventsSearch.execute(request, collection, upcomingEventPage, pageLimit, 0);
    }

    public Listing searchPrevious(HttpServletRequest request, Resource collection, int upcomingEventPage,
            int pageLimit, int offset) throws Exception {
        return this.previousEventsSearch.execute(request, collection, upcomingEventPage, pageLimit, 0);
    }

    public List<GroupedEvents> searchGroupedByDayEvents(HttpServletRequest request, Resource collection, int daysAhead)
            throws Exception {
        List<GroupedEvents> groupedByDayEvents = this.getGroupedByDayEvents(request, collection, daysAhead);
        return groupedByDayEvents;
    }

    public Listing searchFurtherUpcoming(HttpServletRequest request, Resource collection, int daysAhead,
            int furtherUpcomingPageLimit) throws Exception {
        Listing furtherUpcoming = this.getFurtherUpcomingEvents(request, collection, daysAhead,
                furtherUpcomingPageLimit);
        return furtherUpcoming;
    }

    public Listing searchSpecificDate(HttpServletRequest request, Resource collection, Date date,
            SpecificDateSearchType searchType) throws Exception {
        this.processedQuerySearchComponent.setProcessedQuery(getProcessedSpecificDayQueryString(date, searchType));
        Listing specificDateEventListing = this.processedQuerySearchComponent.execute(request, collection, 1, 100, 0);
        return specificDateEventListing;
    }

    private List<GroupedEvents> getGroupedByDayEvents(HttpServletRequest request, Resource collection, int daysAhead)
            throws Exception {
        List<GroupedEvents> groupedByDayEvents = new ArrayList<GroupedEvents>();
        for (int i = 0; i < daysAhead; i++) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_MONTH, i);
            this.processedQuerySearchComponent.setProcessedQuery(this.getProcessedGroupedByDayQueryString(i));
            Listing result = this.processedQuerySearchComponent.execute(request, collection, 1, 100, 0);
            if (result.size() > 0) {
                groupedByDayEvents.add(new GroupedEvents(cal.getTime(), result));
            }
        }
        return groupedByDayEvents;
    }

    private Listing getFurtherUpcomingEvents(HttpServletRequest request, Resource collection, int daysAhead,
            int furtherUpcomingPageLimit) throws Exception {
        this.processedQuerySearchComponent.setProcessedQuery(this.getProcessedFurtherUpcomingQueryString(daysAhead));
        Listing furtherUpcomingEvents = this.processedQuerySearchComponent.execute(request, collection, 1,
                furtherUpcomingPageLimit, 0);
        return furtherUpcomingEvents;
    }

    private String getProcessedGroupedByDayQueryString(int i) {
        String processedQueryString = this.groupedEventSearchString.replace("[1]", String.valueOf(i)).replace("[2]",
                String.valueOf(i + 1));
        return processedQueryString;
    }

    private String getProcessedFurtherUpcomingQueryString(int daysAhead) {
        String processedQueryString = this.furtherUpcomingSearchString.replace("[1]", String.valueOf(daysAhead));
        return processedQueryString;
    }

    private String getProcessedSpecificDayQueryString(Date date, SpecificDateSearchType searchType) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        switch (searchType) {
        case Day:
            cal.add(Calendar.DAY_OF_MONTH, 1);
            break;
        case Month:
            cal.add(Calendar.MONTH, 1);
            break;
        case Year:
            cal.add(Calendar.YEAR, 1);
        default:
            break;
        }
        String processedQueryString = this.specificDateEventSearchString.replace("[1]", String.valueOf(date.getTime()))
                .replace("[2]", String.valueOf(cal.getTime().getTime()));
        return processedQueryString;
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
    public void setProcessedQuerySearchComponent(ProcessedQuerySearchComponenet processedQuerySearchComponent) {
        this.processedQuerySearchComponent = processedQuerySearchComponent;
    }

    @Required
    public void setGroupedEventSearchString(String groupedEventSearchString) {
        this.groupedEventSearchString = groupedEventSearchString;
    }

    @Required
    public void setFurtherUpcomingSearchString(String furtherUpcomingSearchString) {
        this.furtherUpcomingSearchString = furtherUpcomingSearchString;
    }

    @Required
    public void setSpecificDateEventSearchString(String specificDateEventSearchString) {
        this.specificDateEventSearchString = specificDateEventSearchString;
    }

    public class GroupedEvents {

        private Date day;
        private Listing events;

        public GroupedEvents(Date day, Listing events) {
            this.day = day;
            this.events = events;
        }

        public Date getDay() {
            return day;
        }

        public Listing getEvents() {
            return events;
        }

    }

}

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
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Resource;
import org.vortikal.web.search.Listing;
import org.vortikal.web.search.SearchComponent;

public class EventListingSearcher {

    private SearchComponent upcomingEventsSearch;
    private SearchComponent previousEventsSearch;
    private SearchComponent groupedByDayEventSearchComponent;
    private SearchComponent furtherUpcomingSearchComponent;
    private SearchComponent specificDateEventSearchComponent;

    public Listing searchUpcoming(HttpServletRequest request, Resource collection, int upcomingEventPage,
            int pageLimit, int offset) throws Exception {
        return this.upcomingEventsSearch.execute(request, collection, upcomingEventPage, pageLimit, 0);
    }

    public Listing searchPrevious(HttpServletRequest request, Resource collection, int upcomingEventPage,
            int pageLimit, int offset) throws Exception {
        return this.previousEventsSearch.execute(request, collection, upcomingEventPage, pageLimit, 0);
    }

    public List<GroupedEvents> searchGroupedByDayEvents(HttpServletRequest request, Resource collection)
            throws Exception {
        List<GroupedEvents> groupedByDayEvents = new ArrayList<GroupedEvents>();
        Listing result = this.groupedByDayEventSearchComponent.execute(request, collection, 1, 100, 0);
        if (result.size() > 0) {
            // XXX group and add to resultlist
            // groupedByDayEvents.add(new GroupedEvents(cal.getTime(), result));
        }
        return groupedByDayEvents;
    }

    public Listing searchFurtherUpcoming(HttpServletRequest request, Resource collection, int furtherUpcomingPageLimit)
            throws Exception {
        Listing furtherUpcomingEvents = this.furtherUpcomingSearchComponent.execute(request, collection, 1,
                furtherUpcomingPageLimit, 0);
        return furtherUpcomingEvents;
    }

    public Listing searchSpecificDate(HttpServletRequest request, Resource collection, int pageLimit, int page)
            throws Exception {
        return this.specificDateEventSearchComponent.execute(request, collection, page, pageLimit, 0);
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
    public void setGroupedByDayEventSearchComponent(SearchComponent groupedByDayEventSearchComponent) {
        this.groupedByDayEventSearchComponent = groupedByDayEventSearchComponent;
    }

    @Required
    public void setFurtherUpcomingSearchComponent(SearchComponent furtherUpcomingSearchComponent) {
        this.furtherUpcomingSearchComponent = furtherUpcomingSearchComponent;
    }

    @Required
    public void setSpecificDateEventSearchComponent(SearchComponent specificDateEventSearchComponent) {
        this.specificDateEventSearchComponent = specificDateEventSearchComponent;
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

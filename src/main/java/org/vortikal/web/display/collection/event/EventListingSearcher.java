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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.Resource;
import org.vortikal.web.search.Listing;
import org.vortikal.web.search.SearchComponent;
import org.vortikal.web.service.URL;

public class EventListingSearcher {

    private SearchComponent upcomingEventsSearch;
    private SearchComponent previousEventsSearch;
    private SearchComponent groupedByDayEventSearchComponent;
    private SearchComponent furtherUpcomingSearchComponent;
    private SearchComponent specificDateEventSearchComponent;
    private int daysAhead;

    public Listing searchUpcoming(HttpServletRequest request, Resource collection, int upcomingEventPage,
            int pageLimit, int offset) throws Exception {
        return this.upcomingEventsSearch.execute(request, collection, upcomingEventPage, pageLimit, offset);
    }

    public Listing searchPrevious(HttpServletRequest request, Resource collection, int upcomingEventPage,
            int pageLimit, int offset) throws Exception {
        return this.previousEventsSearch.execute(request, collection, upcomingEventPage, pageLimit, offset);
    }

    public List<GroupedEvents> searchGroupedByDayEvents(HttpServletRequest request, Resource collection)
            throws Exception {
        List<GroupedEvents> groupedByDayEvents = new ArrayList<GroupedEvents>();
        Listing result = this.groupedByDayEventSearchComponent.execute(request, collection, 1, 100, 0);
        if (result.size() > 0) {
            List<PropertySet> allEvents = result.getFiles();
            for (int i = 0; i <= this.daysAhead; i++) {
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DAY_OF_MONTH, i);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                Listing subListing = new Listing(result.getResource(), result.getTitle(), result.getName(), result
                        .getOffset());
                subListing.setDisplayPropDefs(result.getDisplayPropDefs());
                List<PropertySet> events = new ArrayList<PropertySet>();
                Map<String, URL> urls = new HashMap<String, URL>();
                for (PropertySet ps : allEvents) {
                    if (this.isWithinDaysAhead(cal.getTime(), ps)) {
                        events.add(ps);
                        String urlString = ps.getURI().toString();
                        urls.put(urlString, result.getUrls().get(urlString));
                    }
                }
                if (events.size() > 0) {
                    subListing.setFiles(events);
                    subListing.setUrls(urls);
                    groupedByDayEvents.add(new GroupedEvents(cal.getTime(), subListing));
                }
            }
        }
        return groupedByDayEvents;
    }

    private boolean isWithinDaysAhead(Date time, PropertySet ps) {
        Property sdProp = this.getProperty(ps, "start-date");
        Date sd = sdProp.getDateValue();
        Calendar cal = Calendar.getInstance();
        cal.setTime(time);
        cal.add(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        boolean isToday = sd.equals(time) || (sd.after(time) && sd.before(cal.getTime()));
        Property edProp = this.getProperty(ps, "end-date");
        if (edProp == null) {
            return isToday;
        }
        Date ed = edProp.getDateValue();
        return isToday || (sd.before(time) && (ed.after(time) || ed.equals(time)));
    }

    private Property getProperty(PropertySet ps, String key) {
        Property prop = ps.getProperty(Namespace.STRUCTURED_RESOURCE_NAMESPACE, key);
        if (prop == null) {
            prop = ps.getProperty(Namespace.DEFAULT_NAMESPACE, key);
        }
        return prop;
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

    @Required
    public void setDaysAhead(int daysAhead) {
        this.daysAhead = daysAhead;
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

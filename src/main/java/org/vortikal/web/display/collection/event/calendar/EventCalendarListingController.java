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
package org.vortikal.web.display.collection.event.calendar;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.Resource;
import org.vortikal.web.display.collection.event.EventListingController;
import org.vortikal.web.display.collection.event.EventListingHelper;
import org.vortikal.web.display.listing.ListingPager;
import org.vortikal.web.search.Listing;
import org.vortikal.web.search.SearchComponent;
import org.vortikal.web.service.Service;
import org.vortikal.web.service.URL;

public class EventCalendarListingController extends EventListingController {

    protected EventListingHelper helper;

    private int daysAhead;
    private Service viewAllUpcomingService;
    private Service viewAllPreviousService;
    private SearchComponent calendarSearchComponent;

    // max 5 further upcoming events on 1 page
    private final int furtherUpcomingLimit = 5;

    @Override
    public void runSearch(HttpServletRequest request, Resource collection, Map<String, Object> model, int pageLimit)
            throws Exception {

        Listing result = this.calendarSearchComponent.execute(request, collection, 1, 500, 0);

        Calendar currentMonth = this.helper.getCurrentMonth();
        DateTimeFormatter dtf = DateTimeFormat.forPattern("YYYY-MM");
        String requestedDate = dtf.print(currentMonth.getTimeInMillis());
        model.put("requestedDate", requestedDate);

        // Days with events, with localized calendar titles for clickable days.
        String eventDates = this.helper.getCalendarWidgetEventDates(result, currentMonth);
        model.put("allowedDates", eventDates);
        this.helper.setCalendarTitles(request, collection, model);

        int page = ListingPager.getPage(request, ListingPager.UPCOMING_PAGE_PARAM);
        model.put(MODEL_KEY_PAGE, page);

        List<GroupedEvents> groupedByDayEvents = this.groupEvents(result);
        model.put("groupedByDayEvents", groupedByDayEvents);
        String groupedByDayTitle = this.helper.getLocalizedTitle(request, "eventListing.groupedEvents",
                new Object[] { this.daysAhead });
        model.put("groupedEventsTitle", groupedByDayTitle);

        Listing furtherUpcoming = this.getFurtherUpcoming(result, groupedByDayEvents);
        model.put("furtherUpcoming", furtherUpcoming);
        String titleKey = groupedByDayEvents.size() == 0 ? "eventListing.allupcoming"
                : "eventListing.furtherUpcomingEvents";
        String furtherUpcomingTitle = this.helper.getEventTypeTitle(request, collection, titleKey, false);
        model.put("furtherUpcomingTitle", furtherUpcomingTitle);

        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DATE, 1);

        model.put("tomorrow", tomorrow.getTime());
        model.put("today", new Date());

        if (groupedByDayEvents.size() > 0 || (furtherUpcoming != null && furtherUpcoming.size() > 0)) {
            model.put(EventListingHelper.DISPLAY_LISTING_ICAL_LINK, true);
        }

        URL viewAllUpcomingURL = this.viewAllUpcomingService.constructURL(collection);
        model.put("viewAllUpcomingURL", viewAllUpcomingURL);
        model.put("viewAllUpcomingTitle",
                this.helper.getEventTypeTitle(request, collection, "eventListing.viewAllUpcoming", false));

        URL viewAllPreviousURL = this.viewAllPreviousService.constructURL(collection);
        model.put("viewAllPreviousURL", viewAllPreviousURL);
        model.put("viewAllPreviousTitle",
                this.helper.getEventTypeTitle(request, collection, "eventListing.viewAllPrevious", false));

    }

    private List<GroupedEvents> groupEvents(Listing result) {
        List<GroupedEvents> groupedByDayEvents = new ArrayList<GroupedEvents>();
        if (result != null && result.size() > 0) {
            List<PropertySet> allEvents = result.getFiles();
            for (int i = 0; i < this.daysAhead; i++) {
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DAY_OF_MONTH, i);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                Listing subListing = new Listing(result.getResource(), null, result.getName(), result.getOffset());
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
        if (sdProp == null) {
            sdProp = this.getProperty(ps, "publish-date");
        }
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

    private Listing getFurtherUpcoming(Listing result, List<GroupedEvents> groupedEvents) {
        if (result != null && result.size() > 0) {

            // Default is now, in case there are no grouped by events
            Date lastGroupedByDay = Calendar.getInstance().getTime();
            Set<PropertySet> allGroupedByEvents = new HashSet<PropertySet>();
            if (groupedEvents != null && groupedEvents.size() > 0) {
                for (GroupedEvents ge : groupedEvents) {
                    allGroupedByEvents.addAll(ge.getEvents().getFiles());
                    Date groupedByDay = ge.getDay();
                    if (groupedByDay.after(lastGroupedByDay)) {
                        lastGroupedByDay = groupedByDay;
                    }
                }
            }

            List<PropertySet> filteredEventsWithoutGrouped = new ArrayList<PropertySet>(result.getFiles());
            filteredEventsWithoutGrouped.removeAll(allGroupedByEvents);
            if (filteredEventsWithoutGrouped.size() == 0) {
                return null;
            }

            // No need to sort, already sorted as we want and we have removed
            // grouped events. Now get "furtherUpcomingLimit" further events
            // "daysAhead" from now

            List<PropertySet> furtherEvents = new ArrayList<PropertySet>();
            Map<String, URL> urls = new HashMap<String, URL>();
            for (PropertySet ps : filteredEventsWithoutGrouped) {
                if (this.isWithinFurtherUpcoming(ps, lastGroupedByDay)) {
                    furtherEvents.add(ps);
                    String urlString = ps.getURI().toString();
                    urls.put(urlString, result.getUrls().get(urlString));
                    if (furtherEvents.size() == this.furtherUpcomingLimit) {
                        break;
                    }
                }
            }

            if (furtherEvents.size() > 0) {
                Listing furtherUpcoming = new Listing(result.getResource(), null, result.getName(), result.getOffset());
                furtherUpcoming.setFiles(furtherEvents);
                furtherUpcoming.setUrls(urls);
                return furtherUpcoming;
            }

        }
        return null;
    }

    private boolean isWithinFurtherUpcoming(PropertySet ps, Date lastGroupedByDay) {
        Property sdProp = this.getProperty(ps, "start-date");
        Date eventStartDate = null;
        if (sdProp != null) {
            eventStartDate = sdProp.getDateValue();
        }
        if (eventStartDate != null && eventStartDate.after(lastGroupedByDay)) {
            return true;
        }

        // XXX Check end date, only if start date was null

        return false;
    }

    @Required
    public void setHelper(EventListingHelper helper) {
        this.helper = helper;
    }

    @Required
    public void setDaysAhead(int daysAhead) {
        this.daysAhead = daysAhead;
    }

    @Required
    public void setViewAllUpcomingService(Service viewAllUpcomingService) {
        this.viewAllUpcomingService = viewAllUpcomingService;
    }

    @Required
    public void setViewAllPreviousService(Service viewAllPreviousService) {
        this.viewAllPreviousService = viewAllPreviousService;
    }

    @Required
    public void setCalendarSearchComponent(SearchComponent calendarSearchComponent) {
        this.calendarSearchComponent = calendarSearchComponent;
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

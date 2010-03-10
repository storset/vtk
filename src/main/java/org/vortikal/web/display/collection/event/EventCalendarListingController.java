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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.servlet.support.RequestContext;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.DateValueFormatter;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.web.display.collection.event.EventListingSearcher.GroupedEvents;
import org.vortikal.web.display.collection.event.EventListingSearcher.SpecificDateSearchType;
import org.vortikal.web.search.Listing;
import org.vortikal.web.service.Service;
import org.vortikal.web.service.URL;
import org.vortikal.web.servlet.ResourceAwareLocaleResolver;

import com.ibm.icu.util.Calendar;

public class EventCalendarListingController extends EventListingController {

    public static final String REQUEST_PARAMETER_DATE = "date";
    public static final String REQUEST_PARAMETER_VIEW = "view";
    public static final String VIEW_TYPE_ALL_UPCOMING = "allupcoming";
    public static final String VIEW_TYPE_ALL_PREVIOUS = "allprevious";

    private DateValueFormatter dateValueFormatter;
    private ResourceAwareLocaleResolver localeResolver;

    private final int daysAhead = 5; // 5 days ahead
    private final int furtherUpcomingPageLimit = 3; // 3 events on 1 page

    @Override
    public void runSearch(HttpServletRequest request, Resource collection, Map<String, Object> model, int pageLimit)
            throws Exception {

        if (!this.searchSpecificDate(request, collection, model)) {

            String viewType = request.getParameter(REQUEST_PARAMETER_VIEW);
            if (viewType != null && !"".equals(viewType.trim())) {

                if (VIEW_TYPE_ALL_UPCOMING.equals(viewType)) {
                    Listing upcoming = this.searcher.searchUpcoming(request, collection, 1, this.defaultPageLimit, 0);
                    model.put("allUpcoming", upcoming);
                } else if (VIEW_TYPE_ALL_PREVIOUS.equals(viewType)) {
                    Listing previuos = this.searcher.searchPrevious(request, collection, 1, this.defaultPageLimit, 0);
                    model.put("allPrevious", previuos);
                }

            } else {

                List<GroupedEvents> groupedByDayEvents = this.searcher.searchGroupedByDayEvents(request, collection,
                        this.daysAhead);
                model.put("groupedByDayEvents", groupedByDayEvents);
                String groupedByDayTitle = getTitle(request, "eventListing.groupedEvents",
                        new Object[] { this.daysAhead });
                model.put("groupedEventsTitle", groupedByDayTitle);

                Listing furtherUpcoming = this.searcher.searchFurtherUpcoming(request, collection, this.daysAhead,
                        this.furtherUpcomingPageLimit);
                model.put("furtherUpcoming", furtherUpcoming);
                String furtherUpcomingTitle = getTitle(request, "eventListing.furtherUpcomingEvents", null);
                model.put("furtherUpcomingTitle", furtherUpcomingTitle);

            }
        }

        URL viewAllUpcomingURL = createURL(collection, REQUEST_PARAMETER_VIEW, VIEW_TYPE_ALL_UPCOMING);
        model.put("viewAllUpcomingURL", viewAllUpcomingURL);
        URL viewAllPreviousURL = createURL(collection, REQUEST_PARAMETER_VIEW, VIEW_TYPE_ALL_PREVIOUS);
        model.put("viewAllPreviousURL", viewAllPreviousURL);

    }

    private boolean searchSpecificDate(HttpServletRequest request, Resource collection, Map<String, Object> model)
            throws Exception {
        String specificDate = request.getParameter(REQUEST_PARAMETER_DATE);
        if (specificDate != null && !"".equals(specificDate.trim())) {
            SimpleDateFormat sdf = null;
            SpecificDateSearchType searchType = null;
            if (specificDate.matches("\\d{4}-\\d{2}-\\d{2}")) {
                sdf = new SimpleDateFormat("yyyy-MM-dd");
                searchType = SpecificDateSearchType.Day;
            } else if (specificDate.matches("\\d{4}-\\d{2}")) {
                sdf = new SimpleDateFormat("yyyy-MM");
                searchType = SpecificDateSearchType.Month;
            } else if (specificDate.matches("\\d{4}")) {
                sdf = new SimpleDateFormat("yyyy");
                searchType = SpecificDateSearchType.Year;
            } else {
                return false;
            }
            try {
                Date date = sdf.parse(specificDate);
                Listing specificDateEvents = this.searcher.searchSpecificDate(request, collection, date, searchType);

                model.put("specificDate", Boolean.TRUE);

                String titleDate = specificDate;
                String titleKey = "eventListing.upcomingSpecificDateEvent";
                Calendar now = Calendar.getInstance();
                if (searchType != SpecificDateSearchType.Year) {
                    Locale locale = this.localeResolver.resolveResourceLocale(request, collection.getURI());
                    Calendar requestedCal = Calendar.getInstance();
                    requestedCal.setTime(date);
                    String format = "short";
                    if (searchType == SpecificDateSearchType.Day) {
                        if (requestedCal.get(Calendar.DAY_OF_MONTH) < now.get(Calendar.DAY_OF_MONTH)) {
                            titleKey = "eventListing.previousSpecificDateEvent";
                        }
                    } else if (searchType == SpecificDateSearchType.Month) {
                        format = "month-year";
                        if (requestedCal.get(Calendar.MONTH) < now.get(Calendar.MONTH)) {
                            titleKey = "eventListing.previousSpecificDateEvent";
                        }
                    }
                    titleDate = this.dateValueFormatter.valueToString(new Value(date, false), format, locale);
                } else {
                    int requestedYear = Integer.parseInt(specificDate);
                    if (requestedYear < now.get(Calendar.YEAR)) {
                        titleKey = "eventListing.previousSpecificDateEvent";
                    }
                }

                if (specificDateEvents.size() > 0) {
                    model.put("specificDateEvents", specificDateEvents);
                    model.put("specificDateEventsTitle", getTitle(request, titleKey, new Object[] { titleDate }));
                } else {
                    model.put("noPlannedEventsMsg", getTitle(request, "eventListing.noPlannedEvents",
                            new Object[] { titleDate }));
                }

            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    private String getTitle(HttpServletRequest request, String key, Object[] params) {
        RequestContext springRequestContext = new RequestContext(request);
        if (params != null) {
            return springRequestContext.getMessage(key, params);
        }
        return springRequestContext.getMessage(key);
    }

    private URL createURL(Resource collection, String parameterKey, String parameterValue) {
        Service service = org.vortikal.web.RequestContext.getRequestContext().getService();
        URL baseULR = service.constructURL(collection.getURI());
        URL url = URL.create(baseULR);
        url.addParameter(parameterKey, parameterValue);
        return url;
    }

    @Required
    public void setDateValueFormatter(DateValueFormatter dateValueFormatter) {
        this.dateValueFormatter = dateValueFormatter;
    }

    @Required
    public void setLocaleResolver(ResourceAwareLocaleResolver localeResolver) {
        this.localeResolver = localeResolver;
    }

}

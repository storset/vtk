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
package org.vortikal.web.referencedata.provider;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.ResourceTypeTree;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.web.RequestContext;
import org.vortikal.web.display.collection.event.EventListingHelper;
import org.vortikal.web.display.collection.event.EventListingHelper.SpecificDateSearchType;
import org.vortikal.web.display.collection.event.EventListingSearcher;
import org.vortikal.web.referencedata.ReferenceDataProvider;
import org.vortikal.web.search.Listing;
import org.vortikal.web.service.Service;
import org.vortikal.web.service.URL;

public class EventCalenderContentProvider implements ReferenceDataProvider {

    private EventListingHelper helper;
    private EventListingSearcher searcher;
    private ResourceTypeTree resourceTypeTree;
    private PropertyTypeDefinition displayTypePropDef;
    private String startDatePropDefPointer;
    private String endDatePropDefPointer;
    private Service viewAllUpcomingService;
    private Service viewAllPreviousService;

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void referenceData(Map model, HttpServletRequest request) throws Exception {

        SpecificDateSearchType searchType = this.helper.getSpecificDateSearchType(request);
        if (searchType != null) {
            // valid date on request
            String requestedDate = request.getParameter(EventListingHelper.REQUEST_PARAMETER_DATE);
            model.put("requestedDate", requestedDate);
        }
        RequestContext requestContext = RequestContext.getRequestContext();
        String token = requestContext.getSecurityToken();
        Path resourceURI = requestContext.getResourceURI();
        Repository repository = requestContext.getRepository();
        Resource resource = repository.retrieve(token, resourceURI, true);

        setAllowedDates(request, resource, model);
        setCalendarTitles(request, resource, model);

        URL viewAllUpcomingURL = this.viewAllUpcomingService.constructURL(resource);
        model.put("viewAllUpcomingURL", viewAllUpcomingURL);
        model.put("viewAllUpcomingTitle",
                this.helper.getEventTypeTitle(request, resource, "eventListing.viewAllUpcoming", false));

        URL viewAllPreviousURL = this.viewAllPreviousService.constructURL(resource);
        model.put("viewAllPreviousURL", viewAllPreviousURL);
        model.put("viewAllPreviousTitle",
                this.helper.getEventTypeTitle(request, resource, "eventListing.viewAllPrevious", false));

    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void setAllowedDates(HttpServletRequest request, Resource resource, Map model) throws Exception {
        Property displayTypeProp = resource.getProperty(this.displayTypePropDef);
        if (displayTypeProp != null && "calendar".equals(displayTypeProp.getStringValue())) {

            PropertyTypeDefinition startDatePropDef = this.resourceTypeTree
                    .getPropertyDefinitionByPointer(this.startDatePropDefPointer);
            PropertyTypeDefinition endDatePropDef = this.resourceTypeTree
                    .getPropertyDefinitionByPointer(this.endDatePropDefPointer);

            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);

            String dateString = request.getParameter(EventListingHelper.REQUEST_PARAMETER_DATE);
            if (dateString != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM");
                try {
                    Date requestedMonth = sdf.parse(dateString);
                    cal.setTime(requestedMonth);
                } catch (ParseException e) {
                    // Ignore, show current month
                }
            }

            Set<String> eventDatesList = new HashSet<String>();
            SimpleDateFormat eventDateFormat = new SimpleDateFormat("yyyy-M-d");
            Listing plannedEvents = this.searcher.searchSpecificDate(request, resource, 500, 1);
            for (PropertySet propSet : plannedEvents.getFiles()) {
                Property startDateProp = propSet.getProperty(startDatePropDef);
                Date eventStart = startDateProp != null ? startDateProp.getDateValue() : cal.getTime();
                Property endDateProp = propSet.getProperty(endDatePropDef);
                Date eventEnd = endDateProp != null ? endDateProp.getDateValue() : eventStart;
                Calendar eventStartCal = Calendar.getInstance();
                eventStartCal.setTime(eventStart);
                if (eventStartCal.get(Calendar.MONTH) < cal.get(Calendar.MONTH)) {
                    eventStartCal.setTime(cal.getTime());
                }
                Calendar eventEndCal = Calendar.getInstance();
                eventEndCal.setTime(eventEnd);
                if (eventEndCal.get(Calendar.MONTH) > cal.get(Calendar.MONTH)) {
                    eventEndCal.set(Calendar.DAY_OF_MONTH, eventEndCal.getActualMaximum(Calendar.DAY_OF_MONTH));
                }

                if (eventStartCal.equals(eventEndCal)) {
                    eventDatesList.add(eventDateFormat.format(eventStartCal.getTime()));
                } else {
                    while (eventStartCal.before(eventEndCal)) {
                        eventDatesList.add(eventDateFormat.format(eventStartCal.getTime()));
                        eventStartCal.add(Calendar.DAY_OF_MONTH, 1);
                    }
                }

            }
            String eventDates = getEventDatesAsArrayString(eventDatesList);
            model.put("allowedDates", eventDates);

        }
    }

    @SuppressWarnings("unchecked")
    private void setCalendarTitles(HttpServletRequest request, Resource resource, Map model) {
        model.put("dayHasPlannedEventsTitle",
                this.helper.getEventTypeTitle(request, resource, "eventListing.calendar.dayHasPlannedEvents", false));
        model.put("dayHasNoPlannedEventsTitle",
                this.helper.getEventTypeTitle(request, resource, "eventListing.calendar.dayHasNoPlannedEvents", false));
    }

    private String getEventDatesAsArrayString(Set<String> eventDates) {
        StringBuilder sb = new StringBuilder("[");
        Iterator<String> it = eventDates.iterator();
        boolean first = true;
        while (it.hasNext()) {
            if (!first) {
                sb.append(", ");
            }
            sb.append("'" + it.next() + "'");
            first = false;
        }
        sb.append("]");
        return sb.toString();
    }

    @Required
    public void setHelper(EventListingHelper helper) {
        this.helper = helper;
    }

    @Required
    public void setSearcher(EventListingSearcher searcher) {
        this.searcher = searcher;
    }

    @Required
    public void setResourceTypeTree(ResourceTypeTree resourceTypeTree) {
        this.resourceTypeTree = resourceTypeTree;
    }

    @Required
    public void setDisplayTypePropDef(PropertyTypeDefinition displayTypePropDef) {
        this.displayTypePropDef = displayTypePropDef;
    }

    @Required
    public void setStartDatePropDefPointer(String startDatePropDefPointer) {
        this.startDatePropDefPointer = startDatePropDefPointer;
    }

    @Required
    public void setEndDatePropDefPointer(String endDatePropDefPointer) {
        this.endDatePropDefPointer = endDatePropDefPointer;
    }

    @Required
    public void setViewAllUpcomingService(Service viewAllUpcomingService) {
        this.viewAllUpcomingService = viewAllUpcomingService;
    }

    @Required
    public void setViewAllPreviousService(Service viewAllPreviousService) {
        this.viewAllPreviousService = viewAllPreviousService;
    }

}
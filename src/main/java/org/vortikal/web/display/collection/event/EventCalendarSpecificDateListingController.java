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

import org.vortikal.repository.Resource;
import org.vortikal.web.RequestContext;
import org.vortikal.web.display.collection.event.EventListingHelper.SpecificDateSearchType;
import org.vortikal.web.display.listing.ListingPager;
import org.vortikal.web.display.listing.ListingPagingLink;
import org.vortikal.web.search.Listing;
import org.vortikal.web.service.Service;
import org.vortikal.web.service.URL;

public class EventCalendarSpecificDateListingController extends EventCalendarListingController {

    @Override
    public void runSearch(HttpServletRequest request, Resource collection, Map<String, Object> model, int pageLimit)
            throws Exception {

        int page = ListingPager.getPage(request, ListingPager.UPCOMING_PAGE_PARAM);
        model.put(MODEL_KEY_PAGE, page);

        Date date = this.helper.getSpecificSearchDate(request);
        if (date != null) {
            SpecificDateSearchType searchType = this.helper.getSpecificDateSearchType(request);
            Listing specificDateEvents = this.searcher.searchSpecificDate(request, collection, pageLimit, page);

            model.put("specificDate", Boolean.TRUE);
            String messageKey = searchType == SpecificDateSearchType.Day ? "eventListing.specificDayEvent"
                    : "eventListing.specificDateEvent";
            String specificDateEventsTitle = this.helper.getEventTypeTitle(request, collection, searchType, date,
                    messageKey, true, true);
            model.put("specificDateEventsTitle", specificDateEventsTitle);
            model.put(MODEL_KEY_OVERRIDDEN_TITLE, specificDateEventsTitle);

            if (specificDateEvents != null && !specificDateEvents.getFiles().isEmpty()) {
                model.put("specificDateEvents", specificDateEvents);

                Service service = RequestContext.getRequestContext().getService();
                URL baseURL = service.constructURL(RequestContext.getRequestContext().getResourceURI());
                baseURL.setParameter(EventListingHelper.REQUEST_PARAMETER_DATE, request
                        .getParameter(EventListingHelper.REQUEST_PARAMETER_DATE));

                List<ListingPagingLink> urls = ListingPager.generatePageThroughUrls(specificDateEvents.getTotalHits(), pageLimit,
                        baseURL, page);
                model.put(MODEL_KEY_PAGE_THROUGH_URLS, urls);
            } else {
                model.put("noPlannedEventsMsg", this.helper.getEventTypeTitle(request, collection,
                        "eventListing.noPlannedEvents", false));
            }
        } else {
            // invalid date given in request, run default search
            super.runSearch(request, collection, model, pageLimit);
        }

    }

}

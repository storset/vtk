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

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.vortikal.repository.Resource;
import org.vortikal.web.RequestContext;
import org.vortikal.web.display.listing.ListingPager;
import org.vortikal.web.display.listing.ListingPagingLink;
import org.vortikal.web.search.Listing;
import org.vortikal.web.service.Service;
import org.vortikal.web.service.URL;

public abstract class EventCalendarAllListingController extends EventCalendarListingController {

    protected abstract Listing getSearchResult(HttpServletRequest request, Resource collection,
            Map<String, Object> model, int page, int pageLimit) throws Exception;

    @Override
    public void runSearch(HttpServletRequest request, Resource collection, Map<String, Object> model, int pageLimit)
            throws Exception {

        int page = ListingPager.getPage(request, ListingPager.UPCOMING_PAGE_PARAM);
        model.put(MODEL_KEY_PAGE, page);

        Listing result = this.getSearchResult(request, collection, model, page, pageLimit);

        Service service = RequestContext.getRequestContext().getService();
        URL serviceURL = service.constructURL(collection.getURI());
        String viewType = serviceURL.getParameter(EventListingHelper.REQUEST_PARAMETER_VIEW);

        model.put(viewType, result);
        String title = this.helper.getEventTypeTitle(request, collection, "eventListing." + viewType, false);
        String titleKey = viewType + "Title";
        model.put(titleKey, title);

        if (result == null || result.getFiles().isEmpty()) {
            String noPlannedTitle = this.helper.getEventTypeTitle(request, collection, "eventListing.noPlanned."
                    + viewType, false);
            String noPlannedTitleKey = viewType + "NoPlannedTitle";
            model.put(noPlannedTitleKey, noPlannedTitle);
        } else {
            List<ListingPagingLink> urls = ListingPager.generatePageThroughUrls(result.getTotalHits(), pageLimit, serviceURL, page);
            model.put(MODEL_KEY_PAGE_THROUGH_URLS, urls);
        }
    }

}

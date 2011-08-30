/* Copyright (c) 2008, University of Oslo, Norway
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
package org.vortikal.web.display.collection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Resource;
import org.vortikal.web.RequestContext;
import org.vortikal.web.display.listing.ListingPager;
import org.vortikal.web.display.listing.ListingPagingLink;
import org.vortikal.web.search.Listing;
import org.vortikal.web.search.SearchComponent;
import org.vortikal.web.service.Service;
import org.vortikal.web.service.URL;

public class CollectionListingController extends AbstractCollectionListingController {

    protected List<SearchComponent> searchComponents;

    @Override
    public void runSearch(HttpServletRequest request, Resource collection, Map<String, Object> model, int pageLimit)
            throws Exception {
        int page = ListingPager.getPage(request, ListingPager.UPCOMING_PAGE_PARAM);
        int offset = (page - 1) * pageLimit;
        int limit = pageLimit;
        int totalHits = 0;

        List<Listing> results = new ArrayList<Listing>();
        for (SearchComponent component : this.searchComponents) {
            Listing listing = component.execute(request, collection, page, limit, 0);
            totalHits += listing.getTotalHits();
            // Add the listing to the results
            if (listing.getFiles().size() > 0) {
                results.add(listing);
            }
            // Check previous result (by redoing the previous search),
            // to see if we need to adjust the offset.
            // XXX: is there a better way?
            if (listing.getFiles().size() == 0 && offset > 0) {
                Listing prevListing = component.execute(request, collection, page - 1, limit, 0);
                if (prevListing.getFiles().size() > 0 && !prevListing.hasMoreResults()) {
                    offset -= prevListing.getFiles().size();
                }
            }

            // We have more results to display for this listing
            if (listing.hasMoreResults()) {
                break;
            }
            // Only include enough results to fill the page:
            if (listing.getFiles().size() > 0) {
                limit -= listing.getFiles().size();
            }

        }
        Service service = RequestContext.getRequestContext().getService();
        URL baseURL = service.constructURL(RequestContext.getRequestContext().getResourceURI());

        List<ListingPagingLink> urls = ListingPager.generatePageThroughUrls(totalHits, pageLimit, baseURL, page);
        model.put(MODEL_KEY_PAGE_THROUGH_URLS, urls);
        model.put(MODEL_KEY_SEARCH_COMPONENTS, results);
        model.put(MODEL_KEY_PAGE, page);
        if (results.size() > 0 && results.get(0) != null) {
            model.put("numberOfRecords", getNumberOfRecords(page, pageLimit, results.get(0).size()));
        }
    }

    protected Map<String, Integer> getNumberOfRecords(int page, int pageLimit, int resultSize) {
        Map<String, Integer> numbers = new HashMap<String, Integer>();
        int numberShownElements = ((page - 1) * pageLimit) + 1;
        int includingThisPage = ((page - 1) * pageLimit) + resultSize;
        numbers.put("elementsOnPreviousPages", numberShownElements);
        numbers.put("elementsIncludingThisPage", includingThisPage);
        return numbers;
    }

    @Required
    public void setSearchComponents(List<SearchComponent> searchComponents) {
        this.searchComponents = searchComponents;
    }

}

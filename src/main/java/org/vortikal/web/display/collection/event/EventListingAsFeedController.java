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

import javax.servlet.http.HttpServletRequest;

import org.apache.abdera.model.Feed;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.web.RequestContext;
import org.vortikal.web.display.AtomFeedController;
import org.vortikal.web.display.collection.event.EventListingHelper.SpecificDateSearchType;
import org.vortikal.web.search.Listing;

public class EventListingAsFeedController extends AtomFeedController {

    private EventListingHelper helper;
    private EventListingSearcher searcher;
    private PropertyTypeDefinition displayTypePropDef;
    private String overridePublishDatePropDefPointer;

    @Override
    protected Feed createFeed(RequestContext requestContext) throws Exception {
        Path uri = requestContext.getResourceURI();
        String token = requestContext.getSecurityToken();
        Resource collection = requestContext.getRepository().retrieve(token, uri, false);
        HttpServletRequest request = requestContext.getServletRequest();

        String feedTitle = getTitle(collection, requestContext);

        Listing feedContent = null;
        boolean showIntroduction = true;
        Property displayTypeProp = collection.getProperty(this.displayTypePropDef);
        if (displayTypeProp != null && "calendar".equals(displayTypeProp.getStringValue())) {
            SpecificDateSearchType searchType = this.helper.getSpecificDateSearchType(request);
            if (searchType != null) {
                Date date = this.helper.getSpecificSearchDate(request);
                String messageKey = searchType == SpecificDateSearchType.Day ? "eventListing.specificDayEvent"
                        : "eventListing.specificDateEvent";
                feedTitle = this.helper.getEventTypeTitle(request, collection, searchType, date, messageKey, true,
                        false);
                feedContent = this.searcher.searchSpecificDate(request, collection, this.entryCountLimit, 1);
            } else {
                String viewType = request.getParameter(EventListingHelper.REQUEST_PARAMETER_VIEW);
                if (EventListingHelper.VIEW_TYPE_ALL_UPCOMING.equals(viewType)
                        || EventListingHelper.VIEW_TYPE_ALL_PREVIOUS.equals(viewType)) {
                    feedTitle = this.helper.getEventTypeTitle(request, collection, "eventListing.allupcoming", false);
                }
            }
            showIntroduction = false;
        }
        if (feedContent == null) {
            feedContent = this.searcher.searchUpcoming(request, collection, 1, this.entryCountLimit, 0);
        }

        Feed feed = populateFeed(collection, feedTitle, showIntroduction);
        for (PropertySet feedEntry : feedContent.getFiles()) {
            addEntry(feed, requestContext, feedEntry);
        }

        return feed;
    }

    @Override
    protected Property getPublishDate(PropertySet resource) {
        PropertyTypeDefinition overridePublishDatePropDef = this.resourceTypeTree
                .getPropertyDefinitionByPointer(this.overridePublishDatePropDefPointer);
        Property overridePublishDateProp = resource.getProperty(overridePublishDatePropDef);
        if (overridePublishDateProp != null) {
            return overridePublishDateProp;
        }
        return this.getDefaultPublishDate(resource);
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
    public void setDisplayTypePropDef(PropertyTypeDefinition displayTypePropDef) {
        this.displayTypePropDef = displayTypePropDef;
    }

    @Required
    public void setOverridePublishDatePropDefPointer(String overridePublishDatePropDefPointer) {
        this.overridePublishDatePropDefPointer = overridePublishDatePropDefPointer;
    }

}

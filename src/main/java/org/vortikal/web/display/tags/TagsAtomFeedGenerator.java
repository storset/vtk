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
package org.vortikal.web.display.tags;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.apache.abdera.model.Feed;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.web.RequestContext;
import org.vortikal.web.display.feed.AtomFeedGenerator;
import org.vortikal.web.search.Listing;
import org.vortikal.web.search.SearchComponent;
import org.vortikal.web.service.Service;
import org.vortikal.web.tags.TagsHelper;

public class TagsAtomFeedGenerator extends AtomFeedGenerator {

    private PropertyTypeDefinition overridePublishDatePropDef;
    private SearchComponent searchComponent;
    protected TagsHelper tagsHelper;

    @Override
    protected void addFeedEntries(Feed feed, Resource feedScope) throws Exception {

        Listing entryElements = searchComponent.execute(RequestContext.getRequestContext().getServletRequest(),
                feedScope, 1, entryCountLimit, 0);

        for (PropertySet feedEntry : entryElements.getFiles()) {
            addPropertySetAsFeedEntry(feed, feedEntry);
        }

    }

    @Override
    protected Resource getFeedScope() throws Exception {
        RequestContext requestContext = RequestContext.getRequestContext();
        String token = requestContext.getSecurityToken();
        HttpServletRequest request = requestContext.getServletRequest();

        return tagsHelper.getScopedResource(token, request);
    }

    @Override
    protected String getFeedTitle(Resource feedScope, RequestContext requestContext) {
        Service service = requestContext.getService();
        return service.getLocalizedName(feedScope, requestContext.getServletRequest());
    }

    @Override
    protected boolean showFeedIntroduction(Resource feedScope) {
        return false;
    }

    @Override
    protected String getFeedPrefix() {
        return "tags:";
    }

    @Override
    protected Date getLastModified(PropertySet collection) {
        return new Date();
    }

    @Override
    protected Property getPublishDate(PropertySet resource) {
        Property overridePublishDateProp = resource.getProperty(overridePublishDatePropDef);
        if (overridePublishDateProp != null) {
            return overridePublishDateProp;
        }
        return getDefaultPublishDate(resource);
    }

    @Required
    public void setTagsHelper(TagsHelper tagsHelper) {
        this.tagsHelper = tagsHelper;
    }

    public void setSearchComponent(SearchComponent searchComponent) {
        this.searchComponent = searchComponent;
    }

    public void setOverridePublishDatePropDef(PropertyTypeDefinition overridePublishDatePropDef) {
        this.overridePublishDatePropDef = overridePublishDatePropDef;
    }

}

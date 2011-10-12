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
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.web.RequestContext;
import org.vortikal.web.display.AtomFeedController;
import org.vortikal.web.search.Listing;
import org.vortikal.web.search.SearchComponent;
import org.vortikal.web.service.Service;
import org.vortikal.web.tags.TagsHelper;

public class TagsAsFeedController extends AtomFeedController {

    private PropertyTypeDefinition overridePublishDatePropDef;
    private SearchComponent searchComponent;
    private TagsHelper tagsHelper;

    @Override
    protected Feed createFeed(RequestContext requestContext) throws Exception {

        String token = requestContext.getSecurityToken();
        HttpServletRequest request = requestContext.getServletRequest();

        Resource scope = this.tagsHelper.getScope(token, request);

        String tag = request.getParameter(TagsHelper.TAG_PARAMETER);
        if (StringUtils.isBlank(tag)) {
            return null;
        }

        RequestContext rc = RequestContext.getRequestContext();
        Service service = rc.getService();
        String feedTitle = service.getLocalizedName(scope, request);
        Feed feed = populateFeed(scope, feedTitle);

        Listing searchResult = searchComponent.execute(request, scope, 1, this.entryCountLimit, 0);

        for (PropertySet result : searchResult.getFiles()) {
            addEntry(feed, requestContext, result);
        }

        return feed;

    }

    @Override
    protected String getFeedPrefix() {
        return "tags:";
    }

    @Override
    protected Date getLastModified(PropertySet collection) {
        return new Date();
    }

    @Required
    public void setSearchComponent(SearchComponent searchComponent) {
        this.searchComponent = searchComponent;
    }

    @Required
    public void setTagsHelper(TagsHelper tagsHelper) {
        this.tagsHelper = tagsHelper;
    }

    @Override
    protected Property getPublishDate(PropertySet resource) {
        Property overridePublishDateProp = resource.getProperty(this.overridePublishDatePropDef);
        if (overridePublishDateProp != null) {
            return overridePublishDateProp;
        }
        return this.getDefaultPublishDate(resource);
    }

    public void setOverridePublishDatePropDef(PropertyTypeDefinition overridePublishDatePropDef) {
        this.overridePublishDatePropDef = overridePublishDatePropDef;
    }

}

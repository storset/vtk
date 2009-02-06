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
package org.vortikal.web.controller;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.abdera.model.Feed;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.Resource;
import org.vortikal.web.RequestContext;
import org.vortikal.web.controller.feed.AtomFeedController;
import org.vortikal.web.search.Listing;
import org.vortikal.web.search.SearchComponent;

public class TagsAsFeedController extends AtomFeedController {

    private SearchComponent searchComponent;


    @Override
    protected Feed createFeed(HttpServletRequest request, HttpServletResponse response, String token)
            throws Exception {

        Resource scope = getScope(token, request);

        String tag = request.getParameter("tag");
        if (StringUtils.isBlank(tag)) {
            response.sendError(404, "Missing tag parameter");
            return null;
        }

        String feedTitle = getTitle(scope, tag, request);
        Feed feed = populateFeed(scope, feedTitle);

        Listing searchResult = searchComponent.execute(request, scope, 1, 25, 0);

        for (PropertySet result : searchResult.getFiles()) {
            populateEntry(token, result, feed.addEntry());
        }

        return feed;

    }


    @Override
    protected String getFeedPrefix() {
        return "tags:";
    }


    @Override
    protected Date getLastModified(Resource collection) {
        return new Date();
    }


    @Override
    protected Property getPicture(Resource collection) {
        return null;
    }


    private Resource getScope(String token, HttpServletRequest request) throws Exception {
        String scopeFromRequest = request.getParameter("scope");
        if (scopeFromRequest == null || scopeFromRequest.equals("")) {
            return this.repository.retrieve(token, Path.ROOT, true);
        }
        if (".".equals(scopeFromRequest)) {
            Path currentCollection = RequestContext.getRequestContext().getCurrentCollection();
            return this.repository.retrieve(token, currentCollection, true);
        }
        if (scopeFromRequest.startsWith("/")) {
            Resource scopedResource = this.repository.retrieve(token, Path
                    .fromString(scopeFromRequest), true);
            if (!scopedResource.isCollection()) {
                throw new IllegalArgumentException("scope must be a collection");
            }
            return scopedResource;
        }

        throw new IllegalArgumentException("Scope must be empty, '.' or be a valid collection");
    }


    private String getTitle(Resource scope, String tag, HttpServletRequest request) {
        org.springframework.web.servlet.support.RequestContext rc = new org.springframework.web.servlet.support.RequestContext(
                request);
        if (Path.ROOT.equals(scope.getURI())) {
            return rc.getMessage("tags.title", new Object[] { this.repository.getId(), tag });
        }
        return rc.getMessage("tags.scopedTitle", new Object[] { scope.getTitle(), tag });
    }


    @Required
    public void setSearchComponent(SearchComponent searchComponent) {
        this.searchComponent = searchComponent;
    }

}

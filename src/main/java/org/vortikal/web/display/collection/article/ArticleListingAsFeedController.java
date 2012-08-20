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
package org.vortikal.web.display.collection.article;

import java.util.ArrayList;
import java.util.List;

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
import org.vortikal.web.search.Listing;

public class ArticleListingAsFeedController extends AtomFeedController {

    private ArticleListingSearcher searcher;
    private PropertyTypeDefinition overridePublishDatePropDef;

    @Override
    protected Feed createFeed(RequestContext requestContext) throws Exception {

        Path uri = requestContext.getResourceURI();
        String token = requestContext.getSecurityToken();
        Resource collection = requestContext.getRepository().retrieve(token, uri, true);

        String feedTitle = getTitle(collection, requestContext);
        Feed feed = populateFeed(collection, feedTitle);

        List<Listing> results = new ArrayList<Listing>();
        HttpServletRequest request = requestContext.getServletRequest();
        Listing featuredArticles = this.searcher.getFeaturedArticles(request, collection, 1, this.entryCountLimit, 0);
        if (featuredArticles != null && featuredArticles.size() > 0) {
            results.add(featuredArticles);
        }

        Listing articles = this.searcher.getArticles(request, collection, 1, this.entryCountLimit, 0);
        if (articles.size() > 0) {
            results.add(articles);
        }

        for (Listing searchResult : results) {
            for (PropertySet result : searchResult.getFiles()) {
                addEntry(feed, requestContext, result);
            }
        }
        return feed;
    }

    @Override
    protected Property getPublishDate(PropertySet resource) {
        Property overridePublishDateProp = resource.getProperty(this.overridePublishDatePropDef);
        if (overridePublishDateProp != null) {
            return overridePublishDateProp;
        }
        return this.getDefaultPublishDate(resource);
    }

    @Required
    public void setSearcher(ArticleListingSearcher searcher) {
        this.searcher = searcher;
    }

    @Required
    public void setOverridePublishDatePropDef(PropertyTypeDefinition overridePublishDatePropDef) {
        this.overridePublishDatePropDef = overridePublishDatePropDef;
    }
}

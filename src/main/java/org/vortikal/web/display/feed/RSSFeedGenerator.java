/* Copyright (c) 2013, University of Oslo, Norway
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
package org.vortikal.web.display.feed;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.servlet.ModelAndView;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.HtmlValueFormatter;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;
import com.sun.syndication.io.SyndFeedOutput;

/**
 * 
 * Creates a RSS 2.0 feed using the Rome library, adhering to:
 * http://cyber.law.harvard.edu/rss/rss.html
 * 
 * Subclasses provide results for and add entries to feed, as well as override
 * title and certain other properties (date, author ++).
 * 
 */
public abstract class RSSFeedGenerator implements FeedGenerator {

    // Supported feed types as of Apr. 2013 are:
    // rss_0.9, rss_0.91, rss_0.92, rss_0.93, rss_0.94, rss_1.0, rss_2.0,
    // atom_0.3, atom_1.0
    // We only provide RSS 2.0
    public static final String SUPPORTED_FEED_TYPE = "rss_2.0";

    protected Service viewService;
    protected PropertyTypeDefinition titlePropDef;
    protected PropertyTypeDefinition publishDatePropDef;
    protected PropertyTypeDefinition lastModifiedPropDef;

    // Must be overriden by subclasses to provide content for feed entries and
    // add these to feed
    protected abstract List<SyndEntry> getFeedEntries(Resource feedScope) throws Exception;

    @Override
    public ModelAndView generateFeed(HttpServletRequest request, HttpServletResponse response) throws Exception {

        RequestContext requestContext = RequestContext.getRequestContext();
        Path uri = requestContext.getResourceURI();
        String token = requestContext.getSecurityToken();
        Resource feedScope = requestContext.getRepository().retrieve(token, uri, true);

        SyndFeed feed = new SyndFeedImpl();
        feed.setFeedType(SUPPORTED_FEED_TYPE);
        feed.setTitle(getTitle(feedScope, requestContext));
        feed.setLink(request.getRequestURL().toString());

        // Description must be string
        Namespace NS_CONTENT = Namespace.getNamespace("http://www.uio.no/content");
        Property descriptionProp = feedScope.getProperty(NS_CONTENT, PropertyType.DESCRIPTION_PROP_NAME);
        String description = descriptionProp != null ? descriptionProp.getFormattedValue(
                HtmlValueFormatter.FLATTENED_FORMAT, null) : "";
        feed.setDescription(description);

        List<SyndEntry> entries = getFeedEntries(feedScope);
        if (entries.size() > 0) {
            feed.setEntries(entries);
        }

        SyndFeedOutput output = new SyndFeedOutput();
        response.setContentType("text/xml;charset=utf-8");
        output.output(feed, response.getWriter());

        return null;
    }

    protected String getTitle(Resource feedScope, RequestContext requestContext) {
        String feedTitle = feedScope.getTitle();
        if (Path.ROOT.equals(feedScope.getURI())) {
            feedTitle = requestContext.getRepository().getId();
        }
        return feedTitle;
    }

    @Required
    public void setViewService(Service viewService) {
        this.viewService = viewService;
    }

    @Required
    public void setTitlePropDef(PropertyTypeDefinition titlePropDef) {
        this.titlePropDef = titlePropDef;
    }

    @Required
    public void setPublishDatePropDef(PropertyTypeDefinition publishDatePropDef) {
        this.publishDatePropDef = publishDatePropDef;
    }

    @Required
    public void setLastModifiedPropDef(PropertyTypeDefinition lastModifiedPropDef) {
        this.lastModifiedPropDef = lastModifiedPropDef;
    }
}

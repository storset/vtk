/* Copyright (c) 2004, University of Oslo, Norway
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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.referencedata.ReferenceDataProvider;
import org.vortikal.web.service.Service;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;

/**
 * <p>
 * Add data for the model so the model can be used to display a feed (atom/rss) based on the data
 * that already exists in the model. This provider is for preproccesing the model for the view
 * {@link org.vortikal.web.view.RomeFeedView}.
 * </p>
 * <p>
 * Configurable properties:
 * </p>
 * <ul>
 * <li><code>defaultFeedType</code> - The feedType which will be used for the feed if the
 * parameter "format" is not available in the request or this parameter contains an unssupported
 * feedType</li>
 * <li><code>charset</code> - The characterset used for the feed</li>
 * </ul>
 * 
 * <p>
 * The model is first searched for an object of type <code>java.util.Map</code> and key
 * <code>feedModel</code>. If no such object exists in the model, this provider does not change
 * the model.
 * </p>
 * <p>
 * feedModel can contain the following data:
 * </p>
 * <ul>
 * <li><code>title</code> - title for the rssfeed to be used in
 * {@link com.sun.syndication.feed.synd.SyndFeed#setTitle}
 * <li><code>description</code> - description for the rssfeed to be used in
 * {@link com.sun.syndication.feed.synd.SyndFeed#setDescription}
 * <li><code>url</code> - url for the feed to be used in
 * {@link com.sun.syndication.feed.synd.SyndFeed#setLink}
 * <li><code>rersources</code> - An array of {@link org.vortikal.repository.Resource} to be
 * included in the rssfeed
 * </ul>
 * 
 */

public class RomeFeedProvider implements ReferenceDataProvider, InitializingBean {

    private static final Log logger = LogFactory.getLog(RomeFeedProvider.class);

    private final String FORMAT_PARAMETER_NAME = "format";
    private Service browsingService;

    private String defaultFeedType;
    
    private boolean useTimestampInIdentifier = false;
    
    private String charset = "utf-8";
    
    public void afterPropertiesSet() {
        if (browsingService == null) {
            throw new BeanInitializationException("JavaBean Property 'browsingService' must be set");
        }
        if (defaultFeedType == null) {
            throw new BeanInitializationException("JavaBean Property 'defaultFeedType' must be set");
        }
    }

    public void referenceData(Map model, HttpServletRequest request) throws Exception {

        Map feedModel = (Map) model.get("feedModel");
        if (feedModel == null) {
            return;
        }

        // Generate and write RSS file:
        SyndFeed feed = new SyndFeedImpl();

        String title = (String) feedModel.get("title");
        feed.setTitle(title);

        String description = (String) feedModel.get("description");
        feed.setDescription(description);

        String url = (String) feedModel.get("url");
        feed.setLink(url);
        feed.setUri(url);

        Resource[] resources = (Resource[]) feedModel.get("resources");
        // Created and add list of entries
        // (Each entry is set with a title, link, published date and a description)
        // ( -> Description can be plain text or HTML)
        List feedEntries = new ArrayList();

        Principal principal = SecurityContext.getSecurityContext().getPrincipal();

        for (int i = 0; i < resources.length; i++) {
            SyndEntry entry = getSyndEntryForResource(resources[i], principal);
            feedEntries.add(entry);
        }
       
        feed.setFeedType(getFormatFromRequest(request, feed));
        feed.setEncoding(charset);

        // Maybe a better implemantation is to set publishedDate for the feed to the lastest of the
        // updated
        // dates for the entries.
        feed.setPublishedDate(Calendar.getInstance().getTime());

        feed.setEntries(feedEntries);
        model.put("romeFeed", feed);
    }

    private String getFormatFromRequest(HttpServletRequest request, SyndFeed feed) {
        String feedType = request.getParameter(FORMAT_PARAMETER_NAME);
        if (feedType == null || !isSuuportedFeedType(feedType, feed)) {
            feedType = defaultFeedType;
        }
        return feedType;
    }
    
    private boolean isSuuportedFeedType(String feedType, SyndFeed feed) {
        return feed.getSupportedFeedTypes().contains(feedType);
    }

    /**
     * Helper method to build a RSS SyndEntry from provided arguments
     * 
     * @param String
     *            title, String url, String publishDate, String doctype, String tilhorighet
     * @return SyndEntry RSS feed with values corresponding to document values
     */
    private SyndEntry getSyndEntryForResource(Resource resource, Principal principal) {
        // RSS entry objects
        SyndEntry entry = new SyndEntryImpl();
        String title = resource.getName();
        if (!title.equals("")) {
            // Throws FeedException if title exceeds 100 characters
            if (title.length() > 99) {
                logger.warn("Title of current notice is too long for the feed");
                title = title.substring(0, 95) + "...";
                entry.setTitle(formatTitle(title));
            } else
                entry.setTitle(formatTitle(title));
        } else {
            entry.setTitle("title is missing");
        }
        
        entry.setAuthor(resource.getOwner().getName());
     
        String link = browsingService.constructLink(resource, principal);
        entry.setLink(link);

        if (useTimestampInIdentifier) {
            long lastModified = resource.getLastModified().getTime();
            entry.setUri(link + "#" + lastModified); 
        } else {
            entry.setUri(link); 
        }
        
        entry.setPublishedDate(resource.getCreationTime());
        //entry.setPublishedDate(resource.getLastModified());
        entry.setUpdatedDate(resource.getLastModified());
        return entry;
    }

    /**
     * Private helper method for setEntry() to format a title which contains multiple, separated
     * elements
     * 
     * @param noticeTitle
     * @return title with any occurences of separators "|" replaced by ": "
     */
    private String formatTitle(String noticeTitle) {
        return noticeTitle.replaceAll("\\|", ": ");
    }

    public void setBrowsingService(Service browsingService) {
        this.browsingService = browsingService;
    }

    /**
     * 
     * @param defaultFeedType
     *            
     */
    public void setDefaultFeedType(String defaultFeedType) {
        this.defaultFeedType = defaultFeedType;
    }

    /**
     * 
     * @param charset
     */
    public void setCharset(String charset) {
        this.charset = charset;
    }

    public void setUseTimestampInIdentifier(boolean useTimestampInIdentifier) {
        this.useTimestampInIdentifier = useTimestampInIdentifier;
    }

}

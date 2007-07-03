/* Copyright (c) 2007, University of Oslo, Norway
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
package org.vortikal.web.view.decorating.components;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.web.context.ServletContextAware;
import org.springframework.web.servlet.View;

import org.vortikal.util.cache.ContentCache;
import org.vortikal.web.RequestContext;
import org.vortikal.web.servlet.BufferedResponse;
import org.vortikal.web.servlet.VortikalServlet;
import org.vortikal.web.view.decorating.DecoratorRequest;
import org.vortikal.web.view.decorating.DecoratorResponse;

import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;


/**
 * XXX: this class currently depends on the thread safety of the
 * SyndFeed implementation: if it turns out that it is not thread
 * safe, its data has to be extracted to a custom bean after
 * fetching a feed.
 */
public class SyndicationFeedComponent extends AbstractDecoratorComponent
  implements ServletContextAware {

    private static final String PARAMETER_SORT = "sort";
    private static final String PARAMETER_SORT_DESC = 
        "Default sorted by published date. Set to 'item-title' to sort by this instead.";
    
    private static final String PARAMETER_ALL_MESSAGES_LINK = "all-messages-link";
    private static final String PARAMETER_ALL_MESSAGES_LINK_DESC = 
        "Defaults to 'true' displaying 'All messages' link at the bottom. Set to 'false' to remove this link.";
    
    private static final String PARAMETER_PUBLISHED_DATE = "published-date";
    private static final String PARAMETER_PUBLISHED_DATE_DESC = 
        "How to display published date, defaults to date and time. Set to 'date' to only display the date, or 'none' to not show the date";
    
    private static final String PARAMETER_MAX_MESSAGES = "max-messages";
    private static final String PARAMETER_MAX_MESSAGES_DESC = "The max number of messages to display, defaults to 10";
    
    private static final String PARAMETER_ITEM_DESCRIPTION = "item-description";
    private static final String PARAMETER_ITEM_DESCRIPTION_DESC = "Must be set to 'true' to show item descriptions";

    private static final String PARAMETER_FEED_DESCRIPTION = "feed-description";
    private static final String PARAMETER_FEED_DESCRIPTION_DESC = "Must be set to 'true' to show feed description";

    private static final String PARAMETER_URL = "url";
    private static final String PARAMETER_URL_DESC = "The feed url. For host " +
            "local feeds, you get authenticated retrieval of the resource if you skip the protocol/host part";

    private static final String PARAMETER_FEED_TITLE = "feed-title";
    private static final String PARAMETER_FEED_TITLE_DESC = "Set to 'false' if you don't want to show feed title";

    private static Log logger = LogFactory.getLog(SyndicationFeedComponent.class);
    private ContentCache cache;
    private View view;

    private ServletContext servletContext;


    public void setView(View view) {
        this.view = view;
    }

    public void setContentCache(ContentCache cache) {
        this.cache = cache;
    }
    
    public void render(DecoratorRequest request, DecoratorResponse response)
        throws Exception {

        SyndicationFeedConfig conf = new SyndicationFeedConfig();
        
        String url = request.getStringParameter(PARAMETER_URL);
        if (url == null) {
            throw new DecoratorComponentException(
                "Component parameter 'url' is required");
        }

        String feedTitleString = request.getStringParameter(PARAMETER_FEED_TITLE); 
        if (feedTitleString != null && "false".equals(feedTitleString)) {
            conf.setFeedTitle(false);
        }
        
        String feedDescriptionString = request.getStringParameter(PARAMETER_FEED_DESCRIPTION);
        if (feedDescriptionString != null && "true".equals(feedDescriptionString)) {
            conf.setFeedDescription(true);
        }

        String itemDescriptionString = request.getStringParameter(PARAMETER_ITEM_DESCRIPTION); 
        if (itemDescriptionString != null && "true".equals(itemDescriptionString)) {
            conf.setItemDescription(true);
        }

        String maxMsgsString = request.getStringParameter(PARAMETER_MAX_MESSAGES);
        if (maxMsgsString != null) {
            try {
                int tmpInt = Integer.parseInt(maxMsgsString);
                if (tmpInt > 0) {
                    conf.setMaxMsgs(tmpInt);
                }
            } catch (Exception e) { }
        }

        String publishedDateString = request.getStringParameter(PARAMETER_PUBLISHED_DATE);
        if ("none".equals(publishedDateString)) {
            conf.setPublishedDate(null);
        } else if ("date".equals(publishedDateString)) {
            conf.setPublishedDate("short");
        }
        
        String bottomLinkToAllMessagesString = request.getStringParameter(
            PARAMETER_ALL_MESSAGES_LINK);
        if ("false".equals(bottomLinkToAllMessagesString)) {
            conf.setBottomLinkToAllMessages(false);
        }

        String sortString = request.getStringParameter(PARAMETER_SORT);
        if ("item-title".equals(sortString)) {
            conf.setSortByTitle(true);
        }
        
        SyndFeed feed = null;
        
        if (!url.startsWith("/")) {
            feed = (SyndFeed) this.cache.get(url);
        } else {
            feed = getLocalFeed(url, request);
        }
        Map<String, Object> model = new HashMap<String, Object>();

        model.put("feed", feed);
        model.put("conf", conf);

        BufferedResponse tmpResponse = new BufferedResponse();

        this.view.render(
            model, RequestContext.getRequestContext().getServletRequest(),
            tmpResponse);

        if (logger.isDebugEnabled()) {
            logger.debug("Rendered wrapped view " + this.view + ". "
                         + "Character encoding was: "
                         + tmpResponse.getCharacterEncoding() + ", "
                         + "Content-Length was: " + tmpResponse.getContentLength());
        }

        response.setCharacterEncoding(tmpResponse.getCharacterEncoding());
        OutputStream out = response.getOutputStream();
        out.write(tmpResponse.getContentBuffer());
        out.close();
    }

    private SyndFeed getLocalFeed(String url, DecoratorRequest request) throws Exception {
        
        InputStream stream = retrieveLocalStream(url, request);
        XmlReader xmlReader = new XmlReader(stream);
        SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed = input.build(xmlReader);
        return feed;

    }

    private InputStream retrieveLocalStream(String uri, DecoratorRequest request)
        throws Exception {

        HttpServletRequest servletRequest = request.getServletRequest();
        if (servletRequest.getAttribute(IncludeComponent.INCLUDE_ATTRIBUTE_NAME) != null) {
            throw new DecoratorComponentException("Error including URI '" + uri
                    + "': possible include loop detected ");
        }

        // XXX: encode URI?
        RequestWrapper requestWrapper = new RequestWrapper(servletRequest, uri);
        requestWrapper.setAttribute(IncludeComponent.INCLUDE_ATTRIBUTE_NAME, new Object());

        String servletName = (String) servletRequest
                .getAttribute(VortikalServlet.SERVLET_NAME_REQUEST_ATTRIBUTE);

        RequestDispatcher rd = this.servletContext.getNamedDispatcher(servletName);

        if (rd == null) {
            throw new RuntimeException("No request dispatcher for name '"
                    + servletName + "' available");
        }

        BufferedResponse servletResponse = new BufferedResponse();
        rd.forward(requestWrapper, servletResponse);

        requestWrapper.setAttribute(IncludeComponent.INCLUDE_ATTRIBUTE_NAME, null);
        return new ByteArrayInputStream(servletResponse.getContentBuffer());
    }


    protected String getDescriptionInternal() {
        return "Inserts a feed (RSS, Atom) component on the page";
    }


    protected Map<String, String> getParameterDescriptionsInternal() {
        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put(PARAMETER_URL, PARAMETER_URL_DESC);
        map.put(PARAMETER_MAX_MESSAGES, PARAMETER_MAX_MESSAGES_DESC);
        map.put(PARAMETER_FEED_TITLE, PARAMETER_FEED_TITLE_DESC);
        map.put(PARAMETER_FEED_DESCRIPTION, PARAMETER_FEED_DESCRIPTION_DESC);
        map.put(PARAMETER_ITEM_DESCRIPTION, PARAMETER_ITEM_DESCRIPTION_DESC);
        map.put(PARAMETER_ALL_MESSAGES_LINK, PARAMETER_ALL_MESSAGES_LINK_DESC);
        map.put(PARAMETER_PUBLISHED_DATE, PARAMETER_PUBLISHED_DATE_DESC);
        map.put(PARAMETER_SORT, PARAMETER_SORT_DESC);
        return map;
    }


    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }


    private class RequestWrapper extends HttpServletRequestWrapper {

        private String requestUri;
        private String queryString;
        private Map<String, String> params = new HashMap<String, String>();
        
        public RequestWrapper(HttpServletRequest request, String uri) {
            super(request);
            if (uri.indexOf("?") == -1) {
                this.requestUri = uri;
            } else {
                this.requestUri = uri.substring(0, uri.indexOf("?"));
                this.queryString = uri.substring(uri.indexOf("?") + 1);
                StringTokenizer tokenizer = new StringTokenizer(this.queryString, "&");
                while (tokenizer.hasMoreTokens()) {
                    String s = tokenizer.nextToken();
                    if (s.indexOf("=") == -1) {
                        params.put(s, null);
                    } else {
                        params.put(s.substring(0, s.indexOf("=")),
                                   s.substring(s.indexOf("=") + 1));
                    }
                }
            }
        }
        
        public String getRequestURI() {
            return requestUri;
        }

        public String getQueryString() {
            return this.queryString;
        }

        public String getParameter(String name) {
            return this.params.get(name);
        }

        public Map<String, String> getParameterMap() {
            return Collections.unmodifiableMap(this.params);
        }

        public Enumeration<String> getParameterNames() {
            return Collections.enumeration(this.params.keySet());
        }

        public String[] getParameterValues(String name) {
            String value = this.params.get(name);
            if (value == null)
                return null;
            return new String[] {value};
        }
    }

}

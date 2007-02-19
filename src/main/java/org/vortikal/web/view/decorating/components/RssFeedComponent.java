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

import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.web.servlet.View;

import org.vortikal.web.RequestContext;
import org.vortikal.web.servlet.BufferedResponse;
import org.vortikal.web.view.decorating.DecoratorRequest;
import org.vortikal.web.view.decorating.DecoratorResponse;

// XXX: this class currently depends on the thread safety of the
// SyndFeed implementation: if it turns out that it is not thread
// safe, its data has to be extracted data to a custom bean after
// fetching a feed.
// 
public class RssFeedComponent extends AbstractDecoratorComponent {

    private static Log logger = LogFactory.getLog(RssFeedComponent.class);
    
    private int readTimeout = -1;
    private int connectTimeout = -1;

    private Map cache = new HashMap();
    private long cacheTimeout = 5 * 60 * 1000;

    private String identifier = "Anonymous Feed Fetcher";

    private View view;

    private String defaultDateFormat = "yyyy-MM-dd HH:mm";

    public void setView(View view) {
        this.view = view;
    }
    
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public void setConnectTimeoutSeconds(int connectTimeout) {
        this.connectTimeout = connectTimeout * 1000;
    }

    public void setReadTimeoutSeconds(int readTimeout) {
        this.readTimeout = readTimeout * 1000;
    }
    

    public void setCacheSeconds(int cacheSeconds) {
        this.cacheTimeout = cacheSeconds * 1000;
    }
    
    public void setDefaultDateFormat(String defaultDateFormat) {
        if (defaultDateFormat == null) {
            throw new IllegalArgumentException("Date format not valid");
        }
        this.defaultDateFormat = defaultDateFormat;
    }

    public void render(DecoratorRequest request, DecoratorResponse response)
        throws Exception {
        String result = null;
        String address = request.getStringParameter("address");
        if (address == null) {
            throw new DecoratorComponentException(
                "Component parameter 'address' is required");
        }

        boolean includeLogo = "true".equals(request.getParameter("includeLogo"));
        boolean includeTitle = "true".equals(request.getParameter("includeTitle"));
        boolean includeDescription = "true".equals(request.getParameter(
                                                       "includeDescription"));
        boolean includePublishedDate = "true".equals(request.getParameter(
                                                         "includePublishedDate"));
        boolean includeUpdatedDate = "true".equals(request.getParameter(
                                                       "includeUpdatedDate"));

        String dateFormat = request.getStringParameter("dateFormat");
        if (dateFormat == null) {
            dateFormat = this.defaultDateFormat;
        }

        Integer maxMsgs = new Integer(5);
        String numStr = request.getStringParameter("maxMsgs");
        if (numStr != null) {
            try {
                maxMsgs = Integer.valueOf(numStr);
            } catch (Exception e) { }
        }

        SyndFeed feed = getFeed(address);

        Map conf = new HashMap();
        conf.put("includeLogo", new Boolean(includeLogo));
        conf.put("includeTitle", new Boolean(includeTitle));
        conf.put("includeDescription", new Boolean(includeDescription));
        conf.put("includePublishedDate", new Boolean(includePublishedDate));
        conf.put("includeUpdatedDate", new Boolean(includeUpdatedDate));
        conf.put("maxMsgs", maxMsgs);

        Map model = new HashMap();

        model.put("feed", feed);
        model.put("dateFormatter", new DateFormatter(dateFormat));
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

    public SyndFeed getFeed(String address) throws Exception {

        if (this.cacheTimeout <= 0) {
            if (logger.isDebugEnabled()) {
                logger.debug("Returning uncached feed: '" + address + "'");
            }
            return fetchFeed(address);
        }

        FeedItem feed = (FeedItem) this.cache.get(address);
        if (feed == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Caching feed: '" + address + "'");
            }
            cacheFeed(address);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Returning feed '" + address + "' from cache");
        }

        feed = (FeedItem) this.cache.get(address);
        if (feed.getTimestamp().getTime() + this.cacheTimeout <= System.currentTimeMillis()) {
            triggerFeedRefresh(address);
        }


        return feed.getFeed();
    }

    private void triggerFeedRefresh(final String address) {
        Runnable fetcher = new Runnable() {
           public void run() {
              try {
                 cacheFeed(address);
              } catch (Exception e) {
                 logger.info("Error refreshing feed '" + address + "'", e);
              }
           }
        };
        new Thread(fetcher).start();
    }
    


    private synchronized void cacheFeed(String address) throws Exception {

        FeedItem item = (FeedItem) this.cache.get(address);
        long now = new Date().getTime();

        if (item == null ||
            (item.getTimestamp().getTime() + this.cacheTimeout <= now)) {
            SyndFeed feed = fetchFeed(address);
            this.cache.put(address, new FeedItem(feed));
            logger.info("Cached feed '" + address + "'");
        }
    }



    private SyndFeed fetchFeed(String address) throws Exception {

        URLConnection connection = new URL(address).openConnection();
        setTimeouts(connection);

        connection.setRequestProperty("User-Agent", this.identifier);
        connection.setUseCaches(true);

        XmlReader xmlReader = new XmlReader(connection.getInputStream());
        SyndFeedInput input = new SyndFeedInput();

        SyndFeed feed = input.build(xmlReader);
        return feed;
    }
    

    private void setTimeouts(URLConnection connection) {
        // XXX: In Java 1.5 timeouts can (and should be) be specified
        // on a URLConnection. Coding these using reflection until we
        // are officially on 1.5:

        try {
            java.lang.reflect.Method setConnectTimeout = connection.getClass().getMethod(
                "setConnectTimeout", new Class[]{int.class});
            java.lang.reflect.Method setReadTimeout = connection.getClass().getMethod(
                "setReadTimeout", new Class[]{int.class});
            if (this.connectTimeout > 0) {
                setConnectTimeout.invoke(connection, new java.lang.Object[]{
                        new Integer(this.connectTimeout)});
            }
            if (this.readTimeout > 0) {
                setReadTimeout.invoke(connection, new java.lang.Object[]{
                        new Integer(this.readTimeout)});
            }
            
        } catch (Throwable t) {
            // Connection timeouts not available
        }
    }
    
    private class FeedItem {
        private SyndFeed feed;
        private Date timestamp;

        public FeedItem(SyndFeed feed) {
            this.feed = feed;
            this.timestamp = new Date();
        }

        public SyndFeed getFeed() {
            return this.feed;
        }

        public Date getTimestamp() {
            return this.timestamp;
        }
    }

    private class DateFormatter {
        private SimpleDateFormat dateFormat; 

        public DateFormatter(String dateFormat) {
            this.dateFormat = new SimpleDateFormat(dateFormat);
        }
        
        public String formatDate(Date date) {
            String formattedDate = "no date present";
            if (date != null) {
                formattedDate = this.dateFormat.format(date);
            }
            return formattedDate;
        }
    }


}

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
package org.vortikal.web.decorating.components;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.vortikal.text.html.HtmlElement;
import org.vortikal.text.html.HtmlFragment;
import org.vortikal.util.cache.ContentCache;
import org.vortikal.web.RequestContext;
import org.vortikal.web.decorating.DecoratorRequest;
import org.vortikal.web.decorating.DecoratorResponse;
import org.vortikal.web.service.URL;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;

public class AggregatedFeedsComponent extends AbstractFeedComponent {

    private ContentCache<String, SyndFeed> cache;
    private LocalFeedFetcher localFeedFetcher;

    public void setContentCache(ContentCache<String, SyndFeed> cache) {
        this.cache = cache;
    }

    public void setLocalFeedFetcher(LocalFeedFetcher localFeedFetcher) {
        this.localFeedFetcher = localFeedFetcher;
    }

    @Override
    protected void processModel(Map<Object, Object> model, DecoratorRequest request, DecoratorResponse response)
            throws Exception {
        super.processModel(model, request, response);

        Map<String, Object> conf = new HashMap<String, Object>();

        String urls = request.getStringParameter(Parameter.URLS.getId());
        if (urls == null) {
            throw new DecoratorComponentException("Component parameter 'urls' is required");
        }

        String feedTitle = request.getStringParameter(Parameter.FEED_TITLE.getId());
        if (feedTitle != null) {
            conf.put("feedTitleValue", feedTitle.trim());
        }

        if (prameterHasValue(PARAMETER_ITEM_PICTURE, "true", request)) {
            conf.put("itemPicture", true);
        }

        if (!prameterHasValue(Parameter.DISPLAY_CHANNEL.getId(), "false", request)) {
            conf.put("displayChannel", true);
        }

        if (prameterHasValue(Parameter.ITEM_DESCRIPTION.getId(), "true", request)) {
            conf.put("itemDescription", true);
        }

        conf.put("maxMsgs", 10);
        String maxMsgsString = request.getStringParameter(Parameter.MAX_MESSAGES.getId());
        if (maxMsgsString != null) {
            try {
                int tmpInt = Integer.parseInt(maxMsgsString);
                if (tmpInt > 0) {
                    conf.put("maxMsgs", tmpInt);
                }
            } catch (Exception e) {
            }
        }

        String publishedDateString = request.getStringParameter(Parameter.PUBLISHED_DATE.getId());
        if ("none".equals(publishedDateString)) {
            conf.put("publishedDate", null);
        } else if ("date".equals(publishedDateString)) {
            conf.put("publishedDate", "short");
        } else {
            conf.put("publishedDate", "long");
        }

        // Typical sort strings we handle:
        // asc
        // item-title
        // item-title desc
        // desc item-title
        // etc..
        String sortString = request.getStringParameter(Parameter.SORT.getId());
        boolean directionSpecified = false; // Indicates explicitly set sort
                                            // direction
        if (sortString != null) {
            StringTokenizer tokenizer = new StringTokenizer(sortString);
            while (tokenizer.hasMoreTokens()) {
                String token = tokenizer.nextToken();
                if ("item-title".equals(token)) {
                    conf.put("sortByTitle", true);
                    if (!directionSpecified) {
                        // Set to default for title, if not already specified.
                        conf.put("sortAscending", true);
                    }
                } else if ("asc".equalsIgnoreCase(token)) {
                    conf.put("sortAscending", true);
                    directionSpecified = true;
                } else if ("desc".equalsIgnoreCase(token)) {
                    conf.remove("sortAscending");
                    directionSpecified = true;
                }
            }
        }

        boolean includeIfEmpty = true;
        String includeIfEmptyParam = request.getStringParameter(Parameter.INCLUDE_IF_EMPTY.getId());
        if ("false".equalsIgnoreCase(includeIfEmptyParam)) {
            includeIfEmpty = false;
        }
        conf.put("includeIfEmpty", includeIfEmpty);

        String displayCategoriesParam = request.getStringParameter(Parameter.DISPLAY_CATEGORIES.getId());
        if (displayCategoriesParam != null && "true".equalsIgnoreCase(displayCategoriesParam)) {
            conf.put("displayCategories", true);
        }

        SyndFeed feed = new SyndFeedImpl();
        feed.setTitle("Aggregated Feed");
        feed.setDescription("Vortex Aggregated Feed");
        feed.setAuthor("Vortex");
        feed.setLink("http://www.uio.no");

        List<SyndEntry> entries = new ArrayList<SyndEntry>();
        feed.setEntries(entries);

        Map<SyndEntry, SyndFeed> feedMapping = new HashMap<SyndEntry, SyndFeed>();

        boolean displayChannel = conf.get("displayChannel") != null && (Boolean) conf.get("displayChannel");
        if (displayChannel) {
            model.put("feedMapping", new FeedMapping(feedMapping));
        }

        URL requestURL = RequestContext.getRequestContext().getRequestURL();

        String[] urlArray = urls.split(",");
        Map<String, String> imgMap = new HashMap<String, String>();
        Map<String, String> descriptionNoImage = new HashMap<String, String>();

        parseFeeds(request, entries, feedMapping, imgMap, descriptionNoImage, requestURL, urlArray);

        try {
            sort(entries);
        } catch (MissingPublishedDateException e) {
            SyndFeed f = feedMapping.get(e.getEntry());
            throw new MissingPublishedDateException("Unable to sort feed '" + f.getUri()
                    + "': does not contain published date");
        }

        List<String> elementOrder = getElementOrder(PARAMETER_FEED_ELEMENT_ORDER, request);
        model.put("elementOrder", elementOrder);

        model.put("descriptionNoImage", descriptionNoImage);
        model.put("imageMap", imgMap);

        String displayIfEmptyMessage = request.getStringParameter(PARAMETER_IF_EMPTY_MESSAGE);
        if (displayIfEmptyMessage != null && displayIfEmptyMessage.length() > 0) {
            model.put("displayIfEmptyMessage", displayIfEmptyMessage);
        }

        model.put("feed", feed);
        model.put("conf", conf);
    }

    void sort(List<SyndEntry> entries) throws MissingPublishedDateException {
        Collections.sort(entries, new Comparator<SyndEntry>() {
            public int compare(SyndEntry entry1, SyndEntry entry2) {

                Date pubDate1 = entry1.getPublishedDate();
                if (pubDate1 == null) {
                    throw new MissingPublishedDateException(entry1);
                }
                Date pubDate2 = entry2.getPublishedDate();
                if (pubDate2 == null) {
                    throw new MissingPublishedDateException(entry2);
                }
                return pubDate2.compareTo(pubDate1);
            }
        });
    }

    void parseFeeds(DecoratorRequest request, List<SyndEntry> entries, Map<SyndEntry, SyndFeed> feedMapping,
            Map<String, String> imgMap, Map<String, String> descriptionNoImage, URL requestURL, String[] urlArray)
            throws Exception {

        for (String url : urlArray) {
            url = url.trim();
            SyndFeed tmpFeed = null;
            URL feedURL = requestURL.relativeURL(url);
            URL baseURL;
            try {
                if (feedURL.getHost().equals(requestURL.getHost())) {
                    baseURL = new URL(requestURL);
                    retrieveLocalResource(feedURL);
                    tmpFeed = this.localFeedFetcher.getFeed(feedURL, request);
                } else {
                    baseURL = new URL(feedURL);
                    tmpFeed = this.cache.get(url);
                }
            } catch (Exception e) {
                String m = e.getMessage();
                if (m == null) {
                    m = e.getClass().getName();
                }
                throw new RuntimeException("Could not read feed url " + url + ": " + m);
            }
            if (tmpFeed == null) {
                throw new RuntimeException("Unable to load feed: " + url);
            }
            @SuppressWarnings("unchecked")
            List<SyndEntry> tmpEntries = tmpFeed.getEntries();
            boolean filter = !prameterHasValue(PARMATER_DISPLAY_CLASS, "true", request);
            for (SyndEntry entry : tmpEntries) {
                feedMapping.put(entry, tmpFeed);
                HtmlFragment description = getDescription(entry, baseURL, requestURL, filter);
                if (description == null) {
                    descriptionNoImage.put(entry.toString(), null);
                    continue;
                }
                HtmlElement image = removeImage(description);
                if (image != null) {
                    imgMap.put(entry.toString(), image.getEnclosedContent());
                }
                descriptionNoImage.put(entry.toString(), description.getStringRepresentation());
            }
            entries.addAll(tmpEntries);
        }
    }

    @SuppressWarnings("serial")
    class MissingPublishedDateException extends RuntimeException {
        private SyndEntry entry;

        public MissingPublishedDateException(String message) {
            super(message);
        }

        public MissingPublishedDateException(SyndEntry entry) {
            super();
            this.entry = entry;
        }

        public SyndEntry getEntry() {
            return entry;
        }
    }

    protected String getDescriptionInternal() {
        return "Inserts a feed (RSS, Atom) component on the page";
    }

    private enum Parameter {
        URLS("urls", "Comma separated list of feed urls."), FEED_TITLE("feed-title",
                "An optional string to show as feed title"), DISPLAY_CHANNEL("display-channel",
                "Defaults to 'true', displaying the items source feed"), SORT(
                "sort",
                "Default sorted by published date. Set to 'item-title' to sort by this instead. "
                        + "You can control the direction of the sorting by using the keywords 'asc' or 'desc'. "
                        + "Usage examples: sort=[asc], sort=[item-title desc], sort=[published-date asc], etc. "
                        + "The default is descending (newest first) for published date and ascending when sorting by 'item-title'."), PUBLISHED_DATE(
                "published-date",
                "How to display published date, defaults to date and time. Set to 'date' to only display the date, or 'none' to not show the date"), MAX_MESSAGES(
                "max-messages", "The max number of messages to display, defaults to 10"), ITEM_DESCRIPTION(
                "item-description", "Must be set to 'true' to show item descriptions"), INCLUDE_IF_EMPTY(
                "include-if-empty", "Set to 'false' if you don't want to display empty feeds. Default is 'true'."), DISPLAY_CATEGORIES(
                "display-categories", "Set to 'true' if feed elements should display contents of category field.");

        private final String id;
        private final String desc;

        private Parameter(String id, String desc) {
            this.id = id;
            this.desc = desc;
        }

        public String getId() {
            return id;
        }

        public String getDesc() {
            return desc;
        }
    }

    protected Map<String, String> getParameterDescriptionsInternal() {
        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put(PARAMETER_ITEM_PICTURE, PARAMETER_ITEM_PICTURE_DESC);
        map.put(PARAMETER_IF_EMPTY_MESSAGE, PARAMETER_IF_EMPTY_MESSAGE_DESC);
        map.put(PARAMETER_FEED_ELEMENT_ORDER, PARAMETER_FEED_ELEMENT_ORDER_DESC);
        map.put(Parameter.URLS.getId(), Parameter.URLS.getDesc());
        map.put(Parameter.FEED_TITLE.getId(), Parameter.FEED_TITLE.getDesc());
        map.put(Parameter.MAX_MESSAGES.getId(), Parameter.MAX_MESSAGES.getDesc());
        map.put(Parameter.DISPLAY_CHANNEL.getId(), Parameter.DISPLAY_CHANNEL.getDesc());
        map.put(Parameter.ITEM_DESCRIPTION.getId(), Parameter.ITEM_DESCRIPTION.getDesc());
        map.put(Parameter.PUBLISHED_DATE.getId(), Parameter.PUBLISHED_DATE.getDesc());
        map.put(Parameter.SORT.getId(), Parameter.SORT.getDesc());
        map.put(Parameter.INCLUDE_IF_EMPTY.getId(), Parameter.INCLUDE_IF_EMPTY.getDesc());
        map.put(Parameter.DISPLAY_CATEGORIES.getId(), Parameter.DISPLAY_CATEGORIES.getDesc());
        return map;
    }

    public class FeedMapping {
        Map<SyndEntry, SyndFeed> feedMapping = new HashMap<SyndEntry, SyndFeed>();

        public FeedMapping(Map<SyndEntry, SyndFeed> feedMapping) {
            super();
            this.feedMapping = feedMapping;
        }

        public String getTitle(SyndEntry entry) {
            return this.feedMapping.get(entry).getTitle();
        }

        public String getUrl(SyndEntry entry) {
            return this.feedMapping.get(entry).getLink();
        }
    }
}

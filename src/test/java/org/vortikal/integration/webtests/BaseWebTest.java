package org.vortikal.integration.webtests;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseWebTest extends AbstractWebTest {
    
    private List<String> collectionListingElements;
    private List<String> eventListingElements;
    private List<String> articleListingElements;
    
    protected void setUp() throws Exception {
        super.setUp();
        // Elements that we expect to find on a standard collectionview
        collectionListingElements = new ArrayList<String>();
        collectionListingElements.add("collectionListing.searchComponent");
        collectionListingElements.add("collectionlisting-article.html-vrtx-resource");
        collectionListingElements.add("collectionlisting-article.html");
        collectionListingElements.add("collectionlisting-event.html-vrtx-resource");
        collectionListingElements.add("collectionlisting-event.html");
        collectionListingElements.add("standard.html-vrtx-resource");
        collectionListingElements.add("standard.html");
        // Elements we expect to find for eventlistings
        eventListingElements = new ArrayList<String>();
        eventListingElements.add("eventListing.previousEventsSearchComponent");
        eventListingElements.add("previousevent.html-vrtx-resource");
        eventListingElements.add("previousevent.html");
        eventListingElements.add("previousevent.html-time-and-place");
        eventListingElements.add("eventListing.upcomingEventsSearchComponent");
        eventListingElements.add("upcomingevent.html");
        eventListingElements.add("upcomingevent.html-vrtx-resource");
        eventListingElements.add("upcomingevent.html-time-and-place");
        // Elements we expect to find for articlelistings
        articleListingElements = new ArrayList<String>();
        articleListingElements.add("articleListing.searchComponent");
        articleListingElements.add("article.html");
        articleListingElements.add("article.html-vrtx-resource");
        articleListingElements.add("article.html-published-date");
    }
    
    protected List<String> getCollectionListingElements() {
        return this.collectionListingElements;
    }
    
    protected List<String> getEventListingElements() {
        return this.eventListingElements;
    }
    
    protected List<String> getArticleListingElements() {
        return this.articleListingElements;
    }
    
    protected String getBaseUrl() throws Exception {
        return getProperty(PROP_VIEW_URL);
    }

    @Override
    protected boolean requiresAuthentication() {
        return false;
    }

}

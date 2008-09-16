package org.vortikal.integration.webtests;

import java.util.ArrayList;
import java.util.List;

public abstract class WebTest extends AbstractWebTest {
    
    private List<String> collectionListingElements;
    private List<String> eventListingElements;
    private List<String> articleListingElements;
    
    protected void setUp() throws Exception {
        super.setUp();
        // Elements that we expect to find on a standard collectionview
        collectionListingElements = new ArrayList<String>();
        collectionListingElements.add("collectionListing.searchComponent");
        collectionListingElements.add("collectionListing.searchComponent-vrtx-resource");
        collectionListingElements.add("collectionListing.searchComponent-vrtx-title");
        // Elements we expect to find for eventlistings
        eventListingElements = new ArrayList<String>();
        eventListingElements.add("eventListing.previousEventsSearchComponent-vrtx-resource");
        eventListingElements.add("eventListing.upcomingEventsSearchComponent-vrtx-resource");
        eventListingElements.add("eventListing.previousEventsSearchComponent-vrtx-title");
        eventListingElements.add("eventListing.upcomingEventsSearchComponent-vrtx-title");
        eventListingElements.add("eventListing.previousEventsSearchComponent");
        eventListingElements.add("eventListing.upcomingEventsSearchComponent");
        eventListingElements.add("time-and-place");
        // Elements we expect to find for articlelistings
        articleListingElements = new ArrayList<String>();
        articleListingElements.add("articleListing.searchComponent");
        articleListingElements.add("articleListing.searchComponent-vrtx-resource");
        articleListingElements.add("articleListing.searchComponent-vrtx-title");
        articleListingElements.add("published-date");
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

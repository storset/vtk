package org.vortikal.integration.webtests.view;

import org.vortikal.integration.webtests.BaseWebTest;

public class CollectionListingTest extends BaseWebTest {
    
    public void testVortexCollections() {
        assertElementPresent("vrtx-collections");
    }
    
    public void testListing() {
        for (String elementId : getDefaultFiles()) {
        	assertLinkPresentWithExactText(elementId);
        }
        for (String elementId : getEvents()) {
        	assertLinkNotPresentWithExactText(elementId);
        }
        for (String elementId : getArticles()) {
        	assertLinkNotPresentWithExactText(elementId);
        }
    }
    
    public void testVortexFeedLink() {
        assertElementPresent("vrtx-feed-link");
    }

}

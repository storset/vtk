package org.vortikal.integration.webtests.view;

import org.vortikal.integration.webtests.BaseWebTest;

public class CollectionListingTest extends BaseWebTest {
    
    protected void setUp() throws Exception {
        super.setUp();
        prepare(this.getClass().getSimpleName().toLowerCase());
    }
    
    public void testVortexCollections() {
        assertElementPresent("vrtx-collections");
    }
    
    public void testListing() {
        for (String elementId : getCollectionListingElements()) {
            assertElementPresent(elementId);
        }
        for (String elementId : getEventListingElements()) {
            assertElementNotPresent(elementId);
        }
        for (String elementId : getArticleListingElements()) {
            assertElementNotPresent(elementId);
        }
    }
    
    public void testVortexFeedLink() {
        assertElementPresent("vrtx-feed-link");
    }

}

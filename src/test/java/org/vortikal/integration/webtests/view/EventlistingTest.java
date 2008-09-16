package org.vortikal.integration.webtests.view;

import org.vortikal.integration.webtests.WebTest;

public class EventlistingTest extends WebTest {
    
    protected void setUp() throws Exception {
        super.setUp();
        prepare(this.getClass().getSimpleName().toLowerCase());
    }
    
    public void testListing() {
        for (String elementId : getEventListingElements()) {
            assertElementPresent(elementId);
        }
        for (String elementId : getCollectionListingElements()) {
            assertElementNotPresent(elementId);
        }        
        for (String elementId : getArticleListingElements()) {
            assertElementNotPresent(elementId);
        }
    }

}

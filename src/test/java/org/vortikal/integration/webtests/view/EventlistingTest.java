package org.vortikal.integration.webtests.view;

import org.vortikal.integration.webtests.BaseWebTest;

public class EventlistingTest extends BaseWebTest {
    
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

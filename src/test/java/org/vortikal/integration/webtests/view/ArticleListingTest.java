package org.vortikal.integration.webtests.view;

import org.vortikal.integration.webtests.BaseWebTest;


public class ArticleListingTest extends BaseWebTest {
    
    protected void setUp() throws Exception {
        super.setUp();
        prepare(this.getClass().getSimpleName().toLowerCase());
    }
    
    public void testListing() {
        for (String elementId : getArticleListingElements()) {
            assertElementPresent(elementId);
        }
        for (String elementId : getCollectionListingElements()) {
            assertElementNotPresent(elementId);
        }
        for (String elementId : getEventListingElements()) {
            assertElementNotPresent(elementId);
        }
    }

}

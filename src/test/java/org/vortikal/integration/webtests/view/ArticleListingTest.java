package org.vortikal.integration.webtests.view;

import org.vortikal.integration.webtests.BaseWebTest;


public class ArticleListingTest extends BaseWebTest {
    
    public void testListing() {
        for (String elementId : getArticles()) {
            assertLinkPresentWithExactText(elementId);
        }
        for (String elementId : getDefaultFiles()) {
        	assertLinkNotPresentWithExactText(elementId);
        }
        for (String elementId : getEvents()) {
        	assertLinkNotPresentWithExactText(elementId);
        }
    }

}

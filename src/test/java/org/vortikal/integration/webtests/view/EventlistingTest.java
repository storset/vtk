package org.vortikal.integration.webtests.view;

import org.vortikal.integration.webtests.BaseWebTest;

public class EventlistingTest extends BaseWebTest {
    
    public void testListing() {
        for (String elementId : getEvents()) {
        	assertLinkPresentWithExactText(elementId);
        }
        for (String elementId : getDefaultFiles()) {
        	assertLinkNotPresentWithExactText(elementId);
        }        
        for (String elementId : getArticles()) {
        	assertLinkNotPresentWithExactText(elementId);
        }
    }

}

package org.vortikal.integration.webtests.view;

import org.vortikal.integration.webtests.BaseWebTest;

public class PathTest extends BaseWebTest {
    
    public void testVisualProfileOff() { 
    	clickLink("visualprofileoff"); 
    	assertElementPresent("breadcrumb");
    	assertLinkPresent("vrtx-manage-url");
    }

    public void testVisualProfileOn() {
     	clickLink("visualprofileon"); 
    	assertElementPresent("breadcrumb");
    	assertLinkPresent("vrtx-admin-link");
    }
    
    public void testFeedElement() {
    	assertLinkPresent("vrtx-feed-link");
    }
    
    public void testFeedList() {
    	assertLinkPresent("feedentry1.html");
    	assertLinkPresent("feedentry2.html");
    	clickLink("vrtx-feed-link");
    	String feedEntryPath = rootCollection + "/" + this.getClass().getSimpleName().toLowerCase();
    	// Links for feedentry 1&2 should be found
        assertMatch("(link href=\"http://)\\S*(/" + feedEntryPath + "/feedentry[1-2].html\"\\S*)");
    }
    
}

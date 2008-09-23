package org.vortikal.integration.webtests.view;

import org.vortikal.integration.webtests.BaseWebTest;

public class PathTest extends BaseWebTest {
	
    protected void setUp() throws Exception {
        super.setUp();
        prepare(this.getClass().getSimpleName().toLowerCase());
    }
    
    public void testVisualProfileOff() { 
    	clickLink("visualprofileoff"); 
    	assertElementPresent("breadcrumb");
    	assertElementPresent("vrtx-manage-url");
    }

    public void testVisualProfileOn() {
     	clickLink("visualprofileon"); 
    	assertElementPresent("breadcrumb");
    	assertElementPresent("vrtx-admin-link");
    }
    
    public void testFeedElement() {
    	assertLinkPresent("vrtx-feed-link");
    }
    
    public void testFeedList() {
    	assertLinkPresent("feedentry1.html");
    	assertLinkPresent("feedentry2.txt");
    	clickLink("vrtx-feed-link");
    }
    
}

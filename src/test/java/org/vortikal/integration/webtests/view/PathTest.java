package org.vortikal.integration.webtests.view;

import org.vortikal.integration.webtests.BaseWebTest;

public class PathTest extends BaseWebTest {
    
    public void testFeedList() {
    	assertLinkPresentWithExactText("feedentry1");
    	assertLinkPresentWithExactText("feedentry2");
    	clickLink("vrtx-feed-link");
    	String feedEntryPath = rootCollection + "/" + this.getClass().getSimpleName().toLowerCase();
    	// Links for feedentry 1&2 should be found
        assertMatch("(link href=\"http://)\\S*(/" + feedEntryPath + "/feedentry1.html\"\\S*)");
        assertMatch("(link href=\"http://)\\S*(/" + feedEntryPath + "/feedentry2.html\"\\S*)");
        assertNoMatch("(link href=\"http://)\\S*(/" + feedEntryPath + "/feedentry[3-9].html\"\\S*)");
    }
    
}

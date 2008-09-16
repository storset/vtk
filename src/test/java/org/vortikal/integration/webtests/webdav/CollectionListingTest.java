package org.vortikal.integration.webtests.webdav;

import org.vortikal.integration.webtests.WebdavWebTest;

public class CollectionListingTest extends WebdavWebTest {

    protected void setUp() throws Exception {
        super.setUp();
        prepare(this.getClass().getSimpleName().toLowerCase());
    }
    
    public void testBreadCrumb() {
        assertElementPresent("breadcrumb");
    }
    
    public void testWebdavMessage() {
        assertElementPresent("webdavmessage");
    }

}

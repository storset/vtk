package org.vortikal.integration.webtests.webdav;

import org.vortikal.integration.webtests.BaseWebdavWebTest;

public class CollectionListingTest extends BaseWebdavWebTest {

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

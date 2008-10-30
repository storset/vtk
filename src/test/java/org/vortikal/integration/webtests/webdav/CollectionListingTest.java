package org.vortikal.integration.webtests.webdav;

import org.vortikal.integration.webtests.BaseWebdavWebTest;

public class CollectionListingTest extends BaseWebdavWebTest {

    public void testBreadCrumb() {
        assertElementPresent("vrtx-breadcrumb");
    }
    
    public void testWebdavMessage() {
        assertElementPresent("vrtx-webdavmessage");
    }

}

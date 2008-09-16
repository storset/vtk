package org.vortikal.integration.webtests.admin;

import org.vortikal.integration.webtests.AuthenticatedWebTest;

public class ManageResourceTest extends AuthenticatedWebTest {
    
    protected void setUp() throws Exception {
        super.setUp();
        prepare(this.getClass().getSimpleName().toLowerCase());
    }
    
    /**
     * Create, copy and delete a collection
     */
    public void testManageCollection() {
        // TODO implement
    }
    
    /**
     * Create, copy and delete a document
     */
    public void testManageDocument() {
        // TODO implement
    }

}

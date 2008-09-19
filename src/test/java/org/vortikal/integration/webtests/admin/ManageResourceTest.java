package org.vortikal.integration.webtests.admin;

import org.vortikal.integration.webtests.BaseAuthenticatedWebTest;

public class ManageResourceTest extends BaseAuthenticatedWebTest {
    
    protected void setUp() throws Exception {
        super.setUp();
        prepare(this.getClass().getSimpleName().toLowerCase());
    }
    
    /**
     * Create and delete a collection
     */
    public void testManageCollection() {
        createAndDeleteResource("createCollectionService", "createcollection", "testcollection");
    }
    
    /**
     * Create and delete a document
     */
    public void testManageDocument() {
        createAndDeleteResource("createDocumentService", "createDocumentForm", "testdocument");
    }
    
    /**
     * Copy a resource
     */
    public void testCopyResource() {
        // TODO implement
    }
    
    private void createAndDeleteResource(String serviceName, String formName, String resourceName) {
        // Start of fresh
        assertLinkNotPresent(resourceName);
        assertFormNotPresent(formName);
        
        // Create resource
        clickLink(serviceName);
        assertFormPresent(formName);
        setWorkingForm(formName);
        setTextField("name", resourceName);
        submit();
        // Verify it's there
        assertLinkPresent(resourceName);
        
        // Delete resource and copy, verify result
        // Ignore the javascript popup
        setScriptingEnabled(false);
        clickLink("delete-" + resourceName);
        assertLinkNotPresent(resourceName);
        
    }

}

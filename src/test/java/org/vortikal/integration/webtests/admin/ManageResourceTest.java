package org.vortikal.integration.webtests.admin;

import org.apache.commons.lang.StringUtils;
import org.vortikal.integration.webtests.BaseAuthenticatedWebTest;

public class ManageResourceTest extends BaseAuthenticatedWebTest {

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
        createAndDeleteResource("createDocumentService", "createDocumentForm", "testdocument.txt");
    }

    /**
     * Copy a resource to same folder
     */
    public void testCopyResourceToSameFolder() {
        copyResource(null);
    }

    /**
     * Copy a resource to another folder
     */
    public void testCopyResourceToOtherFolder() {
        copyResource("copyfolder");
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
        // Delete it
        deleteResource(resourceName);
    }

    private void copyResource(String folderToCopyTo) {
        String resourceName = "testcopy";

        assertLinkPresent(resourceName + ".html");
        String className = this.getClass().getSimpleName().toLowerCase();
        checkCheckbox("/" + rootCollection + "/" + className + "/" + resourceName + ".html");
        clickLink("copyResourceService");

        String copiedResourceName = resourceName;
        if (!StringUtils.isBlank(folderToCopyTo)) {
            clickLink(folderToCopyTo);
            copiedResourceName = copiedResourceName + ".html";
        } else {
            copiedResourceName = copiedResourceName + "(1).html";
        }
        clickLink("copyToSelectedFolderService");
        assertLinkPresent(copiedResourceName);
        deleteResource(copiedResourceName);
    }

    /**
     * Delete resource and verify result
     */
    private void deleteResource(String resourceName) {
        // Ignore the javascript popup (asks for verification -> "do you wanna delete ... ?")
        setScriptingEnabled(false);
        clickLink("delete-" + resourceName);
        assertLinkNotPresent(resourceName);

    }

}

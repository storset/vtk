package org.vortikal.integration.webtests.admin;

import org.apache.commons.lang.StringUtils;
import org.vortikal.integration.webtests.BaseAuthenticatedWebTest;

public class ManageResourceTest extends BaseAuthenticatedWebTest {

    /**
     * Create and delete a collection
     */
    public void testCreateCollection() {
        final String resourceName = "testcollection";
    	createResource(CREATE_COLLECTION_SERVICE, CREATE_COLLECTION_FORM, resourceName);
    	// Verify it's there
        assertLinkPresentWithExactText(resourceName);
        // Delete it
        deleteResource(resourceName);
    }

    /**
     * Create and delete a document
     */
    public void testCreateDocument() {
    	final String resourceName = "testdocument.txt";
        createResource(CREATE_DOCUMENT_SERVICE, CREATE_DOCUMENT_FORM, resourceName);
        // Verify it's there
        assertLinkPresentWithExactText(resourceName);
        // Delete it
        deleteResource(resourceName);
    }

    /**
     * Copy a resource to same folder
     */
    public void testCopyDocumentToSameFolder() {
        copyResource(null);
    }

    /**
     * Copy a resource to another folder
     */
    public void testCopyDocumentToOtherFolder() {
        copyResource("copyfolder");
    }

    private void copyResource(String folderToCopyTo) {
        String resourceName = "testcopy";

        assertLinkPresentWithExactText(resourceName + ".html");
        String className = this.getClass().getSimpleName().toLowerCase();
        checkCheckbox("/" + rootCollection + "/" + className + "/" + resourceName + ".html");
        clickLink("copyResourceService");

        String copiedResourceName = resourceName;
        if (StringUtils.isNotBlank(folderToCopyTo)) {
            clickLinkWithExactText(folderToCopyTo);
            copiedResourceName = copiedResourceName + ".html";
        } else {
            copiedResourceName = copiedResourceName + "(1).html";
        }
        clickLink("copyToSelectedFolderService");
        assertLinkPresentWithExactText(copiedResourceName);
        deleteResource(copiedResourceName);
    }

}

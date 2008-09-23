package org.vortikal.integration.webtests.admin;

import org.vortikal.integration.webtests.BaseAuthenticatedWebTest;

public class ManageResourceTest extends BaseAuthenticatedWebTest {

	private String className = this.getClass().getSimpleName().toLowerCase();

	protected void setUp() throws Exception {
		super.setUp();
		prepare(className);
	}

	/**
	 * Create and delete a collection
	 */
	public void testManageCollection() {
		createAndDeleteResource("createCollectionService", "createcollection",
				"testcollection");
	}

	/**
	 * Create and delete a document
	 */
	public void testManageDocument() {
		createAndDeleteResource("createDocumentService", "createDocumentForm",
				"testdocument");
	}

	/**
	 * Copy a resource to same folder
	 */
	public void testCopyResourceToSameFolder() {

		String resourceName = "testcopy";

		assertLinkPresent(resourceName + ".html");
		checkCheckbox("/" + rootCollection + "/" + className + "/"
				+ resourceName + ".html");
		clickLink("copyResourceService");
		clickLink("copyToSelectedFolderService");

		assertLinkPresent(resourceName + "(1).html");
		deleteResource(resourceName + "(1).html");

	}

	/**
	 * Copy a resource to another folder
	 * 
	 */
	public void testCopyResourceToOtherFolder() {

		String resourceName = "testcopy";

		assertLinkPresent(resourceName + ".html");
		checkCheckbox("/" + rootCollection + "/" + className + "/"
				+ resourceName + ".html");
		clickLink("copyResourceService");
		clickLink("copyfolder");
		clickLink("copyToSelectedFolderService");

		assertLinkPresent(resourceName + ".html");
		deleteResource(resourceName + ".html");

	}

	private void createAndDeleteResource(String serviceName, String formName,
			String resourceName) {
		// Start of fresh
		assertLinkNotPresent(resourceName);
		assertFormNotPresent(formName);

		// Create resource
		clickLink(serviceName);
		assertFormPresent(formName);
		setWorkingForm(formName);
		setTextField("name", resourceName);
		submit();
		// Verify it's there and delete it
		assertLinkPresent(resourceName);
		deleteResource(resourceName);

	}

	/**
	 * Delete resource and verify result
	 * 
	 */
	private void deleteResource(String resourceName) {

		// Ignore the javascript popup
		setScriptingEnabled(false);
		clickLink("delete-" + resourceName);
		assertLinkNotPresent(resourceName);

	}

}

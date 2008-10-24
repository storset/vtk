package org.vortikal.integration.webtests.admin;

import org.vortikal.integration.webtests.BaseAuthenticatedWebTest;

public class ManageCollectionTest extends BaseAuthenticatedWebTest {
	
	/**
	 * Test changing name of folder
	 */
	public void testChangeFolderName() {
		final String testfolder = "testfolder";
		final String renamedTestfolder = "RENAMEDtestfolder";
		
		createResource(CREATE_COLLECTION_SERVICE, CREATE_COLLECTION_FORM, testfolder);
		assertLinkPresent(testfolder);
		clickLink(testfolder);
		
		clickLink("renameService");
		assertFormPresent("rename");
        setWorkingForm("rename");
        setTextField("name", renamedTestfolder);
        submit();
        
        clickLink("navigateToParent");
        assertLinkPresent(renamedTestfolder);
        clickLink(renamedTestfolder);
        
		deleteResource(null);
		
		assertLinkNotPresent(renamedTestfolder);
	}
	
	/**
	 * Test proper escaping of foldername
	 */
	public void testCheckFolderName() {
		checkFolderName("test folder", "test-folder");
		checkFolderName("testæfolder", "testaefolder");
		checkFolderName("teståfolder", "testaafolder");
		checkFolderName("testøfolder", "testoefolder");
	}

	private void checkFolderName(String foldername, String expected) {
		createResource(CREATE_COLLECTION_SERVICE, CREATE_COLLECTION_FORM, foldername);
		assertLinkPresent(expected);
		deleteResource(expected);
	}
	

}

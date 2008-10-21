package org.vortikal.integration.webtests.admin;

import net.sourceforge.jwebunit.exception.TestingEngineResponseException;
import net.sourceforge.jwebunit.html.Cell;
import net.sourceforge.jwebunit.html.Row;
import net.sourceforge.jwebunit.html.Table;

import org.vortikal.integration.webtests.BaseAuthenticatedWebTest;

public class MetaDataTest extends BaseAuthenticatedWebTest {
	
	// Under MetaDataTest for About-tab in Admin
	// TODO: Add more tests + more refactoring.
	
	/**
	 * 
	 * Test if parentfolder->last-modified changes to subfolder->creation time.
	 * 
	 * "Systemtest Del 7 - Redigering av metadata" - Test 1
	 * 
	 */
	public void testParentFolderLastModified() {
		
		String parentFolderName = "parentfolder";
		String subFolderName = "subfolder";
		int cropDateModifiedValue = 19;
		
		// Problem: The test will fail if single seconds is included in last-modified.
		// Solution: Cropfactor = 19 gives [ cropped away ] :
		// Long date : November 21, 2008 3[ :25:12 PM CEST by root@localhost ]
		// Short date: May 4, 2008 10:32:4[ 8 AM CEST by vortex@localhost ]
		
		// 19 seems to work for long and short dates.
		
		// TODO: Test will fail from 1 - 9 May next year when 12hr clock :)
		// ::::: |-> May 4, 2008 2:32:48[ PM CEST by root@localhost ]
		
		createFolder(parentFolderName);
		
		// Goto parent folder
		clickLink(parentFolderName);
		
		// Get last-modified Parent
		String parentFolderLastModified = getLastModifiedAbout().substring(0, cropDateModifiedValue);
		
		// Return to Contents-tab
		clickLink("manageCollectionListingService");
		
		createFolder(subFolderName);
		
		// Goto subfolder
		clickLink(subFolderName);
		
		// Get last-modified Subfolder
		String subFolderLastModified = getLastModifiedAbout().substring(0, cropDateModifiedValue);
		
		// Delete and delete
		deleteResourceFromMenu(subFolderName);
		
		deleteResourceFromMenu(parentFolderName);
		
		// Checks if last-modified on parent is changed to subfolder creation time.
		assertEquals(subFolderLastModified, parentFolderLastModified);
		
	}
	
	/**
	 * 
	 * Test Web Address.
	 * 
	 * "Systemtest Del 7 - Redigering av metadata" - Test 6
	 * 
	 * @throws Exception
	 * @throws TestingEngineResponseException
	 * 
	 */
	public void testWebAddress() throws TestingEngineResponseException, Exception {
		
		// Goto About
		assertLinkPresent("aboutResourceService");
		clickLink("aboutResourceService");
		
		// Click
		assertLinkPresent("aboutWebAddress");
		clickLink("aboutWebAddress");
		
		// Checks that we got to view for collection
		assertLinkPresent("vrtx-feed-link");
		
		// Go back
		gotoPage(getBaseUrl() + "automatedtestresources/metadatatest/?vrtx=admin");
		
	}
	
	/**
	 * 
	 * Test WebDAV Address.
	 * 
	 * "Systemtest Del 7 - Redigering av metadata" - Test 7
	 * 
	 * TODO: WebDAV test for document
	 * 
	 * @throws Exception
	 * @throws TestingEngineResponseException
	 * 
	 */
	public void testWebDAVAddress() throws TestingEngineResponseException, Exception {
		
		// Goto About
		assertLinkPresent("aboutResourceService");
		clickLink("aboutResourceService");
		
		// Click
		assertLinkPresent("aboutWebdavAddress");
		clickLink("aboutWebdavAddress");
		
		// Checks that we got to WebDAV listing
		assertElementPresent("webdavmessage");
		assertElementPresent("directoryListing");
		
		// Go back
		gotoPage(getBaseUrl() + "automatedtestresources/metadatatest/?vrtx=admin");
		
	}
	
	/**
	 * Get last-modified from about on resource
	 * 
	 */
	public String getLastModifiedAbout() {
		
		assertLinkPresent("aboutResourceService"); // To much assertion(?)
		clickLink("aboutResourceService");
		
		String lastModified = getTableValue("resourceInfoMain", 0, 1);
		
		return lastModified;
	}
	
	/**
	 * Get value from table by tablename, row and cell.
	 * 
	 * Put in seperate java class for reuse(?)
	 * 
	 * @param table
	 * @param row
	 * @param cell
	 * @return
	 */
	public String getTableValue(String table, int row, int cell) {
		
		assertElementPresent(table);
		Table resourceInfo = getTable(table);
		
		Row resourceTypeRow = (Row) resourceInfo.getRows().get(row);
		
		Cell resourceType = (Cell) resourceTypeRow.getCells().get(cell);
		
		return resourceType.getValue();
	}
	
	/**
	 * Create folder and verify result
	 * 
	 * TODO: Refactor in own class/package webtests/admin/utils/ResourceManager.java(?)
	 * 
	 * @param folderName
	 */
	public void createFolder(String folderName) {
		
		// Check if not parentfolder is present, and form is closed
		assertLinkNotPresent(folderName);
		assertFormNotPresent("createcollection");
		
		// Create resource
		clickLink("createCollectionService");
		assertFormPresent("createcollection");
		setWorkingForm("createcollection");
		setTextField("name", folderName);
		submit();
		
		// Check if resource was created
		assertLinkPresent(folderName);
		
	}
	
	/**
	 * Delete resource from top-menu
	 * 
	 * TODO: Refactor in own class/package webtests/admin/utils/ResourceManager.java(?)
	 * 
	 * @param folderName
	 */
	private void deleteResourceFromMenu(String resourceName) {
		
		// Ignore the javascript popup (asks for verification -> "do you wanna delete ... ?")
		setScriptingEnabled(false);
		clickLink("delete-resource");
		
	}
}

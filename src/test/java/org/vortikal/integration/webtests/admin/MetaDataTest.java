package org.vortikal.integration.webtests.admin;

import net.sourceforge.jwebunit.html.Cell;
import net.sourceforge.jwebunit.html.Row;
import net.sourceforge.jwebunit.html.Table;

import org.vortikal.integration.webtests.BaseAuthenticatedWebTest;

public class MetaDataTest extends BaseAuthenticatedWebTest {
	
	/**
	 * 
	 * Test if parentfolder->last-modified changes to subfolder->creation time.
	 * 
	 * Systemtest Del 7 - Redigering av metadata - Test 01
	 * 
	 * TODO: Refactor and add more tests for metadata del 7.
	 * 
	 */
	public void testParentFolderLastModified() {
		
		String parentFolderName = "parentfolder";
		String subFolderName = "subfolder";
		int cropDateModifiedValue = 19;
		
		// Cropfactor = 19 gives ( [ ] = cropped away ):
		
		// Long date : November 21, 2008 3[ :25:12 PM CEST by root@localhost ]
		// Short date: May 4, 2008 10:32:4[ 8 AM CEST by vortex@localhost ]
		
		// 19 seems to work for long and short dates.
		
		// The test will fail if single seconds is included in last-modified.
		
		// TODO: Test will fail from 1 - 9 May next year when 12hr clock :)
		// |-> May 4, 2008 2:32:48[ PM CEST by root@localhost ]
		
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
	 * Get last-modified from about on resource
	 * 
	 */
	public String getLastModifiedAbout() {
		
		assertLinkPresent("aboutResourceService"); // To much assertion(?)
		clickLink("aboutResourceService");
		assertElementPresent("resourceInfoMain");
		Table resourceInfo = getTable("resourceInfoMain");
		Row resourceTypeRow = (Row) resourceInfo.getRows().get(0);
		Cell resourceType = (Cell) resourceTypeRow.getCells().get(1);
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
		
		// Check if not parentfolder is present and form is closed
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

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
	 * TODO: Refactor and add more tests for metadata del 7
	 * 
	 */
	public void testParentFolderLastModified() {
		
		String parentFolderName = "parentfolder";
		String subFolderName = "subfolder";
		int cropDateModifiedValue = 20; // TODO: need to be tweaked
		
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
	 * Delete resource in listing and verify result
	 * 
	 * TODO: Refactor in own class/package webtests/admin/utils/ResourceManager.java(?)
	 * 
	 * @param folderName
	 */
	private void deleteResource(String resourceName) {
		
		assertLinkPresent(resourceName);
		
		// Ignore the javascript popup (asks for verification -> "do you wanna delete ... ?")
		setScriptingEnabled(false);
		clickLink("delete-" + resourceName);
		
		assertLinkNotPresent(resourceName);
	}
	
	/**
	 * Delete resource in top-menu
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

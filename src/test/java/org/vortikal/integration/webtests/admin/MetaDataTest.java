package org.vortikal.integration.webtests.admin;

import net.sourceforge.jwebunit.exception.TestingEngineResponseException;
import net.sourceforge.jwebunit.html.Cell;
import net.sourceforge.jwebunit.html.Row;
import net.sourceforge.jwebunit.html.Table;

import org.vortikal.integration.webtests.BaseAuthenticatedWebTest;

public class MetaDataTest extends BaseAuthenticatedWebTest {
	
	// MetaDataTest for About-tab in Admin
	// TODO: Add more tests + more refactoring (more classes in admin with general functions(?))
	
	// About Table metadata position information { id, row, cell }
	// TODO: Refactor in own class (?)
	private static final String LASTMODIFIED[] = { "resourceInfoMain", "0", "1" };
	private static final String CREATED[] = { "resourceInfoMain", "1", "1" };
	private static final String OWNER[] = { "resourceInfoMain", "2", "1" };
	private static final String RESOURCETYPE[] = { "resourceInfoMain", "3", "1" };
	private static final String WEB_ADDRESS[] = { "resourceInfoMain", "4", "1" };
	private static final String WEBDAV_ADDRESS[] = { "resourceInfoMain", "5", "1" };
	private static final String LANGUAGE[] = { "resourceInfoMain", "6", "1" };
	
	private static final String TITLE[] = { "resourceInfoContent", "0", "1" };
	private static final String KEYWORDS[] = { "resourceInfoContent", "1", "1" };
	private static final String DESCRIPTION[] = { "resourceInfoContent", "2", "1" };
	private static final String VERIFIED_DATE[] = { "resourceInfoContent", "3", "1" };
	private static final String AUTHOR[] = { "resourceInfoContent", "4", "1" };
	private static final String AUTHOR_EMAIL[] = { "resourceInfoContent", "5", "1" };
	private static final String AUTHOR_URL[] = { "resourceInfoContent", "6", "1" };
	private static final String SCIENTIFIC_DICIPLINES[] = { "resourceInfoContent", "7", "1" };
	
	private static final String HIDE_FROM_NAVIGATION[] = { "resourceInfoTechnical", "0", "1" };
	private static final String NAVIGATIONAL_IMPORTANCE[] = { "resourceInfoTechnical", "1", "1" };
	private static final String FOLDER_TYPE[] = { "resourceInfoTechnical", "2", "1" };
	
	/**
	 * Test if parentfolder->last-modified changes to subfolder->creation time.
	 * 
	 * "Systemtest Del 7 - Redigering av metadata" - Test 1
	 * 
	 */
	public void testParentFolderLastModified() {
		
		String parentFolderName = "parentfolder";
		String subFolderName = "subfolder";
		
		// 19 seems to work for long and short dates except:
		// Test will fail from 1 -> 9 May next year when 12hr clock :)
		// TODO: Check for whitespace and set substring based on length of month.
		
		int cropDateModifiedValue = 19;
		
		createFolderAndGoto(parentFolderName);
		String parentFolderLastModified = getLastModifiedAbout(true).substring(0, cropDateModifiedValue);
		
		createFolderAndGoto(subFolderName);
		String subFolderLastModified = getLastModifiedAbout(true).substring(0, cropDateModifiedValue);
		
		deleteResourceFromMenu(subFolderName);
		deleteResourceFromMenu(parentFolderName);
		
		assertEquals(subFolderLastModified, parentFolderLastModified);
	}
	
	/**
	 * Test Web Address.
	 * 
	 * "Systemtest Del 7 - Redigering av metadata" - Test 6
	 * 
	 * @throws Exception
	 * @throws TestingEngineResponseException
	 * 
	 */
	public void testWebAddress() throws TestingEngineResponseException, Exception {
		
		gotoAboutTab();
		
		checkAndGotoLink("aboutWebAddress");
		
		// Checks that we got to view for collection
		assertLinkPresent("vrtx-feed-link");
		
		gotoRootTestFolder("metadatatest");
	}
	
	/**
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
		
		gotoAboutTab();
		
		checkAndGotoLink("aboutWebdavAddress");
		
		// Checks that we got to WebDAV listing
		assertElementPresent("webdavmessage");
		assertElementPresent("directoryListing");
		
		gotoRootTestFolder("metadatatest");
	}
	
	// Navigation / functions in Admin
	// TODO: Refactor in admin navigation class(?)
	// ****************************************************************************************
	
	public void gotoAboutTab() {
		checkAndGotoLink("aboutResourceService");
	}
	
	public void gotoContentsTab() {
		checkAndGotoLink("manageCollectionListingService");
	}
	
	public void gotoFolder(String folderName) {
		checkAndGotoLink(folderName);
	}
	
	public void createFolderAndGoto(String folderName) {
		createFolder(folderName);
		checkAndGotoLink(folderName);
	}
	
	public void gotoRootTestFolder(String folderName) throws TestingEngineResponseException, Exception {
		gotoPage(getBaseUrl() + "automatedtestresources/" + folderName + "/?vrtx=admin");
	}
	
	public void checkAndGotoLink(String linkId) {
		assertLinkPresent(linkId);
		clickLink(linkId);
	}
	
	// Get values from About-tab
	// TODO: Refactor in own class(?)
	// ****************************************************************************************
	
	/**
	 * Get last-modified from About on resource
	 * 
	 * @param returnToContents
	 */
	public String getLastModifiedAbout(boolean returnToContents) {
		
		gotoAboutTab();
		
		String lastModified = getTableValue(LASTMODIFIED);
		
		if (returnToContents) {
			gotoContentsTab();
		}
		return lastModified;
	}
	
	/**
	 * Get language from About on resource
	 * 
	 * @param returnToContents
	 */
	public String getLanguageAbout(boolean returnToContents) {
		
		gotoAboutTab();
		
		String language = getTableValue(LANGUAGE);
		
		if (returnToContents) {
			gotoContentsTab();
		}
		return language;
	}
	
	// General methods for creation, deletion of resources and getting values from table.
	// TODO: Refactor in own bottom-layer core vortex webtesting function class(?)
	// ****************************************************************************************
	
	/**
	 * Get value from Table by valueToExtract.
	 * 
	 * @param valueToExtract
	 * @return
	 */
	public String getTableValue(String[] valueToExtract) {
		
		// Checks if table exists
		assertElementPresent(valueToExtract[0]);
		
		// Get the cell
		Table resourceInfo = getTable(valueToExtract[0]);
		Row resourceTypeRow = (Row) resourceInfo.getRows().get(Integer.parseInt(valueToExtract[1]));
		Cell resourceType = (Cell) resourceTypeRow.getCells().get(Integer.parseInt(valueToExtract[2]));
		
		return resourceType.getValue();
	}
	
	/**
	 * Create folder and verify result
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
	 * @param resourceName
	 */
	private void deleteResourceFromMenu(String resourceName) {
		
		// Ignore the javascript popup (asks for verification -> "do you wanna delete ... ?")
		setScriptingEnabled(false);
		clickLink("delete-resource");
	}
}

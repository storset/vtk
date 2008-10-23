package org.vortikal.integration.webtests.admin;

import net.sourceforge.jwebunit.exception.TestingEngineResponseException;
import net.sourceforge.jwebunit.html.Cell;
import net.sourceforge.jwebunit.html.Row;
import net.sourceforge.jwebunit.html.Table;

import org.vortikal.integration.webtests.BaseAuthenticatedWebTest;

public class MetaDataTest extends BaseAuthenticatedWebTest {
	
	// About Table metadata position information { id, row, cell }
	private static final String LASTMODIFIED[] = { "resourceInfoMain", "0", "1" };
	private static final String CREATED[] = { "resourceInfoMain", "1", "1" };
	private static final String OWNER[] = { "resourceInfoMain", "2", "1" };
	private static final String RESOURCETYPE[] = { "resourceInfoMain", "3", "1" };
	private static final String WEB_ADDRESS[] = { "resourceInfoMain", "4", "1" };
	private static final String WEBDAV_ADDRESS[] = { "resourceInfoMain", "5", "1" };
	private static final String LANGUAGE[] = { "resourceInfoMain", "6", "1" };
	// File
	private static final String SIZE[] = { "resourceInfoMain", "7", "1" };
	
	private static final String TITLE[] = { "resourceInfoContent", "0", "1" };
	private static final String KEYWORDS[] = { "resourceInfoContent", "1", "1" };
	private static final String DESCRIPTION[] = { "resourceInfoContent", "2", "1" };
	private static final String VERIFIED_DATE[] = { "resourceInfoContent", "3", "1" };
	private static final String AUTHOR[] = { "resourceInfoContent", "4", "1" };
	private static final String AUTHOR_EMAIL[] = { "resourceInfoContent", "5", "1" };
	private static final String AUTHOR_URL[] = { "resourceInfoContent", "6", "1" };
	private static final String SCIENTIFIC_DICIPLINES[] = { "resourceInfoContent", "7", "1" };
	// Folder
	private static final String HIDE_FROM_NAVIGATION[] = { "resourceInfoTechnical", "0", "1" };
	private static final String NAVIGATIONAL_IMPORTANCE[] = { "resourceInfoTechnical", "1", "1" };
	private static final String FOLDER_TYPE[] = { "resourceInfoTechnical", "2", "1" };
	// File
	private static final String CONTENT_TYPE[] = { "resourceInfoTechnical", "0", "1" };
	private static final String CHARACTER_ENCODING[] = { "resourceInfoTechnical", "1", "1" };
	private static final String EDIT_AS_PLAINTEXT[] = { "resourceInfoTechnical", "2", "1" };
	
	private String returnUrl;
	private String returnUrlView;
	
	protected void setUp() throws Exception {
		super.setUp();
		returnUrl = getBaseUrl() + "/" + rootCollection + "/" + this.getClass().getSimpleName().toLowerCase()
				+ "/?vrtx=admin";
		
		returnUrlView = getBaseUrl() + "/" + rootCollection + "/" + this.getClass().getSimpleName().toLowerCase() + "/";
	}
	
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
		
		// Delete subfolder -> will implicitly redirect to parentfolder
		deleteCurrentResource();
		
		// Delete parent folder
		deleteCurrentResource();
		
		assertEquals(subFolderLastModified, parentFolderLastModified);
	}
	
	public void testWebAddress() throws TestingEngineResponseException, Exception {
		
		gotoAboutTab();
		
		checkAndGotoLink("aboutWebAddress");
		
		// Checks that we got to view for collection
		assertLinkPresent("vrtx-feed-link");
		
		gotoPage(returnUrl);
	}
	
	public void testWebDAVAddress() throws TestingEngineResponseException, Exception {
		
		gotoAboutTab();
		
		checkAndGotoLink("aboutWebdavAddress");
		
		// Checks that we got to WebDAV listing
		assertElementPresent("webdavmessage");
		assertElementPresent("directoryListing");
		
		gotoPage(returnUrl);
	}
	
	public void testSettingLanguage() {
		
		String languageFolder = "testlanguage";
		
		String languagesToTest[][] = { { "Norwegian (bokmål)", "RSS-strøm fra denne siden" },
				{ "Norwegian (nynorsk)", "RSS-strøm frå denne sida" }, { "English", "Feed from this page" } };
		
		createFolderAndGoto(languageFolder);
		
		for (int i = 0; i < languagesToTest.length; i++) {
			
			gotoAdminAboutEditLinkFolder(languageFolder, "contentLocale"); // TODO: id for specific (edit) to use
			// clickLink()
			setPropertyOption("propertyForm", "value", languagesToTest[i][0]);
			
			gotoViewNoIframe(languageFolder);
			
			// Check if we got the language selected
			assertTextPresent(languagesToTest[i][1]);
			
		}
		
		gotoAdminOfSubFolder(languageFolder);
		
		deleteCurrentResource();
	}
	
	public void testInheritLanguage() {
		
		// TODO: Sub-sub folder test(?)
		
		// { Parent folder name, subfolder name, parent language, exp. subfolder language, exp. text language on view }
		String languagesToTestExtended[][] = {
				{ "no-folder", "no-subfolder", "Norwegian (bokmål)", "Not set , inherits norwegian (bokmål) ( edit )",
						"RSS-strøm fra denne siden" },
				{ "nn-folder", "nn-subfolder", "Norwegian (nynorsk)",
						"Not set , inherits norwegian (nynorsk) ( edit )", "RSS-strøm frå denne sida" },
				{ "en-folder", "en-subfolder", "English", "Not set , inherits english ( edit )", "Feed from this page" } };
		
		for (int i = 0; i < languagesToTestExtended.length; i++) {
			
			createFolderAndGoto(languagesToTestExtended[i][0]);
			gotoAdminAboutEditLinkFolder(languagesToTestExtended[i][0], "contentLocale");
			setPropertyOption("propertyForm", "value", languagesToTestExtended[i][2]);
			gotoContentsTab();
			
			// Check if subfolder has inherited language from parent folder
			createFolderAndGoto(languagesToTestExtended[i][1]);
			gotoAboutTab();
			String inheritedLanguage = getLanguageAbout(true);
			assertEquals(languagesToTestExtended[i][3], inheritedLanguage);
			
			// Check if subfolder is actually viewed in correct language (to much testing(?))
			gotoViewNoIframe(languagesToTestExtended[i][0] + "/" + languagesToTestExtended[i][1]);
			assertTextPresent(languagesToTestExtended[i][4]);
			gotoAdminOfSubFolder(languagesToTestExtended[i][0] + "/" + languagesToTestExtended[i][1]);
			
			deleteCurrentResource();
			deleteCurrentResource();
		}
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
	
	public void checkAndGotoLink(String linkId) {
		assertLinkPresent(linkId);
		clickLink(linkId);
	}
	
	public void gotoAdminAboutEditLinkFolder(String folder, String linkName) {
		gotoPage(returnUrlView + folder + "/?name=" + linkName + "&vrtx=admin&mode=about");
	}
	
	public void gotoAdminAboutEditLinkFile(String folder, String file, String linkName) {
		if (folder.equals("")) {
			gotoPage(returnUrlView + file + "?name=" + linkName + "&vrtx=admin&mode=about");
		} else {
			gotoPage(returnUrlView + folder + file + "?name=" + linkName + "&vrtx=admin&mode=about");
		}
	}
	
	public void gotoViewNoIframe(String folderName) {
		gotoPage(returnUrlView + folderName + "/");
	}
	
	public void gotoAdminOfSubFolder(String folderName) {
		gotoPage(returnUrlView + folderName + "/?vrtx=admin");
	}
	
	// Get values from About-tab
	// TODO: Refactor in own class(?)
	// ****************************************************************************************
	public String getLastModifiedAbout(boolean returnToContents) {
		
		gotoAboutTab();
		
		String lastModified = getTableValue(LASTMODIFIED);
		
		if (returnToContents) {
			gotoContentsTab();
		}
		return lastModified;
	}
	
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
	private void setPropertyOption(String formName, String selectElementName, String selectedOptionValue) {
		
		assertFormPresent(formName);
		setWorkingForm(formName);
		selectOption(selectElementName, selectedOptionValue);
		clickButtonWithText("save");
	}
	
	private void setPropertyTextField(String formName, String textfieldName, String value) {
		
		assertFormPresent(formName);
		setWorkingForm(formName);
		setTextField(textfieldName, value);
		clickButtonWithText("save");
	}
	
	public String getTableValue(String[] valueToExtract) {
		
		// Checks if table exists
		assertElementPresent(valueToExtract[0]);
		
		// Get the cell
		Table resourceInfo = getTable(valueToExtract[0]);
		Row resourceTypeRow = (Row) resourceInfo.getRows().get(Integer.parseInt(valueToExtract[1]));
		Cell resourceType = (Cell) resourceTypeRow.getCells().get(Integer.parseInt(valueToExtract[2]));
		
		return resourceType.getValue();
	}
	
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
	
	private void deleteCurrentResource() {
		// Ignore the javascript popup (asks for verification -> "do you wanna delete ... ?")
		setScriptingEnabled(false);
		clickLink("delete-resource");
	}
}

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
	private static final String SCIENTIFIC_DISCIPLINES[] = { "resourceInfoContent", "7", "1" };
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
			
			gotoAdminAboutEditLinkFolder(languageFolder, "contentLocale", "");
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
			gotoAdminAboutEditLinkFolder(languagesToTestExtended[i][0], "contentLocale", "");
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
	
	public void testEditPropertiesOnFolder() {
		
		String testfolder = "testfolder";
		String testString = "12.12.2008 12:12:12";
		
		// Special cases
		String exptectedDateString = "December 12, 2008 12:12:12 PM CET ( edit )";
		String scientificDiscipline = "011";
		String exptectedScientificDiscipline = "General linguistics and phonetics ( edit )";
		
		// Need to treat textfields, optionelements and radiobuttons separate
		String propertiesToTestTextFields[] = { "userTitle" };
		String propertiesToTestTextFieldsContentNameSpace[] = { "keywords", "description", "verifiedDate",
				"authorName", "authorEmail", "authorURL" };
		String propertiesToTestTextFieldsScientificNameSpace[] = { "disciplines" };
		String propertiesToTestOptionElement[] = { "importance", "collection-type" };
		String propertiesToTestRadioButtons[] = { "hidden" };
		
		// Namespaces in links
		String contentNameSpace = "&namespace=http://www.uio.no/content";
		String navigationNameSpace = "&namespace=http://www.uio.no/navigation";
		String scientificNameSpace = "&namespace=http://www.uio.no/scientific";
		
		createFolderAndGoto("testfolder");
		
		// Information describing the content
		
		gotoAdminAboutEditLinkFolder(testfolder, propertiesToTestTextFields[0], "");
		setPropertyTextField("propertyForm", "value", testString);
		String value = getTitleAbout(true);
		assertEquals("Set to " + testString + " ( edit )", value);
		
		for (int i = 0; i < propertiesToTestTextFieldsContentNameSpace.length; i++) {
			
			String propertyToSetAndGet = propertiesToTestTextFieldsContentNameSpace[i];
			gotoAdminAboutEditLinkFolder(testfolder, propertyToSetAndGet, contentNameSpace);
			setPropertyTextField("propertyForm", "value", testString);
			
			if (propertyToSetAndGet.equals("keywords")) {
				value = getKeywordsAbout(false);
				assertEquals(testString + " ( edit )", value);
			} else if (propertyToSetAndGet.equals("description")) {
				value = getDescriptionAbout(false);
				assertEquals(testString + " ( edit )", value);
			} else if (propertyToSetAndGet.equals("verifiedDate")) {
				value = getVerifiedDateAbout(false);
				assertEquals(exptectedDateString, value);
			} else if (propertyToSetAndGet.equals("authorName")) {
				value = getAuthorAbout(false);
				assertEquals(testString + " ( edit )", value);
			} else if (propertyToSetAndGet.equals("authorEmail")) {
				value = getAuthorEmailAbout(false);
				assertEquals(testString + " ( edit )", value);
			} else if (propertyToSetAndGet.equals("authorURL")) {
				value = getAuthorURLAbout(false);
				assertEquals(testString + " ( edit )", value);
			} else {
				
			}
		}
		
		gotoAdminAboutEditLinkFolder(testfolder, propertiesToTestTextFieldsScientificNameSpace[0], scientificNameSpace);
		setPropertyTextField("propertyForm", "value", scientificDiscipline);
		value = getScientificDisciplinesAbout(true);
		assertEquals(exptectedScientificDiscipline, value);
		
		// Technical Details
		
		gotoAdminAboutEditLinkFolder(testfolder, propertiesToTestRadioButtons[0], navigationNameSpace);
		setRadioOption("propertyForm", "true");
		value = getHideFromNavigationAbout(false);
		assertEquals("Yes" + " ( edit )", value);
		
		gotoAdminAboutEditLinkFolder(testfolder, propertiesToTestOptionElement[0], navigationNameSpace);
		setPropertyOption("propertyForm", "value", "15");
		value = getNavigationImportanceAbout(false);
		assertEquals("15" + " ( edit )", value);
		
		gotoAdminAboutEditLinkFolder(testfolder, propertiesToTestOptionElement[1], "");
		setPropertyOption("propertyForm", "value", "Article listing");
		value = getFolderTypeAbout(false);
		assertEquals("Article listing" + " ( edit )", value);
		
		deleteCurrentResource();
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
	
	public void gotoAdminAboutEditLinkFolder(String folder, String linkName, String nameSpace) {
		gotoPage(returnUrlView + folder + "/?name=" + linkName + nameSpace + "&vrtx=admin&mode=about");
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
		
		String p = getTableValue(LASTMODIFIED);
		
		if (returnToContents) {
			gotoContentsTab();
		}
		return p;
	}
	
	public String getLanguageAbout(boolean returnToContents) {
		
		gotoAboutTab();
		
		String p = getTableValue(LANGUAGE);
		
		if (returnToContents) {
			gotoContentsTab();
		}
		return p;
	}
	
	public String getTitleAbout(boolean returnToContents) {
		
		gotoAboutTab();
		
		String p = getTableValue(TITLE);
		
		if (returnToContents) {
			gotoContentsTab();
		}
		return p;
	}
	
	public String getKeywordsAbout(boolean returnToContents) {
		
		gotoAboutTab();
		
		String p = getTableValue(KEYWORDS);
		
		if (returnToContents) {
			gotoContentsTab();
		}
		return p;
	}
	
	public String getDescriptionAbout(boolean returnToContents) {
		
		gotoAboutTab();
		
		String p = getTableValue(DESCRIPTION);
		
		if (returnToContents) {
			gotoContentsTab();
		}
		return p;
	}
	
	public String getVerifiedDateAbout(boolean returnToContents) {
		
		gotoAboutTab();
		
		String p = getTableValue(VERIFIED_DATE);
		
		if (returnToContents) {
			gotoContentsTab();
		}
		return p;
	}
	
	public String getAuthorAbout(boolean returnToContents) {
		
		gotoAboutTab();
		
		String p = getTableValue(AUTHOR);
		
		if (returnToContents) {
			gotoContentsTab();
		}
		return p;
	}
	
	public String getAuthorEmailAbout(boolean returnToContents) {
		
		gotoAboutTab();
		
		String p = getTableValue(AUTHOR_EMAIL);
		
		if (returnToContents) {
			gotoContentsTab();
		}
		return p;
	}
	
	public String getAuthorURLAbout(boolean returnToContents) {
		
		gotoAboutTab();
		
		String p = getTableValue(AUTHOR_URL);
		
		if (returnToContents) {
			gotoContentsTab();
		}
		return p;
	}
	
	public String getScientificDisciplinesAbout(boolean returnToContents) {
		
		gotoAboutTab();
		
		String p = getTableValue(SCIENTIFIC_DISCIPLINES);
		
		if (returnToContents) {
			gotoContentsTab();
		}
		return p;
	}
	
	public String getHideFromNavigationAbout(boolean returnToContents) {
		
		gotoAboutTab();
		
		String p = getTableValue(HIDE_FROM_NAVIGATION);
		
		if (returnToContents) {
			gotoContentsTab();
		}
		return p;
	}
	
	public String getNavigationImportanceAbout(boolean returnToContents) {
		
		gotoAboutTab();
		
		String p = getTableValue(NAVIGATIONAL_IMPORTANCE);
		
		if (returnToContents) {
			gotoContentsTab();
		}
		return p;
	}
	
	public String getFolderTypeAbout(boolean returnToContents) {
		
		gotoAboutTab();
		
		String p = getTableValue(FOLDER_TYPE);
		
		if (returnToContents) {
			gotoContentsTab();
		}
		return p;
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
	
	private void setRadioOption(String formName, String selectedRadioOptionValue) {
		
		assertFormPresent(formName);
		setWorkingForm(formName);
		clickElementByXPath("//input[@type='radio' and @id='" + selectedRadioOptionValue + "']");
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

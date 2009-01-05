package org.vortikal.integration.webtests.admin;

import net.sourceforge.jwebunit.html.Cell;
import net.sourceforge.jwebunit.html.Row;
import net.sourceforge.jwebunit.html.Table;

import org.vortikal.integration.webtests.BaseAuthenticatedWebTest;

public class MetaDataTest extends BaseAuthenticatedWebTest {

    // About Table metadata position information { id, row, cell }
    private static final String LASTMODIFIED[] = { "vrtx-resourceInfoMain", "0", "1" };
    private static final String LANGUAGE[] = { "vrtx-resourceInfoMain", "6", "1" };

    private static final String TITLE[] = { "vrtx-resourceInfoContent", "0", "1" };
    private static final String KEYWORDS[] = { "vrtx-resourceInfoContent", "1", "1" };
    private static final String DESCRIPTION[] = { "vrtx-resourceInfoContent", "2", "1" };
    private static final String VERIFIED_DATE[] = { "vrtx-resourceInfoContent", "3", "1" };
    private static final String AUTHOR[] = { "vrtx-resourceInfoContent", "4", "1" };
    private static final String AUTHOR_EMAIL[] = { "vrtx-resourceInfoContent", "5", "1" };
    private static final String AUTHOR_URL[] = { "vrtx-resourceInfoContent", "6", "1" };
    private static final String SCIENTIFIC_DISCIPLINES[] = { "vrtx-resourceInfoContent", "7", "1" };

    // Folder
    private static final String HIDE_FROM_NAVIGATION[] = { "vrtx-resourceInfoTechnical", "0", "1" };
    private static final String NAVIGATIONAL_IMPORTANCE[] = { "vrtx-resourceInfoTechnical", "1",
            "1" };
    private static final String FOLDER_TYPE[] = { "vrtx-resourceInfoTechnical", "2", "1" };

    // Namespaces in links
    private String contentNameSpace = "&namespace=http://www.uio.no/content";
    private String navigationNameSpace = "&namespace=http://www.uio.no/navigation";
    private String scientificNameSpace = "&namespace=http://www.uio.no/scientific";

    private String returnUrl;
    private String returnUrlView;


    protected void setUp() throws Exception {
        super.setUp();
        returnUrl = getBaseUrl() + "/" + rootCollection + "/"
                + this.getClass().getSimpleName().toLowerCase() + "/?vrtx=admin";

        returnUrlView = getBaseUrl() + "/" + rootCollection + "/"
                + this.getClass().getSimpleName().toLowerCase() + "/";
    }


    public void testParentFolderLastModified() {

        String parentFolderName = "parentfolder-last-modified";
        String subFolderName = "subfolder";

        // Use old/existing folder
        clickLinkWithText(parentFolderName);

        createResource(CREATE_COLLECTION_SERVICE, CREATE_COLLECTION_FORM, subFolderName);
        clickLinkWithText(subFolderName);

        String subFolderLastModified = getAboutTabValue(LASTMODIFIED, true);
        subFolderLastModified = cropAccordingToLengthOfMonth(subFolderLastModified);

        deleteResource(null);

        String parentFolderLastModified = getAboutTabValue(LASTMODIFIED, true);
        parentFolderLastModified = cropAccordingToLengthOfMonth(parentFolderLastModified);

        assertEquals(subFolderLastModified, parentFolderLastModified);

        gotoPage(returnUrl);
    }


    public void testWebAddress() throws Exception {

        gotoAboutTab();
        clickLink("vrtx-aboutWebAddress");

        // Checks that we got to view for collection
        assertLinkPresent("vrtx-feed-link");

        gotoPage(returnUrl);
    }


    public void testWebDAVAddress() throws Exception {

        gotoAboutTab();
        clickLink("vrtx-aboutWebdavAddress");

        // Checks that we got to WebDAV listing
        assertElementPresent("vrtx-webdavmessage");
        assertElementPresent("vrtx-directoryListing");

        gotoPage(returnUrl);
    }


    public void testSetLanguage() {

        String languageFolder = "testlanguage";

        String languagesToTest[][] = {
                
                // Encoding issue
                // { "Norwegian (bokmål)", "RSS-strøm fra denne siden" },
                // { "Norwegian (nynorsk)", "RSS-strøm frå denne sida" },
                { "English", "Feed from this page" } };


        for (int i = 0; i < languagesToTest.length; i++) {

            gotoAdminAboutEditLinkFolder(languageFolder, "contentLocale", "");
            setPropertyOption("propertyForm", "value", languagesToTest[i][0]);

            gotoViewNoIframe(languageFolder, "");

            // Check if we got the language selected
            assertTextPresent(languagesToTest[i][1]);

        }

        gotoAdminOfSubFolder(languageFolder);

        // deleteResource(null);
    }


    public void testInheritLanguage() {

        // TODO: Sub-sub folder test(?)

        // { Parent folder name, subfolder name, parent language, exp. subfolder
        // language, exp. text language on view }
        String languagesToTestExtended[][] = {
                
                // Encoding issue
                // { "no-folder", "no-subfolder", "Norwegian (bokmål)",
                //        "Not set, inherits norwegian (bokmål) ( edit )",
                //        "RSS-strøm fra denne siden" },
                // { "nn-folder", "nn-subfolder", "Norwegian (nynorsk)",
                //        "Not set, inherits norwegian (nynorsk) ( edit )",
                //        "RSS-strøm frå denne sida" },
                
                { "en-folder", "en-subfolder", "English", "Not set, inherits english ( edit )",
                        "Feed from this page" } };

        for (int i = 0; i < languagesToTestExtended.length; i++) {

            clickLinkWithExactText(languagesToTestExtended[i][0]);
            // Check if subfolder has inherited language from parent folder
            createResource(CREATE_COLLECTION_SERVICE, CREATE_COLLECTION_FORM,
                    languagesToTestExtended[i][1]);
            clickLinkWithExactText(languagesToTestExtended[i][1]);
            gotoAboutTab();
            String inheritedLanguage = getAboutTabValue(LANGUAGE, true);
            assertEquals(languagesToTestExtended[i][3], inheritedLanguage);

            // Check if subfolder is actually viewed in correct language (to
            // much testing(?))
            gotoViewNoIframe(languagesToTestExtended[i][0] + "/" + languagesToTestExtended[i][1],
                    "");
            assertTextPresent(languagesToTestExtended[i][4]);
            gotoAdminOfSubFolder(languagesToTestExtended[i][0] + "/"
                    + languagesToTestExtended[i][1]);

            deleteResource(null);
            gotoPage(returnUrl);
        }
    }


    public void testEditPropertiesOnFolder() {

        String testfolder = "testfolder";
        String testString = "12.12.2008 12:12:12";

        // Special cases
        String exptectedDateString = "December 12, 2008 12:12:12 PM CET ( edit )";
        // TODO: Use browse application for scientific disciplines
        String scientificDiscipline = "011";
        String exptectedScientificDiscipline = "General linguistics and phonetics ( edit )";

        // Need to treat textfields, optionelements and radiobuttons separate
        String propertiesToTestTextFields[] = { "userTitle" };
        String propertiesToTestTextFieldsContentNameSpace[] = { "keywords", "description",
                "verifiedDate", "authorName", "authorEmail", "authorURL" };
        String propertiesToTestTextFieldsScientificNameSpace[] = { "disciplines" };
        String propertiesToTestOptionElement[] = { "importance", "collection-type" };
        String propertiesToTestRadioButtons[] = { "hidden" };

        createResource(CREATE_COLLECTION_SERVICE, CREATE_COLLECTION_FORM, testfolder);
        clickLinkWithText(testfolder);

        // Information describing the content

        gotoAdminAboutEditLinkFolder(testfolder, propertiesToTestTextFields[0], "");
        setPropertyTextField("propertyForm", "value", testString);
        String value = getAboutTabValue(TITLE, true);
        assertEquals("Set to " + testString + " ( edit )", value);

        for (int i = 0; i < propertiesToTestTextFieldsContentNameSpace.length; i++) {

            String propertyToSetAndGet = propertiesToTestTextFieldsContentNameSpace[i];
            gotoAdminAboutEditLinkFolder(testfolder, propertyToSetAndGet, contentNameSpace);
            setPropertyTextField("propertyForm", "value", testString);

            if (propertyToSetAndGet.equals("keywords")) {
                value = getAboutTabValue(KEYWORDS, false);
                assertEquals(testString + " ( edit )", value);
            } else if (propertyToSetAndGet.equals("description")) {
                value = getAboutTabValue(DESCRIPTION, false);
                assertEquals(testString + " ( edit )", value);
            } else if (propertyToSetAndGet.equals("verifiedDate")) {
                value = getAboutTabValue(VERIFIED_DATE, false);
                assertEquals(exptectedDateString, value);
            } else if (propertyToSetAndGet.equals("authorName")) {
                value = getAboutTabValue(AUTHOR, false);
                assertEquals(testString + " ( edit )", value);
            } else if (propertyToSetAndGet.equals("authorEmail")) {
                value = getAboutTabValue(AUTHOR_EMAIL, false);
                assertEquals(testString + " ( edit )", value);
            } else if (propertyToSetAndGet.equals("authorURL")) {
                value = getAboutTabValue(AUTHOR_URL, false);
                assertEquals(testString + " ( edit )", value);
            } else {

            }
        }

        gotoAdminAboutEditLinkFolder(testfolder, propertiesToTestTextFieldsScientificNameSpace[0],
                scientificNameSpace);
        setPropertyTextField("propertyForm", "value", scientificDiscipline);
        value = getAboutTabValue(SCIENTIFIC_DISCIPLINES, true);
        assertEquals(exptectedScientificDiscipline, value);

        // Technical Details

        gotoAdminAboutEditLinkFolder(testfolder, propertiesToTestRadioButtons[0],
                navigationNameSpace);
        setRadioOption("propertyForm", "true");
        value = getAboutTabValue(HIDE_FROM_NAVIGATION, false);
        assertEquals("Yes" + " ( edit )", value);

        gotoAdminAboutEditLinkFolder(testfolder, propertiesToTestOptionElement[0],
                navigationNameSpace);
        setPropertyOption("propertyForm", "value", "15");
        value = getAboutTabValue(NAVIGATIONAL_IMPORTANCE, false);
        assertEquals("15" + " ( edit )", value);

        gotoAdminAboutEditLinkFolder(testfolder, propertiesToTestOptionElement[1], "");
        setPropertyOption("propertyForm", "value", "Article listing");
        value = getAboutTabValue(FOLDER_TYPE, false);
        assertEquals("Article listing" + " ( edit )", value);

        deleteResource(null);
    }


    public void testContentTypeChange() {

        gotoAdminAboutEditLinkFile("", "xmlfile.xml", "contentType", "");

        assertLinkPresent("editService");

        setPropertyTextField("propertyForm", "value", "text/plain");
        assertLinkNotPresent("editService");
        assertLinkPresent("plaintextEditService");

        gotoAdminAboutEditLinkFile("", "xmlfile.xml", "contentType", "");

        setPropertyTextField("propertyForm", "value", "text/xml");
        assertLinkNotPresent("plaintextEditService");
        assertLinkPresent("editService");
    }


    public void testEditAsTextOnXMLFile() {

        gotoAdminAboutEditLinkFile("", "xmlfile.xml", "plaintext-edit", "");
        setRadioOption("propertyForm", "true");

        // Checks if we can edit the XML-file as text
        assertLinkPresent("plaintextXMLEditService");

        gotoAdminAboutEditLinkFile("", "xmlfile.xml", "plaintext-edit", "");
        setRadioOption("propertyForm", "unset");

        assertLinkNotPresent("plaintextXMLEditService");

        gotoPage(returnUrl);

    }


    private void gotoAboutTab() {
        clickLink("aboutResourceService");
    }


    private void gotoContentsTab() {
        checkAndGotoLink("manageCollectionListingService");
    }


    private void checkAndGotoLink(String linkId) {
        assertLinkPresent(linkId);
        clickLink(linkId);
    }


    private void gotoAdminAboutEditLinkFolder(String folder, String linkName, String nameSpace) {
        gotoPage(returnUrlView + folder + "/?name=" + linkName + nameSpace
                + "&vrtx=admin&mode=about");
    }


    private void gotoAdminAboutEditLinkFile(String folder, String file, String linkName,
            String nameSpace) {
        if (folder.equals("")) {
            gotoPage(returnUrlView + file + "?name=" + linkName + nameSpace
                    + "&vrtx=admin&mode=about");
        } else {
            gotoPage(returnUrlView + folder + file + "?name=" + linkName + nameSpace
                    + "&vrtx=admin&mode=about");
        }
    }


    private void gotoViewNoIframe(String folderName, String file) {
        gotoPage(returnUrlView + folderName + "/" + file);
    }


    private void gotoAdminOfSubFolder(String folderName) {
        gotoPage(returnUrlView + folderName + "/?vrtx=admin");
    }


    private String getAboutTabValue(String[] aboutTab, boolean returnToContents) {
        gotoAboutTab();

        assertElementPresent(aboutTab[0]);

        // Get the cell
        Table resourceInfo = getTable(aboutTab[0]);
        Row resourceTypeRow = (Row) resourceInfo.getRows().get(Integer.parseInt(aboutTab[1]));
        Cell resourceType = (Cell) resourceTypeRow.getCells().get(Integer.parseInt(aboutTab[2]));

        String p = resourceType.getValue();

        if (returnToContents) {
            gotoContentsTab();
        }
        return p;
    }


    private void setPropertyOption(String formName, String selectElementName,
            String selectedOptionValue) {

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


    private String cropAccordingToLengthOfMonth(String string) {

        int cropLastModifiedValueAppended = 14;
        int lengthOfMonth;

        for (lengthOfMonth = 0; lengthOfMonth < string.length(); lengthOfMonth++) {
            String aChar = string.substring(lengthOfMonth, lengthOfMonth + 1);
            if (aChar.equals(" ")) {
                break;
            }
        }

        int cropLastModifiedValue = lengthOfMonth + cropLastModifiedValueAppended;

        return string.substring(0, cropLastModifiedValue);
    }

}

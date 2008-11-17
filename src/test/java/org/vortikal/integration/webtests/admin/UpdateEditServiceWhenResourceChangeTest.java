package org.vortikal.integration.webtests.admin;

import org.apache.commons.lang.StringUtils;
import org.vortikal.integration.webtests.BaseAuthenticatedWebTest;

public class UpdateEditServiceWhenResourceChangeTest extends BaseAuthenticatedWebTest {

    final static String ORIGINAL_RESOURCE_NAME = "xmlresource.xml";

    final static String RESOURCE_NAME = ORIGINAL_RESOURCE_NAME.replace(".", "(1).");


    @Override
    protected void setUp() throws Exception {

        super.setUp();
        copyResource(ORIGINAL_RESOURCE_NAME, RESOURCE_NAME);
    }


    @Override
    protected void tearDown() throws Exception {
        deleteResource(RESOURCE_NAME);
        super.tearDown();
    }


    /**
     * Test to verify a fix for VTK-1184. When a document is locked by the editService and an update is made to this
     * document f.x. by the plainTextEdit service update is not seen by the editService.
     */
    public void testUpdateWhenChange() {
        String baseTitle = "title ";
        String editTitle = baseTitle + "editService";
        String plainTitle = baseTitle + "plaintextXMLEditService";
        clickLinkWithExactText(RESOURCE_NAME);
        clickLink("editService");

        assertTextPresent(editTitle);
        assertTextNotPresent(plainTitle);

        clickLink("plaintextXMLEditService");

        String updateWhenChangeContent = getElementTextByXPath("//textarea[@name='content']");
        String newContent = updateWhenChangeContent.replace(editTitle, plainTitle);
        setTextField("content", newContent);
        clickButton("saveAction");

        clickLink("editService");

        // is before VTK-1184 is fixed
        // assertTextPresent(editTitle);
        // assertTextNotPresent(plainTitle);

        // should be when VTK-1184 is fixed
        assertTextPresent(plainTitle);
        assertTextNotPresent(editTitle);
    }


    private void copyResource(String resourceName, String copiedResourceName) {
        assertLinkPresentWithExactText(resourceName);
        String className = this.getClass().getSimpleName().toLowerCase();
        checkCheckbox("/" + rootCollection + "/" + className + "/" + resourceName);
        clickLink("copyResourceService");
        clickLink("copyToSelectedFolderService");
        assertLinkPresentWithExactText(copiedResourceName);
    }


    @Override
    protected void deleteResource(String resourceName) {
        // Ignore the javascript popup (asks for verification -> "do you wanna delete ... ?")
        setScriptingEnabled(false);

        // Make sure we are in the collection listing for this test
        String className = this.getClass().getSimpleName().toLowerCase();
        clickLinkWithExactText(className);

        if (StringUtils.isNotBlank(resourceName)) {
            clickLinkWithExactText(resourceName);
        }
        clickLink("delete-resource");
    }

}

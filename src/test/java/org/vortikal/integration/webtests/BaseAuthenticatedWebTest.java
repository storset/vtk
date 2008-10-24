package org.vortikal.integration.webtests;

import org.apache.commons.lang.StringUtils;

public abstract class BaseAuthenticatedWebTest extends AbstractWebTest {
	
	protected static final String CREATE_COLLECTION_SERVICE = "createCollectionService";
	protected static final String CREATE_COLLECTION_FORM = "createcollection";
	protected static final String CREATE_DOCUMENT_SERVICE = "createDocumentService";
	protected static final String CREATE_DOCUMENT_FORM = "createDocumentForm";
    
    protected String getBaseUrl() throws Exception {
        return getProperty(PROP_ADMIN_URL);
    }

    @Override
    protected boolean requiresAuthentication() {
        return true;
    }
    
    protected void createResource(String serviceName, String formName, String resourceName) {
        // Start of fresh
        assertLinkNotPresent(resourceName);
        assertFormNotPresent(formName);

        // Create resource
        clickLink(serviceName);
        assertFormPresent(formName);
        setWorkingForm(formName);
        setTextField("name", resourceName);
        submit();
    }
    
    protected void deleteResource(String resourceName) {
        // Ignore the javascript popup (asks for verification -> "do you wanna delete ... ?")
        setScriptingEnabled(false);
        if (StringUtils.isNotBlank(resourceName)) {
        	clickLink("delete-" + resourceName);
        	assertLinkNotPresent(resourceName);
        } else {
        	clickLink("delete-resource");
        }

    }

}

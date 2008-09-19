package org.vortikal.integration.webtests;

public abstract class BaseAuthenticatedWebTest extends AbstractWebTest {
    
    protected String getBaseUrl() throws Exception {
        return getProperty(PROP_ADMIN_URL);
    }

    @Override
    protected boolean requiresAuthentication() {
        return true;
    }

}

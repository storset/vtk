package org.vortikal.integration.webtests;

public abstract class AuthenticatedWebTest extends AbstractWebTest {
    
    protected String getBaseUrl() throws Exception {
        return getProperty(PROP_ADMIN_URL);
    }

    @Override
    protected boolean requiresAuthentication() {
        return true;
    }

}

package org.vortikal.integration.webtests;

public abstract class WebdavWebTest extends AbstractWebTest {

    protected String getBaseUrl() throws Exception {
        return getProperty(PROP_WEBDAV_URL);
    }

    @Override
    protected boolean requiresAuthentication() {
        return false;
    }

}

package org.vortikal.integration.webtests;

import java.io.FileInputStream;
import java.util.Locale;
import java.util.Properties;

import net.sourceforge.jwebunit.junit.WebTestCase;

import org.apache.commons.lang.StringUtils;

public abstract class AbstractWebTest extends WebTestCase {
    
    protected static final String PROP_ADMIN_URL = "adminUrl";
    protected static final String PROP_ADMIN_USR = "adminUser";
    protected static final String PROP_ADMIN_PASSWORD = "adminPassword";
    protected static final String PROP_VIEW_URL = "viewUrl";
    protected static final String PROP_WEBDAV_URL = "webdavUrl";
    
    private static Properties props;
    private static final String propFile = "vrtx-automatedtest.properties";
    private static final String rootCollection = "automatedtestresources";
    
    protected void setUp() throws Exception {
        super.setUp();
        
        if (props == null) {
            String userHome = System.getProperty("user.home");
            props = new Properties();
            props.load(new FileInputStream(userHome + "/" + propFile));
        }
        
        getTestContext().setLocale(new Locale("en"));
        
        getTestContext().setBaseUrl(getBaseUrl());
        
        // Quick fix for error "Cannot call method "toLowerCase" of null" (known bug/issue)
        getTestContext().setUserAgent("Mozilla/5.0 (Windows; U; Windows NT 5.1; de; rv:1.8.1.3) " +
                "Gecko/20070309 Firefox/2.0.0.3");
        
        if (requiresAuthentication()) {
            getTestContext().setAuthorization(getProperty(PROP_ADMIN_USR), getProperty(PROP_ADMIN_PASSWORD));
            beginAt("/?vrtx=admin&authenticate");
        } else {
            beginAt("/");
        }
        
        // Make sure the rootcollection containing testresources is available
        assertLinkPresentWithText(rootCollection);
        clickLinkWithText(rootCollection);
        assertFalse("The requested page is blank", StringUtils.isBlank(getPageSource()));
    }
    
    protected void prepare(String testResourceName) {
        assertLinkPresentWithText(testResourceName);
        clickLinkWithText(testResourceName);
        assertFalse("The requested page is blank", StringUtils.isBlank(getPageSource()));
    }
    
    protected String getProperty(String key) throws Exception {
        if (!props.containsKey(key)) {
            throw new Exception("Missing property '" + key + "'. Make sure '" + propFile + "' is set up properly.");
        }
        return StringUtils.trim(props.getProperty(key));
    }
    
    protected abstract String getBaseUrl() throws Exception;
    
    protected abstract boolean requiresAuthentication();

}

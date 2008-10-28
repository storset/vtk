package org.vortikal.integration.webtests;

import java.util.Locale;
import java.util.Properties;

import junit.framework.AssertionFailedError;
import net.sourceforge.jwebunit.junit.WebTestCase;

import org.apache.commons.lang.StringUtils;

public abstract class AbstractWebTest extends WebTestCase {
    
    protected static final String rootCollection = "automatedtestresources";
    protected static final String PROP_ADMIN_URL = "admin.url";
    protected static final String PROP_ADMIN_USR = "admin.user";
    protected static final String PROP_ADMIN_PASSWORD = "admin.password";
    protected static final String PROP_VIEW_URL = "view.url";
    protected static final String PROP_WEBDAV_URL = "webdav.url";
    
    protected static final String URL_REGEX = "^(http(s?)\\:\\/\\/|www)\\S*";
    
    private static Properties props;
    private static final String propFile = "integration/webtests/webtests.properties";
    
    protected void setUp() throws Exception {
        super.setUp();
        
        if (props == null) {
            props = new Properties();
            props.load(this.getClass().getResourceAsStream( "/" + propFile));
        }
        
        getTestContext().setLocale(new Locale("en"));
        
        String baseUrl = getBaseUrl();
        if (!baseUrl.matches("^(http(s?)\\:\\/\\/|www)\\S*")) {
        	throw new WebTestPropertyException("Invalid url: '" + baseUrl + "'." +
        			"\nPlease specify a valid url in your testsettings");
        }
        getTestContext().setBaseUrl(baseUrl);
        
        // Quick fix for error "Cannot call method "toLowerCase" of null" (known bug/issue)
        getTestContext().setUserAgent("Mozilla/5.0 (Windows; U; Windows NT 5.1; de; rv:1.8.1.3) " +
                "Gecko/20070309 Firefox/2.0.0.3");
        
        if (requiresAuthentication()) {
            getTestContext().setAuthorization(getProperty(PROP_ADMIN_USR), getProperty(PROP_ADMIN_PASSWORD));
            beginAt("/" + rootCollection + "/?vrtx=admin&authenticate");
        } else {
            beginAt("/" + rootCollection);
        }
        
        assertFalse("The requested page is blank", StringUtils.isBlank(getPageSource()));
        prepare();
    }

    private void prepare() {
        String testResourceName = this.getClass().getSimpleName().toLowerCase();
        try {
            assertLinkPresentWithExactText(testResourceName);
        } catch (AssertionFailedError afe) {
            throw new MissingWebTestResourceException("A required resource for this test was not found. \n" +
            		"Make sure a collection with the same name and title as the testclass " +
            		"(" + testResourceName + ") exists under the rootcollection.");
        }
        clickLinkWithExactText(testResourceName);
        assertFalse("The requested page is blank", StringUtils.isBlank(getPageSource()));
    }
    
    protected String getProperty(String key) throws WebTestPropertyException {
    	String prop = props.getProperty(key);
        if (StringUtils.isBlank(prop) || (prop.startsWith("${") && prop.endsWith("}"))) {
            throw new WebTestPropertyException("Missing or invalid property " + key + ": '" +
            		prop + "'.\nMake sure '" + propFile + "' is set up and filtered properly.");
        }
        return StringUtils.trim(prop);
    }
    
    protected abstract String getBaseUrl() throws Exception;
        
    protected abstract boolean requiresAuthentication();

}

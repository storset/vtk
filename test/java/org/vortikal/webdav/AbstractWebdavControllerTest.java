package org.vortikal.webdav;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.BasicConfigurator;
import org.springframework.mock.web.MockHttpServletRequest;
import org.vortikal.webdav.AbstractWebdavController.UriState;

public class AbstractWebdavControllerTest extends TestCase {

    protected Log logger = LogFactory.getLog(this.getClass());
    
    protected void setUp() throws Exception {
        BasicConfigurator.configure();
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testParseIfHeader() {
        String uri = "/hest.html";
        String ifHeader = "If: (<locktoken:a-write-lock-token> [\"I am an ETag\"]) ([\"I am another ETag\"])";
        LockController lockController = new LockController();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("If", ifHeader);
        UriState uriState = lockController.parseIfHeader(request, uri);
        assertNotNull(uriState);
    }
}

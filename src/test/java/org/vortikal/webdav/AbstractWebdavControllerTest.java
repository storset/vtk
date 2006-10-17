package org.vortikal.webdav;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.BasicConfigurator;

public class AbstractWebdavControllerTest extends TestCase {

    protected Log logger = LogFactory.getLog(this.getClass());
    
    static {
        BasicConfigurator.configure();
    }
    
    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void dummyTest() {
        assertTrue(true);
    }
    
}

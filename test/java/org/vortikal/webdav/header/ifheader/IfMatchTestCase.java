package org.vortikal.webdav.header.ifheader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.BasicConfigurator;
import org.jmock.Mock;
import org.jmock.MockObjectTestCase;
import org.springframework.mock.web.MockHttpServletRequest;
import org.vortikal.repository.Resource;
import org.vortikal.webdav.ifheader.IfMatchHeader;
import org.vortikal.webdav.ifheader.IfNoneMatchHeader;

public class IfMatchTestCase extends MockObjectTestCase {

    private static Log logger = LogFactory.getLog(IfMatchTestCase.class);
    private Resource resource;
    
    private final String etag = "\"I am an ETag\"";
    private final String anotherEtag = "\"I am another ETag\"";

    static {
        BasicConfigurator.configure();
    }
    
    protected void setUp() throws Exception {
        super.setUp();
                
        Mock mockResource = mock(Resource.class);
        mockResource.expects(atLeastOnce()).method("getEtag").withNoArguments().will(returnValue(etag));
        resource = (Resource) mockResource.proxy();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    public void testCorrectEtag() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("If-Match", etag);
        IfMatchHeader ifMatchHeader = new IfMatchHeader(request);
        assertTrue(ifMatchHeader.matches(resource));
    }
    
    public void testWrongEtag() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("If-Match", anotherEtag);
        IfMatchHeader ifMatchHeader = new IfMatchHeader(request);
        assertFalse(ifMatchHeader.matches(resource));
    }
    
    public void testAllEtag() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("If-Match", "*");
        IfNoneMatchHeader ifNoneMatchHeader = new IfNoneMatchHeader(request);
        assertTrue(ifNoneMatchHeader.matches(resource));
        resource.getEtag(); //to make to mock object happy
    }
    
    public void testNoEtag() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        IfMatchHeader ifMatchHeader = new IfMatchHeader(request);
        assertTrue(ifMatchHeader.matches(resource));
        resource.getEtag(); //to make to mock object happy
    }


}
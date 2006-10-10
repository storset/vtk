package org.vortikal.webdav.header.ifheader;

import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.BasicConfigurator;
import org.jmock.Mock;
import org.jmock.MockObjectTestCase;
import org.springframework.mock.web.MockHttpServletRequest;
import org.vortikal.repository.Lock;
import org.vortikal.repository.Resource;
import org.vortikal.webdav.ifheader.IfHeader;
import org.vortikal.webdav.ifheader.IfHeaderImpl;

public class IfHeaderImplTestCase extends MockObjectTestCase {

    protected static Log logger = LogFactory.getLog(IfHeaderImplTestCase.class);
    Resource resource;
    String ifHeaderContent;
    String etag;
    String lockToken;
    
    protected void setUp() throws Exception {
        super.setUp();
        BasicConfigurator.configure();

        this.etag = "\"I am an ETag\"";
        this.lockToken = "locktoken:a-write-lock-token";
        
        //ifHeaderContent = "(<locktoken:a-write-lock-token> [\"I am an ETag\"]) ([\"I am another ETag\"])";
        this.ifHeaderContent = "(<locktoken:a-write-lock-token> [\"I am an ETag\"])";
        

    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    public void testIfHeaderImpl() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        request.addHeader("If", this.ifHeaderContent);
        IfHeader ifHeader = new IfHeaderImpl(request);
        assertNotNull(ifHeader);
        assertNotNull(ifHeader.getAllTokens());
        int count = 0;
        Iterator iter = ifHeader.getAllTokens();
        while (iter.hasNext()) {
            String token = (String) iter.next();
            assertNotNull(token);
            assertNotSame("", token);
            logger.info("token:" + token);
            count++;
        }
        assertEquals(1, count);
    }
    
    public void testMatchTrue() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("If", this.ifHeaderContent);
        IfHeader ifHeader = new IfHeaderImpl(request);
        
        Mock mockLock = mock(Lock.class);
        mockLock.stubs().method("getLockToken").withNoArguments().will(returnValue(this.lockToken));
        Lock lock = (Lock) mockLock.proxy();
        
        Mock mockResource = mock(Resource.class);
        mockResource.expects(atLeastOnce()).method("getEtag").withNoArguments().will(returnValue(this.etag));
        mockResource.expects(atLeastOnce()).method("getLock").withNoArguments().will(returnValue(lock));
        this.resource = (Resource) mockResource.proxy();
        
        assertTrue(ifHeader.matches(this.resource, true));
    }
    
    public void testMatchWrongEtag() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("If", this.ifHeaderContent);
        IfHeader ifHeader = new IfHeaderImpl(request);
        
        Mock mockLock = mock(Lock.class);
        mockLock.stubs().method("getLockToken").withNoArguments().will(returnValue(this.lockToken));
        Lock lock = (Lock) mockLock.proxy();
        
        Mock mockResource = mock(Resource.class);
        mockResource.expects(atLeastOnce()).method("getEtag").withNoArguments().will(returnValue("dummy"));
        mockResource.expects(atLeastOnce()).method("getLock").withNoArguments().will(returnValue(lock));
        this.resource = (Resource) mockResource.proxy();
        
        assertFalse(ifHeader.matches(this.resource, true));
    }

    public void testMatchWrongLockToken() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("If", this.ifHeaderContent);
        IfHeader ifHeader = new IfHeaderImpl(request);
        
        Mock mockLock = mock(Lock.class);
        mockLock.stubs().method("getLockToken").withNoArguments().will(returnValue("dummy"));
        Lock lock = (Lock) mockLock.proxy();
        
        Mock mockResource = mock(Resource.class);
        mockResource.expects(atLeastOnce()).method("getEtag").withNoArguments().will(returnValue(this.etag));
        mockResource.expects(atLeastOnce()).method("getLock").withNoArguments().will(returnValue(lock));
        this.resource = (Resource) mockResource.proxy();
        
        assertFalse(ifHeader.matches(this.resource, true));
    }
    
}

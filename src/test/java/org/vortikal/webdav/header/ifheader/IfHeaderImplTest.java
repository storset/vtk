package org.vortikal.webdav.header.ifheader;

import java.util.Iterator;

import junit.framework.TestCase;

import org.apache.log4j.BasicConfigurator;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.springframework.mock.web.MockHttpServletRequest;
import org.vortikal.repository.Lock;
import org.vortikal.repository.Resource;
import org.vortikal.webdav.ifheader.IfHeader;
import org.vortikal.webdav.ifheader.IfHeaderImpl;

@SuppressWarnings("unchecked")
public class IfHeaderImplTest extends TestCase {

    private final String ifHeaderContent = "(<locktoken:a-write-lock-token> [\"I am an ETag\"])";
    private final String etag = "\"I am an ETag\"";
    private final String lockToken = "locktoken:a-write-lock-token";
    
    private Mockery context = new JUnit4Mockery();
    
    protected void setUp() throws Exception {
        super.setUp();
        BasicConfigurator.configure();
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
            count++;
        }
        assertEquals(1, count);
    }
    
    public void testMatchTrue() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("If", this.ifHeaderContent);
        IfHeader ifHeader = new IfHeaderImpl(request);
        
        final Lock mockLock = context.mock(Lock.class, "matchTrueLock");
        context.checking(new Expectations() {{ one(mockLock).getLockToken(); will(returnValue(lockToken)); }});
        
        final Resource mockResource = context.mock(Resource.class, "matchTrueResource");
        context.checking(new Expectations() {{ one(mockResource).getEtag(); will(returnValue(etag)); }});
        context.checking(new Expectations() {{ atLeast(2).of(mockResource).getLock(); will(returnValue(mockLock)); }});
        
        assertTrue(ifHeader.matches(mockResource, true));
    }
    
    public void testMatchWrongEtag() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("If", this.ifHeaderContent);
        IfHeader ifHeader = new IfHeaderImpl(request);
        
        final Lock mockLock = context.mock(Lock.class, "matchWrongEtagLock");
        context.checking(new Expectations() {{ one(mockLock).getLockToken(); will(returnValue(lockToken)); }});
        
        final Resource mockResource = context.mock(Resource.class, "matchWrongEtagResource");
        context.checking(new Expectations() {{ one(mockResource).getEtag(); will(returnValue("dummy")); }});
        context.checking(new Expectations() {{ atLeast(2).of(mockResource).getLock(); will(returnValue(mockLock)); }});
        
        
        assertFalse(ifHeader.matches(mockResource, true));
    }

    public void testMatchWrongLockToken() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("If", this.ifHeaderContent);
        IfHeader ifHeader = new IfHeaderImpl(request);
        
        final Lock mockLock = context.mock(Lock.class, "matchWrongLockTokenLock");
        context.checking(new Expectations() {{ one(mockLock).getLockToken(); will(returnValue("dummy")); }});
        
        final Resource mockResource = context.mock(Resource.class, "matchWrongLockTokenResource");
        context.checking(new Expectations() {{ one(mockResource).getEtag(); will(returnValue(etag)); }});
        context.checking(new Expectations() {{ atLeast(2).of(mockResource).getLock(); will(returnValue(mockLock)); }});
        
        
        assertFalse(ifHeader.matches(mockResource, true));
    }
    
}

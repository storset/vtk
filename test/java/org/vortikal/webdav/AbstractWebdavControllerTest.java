package org.vortikal.webdav;

import java.util.List;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.BasicConfigurator;
import org.springframework.mock.web.MockHttpServletRequest;
import org.vortikal.webdav.AbstractWebdavController.EtagOrStateToken;
import org.vortikal.webdav.AbstractWebdavController.UriState;

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

    public void testParseIfHeaderNoTagOneEtag() {
        String uri = "/hest.html";
        String ifHeader = "(<locktoken:a-write-lock-token> [\"I_am_an_ETag\"])";
        LockController lockController = new LockController();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("If", ifHeader);
        UriState uriState = lockController.parseIfHeader(request, uri);
        assertNotNull(uriState);
        assertEquals(uri, uriState.getURI());
        //We should fine one locktoken and one etag 
        assertEquals(2, uriState.getTokens().size());
        isStateTokensNegated(uriState, new boolean[] {false, false});
        logger.info("tokens: " + uriState.getTokens());
    }
    
    public void testParseIfHeaderNoTagOneEtagNot() {
        String uri = "/hest.html";
        String ifHeader = "(Not <locktoken:write1> <locktoken:write2> Not <locktoken:write3>)";
        LockController lockController = new LockController();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("If", ifHeader);
        UriState uriState = lockController.parseIfHeader(request, uri);
        assertNotNull(uriState);
        assertEquals(uri, uriState.getURI());
        //We should fine one locktoken and one etag 
        assertEquals(3, uriState.getTokens().size());
        isStateTokensNegated(uriState, new boolean[] {true, false, true});

        logger.info("tokens: " + uriState.getTokens());
    }
    
    
    public void testParseIfHeaderNoTagTwoEtags() {
        String uri = "/hest.html";
        String ifHeader = "(<locktoken:a-write-lock-token> [\"I_am_an_ETag\"]) ([\"I_am_another_ETag\"])";
        LockController lockController = new LockController();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("If", ifHeader);
        UriState uriState = lockController.parseIfHeader(request, uri);
        assertNotNull(uriState);
        assertEquals(uri, uriState.getURI());
        //We should fine one locktoken and two etags 
        assertEquals(3, uriState.getTokens().size());
        isStateTokensNegated(uriState, new boolean[] {false, false, false});
        logger.info("tokens: " + uriState.getTokens());
    }
    
    public void xtestParseIfHeaderTagged() {
        String uri = "/hest.html";
        String ifHeader = "<http://www.foo.bar/resource1> (<locktoken:a-write-lock-token> [W/\"A weak ETag\"]) ([\"strong ETag\"])";
        LockController lockController = new LockController();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("If", ifHeader);
        UriState uriState = lockController.parseIfHeader(request, uri);
        assertNotNull(uriState);
        assertEquals(uri, uriState.getURI());
        assertEquals(1, uriState.getTokens().size());
        isStateTokensNegated(uriState, new boolean[] {false, false});
    }
    
    

    private void isStateTokensNegated(final UriState uriState, final boolean[] negated) {
        List tokens = uriState.getTokens();
        assertEquals(tokens.size(), negated.length);
        for (int i=0; i < negated.length; i++) {
            EtagOrStateToken token = (EtagOrStateToken) tokens.get(i);
            assertEquals(negated[i], token.isNegated());
        }
    }
    
   
}

package org.vortikal.web.service;

import junit.framework.TestCase;

import org.junit.Test;
import org.vortikal.repository.Path;
import org.vortikal.repository.ResourceImpl;
import org.vortikal.security.InvalidPrincipalException;
import org.vortikal.security.Principal;
import org.vortikal.security.Principal.Type;
import org.vortikal.security.PrincipalImpl;

public class ResourceContentTypeRegexpAssertionTest extends TestCase {
    
    private ResourceContentTypeRegexpAssertion r;
    private Principal p;
    
    public ResourceContentTypeRegexpAssertionTest() {
        r = new ResourceContentTypeRegexpAssertion();
        p = new MockPrincipalImpl("user", Type.USER);
    }

    @Test
    public void testResourceWordMacro() {
        String testContentType = "application/vnd.ms-word.document.macroEnabled.12";
        String pattern = "application/(msword|vnd\\.ms-word(\\.(document|(document|template)\\.macroEnabled\\.12))|vnd\\.openxmlformats-officedocument\\.wordprocessingml\\.(document|template))";
        assertTrue(matchTestContentTypeWithPattern(testContentType, pattern));
    }
    
    @Test
    public void testResourcePowerpointMacro() {
        String testContentType = "application/vnd.ms-powerpoint.presentation.macroEnabled.12";
        String pattern = "application/(ms-ppt|vnd\\.ms-powerpoint(\\.(addin|presentation|template|slideshow)\\.macroEnabled\\.12)|vnd\\.openxmlformats-officedocument\\.presentationml\\.(presentation|template|slideshow))";
        assertTrue(matchTestContentTypeWithPattern(testContentType, pattern));
    }
    
    @Test
    public void testResourceExcelMacro() {
        String testContentType = "application/vnd.ms-excel.sheet.macroEnabled.12";
        String pattern = "application/(vnd\\.ms-excel(\\.(sheet(|\\.binary)|template|addin|)\\.macroEnabled\\.12)|x-msexcel|vnd\\.openxmlformats-officedocument\\.spreadsheetml\\.(sheet|template))";
        assertTrue(matchTestContentTypeWithPattern(testContentType, pattern));
    }
    
    private boolean matchTestContentTypeWithPattern(String testContentType, String pattern) {
        r.setPattern(pattern);
        return r.matches(new MockResource(testContentType), p);
    }

}


@SuppressWarnings("serial")
class MockPrincipalImpl extends PrincipalImpl {
    public MockPrincipalImpl(String id, Type type) throws InvalidPrincipalException {
        super(id, type);
    }
}

class MockResource extends ResourceImpl {
    
    private String testContentType = "";
    
    public MockResource(Path uri) {
        super(uri);
    };
    
    public MockResource(String testContentType) {
        super(Path.fromString("/"));
        this.testContentType = testContentType.toString();
    }

    public String getContentType() {
        return testContentType;
    }
}
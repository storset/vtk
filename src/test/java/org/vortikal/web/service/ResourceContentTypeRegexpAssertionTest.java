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

    /* MSOffice 2007+ http://filext.com/faq/office_mime_types.php */
    
    @Test
    public void testResourceWordMacro() {
        String[] testContentTypes = {"application/msword",
                                     "application/vnd.ms-word",
                                     "application/vnd.ms-word.document",
                                     "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                     "application/vnd.openxmlformats-officedocument.wordprocessingml.template",
                                     "application/vnd.ms-word.document.macroEnabled.12",
                                     "application/vnd.ms-word.template.macroEnabled.12"};
        
        String pattern = "application/(msword|vnd\\.ms-word(|\\.(document|(document|template)\\.macroEnabled\\.12))|vnd\\.openxmlformats-officedocument\\.wordprocessingml\\.(document|template))";
        assertTrue(matchTestContentTypeWithPattern(testContentTypes, pattern));
    }

    @Test
    public void testResourceExcelMacro() {
        String[] testContentTypes = {"application/vnd.ms-excel",
                                     "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                     "application/vnd.openxmlformats-officedocument.spreadsheetml.template",
                                     "application/vnd.ms-excel.sheet.macroEnabled.12",
                                     "application/vnd.ms-excel.template.macroEnabled.12",
                                     "application/vnd.ms-excel.addin.macroEnabled.12",
                                     "application/vnd.ms-excel.sheet.binary.macroEnabled.12"};

        String pattern = "application/(vnd\\.ms-excel(|\\.(sheet(|\\.binary)|template|addin|)\\.macroEnabled\\.12)|x-msexcel|vnd\\.openxmlformats-officedocument\\.spreadsheetml\\.(sheet|template))";
        assertTrue(matchTestContentTypeWithPattern(testContentTypes, pattern));
    }
    
    @Test
    public void testResourcePowerpointMacro() {
        String[] testContentTypes = {"application/ms-ppt",
                                     "application/vnd.ms-powerpoint",
                                     "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                                     "application/vnd.openxmlformats-officedocument.presentationml.template",
                                     "application/vnd.openxmlformats-officedocument.presentationml.slideshow",
                                     "application/vnd.ms-powerpoint.addin.macroEnabled.12",
                                     "application/vnd.ms-powerpoint.presentation.macroEnabled.12",
                                     "application/vnd.ms-powerpoint.template.macroEnabled.12",
                                     "application/vnd.ms-powerpoint.slideshow.macroEnabled.12"};
        
        String pattern = "application/(ms-ppt|vnd\\.ms-powerpoint(|\\.(addin|presentation|template|slideshow)\\.macroEnabled\\.12)|vnd\\.openxmlformats-officedocument\\.presentationml\\.(presentation|template|slideshow))";
        assertTrue(matchTestContentTypeWithPattern(testContentTypes, pattern));
    }
    
    private boolean matchTestContentTypeWithPattern(String[] testContentTypes, String pattern) {
        for(String testContentType : testContentTypes) {
            if(!matchTestContentTypeWithPattern(testContentType, pattern)) return false;
        }
        return true;
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
package org.vortikal.web.service;

import junit.framework.TestCase;

import org.apache.commons.lang.ArrayUtils;
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
    
    /* MSOffice 2007+: http://filext.com/faq/office_mime_types.php */
    
    private String[] testWordContentTypes = {
            "application/msword",
            "application/vnd.ms-word",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.template",
            "application/vnd.ms-word.document.macroEnabled.12",
            "application/vnd.ms-word.template.macroEnabled.12" };

    private String[] testExcelContentTypes = {
            "application/ms-excel",
            "application/x-msexcel",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.template",
            "application/vnd.ms-excel.sheet.macroEnabled.12",
            "application/vnd.ms-excel.template.macroEnabled.12",
            "application/vnd.ms-excel.addin.macroEnabled.12",
            "application/vnd.ms-excel.sheet.binary.macroEnabled.12" };

    private String[] testPowerpointContentTypes = {
            "application/ms-ppt",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/vnd.openxmlformats-officedocument.presentationml.template",
            "application/vnd.openxmlformats-officedocument.presentationml.slideshow",
            "application/vnd.ms-powerpoint.addin.macroEnabled.12",
            "application/vnd.ms-powerpoint.presentation.macroEnabled.12",
            "application/vnd.ms-powerpoint.template.macroEnabled.12",
            "application/vnd.ms-powerpoint.slideshow.macroEnabled.12" };
    
    // And all together..
    private String[] testOoXmlContentTypes = (String[]) ArrayUtils.addAll(testWordContentTypes, (String[]) ArrayUtils.addAll(testExcelContentTypes, testPowerpointContentTypes));
    
    public ResourceContentTypeRegexpAssertionTest() {
        r = new ResourceContentTypeRegexpAssertion();
        p = new MockPrincipalImpl("user", Type.USER);
    }
    
    @Test
    public void testWordContentTypes() {
        String pattern = "application/(msword|vnd\\.(ms-word(|\\.(document|template)\\.macroEnabled\\.12)|openxmlformats-officedocument\\.wordprocessingml\\.(document|template)))";
        assertTrue(matchTestContentTypeWithPattern(testWordContentTypes, pattern));
    }

    @Test
    public void testExcelContentTypes() {
        String pattern = "application/(ms-excel|x-msexcel|vnd\\.(ms-excel(|\\.(sheet(|\\.binary)|template|addin)\\.macroEnabled\\.12)|openxmlformats-officedocument\\.spreadsheetml\\.(sheet|template)))";
        assertTrue(matchTestContentTypeWithPattern(testExcelContentTypes, pattern));
    }
    
    @Test
    public void testPowerpointContentTypes() {
        String pattern = "application/(ms-ppt|vnd\\.(ms-powerpoint(|\\.(addin|presentation|template|slideshow)\\.macroEnabled\\.12)|openxmlformats-officedocument\\.presentationml\\.(presentation|template|slideshow)))";
        assertTrue(matchTestContentTypeWithPattern(testPowerpointContentTypes, pattern));
    }
    
    @Test
    public void testOoXmlContentTypes() {
        String pattern = "application/(msword|ms-excel|x-msexcel|ms-ppt|vnd\\." +
        		       "((ms-word(|\\.(document|template)\\.macroEnabled\\.12)" +
        		        "|ms-excel(|\\.(sheet(|\\.binary)|template|addin|)\\.macroEnabled\\.12)" +
        		        "|ms-powerpoint(|\\.(addin|presentation|template|slideshow)\\.macroEnabled\\.12))" +
        		        "|(openxmlformats-officedocument\\." +
        		           "(wordprocessingml\\.(document|template)" +
        		           "|spreadsheetml\\.(sheet|template)" +
        		           "|presentationml\\.(presentation|template|slideshow)))))";
        
        assertTrue(matchTestContentTypeWithPattern(testOoXmlContentTypes, pattern));
    }
    
    private boolean matchTestContentTypeWithPattern(String[] testContentTypes, String pattern) {
        for(String testContentType : testContentTypes) {
            if(!matchTestContentTypeWithPattern(testContentType, pattern)) {
                System.out.println(testContentType);
                return false;
            }
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
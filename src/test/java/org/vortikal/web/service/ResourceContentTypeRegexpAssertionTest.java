package org.vortikal.web.service;

import junit.framework.TestCase;

import org.junit.Test;
import org.vortikal.repository.Path;
import org.vortikal.repository.ResourceImpl;
import org.vortikal.security.InvalidPrincipalException;
import org.vortikal.security.Principal.Type;
import org.vortikal.security.PrincipalImpl;

public class ResourceContentTypeRegexpAssertionTest extends TestCase {

    @Test
    public void testResourceExcelMacro() {
        ResourceContentTypeRegexpAssertion r = new ResourceContentTypeRegexpAssertion();
        // Proposed change in msoffice.xml with ^ and .*
        r.setPattern("^application/(vnd\\.ms-excel|x-msexcel|vnd\\.openxmlformats-officedocument\\.spreadsheetml\\.sheet|vnd\\.openxmlformats-officedocument\\.spreadsheetml\\.template).*");
        MockResource mr = new MockResource(Path.fromString("/"));
        assertTrue(r.matches(mr, new MockPrincipalImpl("user", Type.USER)));
    }

}

@SuppressWarnings("serial")
class MockPrincipalImpl extends PrincipalImpl {
    public MockPrincipalImpl(String id, Type type) throws InvalidPrincipalException {
        super(id, type);
    }
}

class MockResource extends ResourceImpl {
    public MockResource(Path uri) {
        super(uri);
    }

    public String getContentType() {
        return "application/vnd.ms-excel.sheet.macroEnabled.12";
    }
}
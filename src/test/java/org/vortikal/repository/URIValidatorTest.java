package org.vortikal.repository;

import junit.framework.TestCase;

public class URIValidatorTest extends TestCase {

    private URIValidator validator;

    @Override
    protected void setUp() throws Exception {
        this.validator = new URIValidator();
    }

    public void testValidateCopyURIs() {
        assertValidity("/", "/", "Cannot copy or move the root resource ('/')");
        assertValidity("/someURI", "/", "Cannot copy or move to the root resource ('/')");
        assertValidity("/copyURI", "/copyURI", "Cannot copy or move a resource to itself");
    }

    private void assertValidity(String srcUri, String destUri, String exceptionMessage) {
        try {
            validator.validateCopyURIs(Path.fromString(srcUri), Path.fromString(destUri));
            fail("Should fail...");
        } catch (IllegalOperationException ioe) {
            assertNotNull("Exception is NULL", ioe);
            assertEquals("Wrong exception message", exceptionMessage, ioe.getMessage());
        }
    }

}

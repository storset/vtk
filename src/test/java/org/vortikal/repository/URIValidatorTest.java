package org.vortikal.repository;

import junit.framework.TestCase;

public class URIValidatorTest extends TestCase {

    private URIValidator validator;

    @Override
    protected void setUp() throws Exception {
        this.validator = new URIValidator();
    }

    public void testValidateURI() {
        // Invalid URIs
        assertValidity(null, false);
        assertValidity("", false);
        assertValidity("  ", false);
        assertValidity("invalid.uri", false);
        assertValidity("/invalid.uri/..", false);
        assertValidity("/invalid.uri/.", false);
        assertValidity("/invalid//uri", false);
        assertValidity("/invalid/../uri", false);
        assertValidity("/invalid.uri/", false);
        String invalidLengthString = getString("i", 1500);
        assertValidity(invalidLengthString, false);
        // Valid URIs
        assertValidity("/", true);
        assertValidity("/valid.uri", true);
        String validLengthString = getString("v", 1499);
        assertValidity(validLengthString, true);
    }

    public void testValidateCopyURIs() {
        assertValidity("/", "", "Cannot copy or move the root resource ('/')");
        assertValidity("", "/", "Cannot copy or move to the root resource ('/')");
        assertValidity("/copyURI", "/copyURI", "Cannot copy or move a resource to itself");
        assertValidity("/copyURI", "/copyURI/", "Cannot copy or move a resource into itself");
    }

    private void assertValidity(String uri, boolean expectedResult) {
        String message = "The URI '" + uri + "' was wrongfully "
                + (expectedResult ? "rejected" : "accepted");
        assertEquals(message, expectedResult, validator.validateURI(uri));
    }

    private void assertValidity(String srcUri, String destUri,
            String exceptionMessage) {
        try {
            validator.validateCopyURIs(srcUri, destUri);
            fail("Should throw IllegalOperationException");
        } catch (IllegalOperationException ioe) {
            assertNotNull("Exception is NULL", ioe);
            assertEquals("Wrong exception message", exceptionMessage, ioe.getMessage());
        }
    }

    private String getString(String chr, int length) {
        StringBuilder sb = new StringBuilder("/");
        for (int i = 1; i < length; i++) {
            sb.append(chr);
        }
        return sb.toString();
    }

}

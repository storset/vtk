package org.vortikal.security.web.saml;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.servlet.http.HttpServletRequest;

import org.joda.time.DateTime;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.Response;
import org.opensaml.saml2.core.StatusCode;
import org.opensaml.saml2.core.SubjectConfirmation;
import org.opensaml.saml2.core.SubjectConfirmationData;
import org.opensaml.xml.Configuration;
import org.opensaml.xml.io.Unmarshaller;
import org.opensaml.xml.util.Base64;
import org.vortikal.web.service.URL;
import org.w3c.dom.Element;

public class SamlResponseHelper {

    /**
     * Fixed SP login URL
     */
    private URL spLoginURL;
    
    /**
     * Encoded IDP certificate string
     */
    private String encodedCertificate;
    
    private HttpServletRequest request;

    public SamlResponseHelper(URL spLoginURL, String encodedCert, HttpServletRequest request) {
        this.spLoginURL = spLoginURL;
        this.encodedCertificate = encodedCert;
        this.request = request;
    }
    
    public UserData getUserData() {
        String encodedSamlResponseString = this.request.getParameter("SAMLResponse");

        Response samlResponse = decodeSamlResponse(encodedSamlResponseString);
        verifyStatusCodeIsSuccess(samlResponse);
        verifyDestinationAddressIsCorrect(this.spLoginURL, samlResponse);
        
        Assertion assertion = extractAssertionFromSamlResponse(samlResponse);
        verifyCryptographicAssertionSignature(assertion);
        validateAssertionContent(assertion);
        
        return new UserData(assertion);
    }

    public void validateAssertionContent(Assertion assertion) throws RuntimeException {
        verifyConfirmationTimeNotExpired(assertion);
        // Sjekk In Reply To ID mot ID vi genererte ved sending av request
        // Sjekk at Assertion ikke er brukt fra f�r. (replay)
        // SJekk destination
        // Sjekk begge timeout.
    }


    private DateTime assertionConfirmationTime(Assertion assertion) {
        DateTime confirmationTime = null;
        for (SubjectConfirmation subjectConfirmation : assertion.getSubject().getSubjectConfirmations()) {
            SubjectConfirmationData data = subjectConfirmation.getSubjectConfirmationData();
            if (data != null && data.getNotOnOrAfter() != null) {
                confirmationTime = data.getNotOnOrAfter();
            }
        }
        return confirmationTime;
    }



    private X509Certificate buildX509CertificateFromEncodedString(String encodedCertificateString)
            throws RuntimeException {
        java.security.cert.X509Certificate cert;
        try {
            cert = OpenSAMLUtilites.buildJavaX509Cert(encodedCertificateString);
        } catch (CertificateException e) {
            throw new RuntimeException("Unable to build x509 certificate from encoded string.", e);
        }
        return cert;
    }


    private Response decodeSamlResponse(String encodedSamlResponseXml) throws RuntimeException {
        try {
            String samlResponseXml = new String(Base64.decode(encodedSamlResponseXml), "UTF-8");
            Element samlElement = OpenSAMLUtilites.loadElementFromString(samlResponseXml);
            Unmarshaller unmarshaller = Configuration.getUnmarshallerFactory().getUnmarshaller(samlElement);
            Response samlResponse = (Response) unmarshaller.unmarshall(samlElement);

            return samlResponse;
        } catch (Exception e) {
            throw new RuntimeException("Exception caught when trying to decode SAML response.", e);
        }
    }


    private Assertion extractAssertionFromSamlResponse(Response samlResponse) {
        Assertion assertion = samlResponse.getAssertions().get(0);
        return assertion;
    }


    private void verifyConfirmationTimeNotExpired(Assertion assertion) throws RuntimeException {
        DateTime confirmationTime = assertionConfirmationTime(assertion);
        if (confirmationTime == null || !confirmationTime.isAfterNow()) {
            throw new RuntimeException("Assertion confirmation time has expired: " + confirmationTime + " before "
                    + new DateTime());
        }
    }


    private void verifyCryptographicAssertionSignature(Assertion assertion) {
        X509Certificate cert = buildX509CertificateFromEncodedString(this.encodedCertificate);
        // TODO If-test
        OpenSAMLUtilites.verifySignature(cert.getPublicKey(), assertion);
    }



    /**
     * Verifies the destination URL. 
     * If the response does not have a destination assertion, or the destination parameter 
     * matches the destination in the assertion, this method returns. Otherwise, an exception is thrown.
     * @param destination the destination
     * @param samlResponse the SAML response
     * @throws RuntimeException
     */
    private void verifyDestinationAddressIsCorrect(URL destination, Response samlResponse) throws RuntimeException {
        if (samlResponse.getDestination() == null) {
            return;
        }
        if (samlResponse.getDestination().equals(destination.toString())) {
            return;
        }
        throw new RuntimeException("Destination mismatch: " + samlResponse.getDestination());
    }


    private void verifyStatusCodeIsSuccess(Response samlResponse) throws RuntimeException {
        String statusCode = samlResponse.getStatus().getStatusCode().getValue();
        if (!StatusCode.SUCCESS_URI.equals(statusCode)) {
            throw new RuntimeException("Wrong status code (" + statusCode + "),  should be: " + StatusCode.SUCCESS_URI);
        }
    }


}

package org.vortikal.security.web.saml;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.joda.time.DateTime;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.Response;
import org.opensaml.saml2.core.StatusCode;
import org.opensaml.saml2.core.SubjectConfirmation;
import org.opensaml.saml2.core.SubjectConfirmationData;
import org.opensaml.xml.Configuration;
import org.opensaml.xml.io.Unmarshaller;
import org.opensaml.xml.util.Base64;
import org.w3c.dom.Element;

public class SamlResponseHelper {
    public static final String ENCODED_IDP_CERT = "";

    public static final String SP_LOGIN_URL = "http://davide-laptop.uio.no:9322/vrtx/__vrtx/app-resources/saml/sp";


    public static UserData retrieveUserDataFromSamlResponse(String encodedSamlResponseString) {
        Assertion assertion = assertionFromSamlResponse(encodedSamlResponseString);
        UserData userData = new UserData(assertion);
        return userData;
    }


    public static void validateAssertionContent(Assertion assertion) throws RuntimeException {
        verifyConfirmationTimeNotExpired(assertion);
        // Sjekk In Reply To ID mot ID vi genererte ved sending av request
        // Sjekk at Assertion ikke er brukt fra f�r. (replay)
        // SJekk destination
        // Sjekk begge timeout.
    }


    private static DateTime assertionConfirmationTime(Assertion assertion) {
        DateTime confirmationTime = null;
        for (SubjectConfirmation subjectConfirmation : assertion.getSubject().getSubjectConfirmations()) {
            SubjectConfirmationData data = subjectConfirmation.getSubjectConfirmationData();
            if (data != null && data.getNotOnOrAfter() != null) {
                confirmationTime = data.getNotOnOrAfter();
            }
        }
        return confirmationTime;
    }


    private static Assertion assertionFromSamlResponse(String encodedSamlResponseString) {
        Response samlResponse = decodeAndVerifySamlResponse(encodedSamlResponseString);
        Assertion assertion = extractAssertionFromSamlResponse(samlResponse);
        verifyCryptographicAssertionSignature(assertion);
        validateAssertionContent(assertion);
        return assertion;
    }


    private static X509Certificate buildX509CertificateFromEncodedString(String encodedCertificateString)
            throws RuntimeException {
        java.security.cert.X509Certificate cert;
        try {
            cert = OpenSAMLUtilites.buildJavaX509Cert(encodedCertificateString);
        } catch (CertificateException e) {
            throw new RuntimeException("Unable to build x509 certificate from encoded string.", e);
        }
        return cert;
    }


    private static Response decodeSamlResponse(String encodedSamlResponseXml) throws RuntimeException {
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


    private static Assertion extractAssertionFromSamlResponse(Response samlResponse) {
        Assertion assertion = samlResponse.getAssertions().get(0);
        return assertion;
    }


    private static void verifyConfirmationTimeNotExpired(Assertion assertion) throws RuntimeException {
        DateTime confirmationTime = assertionConfirmationTime(assertion);
        if (confirmationTime == null || !confirmationTime.isAfterNow()) {
            throw new RuntimeException("Assertion confirmation time has expired: " + confirmationTime + " before "
                    + new DateTime());
        }
    }


    private static void verifyCryptographicAssertionSignature(Assertion assertion) {
        X509Certificate cert = buildX509CertificateFromEncodedString(ENCODED_IDP_CERT);
        // TODO If-test
        OpenSAMLUtilites.verifySignature(cert.getPublicKey(), assertion);
    }


    private static void verifyDestinationAddressIsCorrect(Response samlResponse) throws RuntimeException {
        if (!OpenSAMLUtilites.isDestinationOK(SP_LOGIN_URL, samlResponse)) {
            throw new RuntimeException("Destination mismatch: " + samlResponse.getDestination());
        }
    }


    private static void verifyStatusCodeIsSuccess(Response samlResponse) throws RuntimeException {
        String statusCode = samlResponse.getStatus().getStatusCode().getValue();
        if (!StatusCode.SUCCESS_URI.equals(statusCode)) {
            throw new RuntimeException("Wrong status code (" + statusCode + "),  should be: " + StatusCode.SUCCESS_URI);
        }
    }


    private static Response decodeAndVerifySamlResponse(String encodedSamlResponseString) {
        Response samlResponse = decodeSamlResponse(encodedSamlResponseString);
        verifyStatusCodeIsSuccess(samlResponse);
        verifyDestinationAddressIsCorrect(samlResponse);

        return samlResponse;
    }
}

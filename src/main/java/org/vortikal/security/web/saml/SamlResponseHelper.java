/* Copyright (c) 2010, University of Oslo, Norway
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 
 *  * Neither the name of the University of Oslo nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *      
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.vortikal.security.web.saml;

import java.io.ByteArrayInputStream;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.joda.time.DateTime;
import org.opensaml.common.SignableSAMLObject;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.Response;
import org.opensaml.saml2.core.StatusCode;
import org.opensaml.saml2.core.SubjectConfirmation;
import org.opensaml.saml2.core.SubjectConfirmationData;
import org.opensaml.xml.Configuration;
import org.opensaml.xml.io.Unmarshaller;
import org.opensaml.xml.security.x509.BasicX509Credential;
import org.opensaml.xml.signature.Signature;
import org.opensaml.xml.signature.SignatureValidator;
import org.opensaml.xml.util.Base64;
import org.opensaml.xml.validation.ValidationException;
import org.vortikal.web.service.URL;
import org.w3c.dom.Document;
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


    private X509Certificate buildX509CertificateFromEncodedString(String encodedCertificateString) {
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            ByteArrayInputStream input = new ByteArrayInputStream(Base64.decode(encodedCertificateString));
            java.security.cert.X509Certificate newCert = null;
            newCert = (java.security.cert.X509Certificate) cf.generateCertificate(input);
            return newCert;
        } catch (CertificateException e) {
            throw new RuntimeException("Unable to build x509 certificate from encoded string.", e);
        }
    }
    
//    private X509Certificate buildX509CertificateFromEncodedString(String encodedCertificateString)
//            throws RuntimeException {
//        java.security.cert.X509Certificate cert;
//        try {
//            cert = OpenSAMLUtilites.buildJavaX509Cert(encodedCertificateString);
//        } catch (CertificateException e) {
//            throw new RuntimeException("Unable to build x509 certificate from encoded string.", e);
//        }
//        return cert;
//    }


    private Response decodeSamlResponse(String encodedSamlResponseXml) throws RuntimeException {
        try {
            String samlResponseXml = new String(Base64.decode(encodedSamlResponseXml), "utf-8");
            DocumentBuilderFactory newFactory = DocumentBuilderFactory.newInstance();
            newFactory.setNamespaceAware(true);
            DocumentBuilder builder = newFactory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(samlResponseXml.getBytes("utf-8")));
            Element samlElement = doc.getDocumentElement();
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
        if (!verifySignature(cert, assertion)) {
            throw new RuntimeException("Failed to verify signature of assertion: " + assertion.getID());
        }
    }

    private boolean verifySignature(X509Certificate certificate, Assertion assertion) {
        if (certificate == null) {
            throw new IllegalArgumentException("Certificate cannot be null");
        }
        if (!(assertion instanceof SignableSAMLObject)) {
            throw new IllegalArgumentException("Assertion must be an instance of SignableSAMLObject");
        }        
        SignableSAMLObject signable = (SignableSAMLObject) assertion;
        Signature signature = signable.getSignature();
        if (signature == null) {
            return false;
        }
        PublicKey publicKey = certificate.getPublicKey();
        if (publicKey == null) {
            return false;
        }
        BasicX509Credential x509credential = new BasicX509Credential();
        x509credential.setPublicKey(publicKey);
        SignatureValidator validator = new SignatureValidator(x509credential);
        try {
            validator.validate(signature);
            return true;
        } catch (ValidationException e) {
            return false;
        }
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

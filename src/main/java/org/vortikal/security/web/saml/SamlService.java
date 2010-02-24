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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.UUID;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.opensaml.DefaultBootstrap;
import org.opensaml.common.SAMLObject;
import org.opensaml.common.SAMLVersion;
import org.opensaml.common.SignableSAMLObject;
import org.opensaml.common.binding.BasicSAMLMessageContext;
import org.opensaml.common.binding.SAMLMessageContext;
import org.opensaml.common.xml.SAMLConstants;
import org.opensaml.saml2.binding.decoding.HTTPRedirectDeflateDecoder;
import org.opensaml.saml2.binding.encoding.HTTPRedirectDeflateEncoder;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.AuthnRequest;
import org.opensaml.saml2.core.Issuer;
import org.opensaml.saml2.core.LogoutRequest;
import org.opensaml.saml2.core.LogoutResponse;
import org.opensaml.saml2.core.RequestAbstractType;
import org.opensaml.saml2.core.Response;
import org.opensaml.saml2.core.Status;
import org.opensaml.saml2.core.StatusCode;
import org.opensaml.saml2.core.SubjectConfirmation;
import org.opensaml.saml2.core.SubjectConfirmationData;
import org.opensaml.ws.message.encoder.MessageEncodingException;
import org.opensaml.ws.transport.http.HttpServletRequestAdapter;
import org.opensaml.xml.Configuration;
import org.opensaml.xml.ConfigurationException;
import org.opensaml.xml.Namespace;
import org.opensaml.xml.XMLObjectBuilder;
import org.opensaml.xml.io.Unmarshaller;
import org.opensaml.xml.security.SecurityConfiguration;
import org.opensaml.xml.security.credential.Credential;
import org.opensaml.xml.security.x509.BasicX509Credential;
import org.opensaml.xml.signature.Signature;
import org.opensaml.xml.signature.SignatureValidator;
import org.opensaml.xml.util.Base64;
import org.opensaml.xml.util.XMLHelper;
import org.opensaml.xml.validation.ValidationException;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Path;
import org.vortikal.web.service.URL;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public abstract class SamlService {

    static {
        try {
            DefaultBootstrap.bootstrap();
        } catch (ConfigurationException e) {
            throw new RuntimeException("Exception when trying to bootstrap OpenSAML", e);
        }
    }
    
    protected static final String URL_SESSION_ATTR = SamlAuthenticationHandler.class.getName() + ".SamlSavedURL";
    protected static final String REQUEST_ID_SESSION_ATTR = SamlAuthenticationHandler.class.getName() + ".SamlSavedRequestID";
    

    private CertificateManager certificateManager;
    private String privateKeyAlias;
    
    // SP URI
    private Path serviceProviderURI;

    // SP Identifier
    private String serviceIdentifier;

    // IDP certificate
    private String idpCertificate;

    // IDP login/logout URLs:
    private String authenticationURL;
    private String logoutURL;


    @Required
    public void setCertificateManager(CertificateManager certificateManager) {
        this.certificateManager = certificateManager;
    }
    
    @Required
    public void setPrivateKeyAlias(String privateKeyAlias) {
        this.privateKeyAlias = privateKeyAlias;
    }

    @Required
    public void setServiceProviderURI(String serviceProviderURI) {
        this.serviceProviderURI = Path.fromString(serviceProviderURI);
    }

    public void setServiceIdentifier(String serviceIdentifier) {
        this.serviceIdentifier = serviceIdentifier;
    }

    @Required
    public void setIdpCertificate(String idpCertificate) {
        this.idpCertificate = idpCertificate;
    }

    @Required
    public void setAuthenticationURL(String authenticationURL) {
        this.authenticationURL = authenticationURL;
    }

    @Required
    public void setLogoutURL(String logoutURL) {
        this.logoutURL = logoutURL;
    }

    protected final Credential getSigningCredential() {
        Credential signingCredential = this.certificateManager.getCredential(this.privateKeyAlias);
        if (signingCredential == null) {
            throw new IllegalStateException(
                    "Unable to obtain credentials for signing using keystore alias '" 
                    +  this.privateKeyAlias + "'");
        }
        return signingCredential;
    }
    
    protected final SamlConfiguration newSamlConfiguration(HttpServletRequest request) {
        URL serviceIdentifierURL = getServiceProviderURL(request);
        String id = this.serviceIdentifier;
        if (id == null || "".equals(id.trim())) {
            serviceIdentifierURL = URL.create(request);
            serviceIdentifierURL.clearParameters();
            serviceIdentifierURL.setPath(Path.ROOT);
            id = serviceIdentifierURL.toString();
            id = id.substring(0, id.length() - 1);
        }
        SamlConfiguration configuration = new SamlConfiguration(this.authenticationURL, 
                this.logoutURL, serviceIdentifierURL.toString(), id);
        return configuration;
    }
 
    protected final Response decodeSamlResponse(String encodedSamlResponseXml) throws RuntimeException {
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
    
    protected final void validateAssertionContent(Assertion assertion) throws RuntimeException {
        verifyConfirmationTimeNotExpired(assertion);
    }

    
    protected final void verifyStatusCodeIsSuccess(Response samlResponse) throws RuntimeException {
        String statusCode = samlResponse.getStatus().getStatusCode().getValue();
        if (!StatusCode.SUCCESS_URI.equals(statusCode)) {
            throw new RuntimeException("Wrong status code (" + statusCode + "),  should be: " + StatusCode.SUCCESS_URI);
        }
    }
    
    protected final void verifyDestinationAddressIsCorrect(Response samlResponse) throws RuntimeException {
        if (samlResponse.getDestination() == null) {
            return;
        }
        if (samlResponse.getDestination().equals(this.serviceProviderURI)) {
            return;
        }
        URL url = URL.parse(samlResponse.getDestination());
        if (!url.getPath().equals(this.serviceProviderURI)) {
            throw new RuntimeException("Destination mismatch: " + samlResponse.getDestination());
        }
    }
    
    protected final void verifyCryptographicAssertionSignature(Assertion assertion) {
        X509Certificate cert = buildX509CertificateFromEncodedString(this.idpCertificate);
        if (!verifySignature(cert, assertion)) {
            throw new RuntimeException("Failed to verify signature of assertion: " + assertion.getID());
        }
    }
    
    protected final LogoutResponse getLogoutResponse(HttpServletRequest request) {
        BasicSAMLMessageContext<LogoutResponse, ?, ?> messageContext = new BasicSAMLMessageContext<LogoutResponse, SAMLObject, SAMLObject>();
        messageContext.setInboundMessageTransport(new HttpServletRequestAdapter(request));
        HTTPRedirectDeflateDecoder decoder = new HTTPRedirectDeflateDecoder();
        try {
            decoder.decode(messageContext);
        } catch (Exception e) {
            throw new RuntimeException("Unable to decode LogoutResponse.", e);
        }
        LogoutResponse logoutResponse = messageContext.getInboundSAMLMessage();
        return logoutResponse;
    }
    
    protected final AuthnRequest createAuthenticationRequest(SamlConfiguration config, UUID requestID) throws RuntimeException {
        QName qname = AuthnRequest.DEFAULT_ELEMENT_NAME;
        @SuppressWarnings("unchecked")
        XMLObjectBuilder<AuthnRequest> builder = Configuration.getBuilderFactory().getBuilder(qname);
        AuthnRequest authnRequest = builder.buildObject(qname.getNamespaceURI(), qname.getLocalPart(), qname.getPrefix());

        authnRequest.setID(requestID.toString());
        authnRequest.setForceAuthn(Boolean.FALSE);
        authnRequest.setIssueInstant(new DateTime(DateTimeZone.UTC));
        authnRequest.setProtocolBinding(config.getProtocolBinding());
        authnRequest.setDestination(config.getAuthenticationUrl());
        authnRequest.setAssertionConsumerServiceURL(config.getServiceLoginUrl());
        authnRequest.setIssuer(createIssuer(config.getServiceIdentifier()));

        try {
            authnRequest.validate(true);
        } catch (ValidationException e) {
            throw new RuntimeException("Unable to validate SAML authentication request: " + e);
        }
        return authnRequest;
    }

    protected final LogoutRequest createLogoutRequest(SamlConfiguration config, UUID requestID) {
        QName qname = LogoutRequest.DEFAULT_ELEMENT_NAME;
        @SuppressWarnings("unchecked")
        XMLObjectBuilder<LogoutRequest> builder = Configuration.getBuilderFactory().getBuilder(qname);

        LogoutRequest logoutRequest = builder.buildObject(qname.getNamespaceURI(), qname.getLocalPart(), qname.getPrefix());
        logoutRequest.setID(requestID.toString());
        logoutRequest.setIssueInstant(new DateTime(DateTimeZone.UTC));
        logoutRequest.addNamespace(new Namespace(SAMLConstants.SAML20_NS, SAMLConstants.SAML20_PREFIX));
        logoutRequest.setDestination(config.getLogoutUrl());
        logoutRequest.setReason("urn:oasis:names:tc:SAML:2.0:logout:user");
        logoutRequest.setIssuer(createIssuer(config.getServiceIdentifier()));

        try {
            logoutRequest.validate(true);
        } catch (ValidationException e) {
            throw new RuntimeException("Unable to validate SAML logout request: " + e);
        }

        return logoutRequest;
    }
    
    protected final LogoutResponse createLogoutResponse(SamlConfiguration config, LogoutRequest request, UUID responseID) throws Exception {
        QName qname = LogoutResponse.DEFAULT_ELEMENT_NAME;
        @SuppressWarnings("unchecked")
        XMLObjectBuilder<LogoutResponse> builder = Configuration.getBuilderFactory().getBuilder(qname);
        LogoutResponse logoutResponse = builder.buildObject(qname.getNamespaceURI(), qname.getLocalPart(), qname.getPrefix());

        logoutResponse.setID(responseID.toString());
        logoutResponse.setIssueInstant(new DateTime(DateTimeZone.UTC));
        logoutResponse.setVersion(SAMLVersion.VERSION_20);
        
        qname = Status.DEFAULT_ELEMENT_NAME;
        @SuppressWarnings("unchecked")
        XMLObjectBuilder<Status> statusBuilder = Configuration.getBuilderFactory().getBuilder(qname);
        Status status = statusBuilder.buildObject(qname.getNamespaceURI(), qname.getLocalPart(), qname.getPrefix());
        
        qname = StatusCode.DEFAULT_ELEMENT_NAME;
        @SuppressWarnings("unchecked")
        XMLObjectBuilder<StatusCode> statusCodeBuilder = Configuration.getBuilderFactory().getBuilder(qname);
        StatusCode statusCode = statusCodeBuilder.buildObject(qname.getNamespaceURI(), qname.getLocalPart(), qname.getPrefix());
        statusCode.setValue(StatusCode.SUCCESS_URI);

        status.setStatusCode(statusCode);
        logoutResponse.setStatus(status);

        logoutResponse.setInResponseTo(request.getID());

        logoutResponse.setIssuer(createIssuer(config.getServiceIdentifier()));
        logoutResponse.setDestination(config.getLogoutUrl());

        logoutResponse.validate(true);
        return logoutResponse;
    }
    
    protected final String buildSignedAndEncodedRequestUrl(AuthnRequest authnRequest, UUID relayState) {
        try {
            Encoder enc = new Encoder();
            return enc.buildRedirectURL(getSigningCredential(), relayState, authnRequest);
        } catch (Exception e) {
            throw new RuntimeException("Exception caught when signing and encoding request URL: " + e);
        }
    }

    protected final String buildSignedAndEncodedLogoutRequestUrl(LogoutRequest logoutRequest, UUID relayState) {
        try {
            Encoder enc = new Encoder();
            return enc.buildRedirectURL(getSigningCredential(), relayState, logoutRequest);
        } catch (Exception e) {
            throw new RuntimeException("Exception caught when signing and encoding request URL: " + e);
        }
    }

    protected final Path getServiceProviderURI() {
        return this.serviceProviderURI;
    }
    
    private Issuer createIssuer(String serviceIdentifier) {
        QName qname = Issuer.DEFAULT_ELEMENT_NAME;
        @SuppressWarnings("unchecked")
        XMLObjectBuilder<Issuer> builder = Configuration.getBuilderFactory().getBuilder(qname);
        Issuer issuer = builder.buildObject(qname.getNamespaceURI(), qname.getLocalPart(), qname.getPrefix());
        issuer.setValue(serviceIdentifier);
        return issuer;
    }

    private void verifyConfirmationTimeNotExpired(Assertion assertion) throws RuntimeException {
        DateTime confirmationTime = assertionConfirmationTime(assertion);
        if (confirmationTime == null || !confirmationTime.isAfterNow()) {
            throw new RuntimeException("Assertion confirmation time has expired: " + confirmationTime + " before "
                    + new DateTime());
        }
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
    
    private URL getServiceProviderURL(HttpServletRequest request) {
        URL url = URL.create(request);
        url.clearParameters();
        url.setPath(this.serviceProviderURI);
        return url;
    }
    
    protected static class Encoder extends HTTPRedirectDeflateEncoder {

        @Override
        public String deflateAndBase64Encode(SAMLObject obj) throws MessageEncodingException {
            String messageStr = XMLHelper.nodeToString(marshallMessage(obj));

            ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
            Deflater deflater = new Deflater(Deflater.DEFLATED, true);
            DeflaterOutputStream deflaterStream = new DeflaterOutputStream(bytesOut, deflater);
            try {
                deflaterStream.write(messageStr.getBytes("UTF-8"));
                deflaterStream.finish();
            } catch (IOException e) {
                throw new RuntimeException("Unable to deflate message", e);
            }

            return Base64.encodeBytes(bytesOut.toByteArray(), Base64.DONT_BREAK_LINES);
        }

        @Override
        public String getSignatureAlgorithmURI(Credential arg0, SecurityConfiguration arg1) throws MessageEncodingException {
            return super.getSignatureAlgorithmURI(arg0, arg1);
        }

        @Override
        public String generateSignature(Credential arg0, String arg1, String arg2) throws MessageEncodingException {
            return super.generateSignature(arg0, arg1, arg2);
        }

        public String buildRedirectURL(Credential signingCredential, UUID relayState, RequestAbstractType request)
        throws MessageEncodingException, IOException {
            SAMLMessageContext<?, RequestAbstractType, ?> messageContext = new BasicSAMLMessageContext<SAMLObject, RequestAbstractType, SAMLObject>();
            // Build the parameters for the request
            messageContext.setOutboundSAMLMessage(request);
            messageContext.setRelayState(relayState.toString());

            // Sign the parameters
            messageContext.setOutboundSAMLMessageSigningCredential(signingCredential);

            String messageStr = XMLHelper.nodeToString(marshallMessage(request));

            ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
            Deflater deflater = new Deflater(Deflater.DEFLATED, true);
            DeflaterOutputStream deflaterStream = new DeflaterOutputStream(bytesOut, deflater);
            try {
                deflaterStream.write(messageStr.getBytes("UTF-8"));
                deflaterStream.finish();
            } catch (IOException e) {
                throw new RuntimeException("Unable to deflate message", e);
            }

            String encoded = Base64.encodeBytes(bytesOut.toByteArray(), Base64.DONT_BREAK_LINES);
            return super.buildRedirectURL(messageContext, request.getDestination(), encoded);
        }
        
    }
}

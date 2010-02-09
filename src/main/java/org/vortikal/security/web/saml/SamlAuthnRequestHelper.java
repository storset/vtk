package org.vortikal.security.web.saml;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.opensaml.DefaultBootstrap;
import org.opensaml.common.SAMLObject;
import org.opensaml.common.binding.BasicSAMLMessageContext;
import org.opensaml.common.binding.SAMLMessageContext;
import org.opensaml.common.xml.SAMLConstants;
import org.opensaml.saml2.binding.encoding.HTTPRedirectDeflateEncoder;
import org.opensaml.saml2.core.AuthnRequest;
import org.opensaml.saml2.core.LogoutRequest;
import org.opensaml.saml2.core.RequestAbstractType;
import org.opensaml.ws.message.encoder.MessageEncodingException;
import org.opensaml.xml.ConfigurationException;
import org.opensaml.xml.Namespace;
import org.opensaml.xml.security.credential.Credential;
import org.opensaml.xml.util.Base64;
import org.opensaml.xml.util.XMLHelper;
import org.opensaml.xml.validation.ValidationException;

public class SamlAuthnRequestHelper {

    public static final String CERTIFICATE_KEY = "";

    public static final String KEYSTORE_PATH = "/home/dave/source/testwebclient/keystore";

    private OpenSAMLUtilites utils;


    public SamlAuthnRequestHelper() {
        try {
            DefaultBootstrap.bootstrap();
        } catch (ConfigurationException e) {
            throw new RuntimeException("Exception when trying to bootstrap OpenSAML." + e);
        }
        utils = new OpenSAMLUtilites();
    }


    public String urlToLoginServiceForDomain(String domain, String relayState) {
        SamlConfiguration config = ConfigurationRepository.configurationForDomain(domain);
        AuthnRequest authnRequest = createAuthenticationRequest(config);
        String url = buildSignedAndEncodedRequestUrl(authnRequest, relayState);

        return url;
    }


    private String buildSignedAndEncodedRequestUrl(AuthnRequest authnRequest, String relayState) {

        try {
            SAMLEncoder enc = new SAMLEncoder();
            Credential signingCredential = new CredentialRepository().getCredential(KEYSTORE_PATH, CERTIFICATE_KEY);
            return enc.buildRedirectURL(signingCredential, relayState, authnRequest);
        } catch (Exception e) {
            throw new RuntimeException("Exception caught when signing and encoding request URL: " + e);
        }
    }


    private AuthnRequest createAuthenticationRequest(SamlConfiguration config) throws RuntimeException {
        AuthnRequest authnRequest = OpenSAMLUtilites.buildXMLObject(AuthnRequest.class);

        authnRequest.setID("_" + UUID.randomUUID().toString());
        authnRequest.setForceAuthn(Boolean.FALSE);
        authnRequest.setIssueInstant(new DateTime(DateTimeZone.UTC));
        authnRequest.setProtocolBinding(config.protocolBinding());
        authnRequest.setDestination(config.authenticationUrl());
        authnRequest.setAssertionConsumerServiceURL(config.serviceLoginUrl());
        authnRequest.setIssuer(config.issuer());

        try {
            authnRequest.validate(true);
        } catch (ValidationException e) {
            throw new RuntimeException("Unable to validate SAML authentication request: " + e);
        }

        return authnRequest;
    }


    public String urlToLogoutServiceForDomain(String domain, String relayState) {
        SamlConfiguration config = ConfigurationRepository.configurationForDomain(domain);
        LogoutRequest logoutRequest = createLogoutRequest(config);
        String url = buildSignedAndEncodedLogoutRequestUrl(logoutRequest, relayState);

        return url;
    }


    private String buildSignedAndEncodedLogoutRequestUrl(LogoutRequest logoutRequest, String relayState) {

        try {
            SAMLEncoder enc = new SAMLEncoder();
            Credential signingCredential = new CredentialRepository().getCredential(KEYSTORE_PATH, CERTIFICATE_KEY);
            return enc.buildRedirectURL(signingCredential, relayState, logoutRequest);
        } catch (Exception e) {
            throw new RuntimeException("Exception caught when signing and encoding request URL: " + e);
        }
    }


    private LogoutRequest createLogoutRequest(SamlConfiguration config) {
        LogoutRequest logoutRequest = OpenSAMLUtilites.buildXMLObject(LogoutRequest.class);

        logoutRequest.setID("_" + UUID.randomUUID().toString());
        logoutRequest.setIssueInstant(new DateTime(DateTimeZone.UTC));
        logoutRequest.addNamespace(new Namespace(SAMLConstants.SAML20_NS, SAMLConstants.SAML20_PREFIX));
        logoutRequest.setDestination(config.serviceLogoutUrl());
        logoutRequest.setReason("urn:oasis:names:tc:SAML:2.0:logout:user");
        logoutRequest.setIssuer(config.issuer());

        try {
            logoutRequest.validate(true);
        } catch (ValidationException e) {
            throw new RuntimeException("Unable to validate SAML logout request: " + e);
        }

        return logoutRequest;

    }

    private class SAMLEncoder extends HTTPRedirectDeflateEncoder {

        public String buildRedirectURL(Credential signingCredential, String relayState, RequestAbstractType request)
                throws MessageEncodingException, IOException {
            SAMLMessageContext<?, RequestAbstractType, ?> messageContext = new BasicSAMLMessageContext<SAMLObject, RequestAbstractType, SAMLObject>();
            // Build the parameters for the request
            messageContext.setOutboundSAMLMessage(request);
            messageContext.setRelayState(relayState);

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

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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

import javax.xml.namespace.QName;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.opensaml.Configuration;
import org.opensaml.common.SAMLObject;
import org.opensaml.common.binding.BasicSAMLMessageContext;
import org.opensaml.common.binding.SAMLMessageContext;
import org.opensaml.common.xml.SAMLConstants;
import org.opensaml.saml2.binding.encoding.HTTPRedirectDeflateEncoder;
import org.opensaml.saml2.core.AuthnRequest;
import org.opensaml.saml2.core.Issuer;
import org.opensaml.saml2.core.LogoutRequest;
import org.opensaml.saml2.core.RequestAbstractType;
import org.opensaml.ws.message.encoder.MessageEncodingException;
import org.opensaml.xml.Namespace;
import org.opensaml.xml.XMLObjectBuilder;
import org.opensaml.xml.security.credential.Credential;
import org.opensaml.xml.util.Base64;
import org.opensaml.xml.util.XMLHelper;
import org.opensaml.xml.validation.ValidationException;

public class SamlAuthnRequestHelper {

    private Credential signingCredential;

    public SamlAuthnRequestHelper(Credential signingCredential) {
        this.signingCredential = signingCredential;
    }


    public String urlToLoginServiceForDomain(SamlConfiguration config, String relayState) {
        AuthnRequest authnRequest = createAuthenticationRequest(config);
        String url = buildSignedAndEncodedRequestUrl(authnRequest, relayState);
        return url;
    }


    
    public String urlToLogoutServiceForDomain(SamlConfiguration config, String relayState) {
        LogoutRequest logoutRequest = createLogoutRequest(config);
        String url = buildSignedAndEncodedLogoutRequestUrl(logoutRequest, relayState);

        return url;
    }
    
    private String buildSignedAndEncodedRequestUrl(AuthnRequest authnRequest, String relayState) {
        try {
            SAMLEncoder enc = new SAMLEncoder();
            return enc.buildRedirectURL(this.signingCredential, relayState, authnRequest);
        } catch (Exception e) {
            throw new RuntimeException("Exception caught when signing and encoding request URL: " + e);
        }
    }


    private AuthnRequest createAuthenticationRequest(SamlConfiguration config) throws RuntimeException {
        QName qname = AuthnRequest.DEFAULT_ELEMENT_NAME;
        @SuppressWarnings("unchecked")
        XMLObjectBuilder<AuthnRequest> builder = Configuration.getBuilderFactory().getBuilder(qname);
        AuthnRequest authnRequest = builder.buildObject(qname.getNamespaceURI(), qname.getLocalPart(), qname.getPrefix());
        
        authnRequest.setID("_" + UUID.randomUUID().toString());
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

    private Issuer createIssuer(String serviceIdentifier) {
        QName qname = Issuer.DEFAULT_ELEMENT_NAME;
        @SuppressWarnings("unchecked")
        XMLObjectBuilder<Issuer> builder = Configuration.getBuilderFactory().getBuilder(qname);        
        Issuer issuer = builder.buildObject(qname.getNamespaceURI(), qname.getLocalPart(), qname.getPrefix());
        issuer.setValue(serviceIdentifier);
        return issuer;
    }
    


    private String buildSignedAndEncodedLogoutRequestUrl(LogoutRequest logoutRequest, String relayState) {

        try {
            SAMLEncoder enc = new SAMLEncoder();
            return enc.buildRedirectURL(this.signingCredential, relayState, logoutRequest);
        } catch (Exception e) {
            throw new RuntimeException("Exception caught when signing and encoding request URL: " + e);
        }
    }

    private LogoutRequest createLogoutRequest(SamlConfiguration config) {
        QName qname = LogoutRequest.DEFAULT_ELEMENT_NAME;
        @SuppressWarnings("unchecked")
        XMLObjectBuilder<LogoutRequest> builder = Configuration.getBuilderFactory().getBuilder(qname);

        LogoutRequest logoutRequest = builder.buildObject(qname.getNamespaceURI(), qname.getLocalPart(), qname.getPrefix());
        logoutRequest.setID("_" + UUID.randomUUID().toString());
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

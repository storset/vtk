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

import java.io.IOException;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.opensaml.DefaultBootstrap;
import org.opensaml.xml.ConfigurationException;
import org.opensaml.xml.security.credential.Credential;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.vortikal.repository.Path;
import org.vortikal.security.AuthenticationException;
import org.vortikal.security.AuthenticationProcessingException;
import org.vortikal.security.Principal;
import org.vortikal.security.PrincipalFactory;
import org.vortikal.security.web.AuthenticationChallenge;
import org.vortikal.security.web.AuthenticationHandler;
import org.vortikal.security.web.InvalidAuthenticationRequestException;
import org.vortikal.web.service.URL;

/**
 * Skeleton of what will be a SAML Web browser SSO authentication handler/challenge
 */
public class SamlAuthenticationHandler implements AuthenticationChallenge, AuthenticationHandler, Controller {

    static {
        try {
            DefaultBootstrap.bootstrap();
        } catch (ConfigurationException e) {
            throw new RuntimeException("Exception when trying to bootstrap OpenSAML." + e);
        }
    }
    
    private static final String URL_SESSION_ATTR = SamlAuthenticationHandler.class.getName() + ".SamlSavedURL";

    private PrincipalFactory principalFactory;

    private Path serviceProviderURI;

    private CertificateManager certificateManager;
    private String privateKeyAlias;
    
    private String idpCertificate;

    // IDP login/logout URLs:
    private String authenticationURL;
    private String logoutURL;

    // A string identifying this service provider
    private String serviceIdentifier;


    @Override
    public void challenge(HttpServletRequest req, HttpServletResponse resp) throws AuthenticationProcessingException,
            ServletException, IOException {
        // 1. Save request information in session for later redirect
        URL url = URL.create(req);
        req.getSession(true).setAttribute(URL_SESSION_ATTR, url);

        Credential signingCredential = this.certificateManager.getCredential(this.privateKeyAlias);
        if (signingCredential == null) {
            throw new AuthenticationProcessingException(
                    "Unable to obtain credentials for signing using keystore alias '" 
                    +  this.privateKeyAlias + "'");
        }
        SamlAuthnRequestHelper samlConnector = new SamlAuthnRequestHelper(signingCredential);
        String generatedRelayState = generateRelayState();

        SamlConfiguration samlConfiguration = newSamlConfiguration(req);
        
        String redirURL = samlConnector.urlToLoginServiceForDomain(samlConfiguration, generatedRelayState);
        resp.sendRedirect(redirURL);
    }

    @Override
    public boolean isRecognizedAuthenticationRequest(HttpServletRequest req) throws AuthenticationProcessingException,
            InvalidAuthenticationRequestException {
        // If request is POST and request.uri = this.serviceProviderURI
        // and request contains both parameters (SAMLResponse, RelayState)
        // then return true else return false
        URL url = URL.create(req);
        if (!url.getPath().equals(this.serviceProviderURI)) {
            return false;
        }
        if (!"POST".equals(req.getMethod())) {
            return false;
        }
        if (req.getParameter("SAMLResponse") == null) {
            return false;
        }
        if (req.getParameter("RelayState") == null) {
            return false;
        }
        return true;
    }

    /**
     * Performs the authentication based on the SAMLResponse request parameter
     */
    @Override
    public Principal authenticate(HttpServletRequest req) throws AuthenticationProcessingException,
            AuthenticationException, InvalidAuthenticationRequestException {

        URL loginURL = getServiceProviderURL(req);
        SamlResponseHelper helper = new SamlResponseHelper(loginURL, this.idpCertificate, req);
        UserData userData = helper.getUserData();
        if (userData != null) {
            String id = userData.getUsername();
            return this.principalFactory.getPrincipal(id, Principal.Type.USER);
        }
        // 1) Dekode URL
        // 2) Verifiser signatur
        // 3) Valider assertion...
        // 4) Sjekke at vi har sendt request (in-reply-to-felt...)
        // 5) Hent uid, cn etc. fra assertion

        // Verify SAMLResponse and RelayState form inputs
        // If OK:
        // id = <get user id from request>
        // return this.principalFactory.getPrincipal(id, Principal.Type.USER);
        // Else:
        else {
            throw new AuthenticationException("Unable to authenticate request " + req);
        }
    }

    /**
     * Does a redirect to the original resource after a successful authentication
     */
    @Override
    public boolean postAuthentication(HttpServletRequest req, HttpServletResponse resp)
            throws AuthenticationProcessingException, InvalidAuthenticationRequestException {
        // Get original request URL (saved in challenge()) from session, redirect to it
        URL url = (URL) req.getSession(true).getAttribute(URL_SESSION_ATTR);
        try {
            resp.sendRedirect(url.toString());
            return true;
        } catch (Exception e) {
            throw new AuthenticationProcessingException(e);
        }
    }

    /**
     * Initiates logout process with IDP
     */
    @Override
    public boolean logout(Principal principal, HttpServletRequest req, HttpServletResponse resp)
            throws AuthenticationProcessingException, ServletException, IOException {

        URL savedURL = URL.create(req);
        req.getSession(true).setAttribute(URL_SESSION_ATTR, savedURL);
        
        Credential signingCredential = this.certificateManager.getCredential(this.privateKeyAlias);
        if (signingCredential == null) {
            throw new AuthenticationProcessingException(
                    "Unable to obtain credentials for signing using keystore alias '" 
                    +  this.privateKeyAlias + "'");
        }
        SamlAuthnRequestHelper samlConnector = new SamlAuthnRequestHelper(signingCredential);
        String generatedRelayState = generateRelayState();

        SamlConfiguration samlConfiguration = newSamlConfiguration(req);
        String url = samlConnector.urlToLogoutServiceForDomain(samlConfiguration, generatedRelayState);
        resp.sendRedirect(url);
        return true;
    }

    /**
     * Handles post-logout requests from IDP (redirects to original resource)
     */
    @Override
    public ModelAndView handleRequest(HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        HttpSession session = request.getSession();
        if (session == null){
            throw new IllegalStateException("No session exists, not a post-logout request");
        }
        
        URL url = (URL) session.getAttribute(URL_SESSION_ATTR);
        if (url == null) {
            throw new IllegalStateException("No URL session attribute exists, nowhere to redirect");
        }
        response.sendRedirect(url.toString());
        return null;
    }

    @Override
    public AuthenticationChallenge getAuthenticationChallenge() {
        return this;
    }


    @Override
    public boolean isLogoutSupported() {
        return true;
    }

    private SamlConfiguration newSamlConfiguration(HttpServletRequest request) {
        URL url = getServiceProviderURL(request);
        SamlConfiguration configuration = new SamlConfiguration(this.authenticationURL, 
                this.logoutURL, url.toString(), this.serviceIdentifier);
        return configuration;
    }
 
    private URL getServiceProviderURL(HttpServletRequest request) {
        URL url = URL.create(request);
        url.clearParameters();
        url.setPath(this.serviceProviderURI);
        return url;
    }
    
    private String generateRelayState() {
        return UUID.randomUUID().toString();
    }


    @Required
    public void setPrincipalFactory(PrincipalFactory principalFactory) {
        this.principalFactory = principalFactory;
    }


    @Required
    public void setServiceProviderURI(String serviceProviderURI) {
        this.serviceProviderURI = Path.fromString(serviceProviderURI);
    }


    @Required
    public void setIdpCertificate(String idpCertificate) {
        this.idpCertificate = idpCertificate;
    }


    @Required
    public void setCertificateManager(CertificateManager certificateManager) {
        this.certificateManager = certificateManager;
    }

    @Required
    public void setPrivateKeyAlias(String privateKeyAlias) {
        this.privateKeyAlias = privateKeyAlias;
    }

    @Required
    public void setAuthenticationURL(String authenticationURL) {
        this.authenticationURL = authenticationURL;
    }

    @Required
    public void setLogoutURL(String logoutURL) {
        this.logoutURL = logoutURL;
    }

    @Required
    public void setServiceIdentifier(String serviceIdentifier) {
        this.serviceIdentifier = serviceIdentifier;
    }
}
package org.vortikal.security.web.saml;

import java.io.IOException;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Required;
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
public class SamlAuthenticationHandler implements AuthenticationChallenge, AuthenticationHandler {

    private static final String URL_SESSION_ATTR = SamlAuthenticationHandler.class.getName() + ".SamlSavedURL";

    private PrincipalFactory principalFactory;

    private Path serviceProviderURI;

    private String keystorePath;
    private String certKey;

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

        SamlAuthnRequestHelper samlConnector = new SamlAuthnRequestHelper(this.keystorePath, this.certKey);
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


    @Override
    public Principal authenticate(HttpServletRequest req) throws AuthenticationProcessingException,
            AuthenticationException, InvalidAuthenticationRequestException {

        URL loginURL = getServiceProviderURL(req);
        SamlResponseHelper helper = new SamlResponseHelper(loginURL, this.idpCertificate, req);
        UserData userData = helper.getUserData();
        if (userData != null) {
            String id = userData.username();
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

    @Override
    public AuthenticationChallenge getAuthenticationChallenge() {
        return this;
    }


    @Override
    public boolean isLogoutSupported() {
        return true;
    }


    @Override
    public boolean logout(Principal principal, HttpServletRequest req, HttpServletResponse resp)
            throws AuthenticationProcessingException, ServletException, IOException {

        SamlAuthnRequestHelper samlConnector = new SamlAuthnRequestHelper(this.keystorePath, this.certKey);
        String generatedRelayState = generateRelayState();

        SamlConfiguration samlConfiguration = newSamlConfiguration(req);
        String url = samlConnector.urlToLogoutServiceForDomain(samlConfiguration, generatedRelayState);
        resp.sendRedirect(url);
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
    public void setKeystorePath(String keystorePath) {
        this.keystorePath = keystorePath;
    }


    @Required
    public void setCertKey(String certKey) {
        this.certKey = certKey;
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

    @Required
    public void setServiceIdentifier(String serviceIdentifier) {
        this.serviceIdentifier = serviceIdentifier;
    }
}
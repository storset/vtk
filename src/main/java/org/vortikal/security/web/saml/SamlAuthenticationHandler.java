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

    public static final String DOMAIN = "davide-laptop.uio.no";

    private final static String USERNAME = "username";

    private static final String URL_SESSION_ATTR = SamlAuthenticationHandler.class.getName() + ".SamlSavedURL";

    private PrincipalFactory principalFactory;

    private Path serviceProviderURI;


    @Override
    public void challenge(HttpServletRequest req, HttpServletResponse resp) throws AuthenticationProcessingException,
            ServletException, IOException {
        // 1. Save request information in session for later redirect
        URL url = URL.create(req);
        req.getSession(true).setAttribute(URL_SESSION_ATTR, url);

        // 2. Present client with a XHTML form:
        // <form method="post" action="https://idp.example.org/SAML2/SSO/POST" ...>
        // <input type="hidden" name="SAMLRequest" value="request" />
        // <input type="hidden" name="RelayState" value="token" />
        // ...
        // <input type="submit" value="Submit" />
        // </form>

        String userName = userNameFromSession(req);

        if (userIsAuthenticated(userName)) {
            System.out.println("Access granted");
        } else {
            sendUserToAuthenticationPoint(resp);
        }

        // try {
        // resp.getWriter().write("<html><head></head><body><form...></form></body></html>");
        // } catch (Exception e) {
        // throw new AuthenticationProcessingException(e);
        // }
    }


    private void sendUserToAuthenticationPoint(HttpServletResponse response) throws ServletException, IOException {
        SamlAuthnRequestHelper samlConnector = new SamlAuthnRequestHelper();
        String generatedRelayState = generateRelayState();

        String url = samlConnector.urlToLoginServiceForDomain(DOMAIN, generatedRelayState);
        response.sendRedirect(url);
    }


    private boolean userIsAuthenticated(String userName) {
        return userName != null;
    }


    private String userNameFromSession(HttpServletRequest request) throws ServletException, IOException {
        return (String) request.getSession(true).getAttribute(USERNAME);
    }


    private String generateRelayState() {
        return UUID.randomUUID().toString();
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

        UserData userData = SamlResponseHelper.retrieveUserDataFromSamlResponse(req.getParameter("SAMLResponse"));
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
    public AuthenticationChallenge getAuthenticationChallenge() {
        return this;
    }


    @Override
    public boolean isLogoutSupported() {
        // TODO: implement single logout protocol
        return true;
    }


    @Override
    public boolean logout(Principal principal, HttpServletRequest req, HttpServletResponse resp)
            throws AuthenticationProcessingException, ServletException, IOException {
        // TODO: implement single logout protocol
        sendUserToLogout(resp);
        return true;
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


    private void sendUserToLogout(HttpServletResponse response) throws ServletException, IOException {
        SamlAuthnRequestHelper samlConnector = new SamlAuthnRequestHelper();
        String generatedRelayState = generateRelayState();

        String url = samlConnector.urlToLogoutServiceForDomain(DOMAIN, generatedRelayState);
        response.sendRedirect(url);
    }


    @Required
    public void setPrincipalFactory(PrincipalFactory principalFactory) {
        this.principalFactory = principalFactory;
    }


    @Required
    public void setServiceProviderURI(String serviceProviderURI) {
        this.serviceProviderURI = Path.fromString(serviceProviderURI);
    }

}

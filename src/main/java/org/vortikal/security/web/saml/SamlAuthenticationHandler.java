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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.vortikal.security.AuthenticationException;
import org.vortikal.security.AuthenticationProcessingException;
import org.vortikal.security.Principal;
import org.vortikal.security.PrincipalFactory;
import org.vortikal.security.web.AuthenticationChallenge;
import org.vortikal.security.web.AuthenticationHandler;
import org.vortikal.security.web.InvalidAuthenticationRequestException;
import org.vortikal.web.InvalidRequestException;
import org.vortikal.web.service.URL;

/**
 * Skeleton of what will be a SAML Web browser SSO authentication handler/challenge
 */
public class SamlAuthenticationHandler implements AuthenticationChallenge, AuthenticationHandler, Controller {

    private String identifier;
    private Challenge challenge;
    private Login login;
    private Logout logout;
    private LostPostHandler postHandler;

    private Map<String, String> staticHeaders = new HashMap<String, String>();

    private Set<LoginListener> loginListeners;

    private PrincipalFactory principalFactory;

    private Set<?> categories = Collections.EMPTY_SET;

    @Override
    public void challenge(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationProcessingException, ServletException, IOException {
        if ("POST".equals(request.getMethod())) {
            this.postHandler.saveState(request, response);
        }
        this.challenge.challenge(request, response);
        setHeaders(response);
    }

    @Override
    public boolean isRecognizedAuthenticationRequest(HttpServletRequest req) throws AuthenticationProcessingException,
            InvalidAuthenticationRequestException {
        return this.login.isLoginResponse(req);
    }

    /**
     * Performs the authentication based on the SAMLResponse request parameter
     */
    @Override
    public AuthResult authenticate(HttpServletRequest request) throws AuthenticationProcessingException,
            AuthenticationException, InvalidAuthenticationRequestException {

        if (this.login.isUnsolicitedLoginResponse(request)) {
            this.challenge.prepareUnsolicitedChallenge(request);
            throw new AuthenticationException("Unsolicitated authentication request: " + request);
        }

        UserData userData = this.login.login(request);
        if (userData == null) {
            throw new AuthenticationException("Unable to authenticate request " + request);
        }
        String id = userData.getUsername();
        Principal principal = this.principalFactory.getPrincipal(id, Principal.Type.USER);
        if (this.loginListeners != null) {
            for (LoginListener listener : this.loginListeners) {
                try {
                    listener.onLogin(principal, userData);
                } catch (Exception e) {
                    throw new AuthenticationProcessingException("Failed to invoke login listener: " + listener, e);
                }
            }
        }
        return new AuthResult(principal.getQualifiedName());
    }

    /**
     * Does a redirect to the original resource after a successful authentication
     */
    @Override
    public boolean postAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationProcessingException, InvalidAuthenticationRequestException {
        if (this.postHandler.hasSavedState(request)) {
            this.postHandler.redirect(request, response);
            setHeaders(response);
            return true;
        }

        this.login.redirectAfterLogin(request, response);
        setHeaders(response);
        return true;
    }

    /**
     * Initiates logout process with IDP
     */
    @Override
    public boolean logout(Principal principal, HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationProcessingException, ServletException, IOException {
        this.logout.initiateLogout(request, response);
        setHeaders(response);
        return true;
    }

    /**
     * Handles incoming logout requests (originated from IDP) and responses (from IDP based on request from us)
     */
    @Override
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (request.getParameter("SAMLResponse") == null && request.getParameter("SAMLRequest") == null) {
            throw new InvalidRequestException(
                    "Invalid SAML request: expected one of 'SAMLRequest' or 'SAMLResponse' parameters");
        }
        URL url = null;

        if (login.isLoginResponse(request)) {
            // User typically hit 'back' button after logging in and got sent here from IDP:
            String relayState = request.getParameter("RelayState");
            if (relayState != null) {
                url = URL.parse(relayState);
            }
            if (url != null) {
                if (url.getParameter("authTicket") != null) {
                    url.removeParameter("authTicket");
                }
                response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
                response.setHeader("Location", url.toString());
                setHeaders(response);
                return null;
            }
        } else if (logout.isLogoutRequest(request)) {
            // Logout request from IDP (based on some other SP's request)
            this.logout.handleLogoutRequest(request, response);
            setHeaders(response);
            return null;
        } else if (this.logout.isLogoutResponse(request)) {
            // Logout response (based on our request) from IDP
            this.logout.handleLogoutResponse(request, response);
            setHeaders(response);
            return null;
        }
        // Request is neither logout request nor logout response nor login response.
        throw new InvalidRequestException("Invalid SAML request: ");
    }

    @Override
    public AuthenticationChallenge getAuthenticationChallenge() {
        return this;
    }

    @Override
    public boolean isLogoutSupported() {
        return true;
    }

    @Required
    public void setPrincipalFactory(PrincipalFactory principalFactory) {
        this.principalFactory = principalFactory;
    }

    @Required
    public void setChallenge(Challenge challenge) {
        this.challenge = challenge;
    }

    @Required
    public void setLogin(Login login) {
        this.login = login;
    }

    @Required
    public void setLogout(Logout logout) {
        this.logout = logout;
    }

    @Required
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    @Required
    public void setPostHandler(LostPostHandler postHandler) {
        this.postHandler = postHandler;
    }

    public void setLoginListeners(Set<LoginListener> loginListeners) {
        this.loginListeners = loginListeners;
    }

    public void setStaticHeaders(Map<String, String> staticHeaders) {
        this.staticHeaders = staticHeaders;
    }

    public String getIdentifier() {
        return this.identifier;
    }

    public String toString() {
        return this.getClass().getName() + ":" + this.identifier;
    }

    private void setHeaders(HttpServletResponse response) {
        for (String header : this.staticHeaders.keySet()) {
            response.setHeader(header, this.staticHeaders.get(header));
        }
    }

    public void setCategories(Set<?> categories) {
        this.categories = categories;
    }

    @Override
    public Set<?> getCategories() {
        return this.categories;
    }
}
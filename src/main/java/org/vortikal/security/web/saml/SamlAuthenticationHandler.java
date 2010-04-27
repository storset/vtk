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

/**
 * Skeleton of what will be a SAML Web browser SSO authentication handler/challenge
 */
public class SamlAuthenticationHandler implements AuthenticationChallenge, AuthenticationHandler, Controller {

    private String identifier;
    private Challenge challenge;
    private Login login;
    private Logout logout;
    
    private Set<LoginListener> loginListeners;
    
    private PrincipalFactory principalFactory;

    @Override
    public void challenge(HttpServletRequest request, HttpServletResponse response) throws AuthenticationProcessingException,
            ServletException, IOException {
        this.challenge.challenge(request, response);
    }

    @Override
    public boolean isRecognizedAuthenticationRequest(HttpServletRequest req) throws AuthenticationProcessingException,
            InvalidAuthenticationRequestException {
        return this.login.isLoginRequest(req);
    }

    /**
     * Performs the authentication based on the SAMLResponse request parameter
     */
    @Override
    public Principal authenticate(HttpServletRequest request) throws AuthenticationProcessingException,
            AuthenticationException, InvalidAuthenticationRequestException {
        
        UserData userData = this.login.login(request);
        if (userData != null) {
            String id = userData.getUsername();
            Principal principal = this.principalFactory.getPrincipal(id, Principal.Type.USER);
            if (this.loginListeners != null) {
                for (LoginListener listener : this.loginListeners) {
                    try {
                        listener.onLogin(principal, userData);
                    } catch (Exception e) {
                        throw new AuthenticationProcessingException(
                                "Failed to invoke login listener: " + listener, e);
                    }
                }
            }
            return principal;
        } else {
            throw new AuthenticationException("Unable to authenticate request " + request);
        }
    }

    /**
     * Does a redirect to the original resource after a successful authentication
     */
    @Override
    public boolean postAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationProcessingException, InvalidAuthenticationRequestException {
        this.login.redirectAfterLogin(request, response);
        return true;
    }

    /**
     * Initiates logout process with IDP
     */
    @Override
    public boolean logout(Principal principal, HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationProcessingException, ServletException, IOException {
        this.logout.initiateLogout(request, response);
        return true;
    }

    /**
     * Handles incoming logout requests (originated from IDP) and responses 
     * (from IDP based on request from us)
     */
    @Override
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (request.getParameter("SAMLResponse") == null && request.getParameter("SAMLRequest") == null) {
            throw new InvalidRequestException("Invalid SAML request: expected one of 'SAMLRequest' or 'SAMLResponse' parameters");
        }
        if (request.getParameter("SAMLResponse") == null) {
            // Logout request from IDP (based on some other SP's request)
            this.logout.handleLogoutRequest(request, response);
        } else {
            // Logout response (based on our request) from IDP
            this.logout.handleLogoutResponse(request, response);
        }
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
    
    public void setLoginListeners(Set<LoginListener> loginListeners) {
        this.loginListeners = loginListeners;
    }

    public String getIdentifier() {
        return this.identifier;
    }

}
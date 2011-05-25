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

import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.opensaml.saml2.core.AuthnRequest;
import org.vortikal.security.AuthenticationProcessingException;
import org.vortikal.web.service.URL;

public class Challenge extends SamlService {
    private static final String UNSOLICITED_AUTH_REDIRECT = 
        Challenge.class.getName() + ".unsolicitedRedirectURL";
    

    private String urlSessionAttribute = null;
    
    public void setUrlSessionAttribute(String urlSessionAttribute) {
        if (urlSessionAttribute != null && !"".equals(urlSessionAttribute.trim())) {
            this.urlSessionAttribute = urlSessionAttribute;
        }
    }
    
    public void challenge(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationProcessingException {

        if (request.getAttribute(UNSOLICITED_AUTH_REDIRECT) != null) {
            URL url = (URL) request.getAttribute(UNSOLICITED_AUTH_REDIRECT);
            response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
            response.setHeader("Location", url.toString());
            return;
        }

        if (request.getScheme().equals("http")) {
            redirectToHttps(request, response);
            return;
        }

        redirectToIDP(request, response);
    }

    
    public void prepareUnsolicitedChallenge(HttpServletRequest request) {
        String relayState = request.getParameter("RelayState");
        request.setAttribute(UNSOLICITED_AUTH_REDIRECT, URL.parse(relayState));
    }

    public String urlToLoginServiceForDomain(SamlConfiguration config, UUID requestID, String relayState) {
        AuthnRequest authnRequest = createAuthenticationRequest(config, requestID);
        String url = buildSignedAndEncodedRequestUrl(authnRequest, relayState);
        return url;
    }

    private void redirectToHttps(HttpServletRequest request, HttpServletResponse response) {
        // XXX: remove hard-coded 'authTarget' parameter:
        // XXX: move redirect -> ssl elsewhere
        URL redirectURL = URL.create(request);
        redirectURL.setProtocol("https");
        redirectURL.setParameter("authTarget", "http");
        response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
        response.setHeader("Location", redirectURL.toString());
    }
    
    private void redirectToIDP(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(true);
        URL url = URL.create(request);
        if (this.urlSessionAttribute != null) {
            session.setAttribute(this.urlSessionAttribute, url);
        }
        String relayState = url.toString();
        
        SamlConfiguration samlConfiguration = newSamlConfiguration(request);

        // Generate request ID, save in session
        UUID requestID = UUID.randomUUID();
        setRequestIDSessionAttribute(request, url, requestID);

        String redirectURL = urlToLoginServiceForDomain(samlConfiguration, requestID, relayState);
        response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
        response.setHeader("Location", redirectURL.toString());
    }
    
    
}

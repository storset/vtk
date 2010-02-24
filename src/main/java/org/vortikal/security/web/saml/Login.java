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

import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.Response;
import org.vortikal.security.AuthenticationException;
import org.vortikal.security.AuthenticationProcessingException;
import org.vortikal.web.service.URL;

public class Login extends SamlService {

    public boolean isLoginRequest(HttpServletRequest req) {
        URL url = URL.create(req);
        if (!url.getPath().equals(getServiceProviderURI())) {
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

    public UserData login(HttpServletRequest request) throws AuthenticationException, AuthenticationProcessingException {
        UUID expectedRequestID = (UUID) request.getSession(true).getAttribute(REQUEST_ID_SESSION_ATTR);
        if (expectedRequestID == null) {
            throw new AuthenticationProcessingException("Missing request ID attribute in session");
        }
        request.getSession().removeAttribute(REQUEST_ID_SESSION_ATTR);

        UserData userData = getUserData(request, expectedRequestID);
        if (userData == null) {
            throw new AuthenticationException("Unable to authenticate request " + request);
        }
        return userData;
    }
    
    public void redirectAfterLogin(HttpServletRequest request, HttpServletResponse response) throws AuthenticationProcessingException {
        // Get original request URL (saved in challenge()) from session, redirect to it
        URL url = (URL) request.getSession(true).getAttribute(URL_SESSION_ATTR);
        if (url == null) {
            throw new AuthenticationProcessingException("Missing URL attribute in session");
        }
        try {
            response.sendRedirect(url.toString());
        } catch (Exception e) {
            throw new AuthenticationProcessingException(e);
        }
    }
    
    UserData getUserData(HttpServletRequest request, UUID expectedRequestID) {
        String encodedSamlResponseString = request.getParameter("SAMLResponse");

        // Verify SAMLResponse and RelayState form inputs

        // TODO: verify that assertion has not been used before (replay)
        // TODO: check both timeouts

        Response samlResponse = decodeSamlResponse(encodedSamlResponseString);
        String inResponseToID = samlResponse.getInResponseTo();
        if (!expectedRequestID.toString().equals(inResponseToID)) {
            throw new RuntimeException("Request ID mismatch");
        }
        
        verifyStatusCodeIsSuccess(samlResponse);
        verifyDestinationAddressIsCorrect(samlResponse);
        
        Assertion assertion = samlResponse.getAssertions().get(0);
        verifyCryptographicAssertionSignature(assertion);
        validateAssertionContent(assertion);
        
        return new UserData(assertion);
        
    }
    
}

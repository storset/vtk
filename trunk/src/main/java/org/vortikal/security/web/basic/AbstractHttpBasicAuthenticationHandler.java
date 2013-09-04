/* Copyright (c) 2004, University of Oslo, Norway
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
package org.vortikal.security.web.basic;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.security.AuthenticationProcessingException;
import org.vortikal.security.Principal;
import org.vortikal.security.web.AbstractAuthenticationHandler;
import org.vortikal.security.web.AuthenticationChallenge;
import org.vortikal.util.codec.Base64;

/**
 * Abstract base class for performing HTTP/Basic
 * authentication. Subclasses normally only need to override the
 * {@link #authenticateInternal(Principal, String)} method.
 * 
 * <p>Configurable JavaBean properties:
 * <ul>
 *   <li><code>challenge</code> - an {@link HttpBasicAuthenticationChallenge authentication challenge} (required)
 *   <li><code>requireSecureConnection</code> - defaults to <code>true</code>
 * </ul>
 */
public abstract class AbstractHttpBasicAuthenticationHandler extends AbstractAuthenticationHandler {

    protected HttpBasicAuthenticationChallenge challenge;
    private boolean requireSecureConnection = true;
    
    @Required 
    public void setChallenge(HttpBasicAuthenticationChallenge challenge) {
        this.challenge = challenge;
    }
    
    @Override
    public boolean isRecognizedAuthenticationRequest(HttpServletRequest req)
    throws AuthenticationProcessingException {

        if (this.requireSecureConnection  && !req.isSecure()) {
            return false;
        }
        
        String authHeader = req.getHeader("Authorization");
                
        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            return false;
        }
        
        return super.isRecognizedAuthenticationRequest(req);
    }

    @Override
    public boolean postAuthentication(HttpServletRequest req, HttpServletResponse resp)
        throws AuthenticationProcessingException {
        return false;
    }
    
    @Override
    public boolean isLogoutSupported() {
        return false;
    }

    @Override
    public boolean logout(Principal principal, HttpServletRequest req,
                          HttpServletResponse resp)
        throws AuthenticationProcessingException {
        return false;
    }
    
    @Override
    public AuthenticationChallenge getAuthenticationChallenge() {
        return this.challenge;
    }
    
    @Override
    protected String getUserName(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            throw new IllegalArgumentException(
                "No valid 'Authorization' header in request");
        }

        String encodedString = authHeader.substring(
            "Basic ".length(), authHeader.length());
        String decodedString = Base64.decode(encodedString);
        int pos = decodedString.indexOf(":");

        if (pos == -1) {
            throw new IllegalArgumentException("Malformed 'Authorization' header");
        }

        String username = decodedString.substring(
            0, pos);
        return username;
    }


    @Override
    protected String getPassword(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            throw new IllegalArgumentException(
                "No valid 'Authorization' header in request");
        } 

        String encodedString = authHeader.substring(
            "Basic ".length(), authHeader.length());
        String decodedString = Base64.decode(encodedString);
        String password = decodedString.substring(
            decodedString.indexOf(":") + 1, decodedString.length());

        return password;
    }

    public void setRequireSecureConnection(boolean requireSecureConnection) {
        this.requireSecureConnection = requireSecureConnection;
    }
    
}

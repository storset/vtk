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
package org.vortikal.security.web;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.vortikal.security.AuthenticationException;
import org.vortikal.security.AuthenticationProcessingException;
import org.vortikal.security.Principal;



/**
 * Authentication request handler interface.  Instances of this
 * interface are invoked by the Vortex servlet in two stages:
 * <ol>
 *   <li>To decide whether the request is recognized as an
 *       authentication request
 *   <li>To perform the authentication (if the request was recognized)
 * </ol>
 *
 * In addition, implementors of this interface must provide a way of
 * logging out authenticated users, and an authentication challenge
 * that is used when authentication is required.
 * 
 * $Id: AuthenticationHandler.java 42 2004-05-05 19:16:53Z gormap $
 */
public interface AuthenticationHandler {



    /**
     * Determines whether a request is recognized by this
     * authentication handler. 
     * @param req the request in question
     * @return <code>true</code> if the request is a recognized
     * authentication request, <code>false</code> otherwise.
     */
    public boolean isRecognizedAuthenticationRequest(HttpServletRequest req)
        throws AuthenticationProcessingException;
    
    /**
     * Authenticates a request. 
     * @param req
     * @return principal if the request processing should proceed
     * @throws AuthenticationProcessingException if an underlying problem
     * prevented the request from being processed
     * @throws AuthenticationException if the request wasn't
     * authenticated
     */
    public Principal authenticate(HttpServletRequest req)
        throws AuthenticationProcessingException, AuthenticationException;


    /**
     * Called after a successful authentication. Allows
     * implementations to perform custom processing on the request
     * and/or response, such as redirecting the client.
     *
     * @param req the <code>HttpServletRequest</code>
     * @param resp the <code>HttpServletResponse</code>
     * @return <code>false</code> if the post processing did not
     * affect the request and the processing should proceed after this
     * method returns, or <code>true</code>, indicating that the
     * authentication handler has written the response (e.g. sent a
     * redirect, etc.)
     * @exception AuthenticationProcessingException if an error occurs
     */
    public boolean postAuthentication(HttpServletRequest req, HttpServletResponse resp)
        throws AuthenticationProcessingException;
    


    /**
     * Log out the client from the authentication system.
     *
     * @param principal a <code>Principal</code> value
     * @exception AuthenticationProcessingException if an error occurs
     */
    public void logout(Principal principal)
        throws AuthenticationProcessingException;

    /**
     * @return Returns the authenticationChallenge.
     */
    public AuthenticationChallenge getAuthenticationChallenge();

}

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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.vortikal.security.AuthenticationException;
import org.vortikal.security.AuthenticationProcessingException;
import org.vortikal.security.Principal;
import org.vortikal.security.TokenManager;
import org.vortikal.security.SecurityContext;



/**
 * Initializer for the {@link SecurityContext security context}. A
 * security context is created for every request. Also detects
 * authentication information in requests (using {@link
 * AuthenticationHandler authentication handlers}) and tries to
 * process them.
 *
 * <p>Configurable properties:
 * <ul>
 *   <li><code>authenticationHandlers</code> the list of {@link
 *       AuthenticationHandler authentication handlers} to use. These
 *       handlers are invoked in the same order they are provided.
 *   <li><code>tokenManager</code> the {@link TokenManager} which
 *       stores repository tokens for authenticated principals
 * </ul>
 */
public class SecurityInitializer {


    private static Log logger = LogFactory.getLog(SecurityContext.class);
    private TokenManager tokenManager;

    private AuthenticationHandler[] authenticationHandlers = null;
    

    public boolean createContext(HttpServletRequest req,
                                 HttpServletResponse resp) {

        String token = (String) req.getSession().getAttribute(
            SecurityContext.SECURITY_TOKEN_ATTRIBUTE);
        
        if (token != null) {
            SecurityContext.setSecurityContext(
                    new SecurityContext(token, tokenManager.getPrincipal(token)));
            return true;
        }
        
        AuthenticationHandler handler = null;
                
        for (int i = 0; i < this.authenticationHandlers.length; i++) {
            handler = this.authenticationHandlers[i];

            if (handler.isRecognizedAuthenticationRequest(req)) {
                if (logger.isDebugEnabled()) {
                    logger.debug(
                        "Request " + req + " is recognized as an authentication "
                        + "attempt by handler " + handler + ", will try to authenticate");
                }

                try {
                    Principal principal = handler.authenticate(req);
                    if (logger.isDebugEnabled()) {
                        logger.debug("Got principal: " + principal
                                     + " from authentication handler ");
                    }
                    
                    token = tokenManager.newToken(principal);
                    SecurityContext securityContext = 
                        new SecurityContext(token, tokenManager.getPrincipal(token));
                        
                    SecurityContext.setSecurityContext(securityContext);
                    req.getSession().setAttribute(SecurityContext.SECURITY_TOKEN_ATTRIBUTE,
                            token);

                    if (!handler.postAuthentication(req, resp)) {
                        return true;
                    }

                    return false;
                
                } catch (AuthenticationException exception) {

                    AuthenticationChallenge challenge = 
                        handler.getAuthenticationChallenge();

                    if (logger.isDebugEnabled()) {
                        logger.debug(
                            "Authentication attempt " + req + " rejected by " +
                            "handler " + handler + " with message "
                            + exception.getMessage() + ", presenting challenge "
                            + challenge + " to the client");
                    }
                    challenge.challenge(req, resp);
                    return false;
                }
            }
        }
        SecurityContext.setSecurityContext(new SecurityContext(null, null));
        return true;
    }


    /**
     * @see org.vortikal.web.ContextInitializer#destroyContext()
     */
    public void destroyContext() {
        if (logger.isDebugEnabled())
            logger.debug("Destroying security context: "
                         + SecurityContext.getSecurityContext());
        SecurityContext.setSecurityContext(null);
    }

    /**
     * @param tokenManager The tokenManager to set.
     */
    public void setTokenManager(
            TokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }


    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(getClass().getName());
        sb.append(": ").append(System.identityHashCode(this));
        return sb.toString();
    }

    public void setAuthenticationHandlers(AuthenticationHandler[] authenticationHandlers) {
        this.authenticationHandlers = authenticationHandlers;
    }
    
    /**
     * Logs out the client from the authentication system.
     * 
     * @param req the request
     * @param resp the response
     * @throws AuthenticationProcessingException if an underlying
     *  problem prevented the request from being processed
     */
    public void logout(HttpServletRequest req, HttpServletResponse resp)
            throws AuthenticationProcessingException {

        SecurityContext securityContext = SecurityContext.getSecurityContext();
        Principal principal = securityContext.getPrincipal();
        if (principal == null) return;
        tokenManager.removeToken(securityContext.getToken());
        SecurityContext.setSecurityContext(null);
        req.getSession().invalidate();

        for (int i = 0; i < this.authenticationHandlers.length; i++) {
            AuthenticationHandler handler = this.authenticationHandlers[i];

            handler.logout(principal);

        }

    }

}

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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.OrderComparator;
import org.vortikal.security.AuthenticationException;
import org.vortikal.security.AuthenticationProcessingException;
import org.vortikal.security.CookieLinkStore;
import org.vortikal.security.Principal;
import org.vortikal.security.SecurityContext;
import org.vortikal.security.token.TokenManager;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;

/**
 * Initializer for the {@link SecurityContext security context}. A security context is created for every request. Also
 * detects authentication information in requests (using {@link AuthenticationHandler authentication handlers}) and
 * tries to process them.
 * 
 * <p>
 * Configurable JavaBean properties:
 * <ul>
 * <li><code>authenticationHandlers</code> the list of {@link AuthenticationHandler authentication handlers} to use.
 * These handlers are invoked in the same order they are provided. If unspecified, the application context is searched
 * for authentication handlers.
 * <li><code>tokenManager</code> the {@link TokenManager} which stores repository tokens for authenticated principals
 * </ul>
 */
public class SecurityInitializer implements InitializingBean, ApplicationContextAware {

    private static final String SECURITY_TOKEN_SESSION_ATTR = SecurityInitializer.class.getName() + ".SECURITY_TOKEN";

    private static final String VRTXLINK_COOKIE = "VRTXLINK";

    private static final String VRTX_AUTH_SP_COOKIE = "VRTX_AUTH_SP";

    private static Log logger = LogFactory.getLog(SecurityInitializer.class);

    private static Log authLogger = LogFactory.getLog("org.vortikal.security.web.AuthLog");

    private TokenManager tokenManager;

    private List<AuthenticationHandler> authenticationHandlers;

    private Map<String, AuthenticationHandler> authHandlerMap;

    private ApplicationContext applicationContext;

    private CookieLinkStore cookieLinkStore;

    private String spCookieDomain = null;
    
    // Only relevant when using both https AND http and
    // different session cookie name for each protocol:
    private boolean cookieLinksEnabled = false;

    // Only relevant when using secure protocol:
    private boolean rememberAuthMethod = false;


    @SuppressWarnings("unchecked")
    public void afterPropertiesSet() {
        if (this.authenticationHandlers == null) {
            logger.info("No authentication handlers specified, looking in context");

            Map<?, AuthenticationHandler> matchingBeans = this.applicationContext.getBeansOfType(
                    AuthenticationHandler.class, false, false);

            List<AuthenticationHandler> handlers = new ArrayList<AuthenticationHandler>(matchingBeans.values());
            if (handlers.isEmpty()) {
                throw new IllegalStateException("At least one authentication handler must be specified, "
                        + "either explicitly or in application context");
            }

            Collections.sort(handlers, new OrderComparator());
            this.authenticationHandlers = handlers;

        }
        this.authHandlerMap = new HashMap<String, AuthenticationHandler>();
        for (AuthenticationHandler handler : this.authenticationHandlers) {
            this.authHandlerMap.put(handler.getIdentifier(), handler);
        }
        logger.info("Using authentication handlers: " + this.authenticationHandlers);
    }


    public boolean createContext(HttpServletRequest req, HttpServletResponse resp)
            throws AuthenticationProcessingException, ServletException, IOException {

        // HttpSession session = req.getSession(false);

        HttpSession session = getSession(req);
        String token = null;

        if (session != null) {
            token = (String) session.getAttribute(SECURITY_TOKEN_SESSION_ATTR);
        }

        if (token != null) {
            Principal principal = this.tokenManager.getPrincipal(token);
            if (principal == null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Invalid token '" + token + "' in request session, "
                            + "will proceed to check authentication");
                }
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("Found valid token '" + token + "', principal " + principal
                            + " in request session, setting security context");
                }
                SecurityContext.setSecurityContext(new SecurityContext(token, principal));
                return true;
            }
        }

        for (AuthenticationHandler handler : this.authenticationHandlers) {

            if (handler.isRecognizedAuthenticationRequest(req)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Request " + req + " is recognized as an authentication attempt by handler "
                            + handler + ", will try to authenticate");
                }

                try {
                    Principal principal = handler.authenticate(req);
                    if (logger.isDebugEnabled()) {
                        logger.debug("Successfully authenticated principal: " + principal
                                + " using authentication handler " + handler + ". " + "Setting security context.");
                    }
                    if (authLogger.isDebugEnabled()) {
                        authLogger.debug("Auth: principal: '" + principal + "' - method: '" + handler.getIdentifier()
                                + "' - status: OK");
                    }

                    token = this.tokenManager.newToken(principal, handler);
                    SecurityContext securityContext = new SecurityContext(token, this.tokenManager.getPrincipal(token));

                    SecurityContext.setSecurityContext(securityContext);
                    session = req.getSession(true);
                    session.setAttribute(SECURITY_TOKEN_SESSION_ATTR, token);

                    onSuccessfulAuthentication(req, resp, handler, token);

                    if (!handler.postAuthentication(req, resp)) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Authentication post-processing completed by authentication handler "
                                    + handler + ", request processing will proceed");
                        }
                        return true;
                    }

                    if (logger.isDebugEnabled()) {
                        logger.debug("Authentication post-processing completed by authentication handler " + handler
                                + ", response already committed.");
                    }
                    return false;

                } catch (AuthenticationException exception) {

                    AuthenticationChallenge challenge = handler.getAuthenticationChallenge();

                    if (logger.isDebugEnabled()) {
                        logger.debug("Authentication attempt " + req + " rejected by " + "handler " + handler
                                + " with message " + exception.getMessage() + ", presenting challenge " + challenge
                                + " to the client");
                    }
                    if (authLogger.isDebugEnabled()) {
                        authLogger.debug("Auth: request: '" + req.getRequestURI() + "' - method: '"
                                + handler.getIdentifier() + "' - status: FAIL");
                    }
                    challenge.challenge(req, resp);
                    return false;
                }
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Request " + req + " is not recognized as an authentication "
                    + "attempt by any authentication handler. Creating default " + "security context.");
        }

        SecurityContext.setSecurityContext(new SecurityContext(null, null));
        return true;
    }


    
    public void challenge(HttpServletRequest request, HttpServletResponse response, 
            AuthenticationException ex) throws AuthenticationProcessingException {
        Service service = RequestContext.getRequestContext()
                .getService();
        AuthenticationChallenge challenge = getAuthenticationChallenge(request, service);

        if (logger.isDebugEnabled()) {
            logger.debug("Authentication required for request "
                         + request + ", service " + service + ". "
                         + "Using challenge " + challenge, ex);
        }
        if (challenge == null) {
            throw new IllegalStateException(
                "Authentication challenge for service " + service
                + " (or any of its ancestors) is not specified.");
        }
        try {
            challenge.challenge(request, response);
        } catch (Exception e) {
            throw new AuthenticationProcessingException(
                    "Unable to present authentication challenge " + challenge, e);
        }
    }
    
    /**
     * Removes authentication state from the authentication system. The {@link SecurityContext} is cleared, the current
     * principal is removed from the {@link TokenManager}, but the {@link AuthenticationHandler#logout logout} process
     * is not initiated.
     * 
     * @return <code>true</code> if any state was removed, <code>false</code> otherwise
     */
    public boolean removeAuthState() {
        if (!SecurityContext.exists()) {
            return false;
        }
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        Principal principal = securityContext.getPrincipal();
        if (principal == null) {
            return false;
        }
        this.tokenManager.removeToken(securityContext.getToken());
        SecurityContext.setSecurityContext(null);
        if (authLogger.isDebugEnabled()) {
            authLogger.debug("Logout: principal: '" + principal + "' - method: '<none>' - status: OK");
        }
        return true;
    }


    /**
     * Logs out the client from the authentication system. Clears the {@link SecurityContext} and removes the principal
     * from the {@link TokenManager}. Finally, calls the authentication handler's {@link AuthenticationHandler#logout
     * logout} method.
     * 
     * @param req
     *            the request
     * @param resp
     *            the response
     * @return the return value of the authentication handler's <code>logout()</code> method.
     * @throws AuthenticationProcessingException
     *             if an underlying problem prevented the request from being processed
     * @throws IOException
     * @throws ServletException
     * @see AuthenticationHandler#logout
     */
    public boolean logout(HttpServletRequest req, HttpServletResponse resp) throws AuthenticationProcessingException,
            ServletException, IOException {

        if (!SecurityContext.exists()) {
            return false;
        }
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        Principal principal = securityContext.getPrincipal();
        if (principal == null) {
            return false;
        }
        AuthenticationHandler handler = this.tokenManager.getAuthenticationHandler(securityContext.getToken());

        // FIXME: what if handler.isLogoutSupported() == false?
        boolean result = handler.logout(principal, req, resp);
        String status = result ? "OK" : "FAIL";
        if (authLogger.isDebugEnabled()) {
            authLogger.debug("Logout: principal: '" + principal + "' - method: '" + handler.getIdentifier()
                    + "' - status: " + status);
        }

        this.tokenManager.removeToken(securityContext.getToken());
        SecurityContext.setSecurityContext(null);

        return result;
    }

    /**
     * @see org.vortikal.web.ContextInitializer#destroyContext()
     */
    public void destroyContext() {
        if (logger.isDebugEnabled())
            logger.debug("Destroying security context: " + SecurityContext.getSecurityContext());
        SecurityContext.setSecurityContext(null);
    }



    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(getClass().getName());
        sb.append(": ").append(System.identityHashCode(this));
        sb.append(", authenticationHandlers: [");
        sb.append(this.authenticationHandlers);
        sb.append("]");
        return sb.toString();
    }


    public void setTokenManager(TokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }


    public void setAuthenticationHandlers(List<AuthenticationHandler> authenticationHandlers) {
        this.authenticationHandlers = authenticationHandlers;
    }


    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }


    public void setCookieLinkStore(CookieLinkStore cookieLinkStore) {
        this.cookieLinkStore = cookieLinkStore;
    }


    public void setCookieLinksEnabled(boolean cookieLinksEnabled) {
        this.cookieLinksEnabled = cookieLinksEnabled;
    }


    public void setRememberAuthMethod(boolean rememberAuthMethod) {
        this.rememberAuthMethod = rememberAuthMethod;
    }


    public void setSpCookieDomain(String spCookieDomain) {
        if (spCookieDomain != null && !"".equals(spCookieDomain.trim())) {
            this.spCookieDomain = spCookieDomain;
        }
    }

    private static Cookie getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie;
            }
        }
        return null;
    }

    private HttpSession getSession(HttpServletRequest request) {
        if (!this.cookieLinksEnabled) {
            return request.getSession(false);
        }
        HttpSession session = request.getSession(false);
        if (session == null && request.getCookies() != null && !request.isSecure()) {
            Cookie c = getCookie(request, VRTXLINK_COOKIE);
            if (c != null) {
                UUID id;
                try {
                    id = UUID.fromString(c.getValue());
                } catch (Throwable t) {
                    return null;
                }
                String token = this.cookieLinkStore.getToken(request, id);
                if (token != null) {
                    session = request.getSession(true);
                    session.setAttribute(SECURITY_TOKEN_SESSION_ATTR, token);
                }
            }
        }
        return session;
    }


    private void onSuccessfulAuthentication(HttpServletRequest req, HttpServletResponse resp,
            AuthenticationHandler handler, String token) {
        if (!req.isSecure()) {
            return;
        }
        if (this.rememberAuthMethod) {
            Cookie c = new Cookie(VRTX_AUTH_SP_COOKIE, handler.getIdentifier());
            c.setSecure(true);
            c.setPath("/");
            if (this.spCookieDomain != null) {
                c.setDomain(this.spCookieDomain);
            }
            resp.addCookie(c);
        }
        if (this.cookieLinksEnabled) {
            UUID cookieLinkID = this.cookieLinkStore.addToken(req, token);
            Cookie c = new Cookie(VRTXLINK_COOKIE, cookieLinkID.toString());
            c.setPath("/");
            resp.addCookie(c);
        }
    }
    

    private AuthenticationChallenge getAuthenticationChallenge(HttpServletRequest request, Service service) {
        if (this.rememberAuthMethod) {
            Cookie c = getCookie(request, VRTXLINK_COOKIE);
            if (c != null) {
                String id = c.getValue();
                AuthenticationHandler handler = this.authHandlerMap.get(id);
                if (handler != null) {
                    AuthenticationChallenge challenge = handler.getAuthenticationChallenge();
                    if (challenge != null) {
                        return challenge;
                    }
                }
            }
        }
        AuthenticationChallenge challenge = service.getAuthenticationChallenge();
        
        if (challenge == null && service.getParent() != null) { 
            return getAuthenticationChallenge(request, service.getParent());
        }
        return challenge;
    }
}

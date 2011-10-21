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
import java.util.Set;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.OrderComparator;
import org.vortikal.security.AuthenticationException;
import org.vortikal.security.AuthenticationProcessingException;
import org.vortikal.security.CookieLinkStore;
import org.vortikal.security.Principal;
import org.vortikal.security.PrincipalFactory;
import org.vortikal.security.PrincipalManager;
import org.vortikal.security.SecurityContext;
import org.vortikal.security.token.TokenManager;
import org.vortikal.security.web.AuthenticationHandler.AuthResult;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Assertion;
import org.vortikal.web.service.Service;
import org.vortikal.web.service.URL;

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

    private static final String UIO_AUTH_SSO = "UIO_AUTH_SSO";

    private static final String UIO_AUTH_IDP = "UIO_AUTH_IDP";

    private static final String VRTXID = "VRTXID";

    private static final String VRTXSSLID = "VRTXSSLID";

    private static final String AUTH_HANDLER_SP_COOKIE_CATEGORY = "spCookie";

    private static Log logger = LogFactory.getLog(SecurityInitializer.class);

    private static Log authLogger = LogFactory.getLog("org.vortikal.security.web.AuthLog");

    private TokenManager tokenManager;

    private PrincipalManager principalManager;
    private PrincipalFactory principalFactory;

    private List<AuthenticationHandler> authenticationHandlers;

    private Map<String, AuthenticationHandler> authHandlerMap;

    private ApplicationContext applicationContext;

    private CookieLinkStore cookieLinkStore;

    private String spCookieDomain = null;

    private String serviceProviderURI;

    // Only relevant when using both https AND http and
    // different session cookie name for each protocol:
    private boolean cookieLinksEnabled = false;

    // Only relevant when using secure protocol:
    private boolean rememberAuthMethod = false;

    // Assertion that must match in order to use authentication challenge from cookie:
    private Assertion spCookieAssertion;

    @SuppressWarnings("unchecked")
    @Override
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

    /**
     * 
     * @param req
     * @param resp
     * @return <code>true</code> if request processing should continue after context has been created,
     *         <code>false</code> otherwise (which means that security context initialization handles a challenge or any
     *         authentication post-processing requests by itself).
     * 
     * @throws AuthenticationProcessingException
     * @throws ServletException
     * @throws IOException
     */
    public boolean createContext(HttpServletRequest req, HttpServletResponse resp)
            throws AuthenticationProcessingException, ServletException, IOException {

        /**
         * HttpSession session = getSession(req); String token = null;
         * 
         * if (session != null) { token = (String) session.getAttribute(SECURITY_TOKEN_SESSION_ATTR); }
         */

        String token = getToken(req, resp);
        if (token != null) {

            Principal principal = this.tokenManager.getPrincipal(token);
            if (principal != null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Found valid token '" + token + "', principal " + principal
                            + " in request session, setting security context");
                }
                SecurityContext.setSecurityContext(new SecurityContext(token, principal));

                if (getCookie(req, VRTXLINK_COOKIE) == null && this.cookieLinksEnabled) {
                    UUID cookieLinkID = this.cookieLinkStore.addToken(req, token);
                    Cookie c = new Cookie(VRTXLINK_COOKIE, cookieLinkID.toString());
                    c.setPath("/");
                    resp.addCookie(c);
                    if (logger.isDebugEnabled()) {
                        logger.debug("Setting cookie: " + VRTXLINK_COOKIE + ": " + cookieLinkID.toString());
                    }
                }
                return true;
            }
            if (logger.isDebugEnabled()) {
                logger.debug("Invalid token '" + token + "' in request session, "
                        + "will proceed to check authentication");
            }
        } else if (getCookie(req, UIO_AUTH_SSO) != null && getCookie(req, VRTXLINK_COOKIE) == null
                && req.getParameter("authTarget") == null && !req.getRequestURI().contains(serviceProviderURI)) {

            StringBuffer url = req.getRequestURL();
            Boolean whiteWord = false;
            String[] wordWhiteList = { "/", "html", "htm", "xml", "php" };

            for (String word : wordWhiteList) {
                if (url.toString().endsWith(word)) {
                    whiteWord = true;
                }
            }

            if (whiteWord) {
                String queryString = req.getQueryString();
                if (queryString != null) {
                    url = url.append("?");
                    url = url.append(queryString);
                }
                URL currentURL = URL.parse(url.toString());
                currentURL.addParameter("authTarget", req.getScheme());
                resp.sendRedirect(currentURL.toString());
            }
        }

        for (AuthenticationHandler handler : this.authenticationHandlers) {

            if (handler.isRecognizedAuthenticationRequest(req)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Request " + req + " is recognized as an authentication attempt by handler " + handler
                            + ", will try to authenticate");
                }

                try {
                    AuthResult result = handler.authenticate(req);
                    if (result == null) {
                        throw new IllegalStateException("Principal handler returned NULL AuthResult: " + handler
                                + " for request " + req);
                    }
                    Principal principal = this.principalFactory.getPrincipal(result.getUID(), Principal.Type.USER);
                    boolean valid = this.principalManager.validatePrincipal(principal);
                    if (!valid) {
                        logger.warn("Unknown principal: " + principal + " returned by authentication handler "
                                + handler + ". " + "Not setting security context.");

                        throw new IllegalStateException("Invalid principal: " + principal);
                    }

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
                    HttpSession session = req.getSession(true);
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
                    doChallenge(req, resp, challenge);
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

    public void challenge(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
            throws AuthenticationProcessingException {
        Service service = RequestContext.getRequestContext().getService();
        AuthenticationChallenge challenge = getAuthenticationChallenge(request, service);

        if (logger.isDebugEnabled()) {
            logger.debug("Authentication required for request " + request + ", service " + service + ". "
                    + "Using challenge " + challenge, ex);
        }
        if (challenge == null) {
            throw new IllegalStateException("Authentication challenge for service " + service
                    + " (or any of its ancestors) is not specified.");
        }
        doChallenge(request, response, challenge);
    }

    /**
     * Removes authentication state from the authentication system. The {@link SecurityContext} is cleared, the current
     * principal is removed from the {@link TokenManager}, but the {@link AuthenticationHandler#logout logout} process
     * is not initiated.
     * 
     * @return <code>true</code> if any state was removed, <code>false</code> otherwise
     */
    public boolean removeAuthState(HttpServletRequest request, HttpServletResponse response) {
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
        if (this.rememberAuthMethod) {
            List<String> spCookies = new ArrayList<String>();
            spCookies.add(VRTX_AUTH_SP_COOKIE);
            spCookies.add(UIO_AUTH_IDP);

            for (String cookie : spCookies) {
                Cookie c = getCookie(request, cookie);
                if (c != null) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Deleting cookie " + cookie);
                    }
                    c = new Cookie(cookie, c.getValue());
                    c.setSecure(true);
                    c.setPath("/");
                    if (this.spCookieDomain != null) {
                        c.setDomain(this.spCookieDomain);
                    }
                    c.setMaxAge(0);
                    response.addCookie(c);
                }
            }
        }
        return true;
    }

    /**
     * Logs out the client from the authentication system. Clears the {@link SecurityContext} and removes the principal
     * from the {@link TokenManager}. Finally, calls the authentication handler's {@link AuthenticationHandler#logout
     * logout} method.
     * 
     * @param request
     *            the request
     * @param response
     *            the response
     * @return the return value of the authentication handler's <code>logout()</code> method.
     * @throws AuthenticationProcessingException
     *             if an underlying problem prevented the request from being processed
     * @throws IOException
     * @throws ServletException
     * @see AuthenticationHandler#logout
     */
    public boolean logout(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationProcessingException, ServletException, IOException {

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
        boolean result = handler.logout(principal, request, response);
        String status = result ? "OK" : "FAIL";
        if (authLogger.isDebugEnabled()) {
            authLogger.debug("Logout: principal: '" + principal + "' - method: '" + handler.getIdentifier()
                    + "' - status: " + status);
        }

        this.tokenManager.removeToken(securityContext.getToken());
        SecurityContext.setSecurityContext(null);

        if (this.rememberAuthMethod) {
            List<String> spCookies = new ArrayList<String>();
            spCookies.add(VRTX_AUTH_SP_COOKIE);
            spCookies.add(UIO_AUTH_SSO);
            spCookies.add(UIO_AUTH_IDP);

            for (String cookie : spCookies) {
                Cookie c = getCookie(request, cookie);
                if (c != null) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Deleting cookie " + cookie);
                    }
                    c = new Cookie(cookie, c.getValue());
                    if (!cookie.equals(UIO_AUTH_SSO) || !cookie.equals(VRTXID) || !cookie.equals(VRTXLINK_COOKIE)) {
                        c.setSecure(true);
                    }
                    c.setPath("/");
                    if (this.spCookieDomain != null) {
                        c.setDomain(this.spCookieDomain);
                    }
                    c.setMaxAge(0);
                    response.addCookie(c);
                }
            }
        }
        return result;
    }

    /**
     * @see org.vortikal.web.ContextInitializer#destroyContext()
     */
    public void destroyContext() {
        if (logger.isDebugEnabled()) {
            logger.debug("Destroying security context: " + SecurityContext.getSecurityContext());
        }
        SecurityContext.setSecurityContext(null);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getName());
        sb.append(": ").append(System.identityHashCode(this));
        sb.append(", authenticationHandlers: [");
        sb.append(this.authenticationHandlers);
        sb.append("]");
        return sb.toString();
    }

    @Required
    public void setTokenManager(TokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }

    @Required
    public void setPrincipalManager(PrincipalManager principalManager) {
        this.principalManager = principalManager;
    }

    @Required
    public void setPrincipalFactory(PrincipalFactory principalFactory) {
        this.principalFactory = principalFactory;
    }

    public void setAuthenticationHandlers(List<AuthenticationHandler> authenticationHandlers) {
        this.authenticationHandlers = authenticationHandlers;
    }

    @Override
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

    public void setServiceProviderURI(String serviceProviderURI) {
        this.serviceProviderURI = serviceProviderURI;
    }

    public void setSpCookieAssertion(Assertion spCookieAssertion) {
        this.spCookieAssertion = spCookieAssertion;
    }

    private String getToken(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (!this.cookieLinksEnabled) {
            if (session == null) {
                return null;
            }
            return (String) session.getAttribute(SECURITY_TOKEN_SESSION_ATTR);
        }
        if (session != null && session.getAttribute(SECURITY_TOKEN_SESSION_ATTR) != null) {
            Principal principal = this.tokenManager.getPrincipal(session.getAttribute(SECURITY_TOKEN_SESSION_ATTR)
                    .toString());
            if (principal != null) {
                return (String) session.getAttribute(SECURITY_TOKEN_SESSION_ATTR);
            }
        }
        if (request.getCookies() != null && !request.isSecure()) {
            Cookie c = getCookie(request, VRTXLINK_COOKIE);
            if (logger.isDebugEnabled()) {
                logger.debug("Cookie: " + VRTXLINK_COOKIE + ": " + c);
            }
            if (c != null) {
                UUID id;
                try {
                    id = UUID.fromString(c.getValue());
                } catch (Throwable t) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Invalid UUID cookie value: " + c.getValue(), t);
                    }
                    return null;
                }
                String token = this.cookieLinkStore.getToken(request, id);
                if (token == null) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("No token found from cookie " + VRTXLINK_COOKIE + ", deleting cookie");
                    }
                    c = new Cookie(VRTXLINK_COOKIE, c.getValue());
                    c.setPath("/");
                    c.setMaxAge(0);
                    response.addCookie(c);
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Found token " + token + " from cookie " + VRTXLINK_COOKIE);
                    }
                    session = request.getSession(true);
                    session.setAttribute(SECURITY_TOKEN_SESSION_ATTR, token);
                    return token;
                }
            }
        }
        return null;

    }

    private void onSuccessfulAuthentication(HttpServletRequest req, HttpServletResponse resp,
            AuthenticationHandler handler, String token) {

        if (!req.isSecure()) {
            return;
        }
        Set<?> categories = handler.getCategories();
        if (categories == null)
            categories = Collections.EMPTY_SET;
        if (this.rememberAuthMethod && categories.contains(AUTH_HANDLER_SP_COOKIE_CATEGORY)) {
            List<String> spCookies = new ArrayList<String>();
            spCookies.add(VRTX_AUTH_SP_COOKIE);
            spCookies.add(UIO_AUTH_IDP);
            spCookies.add(UIO_AUTH_SSO);

            for (String cookie : spCookies) {
                Cookie c = new Cookie(cookie, handler.getIdentifier());
                if (!cookie.equals(UIO_AUTH_SSO)) {
                    c.setSecure(true);
                }
                c.setPath("/");

                if (this.spCookieDomain != null) {
                    c.setDomain(this.spCookieDomain);
                }

                resp.addCookie(c);
                if (logger.isDebugEnabled()) {
                    logger.debug("Setting cookie: " + cookie + ": " + handler.getIdentifier());
                }
            }

        }
        if (this.cookieLinksEnabled) {
            UUID cookieLinkID = this.cookieLinkStore.addToken(req, token);
            Cookie c = new Cookie(VRTXLINK_COOKIE, cookieLinkID.toString());
            c.setPath("/");
            resp.addCookie(c);
            if (logger.isDebugEnabled()) {
                logger.debug("Setting cookie: " + VRTXLINK_COOKIE + ": " + cookieLinkID.toString());
            }
        }
    }

    private void doChallenge(HttpServletRequest request, HttpServletResponse response, AuthenticationChallenge challenge) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object token = session.getAttribute(SECURITY_TOKEN_SESSION_ATTR);
            if (token != null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Removing invalid token '" + token + "' from session");
                }
                session.removeAttribute(SECURITY_TOKEN_SESSION_ATTR);
            }
        }
        try {
            challenge.challenge(request, response);

        } catch (AuthenticationProcessingException ape) {
            // VTK-1896
            // Avoid wrapping APE in another APE, otherwise we get banana dance.
            throw ape;
        } catch (Exception e) {
            throw new AuthenticationProcessingException("Unable to present authentication challenge " + challenge, e);
        }
    }

    private AuthenticationChallenge getAuthenticationChallenge(HttpServletRequest request, Service service) {
        AuthenticationChallenge challenge = null;
        if (this.rememberAuthMethod) {
            Cookie c = getCookie(request, VRTX_AUTH_SP_COOKIE);
            if (c != null) {
                String id = c.getValue();
                AuthenticationHandler handler = this.authHandlerMap.get(id);
                if (handler != null) {
                    Set<?> categories = handler.getCategories();
                    if (categories == null) {
                        categories = Collections.EMPTY_SET;
                    }
                    if (handler != null && categories.contains(AUTH_HANDLER_SP_COOKIE_CATEGORY)) {
                        challenge = handler.getAuthenticationChallenge();
                    }
                }
            }
        }
        if (challenge != null) {
            if (this.spCookieAssertion != null) {
                boolean match = this.spCookieAssertion.matches(request, null, null);
                if (!match) {
                    challenge = null;
                }
            }
        }

        if (challenge != null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Using challenge from cookie " + VRTX_AUTH_SP_COOKIE + ": " + challenge);
            }
            return challenge;
        }

        challenge = service.getAuthenticationChallenge();

        if (challenge == null && service.getParent() != null) {
            return getAuthenticationChallenge(request, service.getParent());
        }
        return challenge;
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

}

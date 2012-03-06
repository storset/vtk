package org.vortikal.web.service;

import java.util.Date;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;
import org.vortikal.security.web.SecurityInitializer;

/**
 * Assertion that checks for UIO_AUTH_SSO cookie and if several other conditions are met, appends the authTarget
 * parameter and does a redirect
 * 
 * <p>
 * Configurable properties:
 * <ul>
 * <li><code>serviceProviderURI</code> - the endpoint for the SP
 * <li><code>wordWhitelist</code> - lift of resources that should be SSO redirected.
 * <li><code>ssoTimeout</code> - timeout to do the redirect
 * </ul>
 * 
 */

public class SSOCookieAssertion implements Assertion {

    private static final String UIO_AUTH_SSO = "UIO_AUTH_SSO";
    private String serviceProviderURI;
    private String[] wordWhitelist;
    private Long ssoTimeout;

    @Override
    public boolean conflicts(Assertion assertion) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean matches(HttpServletRequest request, Resource resource, Principal principal) {
        Boolean doRedirect = false;
        if (getCookie(request, UIO_AUTH_SSO) != null && getCookie(request, SecurityInitializer.VRTXLINK_COOKIE) == null
                && request.getParameter("authTarget") == null && !request.getRequestURI().contains(serviceProviderURI)) {

            StringBuffer url = request.getRequestURL();

            for (String word : wordWhitelist) {
                if (url.toString().endsWith(word.trim())) {
                    doRedirect = true;
                }
            }

            Long cookieTimestamp = new Long(0);
            try {
                cookieTimestamp = Long.valueOf(getCookie(request, UIO_AUTH_SSO).getValue());
            } catch (NumberFormatException e) {

            }
            Long currentTime = new Date().getTime();

            if (currentTime - cookieTimestamp > ssoTimeout) {
                doRedirect = false;
            }

        }
        return doRedirect;
    }

    @Override
    public boolean processURL(URL url, Resource resource, Principal principal, boolean match) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void processURL(URL url) {
        // TODO Auto-generated method stub

    }

    public void setServiceProviderURI(String serviceProviderURI) {
        this.serviceProviderURI = serviceProviderURI;
    }

    public void setWordWhitelist(String[] wordWhitelist) {
        this.wordWhitelist = wordWhitelist;
    }

    public void setSsoTimeout(Long ssoTimeout) {
        this.ssoTimeout = ssoTimeout;
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
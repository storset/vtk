package org.vortikal.web.service;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;
import org.vortikal.security.web.saml.SamlAuthenticationHandler;

public class IERemoveCookieAssertion implements Assertion {

    private String ieCookieLogoutTicket;
    private String uioAuthSSO;

    @Override
    public boolean conflicts(Assertion assertion) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean matches(HttpServletRequest request, Resource resource, Principal principal) {

        if (getCookie(request, uioAuthSSO) != null && request.getParameter(ieCookieLogoutTicket) != null
                && SamlAuthenticationHandler.browserIsIE(request)) {
            return true;
        } else {
            return false;
        }
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

    public void setIeCookieLogoutTicket(String ieCookieLogoutTicket) {
        this.ieCookieLogoutTicket = ieCookieLogoutTicket;
    }

    public void setUioAuthSSO(String uioAuthSSO) {
        this.uioAuthSSO = uioAuthSSO;
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

package org.vortikal.web.service;

import javax.servlet.http.HttpServletRequest;

import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;
import org.vortikal.security.web.saml.SamlAuthenticationHandler;

public class IERemoveCookieAssertion implements Assertion {

    private String ieCookieLogoutTicket;

    @Override
    public boolean conflicts(Assertion assertion) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean matches(HttpServletRequest request, Resource resource, Principal principal) {

        if (request.getParameter(ieCookieLogoutTicket) != null && SamlAuthenticationHandler.browserIsIE(request)) {
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
}

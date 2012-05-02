package org.vortikal.security.web.saml;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.vortikal.web.service.URL;

public class IERemoveCookieController implements Controller {

    private String spCookieDomain = null;
    private String ieCookieLogoutTicket;
    private IECookieStore iECookieStore;

    private static Log authLogger = LogFactory.getLog("org.vortikal.security.web.AuthLog");

    private String uioAuthSSO;

    @Override
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        URL currentURL = URL.create(request);

        String cookieTicket = request.getParameter(ieCookieLogoutTicket);
        List<String> cookiesToDelete = new ArrayList<String>();

        if (SamlAuthenticationHandler.getCookie(request, uioAuthSSO) != null) {
            cookiesToDelete.add(uioAuthSSO);
        }

        if (iECookieStore.getToken(request, UUID.fromString(cookieTicket)) != null) {
            for (String key : cookiesToDelete) {
                if (authLogger.isDebugEnabled()) {
                    authLogger.debug("DELETING cookie: " + key);
                }
                Cookie c = new Cookie(key, key);
                c.setPath("/");
                if (this.spCookieDomain != null) {
                    c.setDomain(this.spCookieDomain);
                }
                c.setMaxAge(0);
                response.addCookie(c);
            }
            iECookieStore.dropToken(request, UUID.fromString(cookieTicket));

        }
        currentURL.removeParameter(ieCookieLogoutTicket);
        response.sendRedirect(currentURL.toString());

        return null;
    }

    public void setSpCookieDomain(String spCookieDomain) {
        if (spCookieDomain != null && !"".equals(spCookieDomain.trim())) {
            this.spCookieDomain = spCookieDomain;
        }
    }

    public void setIeCookieLogoutTicket(String ieCookieLogoutTicket) {
        this.ieCookieLogoutTicket = ieCookieLogoutTicket;
    }

    public void setiECookieStore(IECookieStore iECookieStore) {
        this.iECookieStore = iECookieStore;
    }

    public void setUioAuthSSO(String uioAuthSSO) {
        this.uioAuthSSO = uioAuthSSO;
    }
}
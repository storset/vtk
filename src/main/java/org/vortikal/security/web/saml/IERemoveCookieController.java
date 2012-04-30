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
    public static final String VRTXLINK_COOKIE = "VRTXLINK";

    @Override
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        URL currentURL = URL.create(request);

        String cookieTicket = request.getParameter(ieCookieLogoutTicket);
        List<String> cookiesToDelete = new ArrayList<String>();

        cookiesToDelete.add(uioAuthSSO);

        if (SamlAuthenticationHandler.getCookie(request, VRTXLINK_COOKIE) != null) {
            authLogger.debug("IE Cookie remover, found " + VRTXLINK_COOKIE);
            cookiesToDelete.add(VRTXLINK_COOKIE);
        }

        if (iECookieStore.getToken(request, UUID.fromString(cookieTicket)) != null) {
            for (String key : cookiesToDelete) {
                authLogger.debug("DELETING cookie: " + key);
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

        // HttpSession session = request.getSession(false);
        // if (session != null) {
        // session.invalidate();
        // }

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
package org.vortikal.security.web.saml;

import java.net.URLDecoder;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

public class IECookieRedirectController implements Controller {

    private String ieCookieTicket;

    private String vrtxAuthSP;
    private String uioAuthIDP;
    private String uioAuthSSO;
    private String ieReturnURL;

    private IECookieStore iECookieStore;

    private String spCookieDomain = null;

    @Override
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {

        String returnURL = URLDecoder.decode(request.getParameter(ieReturnURL), "UTF-8");

        if (request.getParameter("vrtxPreviewForceRefresh") == null) {
            String cookieTicket = request.getParameter(ieCookieTicket);

            Map<String, String> cookieMap = iECookieStore.getToken(request, UUID.fromString(cookieTicket));
            if (cookieMap != null) {
                String spCookie = cookieMap.get(vrtxAuthSP);
                String idpCookie = cookieMap.get(uioAuthIDP);
                String ssoCookie = cookieMap.get(uioAuthSSO);
                returnURL = cookieMap.get(ieReturnURL);

                if (spCookie != null) {
                    Cookie c = new Cookie(vrtxAuthSP, cookieMap.get(vrtxAuthSP));
                    System.out.println("SETTING COOKIE: " + c.getName() + ":" + c.getValue());
                    c.setSecure(true);
                    c.setPath("/");
                    if (this.spCookieDomain != null) {
                        c.setDomain(this.spCookieDomain);
                    }
                    response.addCookie(c);
                }

                if (idpCookie != null) {
                    Cookie c = new Cookie(uioAuthIDP, cookieMap.get(uioAuthIDP));
                    System.out.println("SETTING COOKIE: " + c.getName() + ":" + c.getValue());
                    c.setSecure(true);
                    c.setPath("/");
                    if (this.spCookieDomain != null) {
                        c.setDomain(this.spCookieDomain);
                    }
                    response.addCookie(c);
                }

                if (ssoCookie != null) {
                    Cookie c = new Cookie(uioAuthSSO, cookieMap.get(uioAuthSSO));
                    System.out.println("SETTING COOKIE: " + c.getName() + ":" + c.getValue());
                    c.setPath("/");
                    if (this.spCookieDomain != null) {
                        c.setDomain(this.spCookieDomain);
                    }
                    response.addCookie(c);
                }
                iECookieStore.dropToken(request, UUID.fromString(cookieTicket));
            }
        }

        response.sendRedirect(returnURL);
        return null;
    }

    public void setIeCookieTicket(String ieCookieTicket) {
        this.ieCookieTicket = ieCookieTicket;
    }

    public void setVrtxAuthSP(String vrtxAuthSP) {
        this.vrtxAuthSP = vrtxAuthSP;
    }

    public void setUioAuthIDP(String uioAuthIDP) {
        this.uioAuthIDP = uioAuthIDP;
    }

    public void setUioAuthSSO(String uioAuthSSO) {
        this.uioAuthSSO = uioAuthSSO;
    }

    public void setIeReturnURL(String ieReturnURL) {
        this.ieReturnURL = ieReturnURL;
    }

    public void setiECookieStore(IECookieStore iECookieStore) {
        this.iECookieStore = iECookieStore;
    }

    public void setSpCookieDomain(String spCookieDomain) {
        if (spCookieDomain != null && !"".equals(spCookieDomain.trim())) {
            this.spCookieDomain = spCookieDomain;
        }
    }
}
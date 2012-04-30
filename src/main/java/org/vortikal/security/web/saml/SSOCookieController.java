package org.vortikal.security.web.saml;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.vortikal.web.service.URL;

public class SSOCookieController implements Controller {

    private static Log authLogger = LogFactory.getLog("org.vortikal.security.web.AuthLog");

    @Override
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        StringBuffer url = request.getRequestURL();
        String queryString = request.getQueryString();
        if (queryString != null) {
            url = url.append("?");
            url = url.append(queryString);
        }
        URL currentURL = URL.parse(url.toString());
        currentURL.addParameter("authTarget", request.getScheme());

        authLogger.debug("SSO-cookie found, redirecting to: " + currentURL.toString());

        response.sendRedirect(currentURL.toString());

        return null;
    }
}

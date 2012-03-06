package org.vortikal.security.web.saml;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.vortikal.web.service.URL;

public class SSOCookieController implements Controller {

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
        response.sendRedirect(currentURL.toString());

        return null;
    }
}

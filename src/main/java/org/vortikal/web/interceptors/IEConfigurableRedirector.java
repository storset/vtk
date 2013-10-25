package org.vortikal.web.interceptors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.vortikal.web.service.URL;

/**
 * Extends the standard ConfigurableRedirector class with handling of the backsteps parameter.
 * 
 * This parameter is used to facilitate correct handling of the back button in IE10.
 * 
 * The parameter is sent from the IDP and handled by javascript to skip to the correct back position.
 */

public class IEConfigurableRedirector extends ConfigurableRedirector {

    private String backstepParameter = "backsteps";

    @Override
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        redirect(request, response);
        return null;
    }

    private void redirect(HttpServletRequest request, HttpServletResponse response) throws Exception {
        URL url = URL.create(request);
        if (this.protocol != null) {
            url.setProtocol(this.protocol);
        }
        if (this.redirectToHostName != null) {

            url.setHost(this.redirectToHostName);
        }
        if (this.port != null) {
            try {
                Integer portInt = Integer.parseInt(this.port);
                url.setPort(portInt);
            } catch (NumberFormatException nfe) {
                // port might be "*", in which case we ignore it
            }
        }
        if (this.addedParameters != null) {
            for (String param : this.addedParameters.keySet()) {
                url.addParameter(param, this.addedParameters.get(param));
            }
        }
        if (this.replacedParameters != null) {
            for (String param : this.replacedParameters.keySet()) {
                url.setParameter(param, this.replacedParameters.get(param));
            }
        }
        if (this.removedParameters != null) {
            for (String param : this.removedParameters) {
                url.removeParameter(param);
            }
        }

        String backstepFragment = "";
        if (url.getParameter(backstepParameter) != null) {
            String backstepValue = url.getParameter(backstepParameter);
            url.removeParameter(backstepParameter);
            backstepFragment = "#" + backstepParameter + "=" + backstepValue;
        }
        if (!backstepFragment.equals("")) {
            response.sendRedirect(url.toString() + backstepFragment);
        } else {
            response.sendRedirect(url.toString());
        }
    }
}

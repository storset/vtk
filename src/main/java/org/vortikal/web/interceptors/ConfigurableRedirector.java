/* Copyright (c) 2010, University of Oslo, Norway
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
package org.vortikal.web.interceptors;

import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.vortikal.web.service.URL;

/**
 * Intercepter that redirects requests to the same URL but with a 
 * configurable protocol, hostname and port 
 */
public class ConfigurableRedirector implements HandlerInterceptor, Controller  {
    
    private String protocol;
    private String redirectToHostName;
    private String port;
    private Map<String, String> addedParameters;
    private Map<String, String> replacedParameters;
    private Set<String> removedParameters;

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public void setHostName(String hostName) {
        if (hostName == null || hostName.trim().equals("")) {
            throw new IllegalArgumentException("Illegal hostname: '" + hostName + "'");
        }
        
        String[] hostNames = StringUtils.tokenizeToStringArray(hostName, ", ");
        if (hostNames.length == 0) {
            throw new IllegalArgumentException(
                "Unable to find host name in argument: '" + hostName + "'");
        }

        this.redirectToHostName = hostNames[0];

        // Makes no sense to redirect to *, but still OK to redirect to a different port
        if ("*".equals(this.redirectToHostName)) {
            this.redirectToHostName = null;
        }
    }
    
    public void setPort(String port) {
        this.port = port;
    }


    public void setAddedParameters(Map<String, String> addedParameters) {
        this.addedParameters = addedParameters;
    }

    public void setReplacedParameters(Map<String, String> replacedParameters) {
        this.replacedParameters = replacedParameters;
    }

    public void setRemovedParameters(Set<String> removedParameters) {
        this.removedParameters = removedParameters;
    }

    @Override
    public ModelAndView handleRequest(HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        redirect(request, response);
        return null;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        redirect(request, response);
        return false;
    }


    @Override
    public void postHandle(HttpServletRequest request,
                           HttpServletResponse response,
                           Object handler, ModelAndView modelAndView) {
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler, Exception ex) {
    }

    private void redirect(HttpServletRequest request,
            HttpServletResponse response) throws Exception {
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
        response.sendRedirect(url.toString());
    }
}

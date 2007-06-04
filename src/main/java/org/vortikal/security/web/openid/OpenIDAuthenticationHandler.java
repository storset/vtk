/* Copyright (c) 2007, University of Oslo, Norway All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *  * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *  * Neither the name of the University of Oslo nor the names of its
 * contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.vortikal.security.web.openid;





import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openid4java.consumer.ConsumerManager;
import org.openid4java.consumer.VerificationResult;
import org.openid4java.discovery.DiscoveryInformation;
import org.openid4java.discovery.Identifier;
import org.openid4java.message.AuthRequest;
import org.openid4java.message.ParameterList;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.Ordered;
import org.vortikal.security.AuthenticationException;
import org.vortikal.security.AuthenticationProcessingException;
import org.vortikal.security.Principal;
import org.vortikal.security.PrincipalFactory;
import org.vortikal.security.web.AuthenticationChallenge;
import org.vortikal.security.web.AuthenticationHandler;
import org.vortikal.web.service.Service;
import java.util.HashMap;



public class OpenIDAuthenticationHandler
  implements AuthenticationHandler, AuthenticationChallenge, Ordered, InitializingBean {

    private Log logger = LogFactory.getLog(this.getClass());
    private PrincipalFactory principalFactory;
    private Set recognizedDomains = null;
    private Set excludedPrincipals = new HashSet();
    private int order = Integer.MAX_VALUE;
    
    private ConsumerManager consumerManager;
    private Service formService;
    private Service openIDAuthenticationService;
    private Service finalRedirectService;
    
    public void setConsumerManager(ConsumerManager consumerManager) {
        this.consumerManager = consumerManager;
    }
    
    public void setOpenIDAuthenticationService(Service openIDAuthenticationService) {
        this.openIDAuthenticationService = openIDAuthenticationService;
    }

    public void setFormService(Service formService) {
        this.formService = formService;
    }

    public void setFinalRedirectService(Service finalRedirectService) {
        this.finalRedirectService = finalRedirectService;
    }

    public void setRecognizedDomains(Set recognizedDomains) {
        this.recognizedDomains = recognizedDomains;
    }

    public void setExcludedPrincipals(Set excludedPrincipals) {
        this.excludedPrincipals = excludedPrincipals;
    }
    
    public void setOrder(int order) {
        this.order = order;
    }
    
    public int getOrder() {
        return this.order;
    }

    public void afterPropertiesSet() {
        if (this.principalFactory == null) {
            throw new BeanInitializationException(
                "JavaBean property 'principalManager' not set.");
        }
        if (this.consumerManager == null) {
            throw new BeanInitializationException(
                "JavaBean property 'consumerManager' not set.");
        }
        if (this.openIDAuthenticationService == null) {
            throw new BeanInitializationException(
                "JavaBean property 'openIDAuthenticationService' not set.");
        }
        if (this.formService == null) {
            throw new BeanInitializationException(
                "JavaBean property 'formService' not set.");
        }
//         if (this.finalRedirectService == null) {
//             throw new BeanInitializationException(
//                 "JavaBean property 'finalRedirectService' not set.");
//         }
    }


    public boolean isLogoutSupported() {
        return false;
    }


    public boolean logout(Principal principal, HttpServletRequest req,
                          HttpServletResponse resp) {
        // FIXME: redirect user to page explaining how to exit the browser? 
        return false;
    }


    public AuthenticationChallenge getAuthenticationChallenge() {
        return this;
    }


    public boolean isRecognizedAuthenticationRequest(HttpServletRequest request) {

        String openidResponse = request.getParameter("openid_response");
        if (openidResponse != null) {
            return true;
        }

        if (!"POST".equals(request.getMethod())) {
            return false;
        }
        String identifier = request.getParameter("openid_identifier");
        return identifier != null;
    }

    public void challenge(HttpServletRequest request, HttpServletResponse response) {

        String identifier = request.getParameter("openid_identifier");

        if (identifier == null) {
            String redirectURL = this.formService.constructLink(request.getRequestURI());
            if (true) {
                try {
                    response.sendRedirect(redirectURL);
                } catch (Exception e) {
                    throw new AuthenticationProcessingException("Unable to redirect to form service", e);
                }
            }
            return;
        }

        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("openid_response", "true");
        String returnURL = this.openIDAuthenticationService.constructLink(
            request.getRequestURI(), parameters);

        // perform discovery on the user-supplied identifier
        List discoveries;

        try {
            discoveries = this.consumerManager.discover(identifier);
        } catch (Exception e) {
            throw new AuthenticationProcessingException(
                "Failed to perform discovery on identifier '" + identifier + "'", e);
        }

        // attempt to associate with the OpenID provider
        // and retrieve one service endpoint for authentication
        DiscoveryInformation discovered = this.consumerManager.associate(discoveries);

        // store the discovery information in the user's session for later use
        request.getSession().setAttribute("discovered", discovered);
        
        try {
            // obtain a AuthRequest message to be sent to the OpenID provider
            AuthRequest authReq = this.consumerManager.authenticate(discovered, returnURL);
            String redirURL = authReq.getDestinationUrl(true);
            response.sendRedirect(redirURL);
        } catch (Exception e) {
            throw new AuthenticationProcessingException("Unable to redirect to OpenID URL", e);
        }
    }



    public Principal authenticate(HttpServletRequest request)
        throws AuthenticationException {

        if (request.getParameter("openid_response") == null) {
            throw new AuthenticationException();
        }
        if (logger.isDebugEnabled()) {
            logger.debug("openid_response: " + request.getParameter("openid_response"));
        }

        // extract the parameters from the authentication response
        // (which comes in as a HTTP request from the OpenID provider)
        ParameterList openidResp = new ParameterList(request.getParameterMap());

        // retrieve the previously stored discovery information
        DiscoveryInformation discovered = (DiscoveryInformation)
            request.getSession().getAttribute("discovered");

        // extract the receiving URL from the HTTP request
        StringBuffer receivingURL = request.getRequestURL();
        String queryString = request.getQueryString();
        if (queryString != null && queryString.length() > 0)
            receivingURL.append("?").append(request.getQueryString());

        // verify the response

        VerificationResult verification;
        try {
            verification = this.consumerManager.verify(
                receivingURL.toString(), openidResp, discovered);
        } catch (Exception e) {
            throw new AuthenticationProcessingException(
                "Failed to verify authentication request", e);
        }

        // examine the verification result and extract the verified identifier
        Identifier verified = verification.getVerifiedId();

        if (verified == null) {
            throw new AuthenticationException();
        }
        Principal principal = this.principalFactory.getUserPrincipal(verified.getIdentifier());
        if (logger.isDebugEnabled()) {
            logger.debug("Authenticated principal: " + principal);
        }
        

        return principal;
    }


    public boolean postAuthentication(HttpServletRequest request,
                                      HttpServletResponse response) {
        
        String redirectURL = this.finalRedirectService.constructLink(request.getRequestURI());
        
        try {
        if (logger.isDebugEnabled()) {
            logger.debug("Redirect after authentication: " + redirectURL);
        }
        response.sendRedirect(redirectURL);
            return true;
        } catch (Exception e) {
            throw new AuthenticationProcessingException("Unable to redirect to URL: '"
                                                        + redirectURL + "'", e);
        }
    }

    public void setPrincipalFactory(PrincipalFactory principalFactory) {
        this.principalFactory = principalFactory;
    }
}

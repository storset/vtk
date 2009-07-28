/* Copyright (c) 2004, University of Oslo, Norway
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
package org.vortikal.web.actions.logout;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;
import org.vortikal.security.SecurityContext;
import org.vortikal.security.web.SecurityInitializer;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;
import org.vortikal.web.service.URL;

/**
 * Logs the current principal out from the security context. Does not
 * return a view, instead redirects to a given service or the referring URL, 
 * depending on the form input.
 *
 * <p>Configurable properties:
 * <ul>
 *   <li><code>securityInitializer</code> - the {@link SecurityInitializer}
 *   <li><code>redirectService</code> - the {@link Service} to redirect to if
 *   the security initializer did not handle the request itself and the form 
 *   input <code>useRedirectService</code> is not specified.
 * </ul>
 */
public class LogoutController extends SimpleFormController implements InitializingBean {

    private Service redirectService = null;
    private SecurityInitializer securityInitializer = null;
    private Repository repository;

    public void setRedirectService(Service redirectService) {
        this.redirectService = redirectService;
    }
    
    public void setSecurityInitializer(SecurityInitializer securityInitializer) {
        this.securityInitializer = securityInitializer;
    }
    
    public void setRepository(Repository repository) {
        this.repository = repository;
    }
    
    public void afterPropertiesSet() {
        if (this.redirectService == null) {
            throw new BeanInitializationException(
                "Bean property 'service' not set");
        }
        if (this.securityInitializer == null) {
            throw new BeanInitializationException(
                "Bean property 'securityInitializer' not set");
        }
        if (this.repository == null) {
            throw new BeanInitializationException(
                "Bean property 'repository' not set");
        }
    }
    
    public class LogoutCommand {
        private URL submitURL;
        private String logoutAction;
        private String useRedirectService;
        
        public LogoutCommand(URL submitURL) {
            this.submitURL = submitURL;
        }
        public URL getSubmitURL() {
            return this.submitURL;
        }
        public void setLogoutAction(String logoutAction) {
            this.logoutAction = logoutAction;
        }
        public String getLogoutAction() {
            return this.logoutAction;
        }
        public void setUseRedirectService(String useRedirectService) {
            this.useRedirectService = useRedirectService;
        }
        public String getUseRedirectService() {
            return this.useRedirectService;
        }
    }
    
    @Override
    protected Object formBackingObject(HttpServletRequest request)
            throws Exception {
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        Principal principal = securityContext.getPrincipal();
        RequestContext requestContext = RequestContext.getRequestContext();
        
        Resource resource = this.repository.retrieve(
                securityContext.getToken(), requestContext.getResourceURI(), true);
                    
        Service service = requestContext.getService();
        URL submitURL = service.constructURL(resource, principal);
        LogoutCommand logoutCommand = new LogoutCommand(submitURL);
        return logoutCommand;
    }

    @Override
    protected ModelAndView onSubmit(HttpServletRequest request,
            HttpServletResponse response, Object command, BindException errors)
            throws Exception {
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        Principal principal = securityContext.getPrincipal();
        RequestContext requestContext = RequestContext.getRequestContext();
        
        Resource resource = this.repository.retrieve(
                securityContext.getToken(), requestContext.getResourceURI(), true);

        LogoutCommand logoutCommand = (LogoutCommand) command;

        boolean responseWritten = this.securityInitializer.logout(request, response);
        if (responseWritten) {
            return null;
        }

        if (logoutCommand.getUseRedirectService() != null) {
            String url = this.redirectService.constructLink(resource, principal);
            sendRedirect(url, request, response);
            return null;
        }
        
        String referrer = request.getHeader("Referer");
        String url = referrer;
        if (url == null) {
            url = this.redirectService.constructLink(resource, principal);
        }
        sendRedirect(url, request, response);
        return null;
    }
    
    private void sendRedirect(String url, HttpServletRequest request, HttpServletResponse response) throws Exception {
        boolean http10 = "HTTP/1.0".equals(request.getProtocol());
        if (http10) {
            response.sendRedirect(url);
        } else {
            response.setStatus(303);
            response.setHeader("Location", url);
        }
    }
}

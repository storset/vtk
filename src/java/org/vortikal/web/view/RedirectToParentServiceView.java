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
package org.vortikal.web.view;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.servlet.view.AbstractView;

import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;


/**
 * HTTP redirect view that sends the client to the URL of the current
 * resource, constructed using the current service's parent.
 *
 * <p>Configurable properties:
 * <ul>
 *   <li><code>repository</code> - the content {@link Repository repository}
 *   <li><code>http10</code> - whether or not to use HTTP/1.0 style
 *   redirects (302). When set to <code>false</code>, a 303 status
 *   code is set instead. The default value is <code>true</code>.
 * </ul>
 */
public class RedirectToParentServiceView
  extends AbstractView implements InitializingBean {

    private boolean http10 = true;

    private Repository repository = null;


    public void setRepository(Repository repository) {
        this.repository = repository;
    }
    

    public void setHttp10(boolean http10) {
        this.http10 = http10;
    }
    

    public void afterPropertiesSet() {
        if (this.repository == null) {
            throw new BeanInitializationException(
                "Bean property 'repository' not set");
        }
    }
    
    
    
    protected void renderMergedOutputModel(Map model, HttpServletRequest request,
                                           HttpServletResponse response)
        throws Exception {

        SecurityContext securityContext = SecurityContext.getSecurityContext();
        Principal principal = securityContext.getPrincipal();

        RequestContext requestContext = RequestContext.getRequestContext();
        Service service = requestContext.getService();
        Service parentService = service.getParent();
        if (parentService == null) {
            parentService = service;
        }
        Resource resource = repository.retrieve(
            securityContext.getToken(), requestContext.getResourceURI(), true);
        

        String url = parentService.constructLink(resource, principal);

        if (logger.isDebugEnabled()) {
            logger.debug("Constructed redirect URL '" + url
                         + "' using service " + parentService);
        }
            
        if (http10) {
            // send status code 302
            response.sendRedirect(url);
        } else {
            // correct HTTP status code is 303, in particular for POST requests
            response.setStatus(303);
            response.setHeader("Location", url);
        }
    }


}

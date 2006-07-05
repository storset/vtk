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
package org.vortikal.web.controller.repository;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.SecurityContext;
import org.vortikal.util.repository.URIUtil;
import org.vortikal.web.RequestContext;

/** Delete the requested resource from repository.
 * 
 *  <p>By default puts the parent resource in the model under the 'resource' name. 
 *  This can be overridden by specifying a path (relative or absolute) to another resource.
 */
public class DeleteResourceController extends AbstractController implements InitializingBean {

    private Repository repository;
    private String viewName;
    private String resourcePath;
    
    
    private String trustedToken;
    
    /**
     * @param repository The repository to set.
     */
    public void setRepository(Repository repository) {
        this.repository = repository;
    }
    
    public void setViewName(String viewName) {
        this.viewName = viewName;
    }
    
    protected ModelAndView handleRequestInternal(HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        RequestContext requestContext = RequestContext.getRequestContext();

        String token = this.trustedToken;
        if (token == null) {
            SecurityContext securityContext = SecurityContext.getSecurityContext();
            token = securityContext.getToken();
        }
        
        String uri = requestContext.getResourceURI();
        Resource resource = this.repository.retrieve(token, uri, false);
        this.repository.delete(token, uri);
    
        Resource modelResource = this.repository.retrieve(token, resource.getParent(), false);

        if (this.resourcePath != null) {
            String newUri = URIUtil.getAbsolutePath(this.resourcePath, uri);
            if (newUri != null) {
                try {
                    modelResource = this.repository.retrieve(token, newUri, false);
                } catch (Exception e) {
                    this.logger.info("Unable to retireve requested resource to view '" + newUri + "'", e);
                    // Do nothing
                }
            }
        }
        
        Map model = new HashMap();
        model.put("resource", modelResource);
        return new ModelAndView(this.viewName, model);
    }

    public void afterPropertiesSet() throws Exception {
        if (this.viewName == null)
            throw new BeanInitializationException("Property 'viewName' must be set");
    }

    public void setTrustedToken(String trustedToken) {
        this.trustedToken = trustedToken;
    }

    /**
     * @param resourcePath The resourcePath to set.
     */
    public void setResourcePath(String resourcePath) {
        this.resourcePath = resourcePath;
    }
    
    protected Repository getRepository() {
        return this.repository;
    }
    
    protected String getTrustedToken() {
        return this.trustedToken;
    }

    protected String getViewName() {
        return this.viewName;
    }
    
}

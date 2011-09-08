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
package org.vortikal.web.referencedata.provider;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repository.Path;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;
import org.vortikal.web.RequestContext;
import org.vortikal.web.referencedata.ReferenceDataProvider;
import org.vortikal.web.service.Service;

/**
 * Creates model data for the browse application. 
 *
 * TODO: make it similar to SessionBeanProvider so that we can insert into DefaultMenuProvider instead of hack in resource-bar.ftl
 *
 * <p>Description:
 *  
 * <p>Configurable properties:
 * <ul>
 *   <li><code>viewService</code> - the service for which to construct a viewURL
 * </ul>
 *
 * <p>Model data published:
 * <ul>
 * <li><code>browseURL</code>: a string with the url to the linked resource
 * <li><code>editfield</code>: id of the form-element to put the url in
 * </ul>
 */
public class BrowseUrlProvider implements ReferenceDataProvider, InitializingBean {

    private static final String BROWSE_SESSION_ATTRIBUTE = "browsesession";

    private Service viewService;

    public void setViewService(Service viewService) {
        this.viewService = viewService;
    }

    public final void afterPropertiesSet() throws Exception {
        if (this.viewService == null) {
            throw new BeanInitializationException("Property 'viewService' not set");
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void referenceData(Map model, HttpServletRequest request) throws Exception {
        RequestContext requestContext = RequestContext.getRequestContext();
        String token = requestContext.getSecurityToken();
        Principal principal = requestContext.getPrincipal();
        Path uri = requestContext.getResourceURI();
        Repository repository = requestContext.getRepository();
        Resource resource = repository.retrieve(token, uri, false);

        // This is the url to the parent of the document that's being edited

        String viewUrl = this.viewService.constructLink(resource, principal);

        BrowseSessionBean sessionBean = (BrowseSessionBean)
        request.getSession(true).getAttribute(BROWSE_SESSION_ATTRIBUTE);

        /* Deleting session if you get a parameterlist which contains
	   'id' because then its a new request on the browse-app */

        if (sessionBean != null && request.getParameter("id") != null){
            request.getSession(true).removeAttribute(BROWSE_SESSION_ATTRIBUTE);
            sessionBean = null;
        }

        if (sessionBean == null) {
            sessionBean = new BrowseSessionBean();
            sessionBean.setEditField(request.getParameter("id"));
            sessionBean.setStartUrl(viewUrl);
            request.getSession(true).setAttribute(BROWSE_SESSION_ATTRIBUTE, sessionBean);
        }

        /* Checking whether to make a relative link or not */

        if (viewUrl.startsWith(sessionBean.getStartUrl()) && !viewUrl.equals(sessionBean.getStartUrl())) {
            viewUrl = viewUrl.substring(sessionBean.getStartUrl().length());
        }

        model.put("browseURL", viewUrl);
        model.put("editField", sessionBean.getEditField());
    }
}


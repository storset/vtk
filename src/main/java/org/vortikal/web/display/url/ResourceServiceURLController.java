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
package org.vortikal.web.display.url;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.Revision;
import org.vortikal.security.Principal;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;
import org.vortikal.web.service.URL;

/**
 * Controller that provides a reference (URL) to the requested resource.
 * 
 * <p>
 * Configurable properties:
 * <ul>
 * <li><code>service</code> - the service used to construct the URL</li>
 * <li><code>viewName</code> - the name of the returned view</li>
 * </ul>
 * </p>
 * 
 * <p>
 * Model data provided:
 * <ul>
 * <li><code>resource</code> - the resource object</li>
 * <li><code>resourceReference</code> - the URL</li>
 * </ul>
 */
public class ResourceServiceURLController implements Controller {

    public static final String DEFAULT_VIEW_NAME = "resourceReference";

    private Service service;
    private String viewName = DEFAULT_VIEW_NAME;
    private String webProtocol;
    private String webProtocolRestricted;
    private boolean displayWorkingRevision;
    private boolean previewUnpublished;
    private boolean previewObsoleted;

    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {

        RequestContext requestContext = RequestContext.getRequestContext();
        Principal principal = requestContext.getPrincipal();
        String token = requestContext.getSecurityToken();
        Repository repository = requestContext.getRepository();
        Path uri = requestContext.getResourceURI();
        Resource resource = repository.retrieve(token, uri, false);
        URL resourceViewURL = this.service.constructURL(resource, principal, false);

        Map<String, Object> model = new HashMap<String, Object>();

        if (this.displayWorkingRevision) {
            Revision workingCopy = null;
            for (Revision rev : repository.getRevisions(token, uri)) {
                if (rev.getType() == Revision.Type.WORKING_COPY) {
                    workingCopy = rev;
                    break;
                }
            }
            if (workingCopy != null) {
                try {
                    resource = repository.retrieve(token, uri, false, workingCopy);
                    model.put("workingCopy", workingCopy);
                    resourceViewURL.addParameter("revision", Revision.Type.WORKING_COPY.name());
                } catch (Throwable t) {
                }
            }
        }

        // Add parameter to preview view unpublished only if resource actually
        // is unpublished
        if (this.previewUnpublished) {
            if (!resource.isPublished()) {
                resourceViewURL.addParameter("vrtxPreviewUnpublished", "true");
            }
        }

        // Add parameter to preview obsoleted only if resource actually is
        // obsoleted
        if (this.previewObsoleted) {

            Property obsoletedProp = resource.getProperty(Namespace.DEFAULT_NAMESPACE, "obsoleted");
            if (obsoletedProp != null && obsoletedProp.getBooleanValue()) {
                model.put("obsoleted", "true");
                resourceViewURL.addParameter("vrtxPreviewObsoleted", "true");
            }

        }

        String resourceURL = resourceViewURL.toString();

        // Hack to ensure https for preview of direct access interfaces
        if ((request.getScheme() == "https") && (request.getServerPort() != 443) && resourceURL.startsWith("http:")) {
            resourceURL = resourceURL.replaceFirst("http:", "https:");
        }

        // Hack to ensure https for preview when not popup and set authTarget
        boolean isViewSelectiveHttps = (this.webProtocol != null && this.webProtocolRestricted != null)
                && !this.webProtocol.equals(this.webProtocolRestricted);
        // Exceptions (https only if readRestricted)
        boolean isPopup = "preview.displayPopupURL".equals(this.viewName) || "previewPopup".equals(this.viewName);

        String authTarget = "http";
        if (isViewSelectiveHttps) {
            authTarget = isPopup ? (resource.isReadRestricted() ? "https" : "http") : "https";
        }
        if (resourceURL.startsWith("http:") && !isPopup && isViewSelectiveHttps) {
            resourceURL = resourceURL.replaceFirst("http:", "https:");
        }

        model.put("resource", resource);
        model.put("resourceReference", resourceURL);
        model.put("authTarget", authTarget);

        return new ModelAndView(this.viewName, model);
    }

    @Required
    public void setService(Service service) {
        this.service = service;
    }

    @Required
    public void setViewName(String viewName) {
        this.viewName = viewName;
    }

    public void setWebProtocol(String webProtocol) {
        this.webProtocol = webProtocol;
    }

    public void setWebProtocolRestricted(String webProtocolRestricted) {
        this.webProtocolRestricted = webProtocolRestricted;
    }

    public void setDisplayWorkingRevision(boolean displayWorkingRevision) {
        this.displayWorkingRevision = displayWorkingRevision;
    }

    public void setPreviewUnpublished(boolean previewUnpublished) {
        this.previewUnpublished = previewUnpublished;
    }

    public void setPreviewObsoleted(boolean previewObsoleted) {
        this.previewObsoleted = previewObsoleted;
    }

}

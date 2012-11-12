/* Copyright (c) 2009, University of Oslo, Norway
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

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.security.Principal;
import org.vortikal.web.RequestContext;
import org.vortikal.web.referencedata.ReferenceDataProvider;
import org.vortikal.web.service.Service;
import org.vortikal.web.service.URL;

public class EditPublishingProvider implements ReferenceDataProvider {

    private Service publishResourceService;
    private Service unpublishResourceService;
    private Service editPublishDateService;
    private Service editUnpublishDateService;
    private PropertyTypeDefinition publishedPropDef;

    @Override
    public void referenceData(Map<String, Object> model, HttpServletRequest request) throws Exception {
        RequestContext requestContext = RequestContext.getRequestContext();
        String token = requestContext.getSecurityToken();
        Path resourceURI = requestContext.getResourceURI();
        Repository repository = requestContext.getRepository();

        Resource resource = repository.retrieve(token, resourceURI, true);
        Property publishedProp = resource.getProperty(this.publishedPropDef);

        Principal principal = requestContext.getPrincipal();

        URL editPublishDateUrl = null;
        try {
            editPublishDateUrl = this.editPublishDateService.constructURL(resource, principal);
        } catch (Throwable t) { }
        model.put("editPublishDateUrl", editPublishDateUrl);

        URL editUnpublishDateUrl = null;
        try {
            editUnpublishDateUrl = this.editUnpublishDateService.constructURL(resource, principal);
        } catch (Throwable t) { }
        model.put("editUnpublishDateUrl", editUnpublishDateUrl);

        if (publishedProp != null && publishedProp.getBooleanValue()) {

            URL unPublishUrl = null;
            try {
                unPublishUrl = this.unpublishResourceService.constructURL(resource, principal);
            } catch (Throwable t) { }
            model.put("unPublishUrl", unPublishUrl);
        } else {
            URL publishUrl = null;
            try {
                publishUrl = this.publishResourceService.constructURL(resource, principal);
            } catch (Throwable t) { }
            model.put("publishUrl", publishUrl);
        }

    }

    @Required
    public void setPublishResourceService(Service publishResourceService) {
        this.publishResourceService = publishResourceService;
    }

    @Required
    public void setUnpublishResourceService(Service unpublishResourceService) {
        this.unpublishResourceService = unpublishResourceService;
    }

    @Required
    public void setEditPublishDateService(Service editPublishDateService) {
        this.editPublishDateService = editPublishDateService;
    }

    @Required
    public void setEditUnpublishDateService(Service editUnpublishDateService) {
        this.editUnpublishDateService = editUnpublishDateService;
    }

    @Required
    public void setPublishedPropDef(PropertyTypeDefinition publishedPropDef) {
        this.publishedPropDef = publishedPropDef;
    }

}

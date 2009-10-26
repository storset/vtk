/* Copyright (c) 2007, University of Oslo, Norway
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
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;
import org.vortikal.web.referencedata.ReferenceDataProvider;
import org.vortikal.web.service.Assertion;

/**
 * A reference data provider that puts a message in the model, based on published status and read-permission.
 * 
 * <p>
 * Configurable JavaBean properties:
 * <ul>
 * <li><code>localizationKey</code> - the localization key to use when looking up the message to display in the model.
 * The resource's name is used as a parameter.
 * <li><code>modelName</code> - the name to use for the sub-model in the main model.
 * <li><code>repository</code> - the repository.
 * <li><code>assertion</code> - assertion for when resource is unpublished.
 * </ul>
 * 
 * <p>
 * Model data provided:
 * <ul>
 * <li>the localized message (in a sub-model whose name is configurable trough the <code>modelName</code> JavaBean
 * property)
 * </ul>
 * 
 */
public class PublishPermissionMessageProvider implements ReferenceDataProvider {

    private String localizationKey;
    private String modelName;
    private Repository repository;
    private Assertion assertion;


    @Required
    public void setLocalizationKey(String localizationKey) {
        this.localizationKey = localizationKey;
    }


    @Required
    public void setModelName(String modelName) {
        this.modelName = modelName;
    }


    // TODO: this whole file is kinda muddy. but wanted as a tabMessage..
    @SuppressWarnings("unchecked")
    public void referenceData(Map model, HttpServletRequest request) throws Exception {

        RequestContext context = RequestContext.getRequestContext();

        SecurityContext securityContext = SecurityContext.getSecurityContext();
        Principal principal = securityContext.getPrincipal();
        String token = securityContext.getToken();

        Path uri = RequestContext.getRequestContext().getResourceURI();
        Resource resource = this.repository.retrieve(token, uri, false);

        String permission = ".allowed";
        if (resource.isReadRestricted()) {
            permission = ".restricted";
        }

        org.springframework.web.servlet.support.RequestContext springContext = new org.springframework.web.servlet.support.RequestContext(
                request);
        String messagePermission = springContext.getMessage(this.localizationKey + permission, new Object[] {},
                this.localizationKey);

        String publishStatus = "published";
        if (assertion.matches(request, resource, principal)) {
            publishStatus = "unpublished";
        }

        String messagePublishState = springContext.getMessage(this.localizationKey + ".state", new Object[] {},
                this.localizationKey);
        String messagePublish = springContext.getMessage(this.localizationKey + "." + publishStatus, new Object[] {},
                this.localizationKey);

        // hmm..
        model.put(this.modelName + "State", messagePublishState);
        model.put(this.modelName + "Publish", messagePublish);
        model.put(this.modelName + "Permission", messagePermission);
    }


    public String toString() {
        StringBuffer sb = new StringBuffer(this.getClass().getName());
        sb.append(" [ ");
        sb.append("modelName = ").append(this.modelName);
        sb.append(", localizationKey = ").append(this.localizationKey);
        sb.append(" ]");
        return sb.toString();
    }


    public Repository getRepository() {
        return repository;
    }


    public void setRepository(Repository repository) {
        this.repository = repository;
    }


    public Assertion getAssertion() {
        return assertion;
    }


    public void setAssertion(Assertion assertion) {
        this.assertion = assertion;
    }

}

/* Copyright (c) 2005, University of Oslo, Norway
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
package org.vortikal.web.view.components.menu;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;
import org.vortikal.web.referencedataprovider.Provider;
import org.vortikal.web.service.Service;
import org.vortikal.web.service.ServiceUnlinkableException;


/**
 * A reference data provider that supplies a populated {@link ListMenu}.
 *  
 * Constructor arguments:
 * <ul>
 *  <li><code>repository</code> - the content repository
 *  <li><code>services</code> - required array of {@link Service}s to create ListMenu to 
 *  <li><code>label</code> - required MenyList type descriptor
 *  <li> <code>modelName</code> - the name to use as model key. The default is 'label', 
 *  override if you have multiple list menus with the same label. 
 * </ul>
 * 
 * Model data provided:
 * <ul>
 *   <li><code>'modelName'</code> - a <code>ListMenu</code> item.
 * 
 */
public class DefaultListMenuProvider implements Provider {

    private String modelName;
    private String label;
    private Repository repository;
    private Service[] services;
    
    
    public DefaultListMenuProvider(String label, Service[] services, Repository repository) {
        this(label, label, services, repository);
    }

    public DefaultListMenuProvider(String label, String modelName, Service[] services, Repository repository) {
        if (label == null)
            throw new IllegalArgumentException("Argument 'label' cannot be null");
        if (modelName == null)
            throw new IllegalArgumentException("Argument 'modelName' cannot be null");
        if (repository == null)
            throw new IllegalArgumentException("Argument 'repository' cannot be null");
        if (services == null)
            throw new IllegalArgumentException("Argument 'services' cannot be null");
            
        this.label = label;
        this.modelName = modelName;
        this.repository = repository;

    }





    public void referenceData(Map model, HttpServletRequest request)
            throws Exception {
        
        ListMenu menu = new ListMenu();
        menu.setLabel(this.label);
        

        RequestContext requestContext = RequestContext.getRequestContext();
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        
        Principal principal = securityContext.getPrincipal();
        Resource resource = repository.retrieve(
            securityContext.getToken(), requestContext.getResourceURI(), false);
        Service currentService = requestContext.getService();
        
        List items = new ArrayList();
        MenuItem activeItem = null;
        
        for (int i = 0; i < services.length; i++) {
            Service service = services[i];

            String label = service.getName();
            String title = getTitle(resource, service, request);
            String url = null;
            try {
                url = service.constructLink(resource, principal);
            } catch (ServiceUnlinkableException ex) {
                // ok
            }

            
            
            MenuItem item = new MenuItem();
            item.setLabel(label);
            item.setTitle(title);
            item.setUrl(url);

            if (service == currentService) {
                item.setActive(true);
                activeItem = item;
            }
            items.add(item);
        }
        
        menu.setItems((MenuItem[]) items.toArray(new MenuItem[items.size()]));
        menu.setActiveItem(activeItem);
        
        model.put(modelName, menu);
        // Create the shiznit
        
        
    }

    private String getTitle(Resource resource, Service service, HttpServletRequest request) {

        org.springframework.web.servlet.support.RequestContext springContext =
            new org.springframework.web.servlet.support.RequestContext(request);
        String name = service.getName();
        
        String defaultDescription = springContext.getMessage(
                this.label + name, name);
            String title = springContext.getMessage(
                this.label + name + "." +
                resource.getContentType(), defaultDescription);

            Property resourceType = resource.getProperty(Property.LOCAL_NAMESPACE, "resource-type");
            if (resourceType != null) {
                title = springContext.getMessage(
                        this.label + name + "." +
                        resource.getContentType() + "." + 
                        resourceType.getValue(), title);
            }

        return title;
    }
    
}

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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;
import org.vortikal.web.referencedata.ReferenceDataProvider;
import org.vortikal.web.service.Service;
import org.vortikal.web.service.ServiceUnlinkableException;


/**
 * A reference data provider that supplies a populated {@link ListMenu}.
 *  
 * <p>Constructor arguments:
 * <ul>
 *  <li><code>repository</code> - the content repository
 *  <li><code>services</code> - required array of {@link Service}s to create ListMenu to 
 *  <li><code>label</code> - required MenyList type descriptor
 *  <li> <code>modelName</code> - the name to use as model key. The
 *  default is 'label', override if you have multiple list menus with
 *  the same label.
 *  <li> <code>matchAssertions</code> - boolean defaults to <code>true</code>,
 *  if the link construction should match assertion. 
 * </ul>
 *
 * <p>Configurable JavaBean properties:
 * <ul>
 *   <li><code>matchAncestorServices</code> - a boolean deciding
 *   whether or not to check ancestors of the current service when
 *   checking if a menu item is selected (or "active"). The default is
 *   <code>false</code> (i.e. an exact service match is required).
 * </ul>
 * 
 * Model data provided:
 * <ul>
 *   <li><code>'modelName'</code> - a {@link ListMenu} object. A note
 *   about the <code>title</code> fields of this list menu's items: It
 *   is looked up from message localization using the following steps:
 *     <ol>
 *       <li>A message key is constructed as follows:
 *           <code>[label].[serviceName].[contentType].[resourceType]</code> where
 *           <code>[serviceName]</code> is the name of the service and
 *           <code>[contentType]</code> is the MIME type of the
 *           resource. 
 *           <code>[resourceType]</code> is the property 'resource-type'
 *           in the namespace 'http://www.uio.no/vortex/custom-properties', and it is appended
 *           only if it exists. A lookup attempt is then made using this key.
 *       </li>
 *       <li>If that lookup does not produce a message, the
 *           <code>.[contentType]</code> and
 *           <code>[resourceType]</code> suffices are removed from the
 *           key, and the lookup is peformed again, using the service
 *           name as the default value.  
 *       </li>
 *     </ol>
 * </ul>
 * 
 */
public class DefaultListMenuProvider implements ReferenceDataProvider {

    private static Log logger = LogFactory.getLog(DefaultListMenuProvider.class);
    private String modelName;
    private String label;
    private Repository repository;
    private Service[] services;
    private boolean matchAncestorServices = false;
    private boolean matchAssertions;
    
    public DefaultListMenuProvider(String label, Service[] services, Repository repository) {
        this(label, label, true, services, repository);
    }

    public DefaultListMenuProvider(String label, String modelName,
                                   Service[] services, Repository repository) {
        this(label, modelName, true, services, repository);
    }
        public DefaultListMenuProvider(String label, String modelName, boolean matchAssertions, Service[] services, Repository repository) {
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
        this.services = services;
        this.repository = repository;
        this.matchAssertions = matchAssertions;
    }

    

    public void setMatchAncestorServices(boolean matchAncestorServices) {
        this.matchAncestorServices = matchAncestorServices;
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
        
        for (int i = 0; i < this.services.length; i++) {
            Service service = this.services[i];

            String label = service.getName();
            String title = getTitle(resource, service, request);
            String url = null;
            try {
                url = service.constructLink(resource, principal, this.matchAssertions);
            } catch (ServiceUnlinkableException ex) {
                // ok
            }

            MenuItem item = new MenuItem();
            item.setLabel(label);
            item.setTitle(title);
            item.setUrl(url);

            if (activeItem == null && isActiveService(currentService, service)) {
                item.setActive(true);
                activeItem = item;
            }

            items.add(item);
        }
        
        menu.setItems((MenuItem[]) items.toArray(new MenuItem[items.size()]));
        menu.setActiveItem(activeItem);
        model.put(modelName, menu);
    }



    private String getTitle(Resource resource, Service service,
                            HttpServletRequest request) {

        org.springframework.web.servlet.support.RequestContext springContext =
            new org.springframework.web.servlet.support.RequestContext(request);
        String name = service.getName();
        
        String messageCode = this.label +"." + name;
        String title = springContext.getMessage(messageCode, name);
        
        messageCode += "." + resource.getContentType();
        title = springContext.getMessage(messageCode, title);

        Property resourceType = resource.getProperty(Property.LOCAL_NAMESPACE,
                "resource-type");

        if (resourceType != null) {
            messageCode += "." + resourceType.getValue();
            title = springContext.getMessage(messageCode , title);
        }

        return title;
    }


    /**
     * Checks whether a service is "active" (that is, the current
     * service of the request is either the same as, or a descendant
     * of this service), depending on the value of
     * <code>matchAncestorServices</code>.
     *
     * @param currentService the current service of the request
     * @param service the service to check for
     * @return if the service is active.
     */
    private boolean isActiveService(Service currentService, Service service) {

        if (this.matchAncestorServices) {
            Service s = currentService;
            while (s != null) {
                boolean match = false;
                if (service == s) {
                    return true;
                }
                s = s.getParent();
            }
            return false;
        } else {
            return (service == currentService);
        }
    }
    

    
}

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
package org.vortikal.web.referencedataprovider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;
import org.vortikal.web.service.ServiceCategoryResolver;
import org.vortikal.web.service.ServiceUnlinkableException;

/**
 * Creates model data for "tabbed" browsing. A tab is simply a link to
 * a service, and can be "active" or "inactive", depending on whether
 * the current service is a child of the tab service or not.
 *
 * <p>The tabs provider looks up all services in the application
 * context that are of a given {@link Service#getCategories category}
 * on startup, using these services as tab services. Currently, the
 * category must be the fully qualified name of this class.
 *
 * <p>Configurable properties:
 * <ul>
 *   <li><code>repository</code> - the {@link Repository content repository}
 *   <li><code>category</code> - the name of this TabsProvider's category
 *   <li><code>modelName</code> - the name to use for this provider's
 *   submodel (default is <code>tabs</code>)
 * </ul>
 *
 * <p>Model data published (in submodel of name configurable by
 * property <code>modelName</code>):
 * <ul>
 * <li><code>tabURLs</code>: an array of the URLs of the tabs</li>
 * <li><code>tabDescriptions</code>: an array of the descriptions of
 *     the tab URLs. These descriptions are interpreted as keys for
 *     message localization, using the following steps:
 *     <ol>
 *       <li>A message key is constructed as follows:
 *           <code>tabs.[serviceName].[contentType]</code> where
 *           <code>[serviceName]</code> is the name of the service and
 *           <code>[contentType]</code> is the MIME type of the
 *           resource. A lookup attempt is made using this key.
 *       </li>
 *       <li>If that lookup does not produce a message, the
 *           <code>.[contentType]</code> suffix is removed from the
 *           key, and the lookup is peformed again, using the service
 *           name as the default value.
 *       </li>
 *     </ol>
 * </li>
 * <li><code>activeTab</code>: an index pointer to the currently active tab</li>
 * <li><code>serviceNames</code>: an array containing the name of the service the tab represents. 
 * Used to add css-classes on each tab.</li> 
 * </ul>
 * 
 */
public class TabsProvider
  implements Provider, InitializingBean, ApplicationContextAware {

    private static Log logger = LogFactory.getLog(TabsProvider.class);
    private Repository repository = null;
    private Service[] services = null;
    private String category = TabsProvider.class.getName();
    private ApplicationContext context;
    private String modelName = "tabs";
    

    public Repository getRepository() {
        return repository;
    }


    public void setRepository(Repository repository) {
        this.repository = repository;
    }


    public void setModelName(String modelName) {
        this.modelName = modelName;
    }
    

    public void setCategory(String category) {
        this.category = category;
    }


    public void setApplicationContext(ApplicationContext applicationContext)
        throws BeansException {
        this.context = applicationContext;
    }

    public final void afterPropertiesSet() throws Exception {
        if (this.repository == null) {
            throw new BeanInitializationException("Property 'repository' not set");
        }

        if (this.modelName == null) {
            throw new BeanInitializationException("Property 'modelName' not set");
        }

        List tabServices = ServiceCategoryResolver.getServicesOfCategory(this.context, this.category);

        if (tabServices.isEmpty()) {
            throw new BeanInitializationException(
                    "No tab registering services defined in context.");
        }
        
        this.services = (Service[]) tabServices.toArray(new Service[0]);
        logger.info("Registered tabs services in the following order: " 
                + tabServices);
    }


    public void referenceData(Map model, HttpServletRequest request)
        throws IOException {
        
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        Principal principal = securityContext.getPrincipal();
        RequestContext requestContext = RequestContext.getRequestContext();
        
        Resource resource = this.repository.retrieve(
            securityContext.getToken(), requestContext.getResourceURI(), false);

        List tabURLs = new ArrayList();
        List tabDescriptions = new ArrayList();
        List accessibleServices = new ArrayList();
        List serviceNames = new ArrayList();
        
        org.springframework.web.servlet.support.RequestContext springContext =
            new org.springframework.web.servlet.support.RequestContext(request);
        for (int i = 0; i < this.services.length; i++) {
            try {
                String url = this.services[i].constructLink(resource, principal);
                tabURLs.add(url);
                String defaultDescription = springContext.getMessage(
                    "tabs." + this.services[i].getName(), this.services[i].getName());
                String description = springContext.getMessage(
                    "tabs." + this.services[i].getName() + "." +
                    resource.getContentType(), defaultDescription);

                if (logger.isDebugEnabled()) {
                    logger.debug("Adding tab element [" + description
                                 + ", " + this.services[i] + "]");
                }
                tabDescriptions.add(description);
                accessibleServices.add(this.services[i]);
                serviceNames.add(this.services[i].getName());
            } catch (ServiceUnlinkableException e) {
                // Service was not accessible for this resource, ignore.
            }
        }

        // Find the "active" tab, based on the current service.

        Service currentService = requestContext.getService();
        int index = 0;
        while (currentService != null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Checking for active tab service: " + currentService);
            }
            if (accessibleServices.contains(currentService)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Found active tab service: " + currentService);
                }
                index = accessibleServices.indexOf(currentService);
                break;
            }
            currentService = currentService.getParent();
            if (logger.isDebugEnabled()) {
                logger.debug("Proceeding to check parent service: " + currentService);
            }
        }

        Map tabsModel = new HashMap();
        tabsModel.put("tabURLs", tabURLs);
        tabsModel.put("tabDescriptions", tabDescriptions);
        tabsModel.put("serviceNames", serviceNames);
        tabsModel.put("activeTab", new Integer(index));
        model.put(this.modelName, tabsModel);
    }


    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(this.getClass().getName()).append(": [");
        for (int i = 0; i < this.services.length; i++) {
            sb.append(this.services[i].getName());
            if (i < this.services.length - 1) sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }
    

}


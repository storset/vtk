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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
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
 * Model builder that has a list of services, providing URLs to these
 * services (if they are available for the current resource). The URLs
 * are made available in the submodel of a configurable name
 * (<code>actions</code> by default).
 * 
 * Configurable properties:
 * <ul>
 *  <li><code>repository</code> - the content repository
 *  <li> <code>modelName</code> - the name to use for the submodel generated
 *       (default <code>actions</code>)
 * 	<li><code>category</code> - required String describing the service category 
 * to look for in the context
 * </ul>
 * 
 * Model data provided:
 * <ul>
 *   <li><code>actions</code> - the <code>actions</code> configuration
 *   property of this class
 *   <li><code>actionNames</code> - names of the action services
 *   <li><code>actionURLs</code> - URLs to each of the services
 *   <li><code>actionLabels</code> - the localized message strings
 * </ul>
 * 
 */
public class ActionBarProvider implements InitializingBean, ApplicationContextAware, Provider {

    private static Log logger = LogFactory.getLog(ActionBarProvider.class);
    
    private String modelName = "actions";
    private List actions; 
    private List actionNames = new ArrayList();
    private Repository repository;
    private ApplicationContext context;
    private String category;
    
    public void setModelName(String modelName) {
        this.modelName = modelName;
    }
    

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public void setApplicationContext(ApplicationContext context) throws BeansException {
        this.context = context;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void afterPropertiesSet() throws Exception {

        if (this.repository == null) {
            throw new BeanInitializationException(
                "Bean property 'repository' must be set");
        }
        
        if (this.category == null) {
            throw new BeanInitializationException(
            "Bean property 'category' must be set");
        }
        
        List actionServices = ServiceCategoryResolver.getServicesOfCategory(context, category);

        if (actionServices.isEmpty()) {
            logger.warn("No action bar registering services defined in context. Something might be wrong.");
        }
        
        this.actions = actionServices;
        
        for (Iterator iter = actionServices.iterator(); iter.hasNext();) {
            Service action = (Service) iter.next();
            actionNames.add(action.getName());
        }
        logger.info("Registered actions services in the following order: " 
                + actionServices);
    }

    public void referenceData(Map model, HttpServletRequest request)
        throws Exception {
        Map actionsMap = new HashMap();
        actionsMap.put("actions", actions);

        RequestContext requestContext = RequestContext.getRequestContext();
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        
        Principal principal = securityContext.getPrincipal();
        Resource resource = repository.retrieve(
            securityContext.getToken(), requestContext.getResourceURI(), false);

        org.springframework.web.servlet.support.RequestContext springContext =
            new org.springframework.web.servlet.support.RequestContext(request);
        
        List actionURLs = new ArrayList();
        List actionLabels = new ArrayList();
        for (Iterator iter = actions.iterator(); iter.hasNext();) {
            Service service = (Service) iter.next();
            
            try {
                String serviceURL = service.constructLink(resource, principal);
                actionURLs.add(serviceURL);
            } catch (ServiceUnlinkableException ex) {
                actionURLs.add(null);
            }
            
            String label = service.getName();
            String description = 
                springContext.getMessage("actions." + label, label);
            
            String contentType = resource.getContentType();
            if (contentType != null && contentType.trim() != "") 
                description = springContext.getMessage(
                        "actions." + label + "." + resource.getContentType(), 
                        description);

            actionLabels.add(description);
        }

        actionsMap.put("actionNames", actionNames);
        actionsMap.put("actionURLs", actionURLs);
        actionsMap.put("actionLabels", actionLabels);
        model.put(modelName, actionsMap);
    }

}

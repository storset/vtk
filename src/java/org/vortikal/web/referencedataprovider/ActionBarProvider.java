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

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;

import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;
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
 *  <li> <code>actions</code> - a list of the services to build URLs for
 *  <li> <code>actionLabels</code> - a list that mirrors the <code>actions</code>
 *       list, containing the name of the action for each service -
 *       typically, this is what will show up as the link text in a
 *       URL. The strings are looked up using Spring's
 *       internationalization functionality, with the text
 *       <code>actions.</code> prepended to each label.
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
public class ActionBarProvider implements InitializingBean, Provider {

    private String modelName = "actions";
    private List actions;
    private List actionNames;
    private List actionLabels;
    private Repository repository = null;

    public void setActions(List actions) {
        this.actions = actions;
        actionNames = new ArrayList();
        for (Iterator iter = actions.iterator(); iter.hasNext();) {
            Service service = (Service) iter.next();
            
            actionNames.add(service.getName());
        }
    }
    
    public void setActionLabels(List actionLabels) {
        this.actionLabels = actionLabels;
    }


    public void setModelName(String modelName) {
        this.modelName = modelName;
    }
    

    public void setRepository(Repository repository) {
        this.repository = repository;
    }


    public void afterPropertiesSet() throws Exception {
        if (this.actions == null) {
            throw new BeanInitializationException(
                "Bean property 'actions' must be set");
        }

        if (this.actionLabels == null) {
            throw new BeanInitializationException(
                "Bean property 'actionLabels' must be set");
        }

        if (this.repository == null) {
            throw new BeanInitializationException(
                "Bean property 'repository' must be set");
        }
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

        List actionURLs = new ArrayList();
        for (Iterator iter = actions.iterator(); iter.hasNext();) {
            Service service = (Service) iter.next();
            
            try {
                String serviceURL = service.constructLink(resource, principal);
                actionURLs.add(serviceURL);
            } catch (ServiceUnlinkableException ex) {
                actionURLs.add(null);
            }
        }
        org.springframework.web.servlet.support.RequestContext springContext =
            new org.springframework.web.servlet.support.RequestContext(request);
        List translatedActionLabels = new ArrayList();
        for (Iterator iter = actionLabels.iterator(); iter.hasNext();) {
            String label = (String) iter.next();
            translatedActionLabels.add(springContext.getMessage("actions." + label, label));
        }

        
        actionsMap.put("actionNames", actionNames);
        actionsMap.put("actionURLs", actionURLs);
        actionsMap.put("actionLabels", translatedActionLabels);
        model.put(modelName, actionsMap);
    }

}

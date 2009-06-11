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
package org.vortikal.web.actions.properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.servlet.mvc.SimpleFormController;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;


/**
 * Controller for deleting resource properties. 
 *
 * @version $Id$
 */
public class DeleteResourcePropertyController
  extends SimpleFormController implements InitializingBean {


    private static Log logger = LogFactory.getLog(DeleteResourcePropertyController.class);
    
    private Repository repository = null;
    
    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public void afterPropertiesSet() {
        if (this.repository == null) {
            throw new BeanInitializationException(
                "Bean property 'repository' not set");
        }
    }
    
    protected boolean isFormSubmission(HttpServletRequest request) {
        String namespace = request.getParameter("namespace");
        String name = request.getParameter("name");
        return (namespace != null && name != null);
    }
    

    protected Object formBackingObject(HttpServletRequest request) throws Exception {

        RequestContext requestContext = RequestContext.getRequestContext();
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        Service service = requestContext.getService();
        
        String namespace = request.getParameter("namespace");
        String name = request.getParameter("name");

        if (namespace == null || name == null || "".equals(namespace.trim())
            || "".equals(name.trim())) {
            throw new ServletException(
                "Both parameters 'name' and 'namespace' must be provided with the request");
        }

        Resource resource = this.repository.retrieve(securityContext.getToken(),
                                                requestContext.getResourceURI(), false);

        String url = service.constructLink(resource, securityContext.getPrincipal());

         ResourcePropertyCommand command =
             new ResourcePropertyCommand(namespace, name, null, url);
        return command;
    }
    
    

    protected void doSubmitAction(Object command) throws Exception {        
        RequestContext requestContext = RequestContext.getRequestContext();
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        
        String token = securityContext.getToken();

        ResourcePropertyCommand propertyCommand =
            (ResourcePropertyCommand) command;

        if (propertyCommand.getCancelAction() != null) {
            propertyCommand.setDone(true);
            return;
        }

        Namespace ns = Namespace.getNamespace(propertyCommand.getNamespace());
        String name = propertyCommand.getName();
        
        Resource resource = this.repository.retrieve(token, requestContext.getResourceURI(), false);
        Property property = resource.getProperty(ns, name);
        if (property == null) {
            if (logger.isDebugEnabled())
                logger.debug("Property " + propertyCommand.getNamespace() + ":" +
                             propertyCommand.getName() + " did not exist on resource " +
                             resource + ", returning");

            propertyCommand.setDone(true);
            return;
        }
        resource.removeProperty(ns, name);
        this.repository.store(token, resource);
        propertyCommand.setDone(true);
    }
}

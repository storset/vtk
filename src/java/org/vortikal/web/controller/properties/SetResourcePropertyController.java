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
package org.vortikal.web.controller.properties;


import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.servlet.mvc.SimpleFormController;


/**
 * Controller for setting resource properties. Property values are
 * interpreted as raw text, unlike in the
 * <code>EditResourcePropertyController</code> property editor, which
 * allows only a set of predefined values.
 *
 * Configurable properties (in addition to those defined by the superclass):
 * <ul>
 *  <li>repository - the repository is required
 * </ul>
 * 
 * 
 * TODO: support setting standard (non-custom) resource properties,
 * such as 'displayName', 'owner', etc. Must have a protected
 * namespace.
 * 
 * @version $Id$
 */
public class SetResourcePropertyController
  extends SimpleFormController implements InitializingBean {


    private static Log logger = LogFactory.getLog(SetResourcePropertyController.class);
    
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
        //String value = request.getParameter("value");
        return (namespace != null && name != null);
    }
    

    protected Object formBackingObject(HttpServletRequest request) throws Exception {

        RequestContext requestContext = RequestContext.getRequestContext();
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        Service service = requestContext.getService();
        
        String namespace = request.getParameter("namespace");
        String name = request.getParameter("name");
        String value = request.getParameter("value");

        if (namespace == null || name == null || "".equals(namespace.trim())
            || "".equals(name.trim())) {
            throw new ServletException(
                "Both parameters 'name' and 'namespace' must be provided with the request");
        }

        Resource resource = repository.retrieve(securityContext.getToken(),
                                                requestContext.getResourceURI(), false);
        Property property = resource.getProperty(namespace, name);
        if (property != null)
            value = property.getValue();

        String url = service.constructLink(resource, securityContext.getPrincipal());

         ResourcePropertyCommand command =
             new ResourcePropertyCommand(namespace, name, value, url);
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

        Resource resource = repository.retrieve(
            token, requestContext.getResourceURI(), false);
        Property[] newProperties = null;
        Property property = resource.getProperty(propertyCommand.getNamespace(),
                                                 propertyCommand.getName());
        if (property != null) {
            property.setValue(propertyCommand.getValue());
        } else {
            Property[] oldProperties = resource.getProperties();
            newProperties = new Property[oldProperties.length + 1];
            for (int i = 0; i < oldProperties.length; i++) {
                newProperties[i] = oldProperties[i];
            }
            property = new Property();
            property.setNamespace(propertyCommand.getNamespace());
            property.setName(propertyCommand.getName());
            property.setValue(propertyCommand.getValue());
            newProperties[newProperties.length - 1] = property;
            resource.setProperties(newProperties);
        } 

        repository.store(token, resource);
        propertyCommand.setDone(true);
    }
}

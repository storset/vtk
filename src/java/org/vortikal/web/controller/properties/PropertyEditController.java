/* Copyright (c) 2006, University of Oslo, Norway
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

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.validation.Validator;
import org.springframework.web.servlet.mvc.SimpleFormController;

import org.vortikal.repository.Namespace;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.ResourceTypeDefinition;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;
import org.vortikal.repository.resourcetype.ValueFactory;



public class PropertyEditController extends SimpleFormController
  implements InitializingBean {

    private Repository repository;
    private ResourceTypeDefinition resourceTypeDefinition;
    private PropertyTypeDefinition propertyTypeDefinition;
    private ValueFactory valueFactory;
    
    
    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public void setPropertyTypeDefinition(PropertyTypeDefinition propertyTypeDefinition) {
        this.propertyTypeDefinition = propertyTypeDefinition;
    }
    
    public void setResourceTypeDefinition(ResourceTypeDefinition resourceTypeDefinition) {
        this.resourceTypeDefinition = resourceTypeDefinition;
    }
    
    public void setValueFactory(ValueFactory valueFactory) {
        this.valueFactory = valueFactory;
    }
    

    public void afterPropertiesSet() {
        if (this.repository == null) {
            throw new BeanInitializationException(
                "JavaBean property 'repository' not set");
        }
        if (this.propertyTypeDefinition == null) {
            throw new BeanInitializationException(
                "JavaBean property 'propertyTypeDefinition' not set");
        }
        setValidator(new PropertyEditValidator(this.propertyTypeDefinition, valueFactory));
    }
    


    protected Object formBackingObject(HttpServletRequest request)
        throws Exception {
        RequestContext requestContext = RequestContext.getRequestContext();
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        Service service = requestContext.getService();
        Resource resource = repository.retrieve(securityContext.getToken(),
                                                requestContext.getResourceURI(), false);
        String value = null;
        
        Property property = resource.getProperty(
            this.resourceTypeDefinition.getNamespace(),
            this.propertyTypeDefinition.getName());
        if (property != null) {
            value = property.getValue().toString();
        }

        Value[] definitionAllowedValues = this.propertyTypeDefinition.getAllowedValues();
        String[] formAllowedValues = null;
        if (definitionAllowedValues != null) {
            formAllowedValues = new String[definitionAllowedValues.length];
            for (int i = 0; i < definitionAllowedValues.length; i++) {
                formAllowedValues[i] = definitionAllowedValues[i].toString();
            }
        }

        String url = service.constructLink(resource, securityContext.getPrincipal());
        PropertyEditCommand command = new PropertyEditCommand(url, value, formAllowedValues);
        return command;
    }




    protected void doSubmitAction(Object command) throws Exception {        
        RequestContext requestContext = RequestContext.getRequestContext();
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        
        String token = securityContext.getToken();

        PropertyEditCommand propertyCommand =
            (PropertyEditCommand) command;

        if (propertyCommand.getCancelAction() != null) {
            propertyCommand.setDone(true);
            return;
        }
        String uri = requestContext.getResourceURI();
        Resource resource = repository.retrieve(token, uri, false);

        Namespace ns = this.resourceTypeDefinition.getNamespace();
        String name = this.propertyTypeDefinition.getName();

        String value = propertyCommand.getValue();
        Property prop = resource.getProperty(ns, name);
        
        if ("".equals(value)) {
            if (prop == null) {
                propertyCommand.setDone(true);
                return;
            }
            resource.removeProperty(ns, name);
        } else {
            if (prop == null) {
                prop = resource.createProperty(ns, name);
            }
            prop.setStringValue(value);
        }
        repository.store(token, resource);
        propertyCommand.setDone(true);
    }
    
}

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

import org.springframework.web.servlet.mvc.SimpleFormController;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.TypeInfo;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;


/**
 * Controller for setting resource properties. Property values are
 * interpreted as raw text, unlike in the
 * <code>EditResourcePropertyController</code> property editor, which
 * allows only a set of predefined values.
 *
 * <p>Configurable properties (in addition to those defined by the {@link
 * SimpleFormController superclass}):
 * <ul>
 *  <li>repository - the repository is required
 * </ul>
 * 
 * 
 * TODO: support setting standard (non-custom) resource properties,
 * such as 'displayName', 'owner', etc. Must have a protected
 * namespace.
 * 
 */
public class SetResourcePropertyController extends SimpleFormController {

    protected boolean isFormSubmission(HttpServletRequest request) {
        String namespace = request.getParameter("namespace");
        String name = request.getParameter("name");
        //String value = request.getParameter("value");
        return (namespace != null && name != null);
    }
    

    protected Object formBackingObject(HttpServletRequest request) throws Exception {
        RequestContext requestContext = RequestContext.getRequestContext();
        Repository repository = requestContext.getRepository();
        Service service = requestContext.getService();
        
        String namespace = request.getParameter("namespace");
        String name = request.getParameter("name");
        String value = request.getParameter("value");

        if (namespace == null || name == null || "".equals(namespace.trim())
            || "".equals(name.trim())) {
            throw new ServletException(
                "Both parameters 'name' and 'namespace' must be provided with the request");
        }

        Resource resource = repository.retrieve(requestContext.getSecurityToken(),
                                                requestContext.getResourceURI(), false);
        Namespace ns = Namespace.getNamespace(namespace);

        Property property = resource.getProperty(ns, name);
        if (property != null)
            value = property.getStringValue();

        String url = service.constructLink(resource, requestContext.getPrincipal());

         ResourcePropertyCommand command =
             new ResourcePropertyCommand(namespace, name, value, url);
        return command;
    }

    protected void doSubmitAction(Object command) throws Exception {        
        RequestContext requestContext = RequestContext.getRequestContext();
        Repository repository = requestContext.getRepository();
        String token = requestContext.getSecurityToken();

        ResourcePropertyCommand propertyCommand =
            (ResourcePropertyCommand) command;

        if (propertyCommand.getCancelAction() != null) {
            propertyCommand.setDone(true);
            return;
        }

        Resource resource = repository.retrieve(
            token, requestContext.getResourceURI(), false);
        TypeInfo typeInfo = repository.getTypeInfo(token, requestContext.getResourceURI());
        Namespace ns = Namespace.getNamespace(propertyCommand.getNamespace());

        Property property = resource.getProperty(ns, propertyCommand.getName());
        if (property == null) {
            property = typeInfo.createProperty(ns, propertyCommand.getName());
            resource.addProperty(property);
        }
        property.setStringValue(propertyCommand.getValue());
        repository.store(token, resource);
        propertyCommand.setDone(true);
    }
}

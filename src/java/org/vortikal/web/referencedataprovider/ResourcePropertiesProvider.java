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
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;
import org.vortikal.web.controller.properties.EnumerationPropertyDescriptor;
import org.vortikal.web.service.Service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;


/**
 * Model builder that provides the list of properties set on a
 * resource, and edit links to them.  The information is made
 * available in the model as a submodel of the name
 * <code>resourceProperties</code>.
 * 
 * Configurable properties:
 * <ul>
 *  <li>repository - the repository is required
 *  <li>editPropertyService - service for editing the properties for a resource
 *  <li>propertyDescriptors - list of
 *      <code>PropertyEditDescriptor</code> objects describing the selected
 *      properties of interest
 * </ul>
 * 
 * Model data provided:
 * <ul>
 *   <li>propertyDescriptors - the list of <code>PropertyEditDescriptor</code> objects
 *   <li>propertyValues - list of property values corresponding to the
 *       indexes in <code>propertyDescriptors</code>
 *   <li>editPropertiesServiceURLs - edit links for each property (enumeration edit)
 * </ul>
 */
public class ResourcePropertiesProvider
  implements Provider, InitializingBean {

    private static Log logger = LogFactory.getLog(
        ResourcePropertiesProvider.class);

    private Repository repository;
    private Service editPropertyService;
    private EnumerationPropertyDescriptor[] propertyDescriptors = null;

    
    /**
     * @param repository The repository to set.
     */
    public void setRepository(Repository repository) {
        this.repository = repository;
    }
    
    public void setEditPropertyService(Service editPropertyService) {
        this.editPropertyService = editPropertyService;
    }


    public void setPropertyDescriptors(EnumerationPropertyDescriptor[]
                                       propertyDescriptors) {
        this.propertyDescriptors = propertyDescriptors;
    }
    


    public void afterPropertiesSet() {
        if (this.repository == null) {
            throw new BeanInitializationException(
                "Bean property 'repository' not set");
        }
        if (this.editPropertyService == null) {
            throw new BeanInitializationException(
                "Bean property 'editPropertyService' not set");
        }
        if (this.propertyDescriptors == null) {
            throw new BeanInitializationException(
                "Bean property 'propertyDescriptors' not set");
        }
    }
    


    public void referenceData(Map model, HttpServletRequest request)
        throws Exception {
        Map resourcePropertiesModel = new HashMap();

        SecurityContext securityContext = SecurityContext.getSecurityContext();
        RequestContext requestContext = RequestContext.getRequestContext();
        
        Resource resource = repository.retrieve(
            securityContext.getToken(), requestContext.getResourceURI(), false);

        List propertyValues = new ArrayList();
        List editPropertiesServiceURLs =
            (this.editPropertyService != null) ? new ArrayList() : null;

        List applicablePropertyDescriptors = new ArrayList();
        
        for (int i = 0; i < propertyDescriptors.length; i++) {
            if (!propertyDescriptors[i].isApplicableProperty(resource)) {
                continue;
            }
            applicablePropertyDescriptors.add(propertyDescriptors[i]);
            Property prop = resource.getProperty(propertyDescriptors[i].getNamespace(),
                                                 propertyDescriptors[i].getName());

            propertyValues.add((prop != null) ? prop.getValue() : null);

            if (editPropertyService != null) {
                Map parameters = new HashMap();
                parameters.put("namespace", propertyDescriptors[i].getNamespace());
                parameters.put("name", propertyDescriptors[i].getName());
                try {
                    String url = editPropertyService.constructLink(
                        resource, securityContext.getPrincipal(), parameters);
                    editPropertiesServiceURLs.add(url);
                } catch (Exception e) { 
                    if (logger.isDebugEnabled()) {
                        logger.debug("Failed to construct link ", e);
                    }
                    editPropertiesServiceURLs.add(null);
                }
            }
        }

        resourcePropertiesModel.put("propertyDescriptors", applicablePropertyDescriptors);
        resourcePropertiesModel.put("propertyValues", propertyValues);
        resourcePropertiesModel.put("editPropertiesServiceURLs", editPropertiesServiceURLs);

        model.put("resourceProperties", resourcePropertiesModel);
    }


    
}


/* Copyright (c) 2004, 2008, University of Oslo, Norway
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
package org.vortikal.web.referencedata.provider;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;
import org.vortikal.web.actions.properties.EnumerationPropertyDescriptor;
import org.vortikal.web.referencedata.ReferenceDataProvider;
import org.vortikal.web.service.Service;
import org.vortikal.web.service.URL;


/**
 * Model builder that provides the list of properties set on a
 * resource, and edit links to them.  The information is made
 * available in the model as a submodel with a configurable name
 * (default <code>resourceProperties</code>).
 * 
 * Configurable properties:
 * <ul>
 *  <li><code>modelName</code> - the name to use for the
 *  submodel. Default is <code>resourceProperties</code>.
 *  <li><code>repository</code> - the {@link Repository content
 *  repository}
 *  <li><code>editPropertyService</code> - {@link Service} for editing
 *  the properties for a resource
 *  <li><code>propertyDescriptors</code> - list of {@link
 *      EnumerationPropertyDescriptor} objects describing the selected
 *      properties of interest
 * </ul>
 * 
 * Model data provided:
 * <ul>
 *   <li><code>propertyDescriptors</code> - the list of {@link
 *   EnumerationPropertyDescriptor} objects that is configured
 *   <li><code>propertyValues</code> - list of property values
 *       corresponding to the indexes in
 *       <code>propertyDescriptors</code>
 *   <li><code>editPropertiesServiceURLs</code> - edit links for each
 *   property 
 * </ul>
 */
public class ResourcePropertiesProvider implements ReferenceDataProvider, InitializingBean {

    private static Log logger = LogFactory.getLog(
        ResourcePropertiesProvider.class);

    private String modelName = "resourceProperties";
    private Repository repository;
    private Service editPropertyService;
    private EnumerationPropertyDescriptor[] propertyDescriptors = null;

    
    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

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
        if (this.modelName == null) {
            throw new BeanInitializationException(
                "Bean property 'modelName' not set");
        }
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
    


    @SuppressWarnings("unchecked")
    public void referenceData(Map model, HttpServletRequest request)
        throws Exception {
        Map<String, Object> resourcePropertiesModel = new HashMap<String, Object>();

        SecurityContext securityContext = SecurityContext.getSecurityContext();
        RequestContext requestContext = RequestContext.getRequestContext();
        
        Resource resource = this.repository.retrieve(
            securityContext.getToken(), requestContext.getResourceURI(), false);

        List<Value> propertyValues = new ArrayList<Value>();
        List<URL> editPropertiesServiceURLs =
            (this.editPropertyService != null) ? new ArrayList<URL>() : null;

        List<EnumerationPropertyDescriptor> applicablePropertyDescriptors = 
            new ArrayList<EnumerationPropertyDescriptor>();
        
        for (int i = 0; i < this.propertyDescriptors.length; i++) {
            if (!this.propertyDescriptors[i].isApplicableProperty(resource, securityContext.getPrincipal())) {
                continue;
            }
            applicablePropertyDescriptors.add(this.propertyDescriptors[i]);
            Namespace ns = Namespace.getNamespace(this.propertyDescriptors[i].getNamespace());
            Property prop = resource.getProperty(ns,
                                                 this.propertyDescriptors[i].getName());

            propertyValues.add((prop != null) ? prop.getValue() : null);

            if (this.editPropertyService != null) {
                Map<String, String> parameters = new HashMap<String, String>();
                parameters.put("namespace", this.propertyDescriptors[i].getNamespace());
                parameters.put("name", this.propertyDescriptors[i].getName());
                try {
                    URL url = this.editPropertyService.constructURL(
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

        resourcePropertiesModel.put("propertyDescriptors",
                                    applicablePropertyDescriptors);
        resourcePropertiesModel.put("propertyValues", propertyValues);
        resourcePropertiesModel.put("editPropertiesServiceURLs",
                                    editPropertiesServiceURLs);
        model.put(this.modelName, resourcePropertiesModel);
    }
    
    public String toString() {
        StringBuffer sb = new StringBuffer(this.getClass().getName());
        sb.append(" [ ");
        sb.append("modelName = ").append(this.modelName);
        sb.append(" ]");
        return sb.toString();
    }

}


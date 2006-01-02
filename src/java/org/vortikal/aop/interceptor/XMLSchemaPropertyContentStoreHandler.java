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

package org.vortikal.aop.interceptor;


import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;

import org.vortikal.repository.Property;
import org.vortikal.repositoryimpl.Resource;
import org.vortikal.security.PrincipalManager;
import org.vortikal.security.roles.RoleManager;
import org.vortikal.util.repository.ContentTypeHelper;
import org.vortikal.web.service.RepositoryAssertion;


/**
 * Content handler for maintaining a schema location property on XML
 * resources, based on their
 * <code>xsi:noNamespaceSchemaLocation</code> attribute in the body.
 *
 * <p>Configurable JavaBean properties:
 * <ul>
 *   <li><code>assertions</code> - an array of {@link
 *   RepositoryAssertion} objects. If set, this list of assertions are
 *   applied to the resource in question, and they all have to match
 *   for this content store handler to process the resource.
 *   <li><code>principalManager</code> - a {@link PrincipalManager}
 *   which is required when the <code>assertions</code> property is
 *   set.
 *   <li><code>roleManager</code> - a {@link RoleManager}
 *   which is required when the <code>assertions</code> property is
 *   set.
 * </ul>
 */
public class XMLSchemaPropertyContentStoreHandler 
  implements InitializingBean, ContentStoreHandler {

    private static Log logger = 
        LogFactory.getLog(XMLSchemaPropertyContentStoreHandler.class);
    
    private RepositoryAssertion[] assertions = null;
    private PrincipalManager principalManager = null;
    private RoleManager roleManager = null;
    

    private String schemaPropertyName = "schema";

    private String xmlSchemaAttributeNamespace = "http://www.w3.org/2001/XMLSchema-instance";
    private String xmlSchemaAttributeName = "noNamespaceSchemaLocation";
    
    

    public void setAssertions(RepositoryAssertion[] assertions) {
        this.assertions = assertions;
    }
    
    public void setPrincipalManager(PrincipalManager principalManager) {
        this.principalManager = principalManager;
    }
    
    public void setRoleManager(RoleManager roleManager) {
        this.roleManager = roleManager;
    }
    

    public void afterPropertiesSet() throws Exception {
        if (this.assertions != null) {
            if (this.principalManager == null || this.roleManager == null) {
                throw new BeanInitializationException(
                    "JavaBean property 'assertions' requires the properties "
                    + "'principalManager' and 'roleManager' to be specified");
            }
        }
    }


    public boolean isApplicableHandler(Resource resource) {
        if (! ContentTypeHelper.isXMLContentType(resource.getContentType())) {
            return false;
        }

        if (this.assertions != null) {

            org.vortikal.repository.Resource dto = null;
            try {
                
                dto = resource.getResourceDTO(null, this.principalManager, this.roleManager);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            for (int i = 0; i < this.assertions.length; i++) {
                if (!this.assertions[i].matches(dto, null)) {
                    return false;
                }
            }
        }
        return true;
    }
    


    public ByteArrayInputStream processContent(
        ByteArrayInputStream contentStream, Resource resource) {

        try {

            SAXBuilder builder = new SAXBuilder();
            Document doc = builder.build(contentStream);
            Element root = doc.getRootElement();

            Namespace ns = Namespace.getNamespace(this.xmlSchemaAttributeNamespace);
            String schemaLocation = root.getAttributeValue(this.xmlSchemaAttributeName, ns);
            
            Vector properties = resource.getProperties();
            List newProperties = new ArrayList();

            boolean foundInDocument = schemaLocation != null;
            boolean propertyAlreadyExisted = false;

            for (int i = 0; i < properties.size(); i++) {
                Vector prop = (Vector) properties.get(i);

                String namespace = (String) prop.get(0);
                String name = (String) prop.get(1);
                String value = (String) prop.get(2);
                
                Property newProp = new Property();
                newProp.setNamespace(namespace);
                newProp.setName(name);
                newProp.setValue(value);
                
                if (prop.get(0).equals(Property.LOCAL_NAMESPACE)
                    && prop.get(1).equals(this.schemaPropertyName)) {
                    propertyAlreadyExisted = true;
                    newProp.setValue(schemaLocation);
                    if (! (propertyAlreadyExisted && !foundInDocument))
                        newProperties.add(newProp);
                } else {
                    newProperties.add(newProp);
                }
            }


            if (!propertyAlreadyExisted && foundInDocument) {
                Property newProp = new Property();
                newProp.setNamespace(Property.LOCAL_NAMESPACE);
                newProp.setName(this.schemaPropertyName);
                newProp.setValue(schemaLocation);
                newProperties.add(newProp);
            } 


            resource.setProperties((Property[]) newProperties.toArray(new Property[0]));

        } catch (Throwable t) {

            if (logger.isDebugEnabled()) {
                logger.debug("Error extracting XML schema attribute from resource "
                             + resource.getURI(), t);

            } else if (logger.isInfoEnabled()) {
                logger.info("Error extracting XML schema attribute from resource "
                            + resource.getURI() + ": Message: " + t.getMessage());
            }

        } finally {
            contentStream.reset();
        }
        return contentStream;
    }
    
}








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

import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;

import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 * Resource property value reference data provider. Takes a list of
 * resource property namespaces and names, and puts their values in
 * the model under a set of configurable names.
 * 
 * 
 * <p>Configurable properties:
 * <ul>
 *  <li> <code>repository</code> - the content {@link Repository}
 *  <li> <code>modelNames</code> - array of the names to use for the submodels generated
 *  <li> <code>properties</code> - array of
 *       <code>namespace:name</code> {@link String strings} of the
 *       resource properties whose values will be put in the model. If
 *       a namespace has a value other than <code>null</code> (i.e. at
 *       least one <code>:</code> is present in the property string),
 *       that namespace is used for {@link Resource#getProperty
 *       retrieving} a resource {@link Property}. Otherwise, the name
 *       maps to standard properties in this manner:
 *       <ul>
 *         <li><code>characterEncoding</code> - {@link Resource#getCharacterEncoding}
 *         <li><code>contentLanguage</code> - {@link Resource#getContentLanguage}
 *         <li><code>contentLastModified</code> - {@link Resource#getContentLastModified}
 *         <li><code>contentLength</code> - {@link Resource#getContentLength}
 *         <li><code>contentModifiedBy</code> - {@link Resource#getContentModifiedBy}
 *         <li><code>contentType</code> - {@link Resource#getContentType}
 *         <li><code>creationTime</code> - {@link Resource#getCreationTime}
 *         <li><code>displayName</code> - {@link Resource#getDisplayName}
 *         <li><code>lastModified</code> - {@link Resource#getLastModified}
 *         <li><code>name</code> - {@link Resource#getName}
 *         <li><code>propertiesLastModified</code> - {@link Resource#getPropertiesLastModified}
 *         <li><code>propertiesModifiedBy</code> - {@link Resource#getPropertiesModifiedBy}
 *         <li><code>uri</code> - {@link Resource#getURI}
 *         <li><code>isCollection</code> - {@link Resource#isCollection}
 *       </ul>
 *  <li><code>localizationKeys</code> - array of the names of
 *      localization keys. If such a key is specified for a property,
 *      instead of providing the regular value in the model, a
 *      localized message with the specified key and the value as
 *      parameter (with the default being the value itself) is looked
 *      up and provided in the model.  For example, if
 *      <code>namespace</code> is <code>null</code>, <code>name</code>
 *      is <code>displayName</code> and <code>localizationKey</code>
 *      is <code>foo.bar</code>, the value provided in the model will
 *      be (in pseudo-code):
 *      <code>getLocalizedMessage(resource.getProperty("displayName").getValue(),
 *      value)</code>.
 * </ul>
 * 
 * <p>Model data provided:
 * <ul>
 *   <li>for every property in the <code>properties</code>
 *   configuration variable, the value of that property, under a name
 *   specified by <code>modelNames</code>, or <code>null</code> if the
 *   property does not exist.
 * </ul>
 * 
 */
public class ResourcePropertiesValueProvider implements Provider, InitializingBean {

    private static Log logger = LogFactory.getLog(ResourcePropertiesValueProvider.class);

    private Repository repository = null;
    private String[] properties = null;

    private String[] namespaces = null;
    private String[] names = null;

    private String[] localizationKeys = null;
    private String[] modelNames = null;



    public void setRepository(Repository repository) {
        this.repository = repository;
    }
    

    public void setModelNames(String[] modelNames) {
        this.modelNames = modelNames;
    }
    

    public void setProperties(String[] properties) {
        this.properties = properties;
    }
    

    public void setLocalizationKeys(String[] localizationKeys) {
        this.localizationKeys = localizationKeys;
    }
    

    public void afterPropertiesSet() {
        if (this.repository == null) {
            throw new BeanInitializationException(
                "Bean property 'repository' must be set");
        }
        if (this.modelNames == null) {
            throw new BeanInitializationException(
                "Bean property 'modelNames' must be set");
        }
        if (this.properties == null) {
            throw new BeanInitializationException(
                "Bean property 'properties' must be set");
        }

        if (this.modelNames.length != this.properties.length) {
            throw new BeanInitializationException(
                "Properties 'modelNames' and 'properties' "
                + "must be of the same size");
        }

        if (this.localizationKeys != null
            && this.localizationKeys.length != this.properties.length) {
            throw new BeanInitializationException(
                "Property 'localizationKeys' must be of the same size "
                + "as 'properties' and 'modelNames'");
        }

        splitProperties();
    }
    


    public void referenceData(Map model, HttpServletRequest request)
            throws Exception {

        RequestContext requestContext = RequestContext.getRequestContext();
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        
        Principal principal = securityContext.getPrincipal();
        Resource resource = repository.retrieve(securityContext.getToken(),
                                                requestContext.getResourceURI(),
                                                true);
        for (int i = 0; i < this.properties.length; i++) {

            Object value = null;
            
            if (this.namespaces[i] != null) {

                Property property = resource.getProperty(this.namespaces[i], this.names[i]);
                if (property != null) {
                    value = property.getValue();
                }
            
            } else {
                value = getStandardPropertyValue(resource, this.names[i]);
            }

            if (value != null && this.localizationKeys != null && this.localizationKeys[i] != null) {
                org.springframework.web.servlet.support.RequestContext springContext =
                    new org.springframework.web.servlet.support.RequestContext(request);
                value = springContext.getMessage(this.localizationKeys[i],
                                                 new Object[] {value},
                                                 value.toString());
            }

            if (logger.isDebugEnabled()) {
                logger.debug(
                    "Publishing resource property: (" + this.namespaces[i] + ": "
                    + this.names[i] + ")" + " with value '" + value + "' "
                    + "in model as '" + this.modelNames[i] + "'");
            }

        
            model.put(this.modelNames[i], value);
        }
    }


    private Object getStandardPropertyValue(Resource resource, String propertyName) {

        if ("characterEncoding".equals(propertyName)) {
            return resource.getCharacterEncoding();
            
        } else if ("contentLanguage".equals(propertyName)) {
            return resource.getContentLanguage();
            
        } else if ("contentLastModified".equals(propertyName)) {
            return resource.getContentLastModified();

        } else if ("contentLength".equals(propertyName)) {
            return new Long(resource.getContentLength());

        } else if ("contentModifiedBy".equals(propertyName)) {
            return resource.getContentModifiedBy();
            
        } else if ("contentType".equals(propertyName)) {
            return resource.getContentType();
            
        } else if ("creationTime".equals(propertyName)) {
            return resource.getCreationTime();
            
        } else if ("displayName".equals(propertyName)) {
            return resource.getDisplayName();
            
        } else if ("lastModified".equals(propertyName)) {
            return resource.getLastModified();
            
        } else if ("name".equals(propertyName)) {
            return resource.getName();
            
        } else if ("propertiesLastModified".equals(propertyName)) {
            return resource.getPropertiesLastModified();
            
        } else if ("propertiesModifiedBy".equals(propertyName)) {
            return resource.getPropertiesModifiedBy();
            
        } else if ("uri".equals(propertyName)) {
            return resource.getURI();
            
        } else if ("isCollection".equals(propertyName)) {
            return new Boolean(resource.isCollection());

        } else {
            return null;
        }
    }
    

    private void splitProperties() {

        this.namespaces = new String[this.properties.length];
        this.names = new String[this.properties.length];

        
        for (int i = 0; i < this.properties.length; i++) {

            String namespace = null;
            String name = this.properties[i];

            if (this.properties[i].indexOf(":") != -1) {

                namespace = this.properties[i].substring(
                    0, this.properties[i].lastIndexOf(":"));

                name = this.properties[i].substring(
                    this.properties[i].lastIndexOf(":") + 1);
            }

            this.namespaces[i] = namespace;
            this.names[i] = name;
        }
    }

}

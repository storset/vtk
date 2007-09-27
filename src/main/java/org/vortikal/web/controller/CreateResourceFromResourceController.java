/* Copyright (c) 2004, 2007, University of Oslo, Norway
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
package org.vortikal.web.controller;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;
import org.vortikal.xml.TransformerManager;


/**
 * Controller that creates a resource from another resource using
 * XSLT transformation.
 * 
 * <p>Configurable properties:
 * <ul>
 *   <li><code>repository</code> - the {@link Repository} (required)
 *   <li><code>resourceName</code> - Create the new resource with this name
 * 	 <li><code>transformerManager</code> - a {@link TransformerManager} (required)
 * 	 <li><code>stylesheetIdentifier</code> - An identifier for the
 * 	 XSL Stylesheet to use (required)
 * 	 <li><code>successView</code> - the success view name (required)
 * 	 <li><code>errorView</code> - the error view (required)
 * 	 <li><code>initialResourceProperties</code> - a map from
 * 	 {@link PropertyTypeDefinition} to {@link Value} objects, listing
 * 	 initial properties that are set on the created resource.
 * 	 <li><code>resourceAlreadyExistsMessageKey</code> - the
 * 	 localization key to put in the model when a resource of the
 * 	 target name already exists.
 * </ul>
 * </p>
 *
 * <p>Model data provided:
 * <ul>
 *   <li><code>resource</code> - the resource to redirect to on success
 *   <li><code>error</code> - error message on error
 * </ul>
 */
public class CreateResourceFromResourceController implements Controller,
        InitializingBean {

    private Log logger = LogFactory.getLog(this.getClass());

    private Repository repository;
    private String resourceName;
    private TransformerManager transformerManager;
    private String stylesheetIdentifier;
    private String errorView;
    private String successView;
    private Map initialResourceProperties = new HashMap();
    private String resourceAlreadyExistsMessageKey = "manage.create.document.exists";
    

    public void setErrorView(String errorView) {
        this.errorView = errorView;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public void setStylesheetIdentifier(String stylesheetIdentifier) {
        this.stylesheetIdentifier = stylesheetIdentifier;
    }

    public void setSuccessView(String successView) {
        this.successView = successView;
    }

    public void setTransformerManager(TransformerManager transformerManager) {
        this.transformerManager = transformerManager;
    }

    public void setResourceAlreadyExistsMessageKey(String resourceAlreadyExistsMessageKey) {
        this.resourceAlreadyExistsMessageKey = resourceAlreadyExistsMessageKey;
    }
    

    public void setInitialResourceProperties(Map initialResourceProperties) {
        if (initialResourceProperties == null) return;

        for (Iterator i = initialResourceProperties.keySet().iterator(); i.hasNext();) {
            Object key = i.next();
            if (!(key instanceof PropertyTypeDefinition)) 
                throw new IllegalArgumentException("All keys must be of class "
                                                   + PropertyTypeDefinition.class.getName());
            Object value = initialResourceProperties.get(key);
            if (!(value instanceof Value)) 
                throw new IllegalArgumentException("All values must be of class "
                                                   + Value.class.getName());
        }
        this.initialResourceProperties = initialResourceProperties;
    }
    

    public void afterPropertiesSet() throws Exception {
        if (this.repository == null) 
            throw new BeanInitializationException("Property 'repository' required");
        
        if (this.resourceName == null)
            throw new BeanInitializationException("Property 'resourceName' required");
    
        if (this.transformerManager == null)
            throw new BeanInitializationException("Property 'transformerManager' required");
        
        if (this.stylesheetIdentifier == null)
            throw new BeanInitializationException("Property 'stylesheetIdentifier' required");

        if (this.successView == null)
            throw new BeanInitializationException("Property 'successView' required");

        if (this.errorView == null)
            throw new BeanInitializationException("Property 'errorView' required");

    }


    public ModelAndView handleRequest(HttpServletRequest req,
            HttpServletResponse resp) throws Exception {

        Map model = new HashMap();

        String uri = RequestContext.getRequestContext().getResourceURI();
        String token = SecurityContext.getSecurityContext().getToken();

        Resource resource = this.repository.retrieve(token, uri, false);
        String newResourceUri = uri.substring(0, uri.lastIndexOf("/") + 1)
                + this.resourceName;

        boolean exists = this.repository.exists(token, newResourceUri);
        if (exists) {
            model.put("createErrorMessage", this.resourceAlreadyExistsMessageKey);
            return new ModelAndView(this.errorView, model);
        }

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        InputStream inStream = this.repository.getInputStream(token, uri, true);

        Transformer transformer = 
            this.transformerManager.getTransformer(this.stylesheetIdentifier);

        transformer.transform(new StreamSource(inStream), 
                new StreamResult(outStream));

        InputStream in = new ByteArrayInputStream(outStream.toByteArray());

        Resource newResource = this.repository.createDocument(token, newResourceUri);
        this.repository.storeContent(token, newResourceUri, in);

        if (this.initialResourceProperties != null) {
            newResource = this.repository.retrieve(token, newResourceUri, true);
            for (Iterator i = this.initialResourceProperties.keySet().iterator(); i.hasNext();) {
                PropertyTypeDefinition def = (PropertyTypeDefinition) i.next();
                Value value = (Value) this.initialResourceProperties.get(def);
                Property prop = newResource.createProperty(def.getNamespace(), def.getName());
                prop.setValue(value);
            }
            this.repository.store(token, newResource);
        }

        Resource parent = null;
        parent = this.repository.retrieve(token, resource.getParent(), false);
        model.put("resource", parent);

        return new ModelAndView(this.successView, model);
    }


}

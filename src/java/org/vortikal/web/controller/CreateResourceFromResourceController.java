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
package org.vortikal.web.controller;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;
import org.vortikal.xml.StylesheetCompilationException;
import org.vortikal.xml.TransformerManager;


/**
 * Controller that creates a resource from another resource.
 * TODO: Make this generic
 * 
 * <p>Configurable properties:
 * <ul>
 *   <li><code>repository</code> - the repository is required</li>
 *   <li><code>resourceName</code> - Create new resource with this name</li>
 * 	 <li><code>transformerManager</code> - required
 * 	 <li><code>stylesheetIdentifier</code> - An identifier for the XSL Stylesheet 
 *   to use - required
 * 	 <li><code>successView</code> - default is 'redirect'
 * 	 <li><code>errorView</code> - default is 'admin'
 * </ul>
 * </p>
 *
 * <p>Model data provided:
 * <ul>
 *   <li><code>resource</code> - the resource to redirect to on success
 * 	 <li><code>error</code> - error message on error
 * </ul>
 */
public class CreateResourceFromResourceController implements Controller,
        InitializingBean {

    private Repository repository;
    private String resourceName;
    private TransformerManager transformerManager;
    private String stylesheetIdentifier;
    private String errorView;
    private String successView;
    
    public ModelAndView handleRequest(HttpServletRequest req,
            HttpServletResponse resp) throws Exception {

        Map model = new HashMap();

        String uri = RequestContext.getRequestContext().getResourceURI();
        String token = SecurityContext.getSecurityContext().getToken();

        Resource resource = repository.retrieve(token, uri, false);
        String newResourceUri = uri.substring(0, uri.lastIndexOf("/") + 1)
                + resourceName;

        boolean exists = repository.exists(token, newResourceUri);
        if (exists) {
            model.put("createErrorMessage", "minutes.exists");
            return new ModelAndView(errorView, model);
        }

        // repository.lock(token,newResourceUri,Lock.LOCKTYPE_EXCLUSIVE_WRITE,"ownerInfo","0",5);

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        InputStream inStream = repository.getInputStream(token, uri, true);

        Transformer transformer = transformerManager
                .getTransformer(stylesheetIdentifier);

        transformer.transform(new StreamSource(inStream), new StreamResult(
                outStream));

        InputStream in = new ByteArrayInputStream(outStream.toByteArray());

        Resource newResource = repository.createDocument(token, newResourceUri);

        String namespace = "http://www.uio.no/vortex/custom-properties";

        // Setting DAV-properties for webedit and transform-view til yes
        Property p = newResource.createProperty(namespace, "web-edit");
        p.setStringValue("yes");

        p = newResource.createProperty(namespace, "transform-view");
        p.setStringValue("yes");

        p = newResource.createProperty(namespace, "visual-profile");
        p.setStringValue("yes");

        repository.store(token, newResource);
        repository.storeContent(token, newResourceUri, in);

        Resource parent = null;
        String parentUri = resource.getParent();
        parent = repository.retrieve(token, parentUri, false);
        model.put("resource", parent);

        return new ModelAndView(successView, model);
    }

    public void afterPropertiesSet() throws Exception {
        if (repository == null) 
            throw new BeanInitializationException("Property 'repository' required");
        
        if (resourceName == null)
            throw new BeanInitializationException("Property 'resourceName' required");
    
        if (transformerManager == null)
            throw new BeanInitializationException("Property 'transformerManager' required");
        
        if (stylesheetIdentifier == null)
            throw new BeanInitializationException("Property 'stylesheetIdentifier' required");

        if (successView == null)
            throw new BeanInitializationException("Property 'successView' required");

        if (errorView == null)
            throw new BeanInitializationException("Property 'errorView' required");

        try {
            transformerManager.getTransformer(stylesheetIdentifier);
        } catch (StylesheetCompilationException e) {
            throw new BeanInitializationException("Stylesheet '" + stylesheetIdentifier
                    + "' didn't compile", e);
        } catch (Exception e) {
            throw new BeanInitializationException("Error trying to compile stylesheet '" + stylesheetIdentifier
                    + "'", e);
        }
    }

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

}

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

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.TransformerConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jdom.Document;
import org.jdom.input.SAXBuilder;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.servlet.mvc.LastModified;

import org.vortikal.repository.Repository;
import org.vortikal.repository.RepositoryException;
import org.vortikal.repository.Resource;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;
import org.vortikal.xml.StylesheetCompilationException;
import org.vortikal.xml.TransformerManager;

/**
 * Controller that fetches an XML resource from the repository and
 * puts it in the model.
 * 
 * <p>Configurable properties:
 * <ul>
 *   <li><code>repository</code> - the content repository
 *   <li><code>transformerManager</code> - the XSLT stylesheet manager
 *       (required)
 *   <li><code>childName</code> - if this optional property is set, it
 *     is appended to the current resource URI when retrieving the
 *     resource to display. This is typically used when displaying
 *     index resources (i.e. <code>childName = 'index.xml'</code>, for
 *     instance).
 *   <li><code>handleLastModified</code> - whether to return the real
 *       last modified value of the resource, or <code>-1</code> (the
 *       default)
 * </ul>
 *
 * <p>Model data provided:
 * <ul>
 *   <li><code>resource</code> - the
 *   <code>org.vortikal.repository.Resource</code></li>
 *   <li><code>jdomDocument</code> - the <code>org.jdom.Document</code>
 *       representation of the resource.</li>
 * </ul>
 *
 */
public class DisplayXmlResourceController
  implements Controller, LastModified, InitializingBean {

    private static Log logger = LogFactory.getLog(DisplayXmlResourceController.class);

    public static final String DEFAULT_VIEW_NAME = "transformXmlResource";

    private Repository repository;
    private TransformerManager transformerManager;
    private String childName;
    private String viewName = DEFAULT_VIEW_NAME;
    private boolean handleLastModified = false;
    

    public void setChildName(String childName) {
        this.childName = childName;
    }

	
    public void setRepository(Repository repository) {
        this.repository = repository;
    }


    public void setTransformerManager(TransformerManager transformerManager) {
        this.transformerManager = transformerManager;
    }
    

    public void setViewName(String viewName) {
        this.viewName = viewName;
    }


    public void setHandleLastModified(boolean handleLastModified) {
        this.handleLastModified = handleLastModified;
    }
    

    /**
     * Describe <code>afterPropertiesSet</code> method here.
     *
     * @exception Exception if an error occurs
     */
    public void afterPropertiesSet() throws Exception {
        if (repository == null) {
            throw new BeanInitializationException("Property 'repository' not set");
        }
        if (transformerManager == null) {
            throw new BeanInitializationException("Property 'transformerManager' not set");
        }
    }


    public long getLastModified(HttpServletRequest request) {
        
        if (!this.handleLastModified) {
            return -1;
        }

        SecurityContext securityContext = SecurityContext.getSecurityContext();
        RequestContext requestContext = RequestContext.getRequestContext();

        String uri = requestContext.getResourceURI();

        if (childName != null) 
            uri += (uri.equals("/")) ? childName : "/" + childName;
        
        Resource resource = null;

        try {
            resource = repository.retrieve(
                securityContext.getToken(), uri, true);
        } catch (RepositoryException e) {
            // These exceptions are expected
            return -1;

        } catch (Throwable t) {
            if (logger.isInfoEnabled()) {
                logger.info(
                    "Unable to get the last modified date for resource "
                    + uri, t);
            }
            return -1;
        }
        
        if (resource.isCollection()) {
            return -1;
        }
        
        return resource.getLastModified().getTime();
    }
    

    public ModelAndView handleRequest(HttpServletRequest request,
                                      HttpServletResponse response) throws Exception {
		
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        RequestContext requestContext = RequestContext.getRequestContext();

        String uri = requestContext.getResourceURI();

        if (childName != null) 
            uri += (uri.equals("/")) ? childName : "/" + childName;
        
        String token = securityContext.getToken();

        Map model = new HashMap();

        Resource resource = repository.retrieve(token, uri, true);

        if (resource.isCollection()) {
            throw new IllegalStateException(
                "Unable to display collections");
        }
        	
        InputStream stream = repository.getInputStream(token, uri, true);
        
        // Build a JDOM tree of the input stream:
        Document document = null;
        try {
                
            SAXBuilder builder = new SAXBuilder();
            document = builder.build(stream);
            document.setBaseURI(uri);
            
        } catch (Exception e) {            
            // FIXME: error handling
            throw new ServletException(
                "Unable to build JDOM document from input stream", e);
        }            

        try {
            transformerManager.getTransformer(resource, document);
        } catch (IOException e) {
            // FIXME: error handling
        } catch (TransformerConfigurationException e) {
            // FIXME: error handling
        } catch (StylesheetCompilationException e) {
            // FIXME: error handling
        }

        model.put("resource", resource);
        model.put("jdomDocument", document);
               
        return new ModelAndView(viewName, model);
    }

}

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

package org.vortikal.web.controller.repository.tidy;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;
import org.vortikal.util.repository.URIUtil;


public class ResourceTransformer implements Controller, InitializingBean {
    
    private static Log logger = LogFactory.getLog(ResourceTransformer.class);
    
    // Private non-configurable variables
    private static final String JTIDY_TRANSFORMER = "JTidyTransformer";
    private final String HTML_TO_XHTML = "htmlToXhtml";
    private final String KUPU_EXTENSION = "-kupu.html";
    
    private Transformer transformer;
        
    // Configurable variables
    private Repository repository;
    private String trustedToken;
    private String transformerType;  // which transformerImpl to use
    private String transformation; // which transformation to actually perform
    
    private String errorView = "admin";
    private String successView = "redirectToManage";
    //private String successView = "manageCollectionListing";
    
    
    
    private Transformer createTransformer(String transformerType) {
        // can so far only perform default transformation 'htmlToXhtml'
        TransformerFactory transformerFactory = new TransformerFactory();
        
        if (JTIDY_TRANSFORMER.equals(transformerType)) {
            logger.debug("Created JTidyTransformer");
            return transformerFactory.createJTidyTransformer();
        } else {
            logger.error("Invalid transformer type");
            return null;
        }
    }
    
    
    
    public synchronized ModelAndView handleRequest(HttpServletRequest req,
            HttpServletResponse resp) throws Exception {
        
        Map model = new HashMap();
        String uri = RequestContext.getRequestContext().getResourceURI();
        
        String token = trustedToken;
        if (token == null)
            token = SecurityContext.getSecurityContext().getToken();
                
        transformer = createTransformer(transformerType);
                
        // JTidyTransformation:
        
        if ( HTML_TO_XHTML.equals(transformation) ) {
            
            logger.debug("transformer == JTIDY_TRANSFORMER");
            
            String newUri = jtidyHelper(model, uri);
            if (newUri == null) {
                // error message is created by jtidyHelper()
                return new ModelAndView(errorView, model);
            }
            
            boolean exists = repository.exists(token, newUri);

            if (exists) {
                /**
                 * XXX
                 * 
                 * FIXME: i18n for feilmelding! 
                 */
                model.put("createErrorMessage", "resource.alreadyKupufied");
                return new ModelAndView(errorView, model);
            }
            
            InputStream is = repository.getInputStream(trustedToken, uri, true);
            
            repository.copy(token, uri, newUri, "0", false, true);
            repository.storeContent( token, newUri, transformer.transform(is, transformation) );
                        
            
            // Setter heller redirect til parent (dvs mappen som ressursene ligger i)
            //Resource newResource = repository.retrieve(trustedToken, newUri, true);
            //model.put("resource", newResource);
                        
            String parentCollectionURI = URIUtil.getParentURI(uri);
            Resource parentCollection = this.repository.retrieve(trustedToken, parentCollectionURI, true);
            
            /**
             * FIXME: Virker ikke hvis man bruker redirect view da det kun tar med ressursen, ikke messages
             */ 
            //model.put("infoMessage", "balla jazzhus");
            //model.put("statusMessage", "balla jazzhus");
            
            model.put("resource", parentCollection);
            
        } else {
            return new ModelAndView(errorView, model);
        }
        
        return new ModelAndView(successView, model);        
    }


    
    /*
     * Helper methods
     */
    private String jtidyHelper(Map model, String uri) {
        String newUri;        
        if (uri.endsWith(".html") || uri.endsWith(".htm") ) {
            newUri = uri.substring(0, uri.lastIndexOf('.')) + KUPU_EXTENSION;
            logger.debug("Creating new XHTML file '" + newUri + "'");
        } else {
            /**
             * XXX
             * 
             * FIXME: i18n for feilmelding! 
             */
            model.put("createErrorMessage", "resource.notHTML");
            return null;
        }
        return newUri;
    }
    
    
    public void afterPropertiesSet() throws Exception {
        if (repository == null) 
            throw new BeanInitializationException("Property 'repository' required");
    }


    /*
     * Public setters for configurable parameters
     */
    
    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public void setTrustedToken(String trustedToken) {
        this.trustedToken = trustedToken;
    }

    public void setTransformerType(String transformation) {
        this.transformerType = transformation;
    }

    public void setTransformation(String transformation) {
        this.transformation = transformation;
    }

    
    public void setErrorView(String errorView) {
        this.errorView = errorView;
    }

    public void setSuccessView(String successView) {
        this.successView = successView;
    }
    
} // end of class ResourceTransformer

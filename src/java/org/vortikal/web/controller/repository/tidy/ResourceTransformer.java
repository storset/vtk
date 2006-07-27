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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
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


public class ResourceTransformer implements Controller, InitializingBean {
    
    private static Log logger = LogFactory.getLog(ResourceTransformer.class);
    
    // Private non-configurable variables
    private static final String JTIDY_TRANSFORMER = "JTidyTransformer";
    private final String HTML_TO_XHTML = "htmlToXhtml";
    private final String KUPU_EXTENSION = "-kupu.html";
    
    private Transformer transformer;
        
    // Configurable variables
    private String trustedToken;
    private Repository repository;
//    private String resourceName;
    //private String templateUri;
    private String errorView = "admin";
    private String successView = "redirect";
    
    // This is the only one with a setter, so far...
    private String transformerType;  // which transformerImpl to use
    private String transformation; // which transformation to actually perform

    
    /*
    public ResourceTransformer(String type) {
        TransformerFactory transformerFactory = new TransformerFactory();
        
        if (JTIDY_TRANSFORMER.equals(type)) {
            transformer = transformerFactory.createJTidyTransformer();
            transformerType = HTML_TO_XHTML;
            // can so far only perform default transformation 'htmlToXhtml'
        } else {
            logger.error("Invalid transformer type");
        }
    }
    */
    

    
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
        
        logger.debug("### Inside handleRequest");

        Map model = new HashMap();
        String uri = RequestContext.getRequestContext().getResourceURI();
        
        String token = trustedToken;
        if (token == null)
            token = SecurityContext.getSecurityContext().getToken();
        
        /*
        String name = resourceName;
        if (name == null) {
            Resource template = repository.retrieve(token, templateUri,false);
            name = template.getName();
        }
        */
        
        
        transformer = createTransformer(transformerType);
        
        
        // JTidyTransformation:
        
        if ( HTML_TO_XHTML.equals(transformation) ) {
            
            logger.debug("transformer == JTIDY_TRANSFORMER");
            
            String newUri = jtidyHelper(model, uri);
            if (newUri == null) {
                // error message is created by jtidyHelper()
                return new ModelAndView(errorView, model);
            }
            
            /*
            // ensure the uri won't start with '//'
            String newResourceUri = "/" + name;
            if (! uri.equals("/")) {
                newResourceUri = uri + newResourceUri;
            }
            */

            //boolean exists = repository.exists(token, newResourceUri);
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
            
            //repository.createDocument(token, newUri);
            repository.storeContent( token, newUri, transformer.transform(is, transformation) );
            
            
            Resource newResource = repository.retrieve(trustedToken, newUri, true);
            model.put("resource", newResource);
            
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
        
//        if (trustedToken == null) 
//            throw new BeanInitializationException("Property 'trustedToken' required");
        
//        if (templateUri == null)
//            throw new BeanInitializationException("Property 'templateUri' required");
        
//        if (! (trustedToken == null || repository.exists(trustedToken,templateUri)))
            //throw new BeanInitializationException("Property 'templateUri' must specify an existing resource");
//            logger.warn("Property 'templateUri' must specify an existing resource");
    }


    /*
     * Public setters for configurable parameters
     */

    public void setErrorView(String errorView) {
        this.errorView = errorView;
    }


    public void setRepository(Repository repository) {
        this.repository = repository;
    }


    public void setSuccessView(String successView) {
        this.successView = successView;
    }

    public void setTransformerType(String transformation) {
        this.transformerType = transformation;
    }

    public void setTransformation(String transformation) {
        this.transformation = transformation;
    }

    public void setTrustedToken(String trustedToken) {
        this.trustedToken = trustedToken;
    }

} // end of class ResourceTransformer

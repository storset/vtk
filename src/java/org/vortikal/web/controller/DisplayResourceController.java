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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.servlet.mvc.LastModified;
import org.vortikal.repository.Repository;
import org.vortikal.repository.RepositoryException;
import org.vortikal.repository.Resource;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;


/**
 * Controller that provides the requested resource and its input
 * stream in the model.
 *
 * <p>Configurable properties:
 * <ul>
 *   <li><code>repository</code> - the repository is required</li>
 *   <li><code>childName</code> - if childName is set and the current
 *       resource is a collection, the child resource of that name is
 *       retrieved instead of the requested resource</li>
 *   <li><code>streamToString</code> - set this to true if you
 *       want to provide the resource as resourceString model data
 *       instead of resourceStream if it's content type matches 'text/*' 
 *   <li><code>viewName</code> - name of the returned view. The
 *       default value is <code>displayResource</code></li>
 *   <li><code>unsupportedResourceView</code> - name of returned view if
 *       the resource type is unsupported. Default value is <code>HTTP_STATUS_NOT_FOUND</code> 
 *   <li><code>unsupportedResourceTypes</code> - list of content types that should return
 *       <code>unsupportedResourceView</code>. Default is 
 *       <code>application/x-vortex-collection</code> 
 *   <li><code>displayProcessed</code> - weither the resource should be retrieved
 *       for processing (uio:readProcessed) or for raw access (dav:read). Defaults 
 *       to false.
 * </ul>
 * </p>
 *
 * <p>Model data provided:
 * <ul>
 *   <li><code>resource</code> - the resource object</li>
 *   <li><code>resourceStream</code> - the input stream of the
 *       resource. (Note: be sure to couple this controller with a
 *       view that closes this stream)</li>
 *   <li><code>resourceString</code> - a string representation of the resource
 *       if <code>streamToString</code> is set and it's a text resource.
 * </ul>
 */
public class DisplayResourceController 
    implements Controller, LastModified, InitializingBean {

    public static final String DEFAULT_VIEW_NAME = "displayResource";
    private static final String defaultCharacterEncoding = "utf-8";

    private boolean displayProcessed = false;

    private static Log logger = LogFactory.getLog(DisplayResourceController.class);

    private Repository repository;
    private String childName;
    private String viewName = DEFAULT_VIEW_NAME;
    private String unsupportedResourceView = "HTTP_STATUS_NOT_FOUND";
    private Set unsupportedResourceTypes ;
    private boolean streamToString = false;
    
    /**
     * @param childName The childName to set.
     */
    public void setChildName(String childName) {
        this.childName = childName;
    }


    /**
     * @param repository The repository to set.
     */
    public void setRepository(Repository repository) {
        this.repository = repository;
    }


    public void setViewName(String viewName) {
        this.viewName = viewName;
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

        Resource resource = repository.retrieve(token, uri, displayProcessed);

        if (unsupportedResourceTypes.contains(resource.getContentType())) {
            return new ModelAndView(unsupportedResourceView);
        }

        model.put("resource", resource);

        InputStream stream = repository.getInputStream(token, uri, true);

        if (!streamToString || !resource.getContentType().startsWith("text/")) {
            model.put("resourceStream", stream);
            return new ModelAndView(this.viewName, model);
        }

            
        // Provide as string instead of stream
        String characterEncoding = resource.getCharacterEncoding();
            if (characterEncoding == null) {
                characterEncoding = defaultCharacterEncoding;
            }

            ByteArrayOutputStream contentStream = new ByteArrayOutputStream();
            try {
                
                byte[] buffer = new byte[5000];
                int n = 0;
                while (((n = stream.read(buffer, 0, 5000)) > 0)) {
                    contentStream.write(buffer, 0, n);
                }
            } finally {
                if (stream != null) stream.close();
            }

            String content = new String(contentStream.toByteArray(),
                                        characterEncoding);
            model.put("resourceString", content);
        
            return new ModelAndView(this.viewName, model);
    }


    public long getLastModified(HttpServletRequest request) {

        SecurityContext securityContext = SecurityContext.getSecurityContext();
        RequestContext requestContext = RequestContext.getRequestContext();

        Resource resource = null;
        
        String uri = requestContext.getResourceURI();
        
        if (childName != null) {
            uri = requestContext.getResourceURI();
            uri += (uri.equals("/")) ? childName : "/" + childName;

        }
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

    public void setStreamToString(boolean streamToString) {
        this.streamToString = streamToString;
    }

    public void setDisplayProcessed(boolean displayProcessed) {
        this.displayProcessed = displayProcessed;
    }


    /**
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    public void afterPropertiesSet() throws Exception {
        if (unsupportedResourceTypes == null) {
            unsupportedResourceTypes = new HashSet();
            unsupportedResourceTypes.add("application/x-vortex-collection");
        }
    }
}

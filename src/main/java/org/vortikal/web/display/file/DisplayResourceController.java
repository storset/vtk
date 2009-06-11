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
package org.vortikal.web.display.file;

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
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.servlet.mvc.LastModified;
import org.vortikal.repository.Path;
import org.vortikal.repository.Repository;
import org.vortikal.repository.RepositoryException;
import org.vortikal.repository.Resource;
import org.vortikal.repository.ResourceNotModifiedException;
import org.vortikal.security.AuthenticationException;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;
import org.vortikal.webdav.PreconditionFailedException;
import org.vortikal.webdav.ifheader.IfMatchHeader;
import org.vortikal.webdav.ifheader.IfNoneMatchHeader;


/**
 * Controller that provides the requested resource and its input
 * stream in the model.
 *
 * <p>Configurable JavaBean properties:
 * <ul>
 *   <li><code>repository</code> - the {@link Repository content
 *       repository}</li>
 *   <li><code>childName</code> - if childName is set and the current
 *       resource is a collection, the child resource of that name is
 *       retrieved instead of the requested resource
 *   <li><code>streamToString</code> - set this to true if you
 *       want to provide the resource as resourceString model data
 *       instead of resourceStream if the content type is text based 
 *   <li><code>viewName</code> - name of the returned view. The
 *       default value is <code>displayResource</code>
 *   <li><code>view</code> - the actual {@link View} object (overrides
 *   <code>viewName</code>.
 *   <li><code>unsupportedResourceView</code> - name of returned view
 *       if the resource type is unsupported. Default value is
 *       <code>HTTP_STATUS_NOT_FOUND</code>
 *   <li><code>unsupportedResourceTypes</code> - list of content types
 *       that should return <code>unsupportedResourceView</code>.
 *   <li><code>displayProcessed</code> - wether the resource should be
 *       retrieved for processing (uio:readProcessed) or for raw
 *       access (dav:read). Defaults to false.
 *   <li><code>ignoreLastModified</code> - wether or not to ignore the
 *       resource's <code>lastModified</code> value. Setting this
 *       property to <code>true</code> means that the resource content
 *       cannot be cached by the client. Default is
 *       <code>false</code>.
 *   <li><code>ignoreLastModifiedOnCollections</code> - wether or not to ignore the
 *       resource's <code>lastModified</code> value when the resource is a collection.
 *       Default is <code>false</code>.
 *   <li><code>supportIfHeaders</code> - wether or not to look for If-Match and 
 *   If-None-Match headers. Default is <code>false</code>.</li>
 * </ul>
 * </p>
 *
 * <p>Model data provided:
 * <ul>
 *   <li><code>resource</code> - the {@link Resource} object</li>
 *   <li><code>resourceStream</code> - the {@link InputStream} of the
 *       resource. (Note: be sure to couple this controller with a
 *       view that closes this stream)</li>
 *   <li><code>resourceString</code> - a {@link String} representation
 *       of the resource if <code>streamToString</code> is set and
 *       it's a text resource.
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
    private View view = null;
    private String unsupportedResourceView = "HTTP_STATUS_NOT_FOUND";
    private Set<String> unsupportedResourceTypes = null;
    private boolean streamToString = false;
    private boolean ignoreLastModified = false;
    private boolean ignoreLastModifiedOnCollections = false;
    private boolean supportIfHeaders = false;
    
    public void setChildName(String childName) {
        this.childName = childName;
    }


    public void setRepository(Repository repository) {
        this.repository = repository;
    }


    public void setViewName(String viewName) {
        this.viewName = viewName;
    }
    

    public void setView(View view) {
        this.view = view;
    }
    
    public void setSupportIfHeaders(boolean supportIfHeaders) {
        this.supportIfHeaders = supportIfHeaders;
    }

    public void setIgnoreLastModified(boolean ignoreLastModified) {
        this.ignoreLastModified = ignoreLastModified;
    }
    

    public void setIgnoreLastModifiedOnCollections(boolean ignoreLastModifiedOnCollections) {
        this.ignoreLastModifiedOnCollections = ignoreLastModifiedOnCollections;
    }
    

    public void setUnsupportedResourceTypes(Set<String> unsupportedResourceTypes) {
        this.unsupportedResourceTypes = unsupportedResourceTypes;
    }
    

    public void setStreamToString(boolean streamToString) {
        this.streamToString = streamToString;
    }


    public void setDisplayProcessed(boolean displayProcessed) {
        this.displayProcessed = displayProcessed;
    }


    public void afterPropertiesSet() throws Exception {
        if (this.unsupportedResourceTypes == null) {
            this.unsupportedResourceTypes = new HashSet<String>();
        }
        if (this.repository == null) {
            throw new BeanInitializationException(
                    "JavaBean property 'repository' must be specified");
        }
        if (this.viewName == null && this.view == null) {
            throw new BeanInitializationException(
                "At least one of JavaBean properties 'viewName' or 'view' must "
                + "be specified");
        }
    }


    public ModelAndView handleRequest(HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        SecurityContext securityContext = SecurityContext.getSecurityContext();
        RequestContext requestContext = RequestContext.getRequestContext();

        Path uri = requestContext.getResourceURI();

        if (this.childName != null) {
            uri = uri.extend(this.childName);
        }

        String token = securityContext.getToken();

        Map<String, Object> model = new HashMap<String, Object>();
        Resource resource = this.repository.retrieve(token, uri, this.displayProcessed);
        if (this.unsupportedResourceTypes.contains(resource.getContentType())) {
            return new ModelAndView(this.unsupportedResourceView);
        }

        if (this.supportIfHeaders) {
            IfMatchHeader ifMatchHeader = new IfMatchHeader(request);
            if (!ifMatchHeader.matches(resource)) {
                throw new PreconditionFailedException();
            }
                
            IfNoneMatchHeader ifNoneMatchHeader = new IfNoneMatchHeader(request);
            if (!ifNoneMatchHeader.matches(resource)) {
                throw new ResourceNotModifiedException(uri);
            }
        }
        
        model.put("resource", resource);


        if (!resource.isCollection()) {

            InputStream stream = this.repository.getInputStream(token, uri, true);

            if (!this.streamToString || !resource.getContentType().startsWith("text/")) {
                model.put("resourceStream", stream);
                if (this.view != null) {
                    return new ModelAndView(this.view, model);
                }
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
        
        }
        if (this.view != null) {
            return new ModelAndView(this.view, model);
        }
        return new ModelAndView(this.viewName, model);
    }


    public long getLastModified(HttpServletRequest request) {

        if (this.ignoreLastModified) {
            if (logger.isDebugEnabled()) {
                logger.debug("Ignoring last-modified value for request "
                             + request.getRequestURI());
            }
            return -1;
        }

        SecurityContext securityContext = SecurityContext.getSecurityContext();
        RequestContext requestContext = RequestContext.getRequestContext();

        Resource resource = null;
        
        Path uri = requestContext.getResourceURI();
        
        if (this.childName != null) {
            uri = uri.extend(this.childName);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Getting last-modified value for resource "
                         + uri);
        }

        try {
            resource = this.repository.retrieve(
                securityContext.getToken(), uri, true);

        } catch (RepositoryException e) {
            // These exceptions are expected
            if (logger.isDebugEnabled()) {
                logger.debug("Unable to get last-modified value for resource "
                             + uri, e);
            }
            return -1;

        } catch (AuthenticationException e) {
            // These exceptions are expected
            if (logger.isDebugEnabled()) {
                logger.debug("Unable to get last-modified value for resource "
                             + uri, e);
            }
            return -1;

        } catch (Throwable t) {
            if (logger.isInfoEnabled()) {
                logger.info(
                    "Unable to get the last-modified value for resource "
                    + uri, t);
            }
            return -1;
        }
        
        if (resource.isCollection() && this.ignoreLastModifiedOnCollections) {
            logger.debug("Ignorig last-modified value for resource "
                         + uri + ": resource is collection");
            return -1;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Returning last-modified value for resource "
                         + uri + ": " + resource.getLastModified());
        }

        return resource.getLastModified().getTime();
    }

}

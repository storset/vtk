/* Copyright (c) 2005, University of Oslo, Norway
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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.servlet.mvc.LastModified;

import org.vortikal.util.repository.MimeHelper;
import org.vortikal.util.web.HttpUtil;
import org.vortikal.web.RequestContext;


/**
 * Controller that serves classpath resources.
 *
 * <p>Configurable JavaBean properties:
 * <ul>
 *   <li><code>basePath</code> - the base class path. This base path
 *   is prepended to every class path resource retrieval.
 *   <li><code>uriPrefix</code> - the URI prefix is stripped off the
 *   request URI before prepending the base class path (optional).
 *   <lI><code>resourceInModelName</code> - if specified, instead of
 *   writing the response directly, this controller will put the class
 *   path resource in the model under this name, and the rendering
 *   must be handled by a view.
 *   <li><code>resourceTransformer</code> (used in conjunction with
 *   <code>resourceInModelName</code>) - a {@link ResourceTransformer},
 *   which, if specified, causes the result of a transformation to be
 *   placed in the model instead of the resource directly.
 *   <lI><code>viewName</code> - optional viewName, used with
 *   <code>resourceInModelName</code>
 *   <lI><code>expiresSeconds</code> - optional integer, will be sent
 *   as the <code>Expires</code> HTTP header 
 * </ul>
 */
public class DisplayClassPathResourceController 
  implements Controller, LastModified, InitializingBean, ApplicationContextAware {

    private Log logger = LogFactory.getLog(this.getClass());
    private String basePath;
    private String uriPrefix;
    private ApplicationContext applicationContext;

    private int expiresSeconds = -1;
    private String resourceInModelName;
    private ResourceTransformer resourceTransformer;
    private String viewName;
    
    
    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public void setUriPrefix(String uriPrefix) {
        this.uriPrefix = uriPrefix;
    }

    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
    

    public void setResourceInModelName(String resourceInModelName) {
        this.resourceInModelName = resourceInModelName;
    }

    public void setResourceTransformer(ResourceTransformer resourceTransformer) {
        this.resourceTransformer = resourceTransformer;
    }

    public void setViewName(String viewName) {
        this.viewName = viewName;
    }
    
    public void setExpiresSeconds(int expiresSeconds) {
        this.expiresSeconds = expiresSeconds;
    }
    

    public void afterPropertiesSet() throws Exception {
        if (this.basePath == null) {
            throw new BeanInitializationException(
                "JavaBean property 'basePath' not specified");
        }

        if (this.resourceInModelName != null && this.viewName == null) {
            throw new BeanInitializationException(
                "JavaBean property 'viewName' must also be specified when property "
                + "'resourceInModelName' is specified");
        }
    }


    public ModelAndView handleRequest(HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        if (!("GET".equals(request.getMethod()) || "HEAD".equals(request.getMethod()))) {
            response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return null;
        }

        RequestContext requestContext = RequestContext.getRequestContext();
        String uri = requestContext.getResourceURI();
        if (this.uriPrefix != null) {
            uri = uri.substring(this.uriPrefix.length());
        }

        String path = this.basePath;
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        path += uri;

        ClassPathResource resource = new ClassPathResource(path);
        
        if (!resource.exists()) {
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("Unable to serve resource: " + resource
                             + " from path: " + path + ": resource does not exist");
            }
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }


        InputStream inStream = null;
        OutputStream outStream = null;

        try {

            if (this.resourceInModelName != null) {
                Object modelObject = resource;
                if (this.resourceTransformer != null) {
                    modelObject = this.resourceTransformer.transformResource(resource);
                    Map model = new HashMap();
                    model.put(this.resourceInModelName, modelObject);
                    return new ModelAndView(this.viewName, model);
                }
            } 

            if (this.expiresSeconds >= 0) {
                long expiresMilliseconds = this.expiresSeconds * 1000;
                Date expires = new Date(System.currentTimeMillis() + expiresMilliseconds);
                response.setHeader("Expires", HttpUtil.getHttpDateString(expires));
            }

            response.setContentType(MimeHelper.map(uri));

            if ("GET".equals(request.getMethod())) {

                inStream = resource.getInputStream();                
                outStream  = response.getOutputStream();
                byte[] buffer = new byte[5000];

                int n = 0;
                while (((n = inStream.read(buffer, 0, 5000)) > 0)) {
                    outStream.write(buffer, 0, n);
                }
            }
            
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("Successfully served resource: " + resource
                             + " from path: " + path);
            }

        } catch (Exception e) {

            if (this.logger.isDebugEnabled()) {
                this.logger.debug("Unable to serve resource: " + resource
                             + " from path: " + path, e);
            }
            response.sendError(HttpServletResponse.SC_NOT_FOUND);

        } finally {
            if (inStream != null) inStream.close();
            if (outStream != null) outStream.close();
        }
        return null;
    }


    public long getLastModified(HttpServletRequest request) {
        RequestContext requestContext = RequestContext.getRequestContext();
        
        if (!"GET".equals(request.getMethod())) {
            return -1;
        }

        String uri = requestContext.getResourceURI();
        if (this.uriPrefix != null) {
            uri = uri.substring(this.uriPrefix.length());
        }

        String path = this.basePath;
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        path += uri;

        ClassPathResource resource = new ClassPathResource(path);
        if (resource.exists()) {
            return this.applicationContext.getStartupDate();
        }

        return -1;
    }

}

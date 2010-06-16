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
package org.vortikal.web.display.classpath;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLConnection;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.servlet.mvc.LastModified;
import org.vortikal.repository.Path;
import org.vortikal.util.repository.MimeHelper;
import org.vortikal.web.RequestContext;
import org.vortikal.web.StaticResourceLocation;
import org.vortikal.web.service.URL;

/**
 * Controller that serves class path resources.
 *
 * <p>Configurable JavaBean properties:
 * <ul>
 *   <li><code>headers</code> - map of (name, value) pairs that will 
 *   be sent as response headers</li> 
 * </ul>
 */
public class DisplayClassPathResourceController 
  implements Controller, LastModified, InitializingBean, ApplicationContextAware {

    private Log logger = LogFactory.getLog(this.getClass());
    private Map<String, String> locationsMap;
    private Map<String, String> headers;
    private ApplicationContext applicationContext;

      public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
    
    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }
    
    @SuppressWarnings("unchecked")
    public void afterPropertiesSet() throws Exception {

        Map<String, StaticResourceLocation> matchingBeans = BeanFactoryUtils.beansOfTypeIncludingAncestors(
            this.applicationContext, StaticResourceLocation.class, true, false);
        Collection<StaticResourceLocation> allLocations = matchingBeans.values();
        this.locationsMap = new HashMap<String, String>();

        for (StaticResourceLocation location: allLocations) {
            String uri = location.getUriPrefix();
            String resourceLocation = location.getResourceLocation();
            this.locationsMap.put(uri, resourceLocation);
            this.locationsMap.put(uri + "/", resourceLocation);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Locations map: " + this.locationsMap);
        }
    }


    public ModelAndView handleRequest(HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        if (!("GET".equals(request.getMethod()) || "HEAD".equals(request.getMethod()))) {
            response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return null;
        }

        Resource resource = resolveResource(request);
        if (resource == null || !resource.exists()) {
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("Unable to serve resource: " + resource
                                  + " from " + resource.getDescription()
                                  + ": resource does not exist");
            }
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }

        InputStream inStream = null;
        OutputStream outStream = null;
        int contentLength = -1;
        try {

            if (resource instanceof ClassPathResource) {
            	java.net.URL url = resource.getURL();
            	URLConnection connection = url.openConnection();
            	contentLength = connection.getContentLength();
            	inStream = connection.getInputStream();
            } else if (resource instanceof FileSystemResource) {
            	File file = resource.getFile();
            	inStream = resource.getInputStream();
            	contentLength = (int) file.length();
            } else {
            	inStream = resource.getInputStream();
            }
            response.setContentType(MimeHelper.map(request.getRequestURI()));
            if (contentLength != -1) {
            	response.setContentLength(contentLength);
            }
            for (String header: this.headers.keySet()) {
                response.addHeader(header, this.headers.get(header));
            }
            
            if ("GET".equals(request.getMethod())) {

                outStream  = response.getOutputStream();
                byte[] buffer = new byte[5000];

                int n = 0;
                while (((n = inStream.read(buffer, 0, 5000)) > 0)) {
                    outStream.write(buffer, 0, n);
                }
            }
            
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("Successfully served resource: " + resource
                                  + " from " + resource.getDescription());
            }
        } catch (Exception e) {
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("Unable to serve resource: " + resource
                                  + " from " + resource.getDescription(), e);
            }
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        } finally {
            if (inStream != null) {
            	inStream.close();
            }
            if (outStream != null) {
            	outStream.close();
            }
        }
        return null;
    }


    public long getLastModified(HttpServletRequest request) {
        if (!("GET".equals(request.getMethod()) || "HEAD".equals(request.getMethod()))) {
            return -1;
        }

        Resource resource = resolveResource(request);
        if (resource.exists()) {
            try {
                File f = resource.getFile();
                return f.lastModified();
            } catch (IOException e) {
                return this.applicationContext.getStartupDate();
            }
        }
        return -1;
    }


    private Resource resolveResource(HttpServletRequest request) {
        URL url = URL.create(request);
        List<Path> paths = url.getPath().getPaths();

        String uriPrefix = null;
        String resourceLocation = null;
        for (int i = paths.size() - 1; i >= 0; i--) {
            String prefix = paths.get(i).toString();
            if (this.locationsMap.containsKey(prefix)) {
                resourceLocation = this.locationsMap.get(prefix);
                uriPrefix = prefix;
            }
        }

        if (resourceLocation == null) {
            return null;
        }

        RequestContext requestContext = RequestContext.getRequestContext();
        Path uri = requestContext.getResourceURI();

        if (uriPrefix != null) {
            uri = Path.fromString(uri.toString().substring(uriPrefix.length()));
        }

        String path = resourceLocation;
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        path += uri;

        Resource resource = null;
        if (path.startsWith("file://")) {
            String actualPath = path.substring("file://".length());
            resource = new FileSystemResource(actualPath);
        } else if (path.startsWith("classpath://")) {
            String actualPath = path.substring("classpath://".length());
            resource = new ClassPathResource(actualPath);
        } 
        return resource;
    }

}

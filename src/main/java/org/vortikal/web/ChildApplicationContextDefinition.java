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
package org.vortikal.web;

import java.io.IOException;
import java.net.URL;

import javax.servlet.ServletContext;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.ServletContextResource;
import org.springframework.web.context.support.XmlWebApplicationContext;



/**
 * Utility class for creating child application contexts using a
 * bean definition.
 *
 * <p>This class creates an application context as a child of the
 * current context. An XML bean definition is read from a location
 * specified by the configuration property
 * <code>configLocation</code>.
 *
 * <p>The syntax for the config locations is the default set supported
 * by {@link org.springframework.core.io.DefaultResourceLoader}. In
 * addition, when executing in a web application context, the syntax
 * <code>servletjar:servletResourceJarPath!entry</code> is supported,
 * which means "get XML bean definition file <code>entry</code> which
 * is located in the web application at
 * <code>servletResourceJarPath</code>. Example:
 * <code>servletjar:/WEB-INF/lib/foo.jar!/beans/bar.xml</code>
 */
public class ChildApplicationContextDefinition
  implements ApplicationContextAware, InitializingBean {

    
    private ApplicationContext applicationContext = null;
    private String configLocation = null;
    

    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }


    public void setConfigLocation(String configLocation) {
        this.configLocation = configLocation;
    }
    

    public void afterPropertiesSet() {
        if (this.applicationContext == null) {
            throw new BeanInitializationException(
                "Bean property 'applicationContext' must be set");
        }
        if (this.configLocation == null) {
            throw new BeanInitializationException(
                "Bean property 'configLocation' must be set");
        }
        new ChildApplicationContext(this.configLocation, this.applicationContext);
    }


    private class ChildApplicationContext extends XmlWebApplicationContext {
        
        public static final String SERVLET_CONTEXT_JAR_URL_PREFIX = "servletjar:";

        private String configLocation = null;

        private WebApplicationContext parentWebApplicationContext = null;


        public ChildApplicationContext(String configLocation,
                                       ApplicationContext parent) {
            this.configLocation = configLocation;

            findParentWebApplicationContext(parent);
            if (this.parentWebApplicationContext != null) {
                setServletContext(this.parentWebApplicationContext.getServletContext());
            }

            setParent(parent);
            refresh();
        }
        

        public String[] getConfigLocations() {
            return new String[] {this.configLocation};
        }


        public Resource getResourceByPath(String path) {

            boolean inWebApplicationContext = (this.parentWebApplicationContext != null);
            
            if (inWebApplicationContext && path.startsWith("/")) {
                ServletContext servletContext =
                    this.parentWebApplicationContext.getServletContext();
                return new ServletContextResource(servletContext, path);
            }

            if (path.startsWith(SERVLET_CONTEXT_JAR_URL_PREFIX)) {
                if (!inWebApplicationContext) {
                    throw new IllegalStateException(
                        "Cannot load resources of type '"
                        + SERVLET_CONTEXT_JAR_URL_PREFIX + "' when no ancestor context "
                        + "is a WebApplicationContext");
                }

                // Resolve the servlet resource to a file:// resource
                String contextPath = path.substring(SERVLET_CONTEXT_JAR_URL_PREFIX.length(),
                                                    path.lastIndexOf("!"));
                String entry = path.substring(path.lastIndexOf("!") + 1);
                Resource resource = getResource(contextPath);

                try {
                    // Convert the file:// URL to a jar: URL:
                    URL resourceURL = resource.getURL();

                    URL url = new URL("jar:" + resourceURL.toExternalForm() + "!" + entry);
                    return new UrlResource(url);

                } catch (IOException e) {
                    throw new RuntimeException("Unable to load resource '" + path + "'", e);
                }
            }
            if (inWebApplicationContext) {
                return new ServletContextResource(
                    this.parentWebApplicationContext.getServletContext(), path);
            }

            return super.getResourceByPath(path);
        }


        private void findParentWebApplicationContext(ApplicationContext parent) {
            if (parent == null) {
                return;
            }
            
            if (parent instanceof WebApplicationContext) {
                this.parentWebApplicationContext = (WebApplicationContext) parent;
                return;
            }

            findParentWebApplicationContext(parent.getParent());
        }

    }
    
}

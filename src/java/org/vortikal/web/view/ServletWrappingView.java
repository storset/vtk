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
package org.vortikal.web.view;

import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.View;
import org.vortikal.web.servlet.BufferedResponseWrapper;


/**
 * View implementation that instantiates a servlet instance and
 * invokes its service() method on rendering, wrapping its response
 * output.
 *
 * <p>Configurable properties:
 * <ul>
 *   <li><code>servletName</code> - the name of the servlet to
 *   instantiate
 *   <li><code>servletClass</code> - class of the servlet to
 *   instantiate
 *   <li><code>initParameters</code> - a {@link Properties} object
 *   containing the init parameters of the servlet
 *   <li><code>defaultContentType</code> - the
 *   <code>Content-Type</code> header to set if the wrapped servlet
 *   does not provide one
 * </ul>
 */
public class ServletWrappingView
  implements ApplicationContextAware, InitializingBean, DisposableBean, ServletConfig, View {

    protected Log logger = LogFactory.getLog(this.getClass());


    private String servletName = null;
    private String servletClass = null;
    private Properties initParameters = new Properties();
    private WebApplicationContext webApplicationContext = null;
    
    private String defaultContentType = "application/octet-stream";

    private Servlet servlet = null;
    

    public void setServletName(String servletName) {
        this.servletName = servletName;
    }

    public void setServletClass(String servletClass) {
        this.servletClass = servletClass;
    }
    
    public void setInitParameters(Properties initParameters) {
        this.initParameters = initParameters;
    }
    
    public void setDefaultContentType(String defaultContentType) {
        this.defaultContentType = defaultContentType;
    }
    
    public void setApplicationContext(ApplicationContext applicationContext) {
        if (!(applicationContext instanceof WebApplicationContext)) {
            throw new IllegalArgumentException(
                "Must be instantiated in a "
                + WebApplicationContext.class.getName()
                + ". Application context was of type "
                + applicationContext.getClass().getName());
        }
        this.webApplicationContext = (WebApplicationContext) applicationContext;
    }
    
    public String getInitParameter(String parameter) {
        return this.initParameters.getProperty(parameter);
    }

    public Enumeration getInitParameterNames() {
        return this.initParameters.keys();
    }

    public String getServletName() {
        return this.servletName;
    }

    public ServletContext getServletContext() {
        return this.webApplicationContext.getServletContext();
    }
    

    public void afterPropertiesSet() throws Exception {
        if (this.servletName == null) {
            throw new BeanInitializationException(
                "Required property 'servletName' not set");
        }
        if (this.servletClass == null) {
            throw new BeanInitializationException(
                "Required property 'servletClass' not set");
        }

        Class clazz = Class.forName(this.servletClass);
        if (!Servlet.class.isAssignableFrom(clazz)) {
            throw new BeanInitializationException("Class " + clazz.getName()
                                                  + " is not a servlet");
        }

        this.servlet = (Servlet) clazz.newInstance();
        this.servlet.init(this);
    }


    public void destroy() {
        this.servlet.destroy();
    }
    


    /**
     * The actual rendering. First obtains the wrapped servlet
     * response via <code>getWrappedResponse()</code>, then invokes
     * <code>render()</code> on the wrapped view object.
     *
     * @param model the Spring MVC model
     * @param request the servlet request
     * @param response the servlet response
     * @exception Exception if an error occurs
     */
    public void render(Map model, HttpServletRequest request,
                       HttpServletResponse response)
        throws Exception {
        BufferedResponseWrapper wrappedResponse = new BufferedResponseWrapper(response);
        this.servlet.service(request, wrappedResponse);
        String contentType = wrappedResponse.getContentType();
        if (contentType == null || "".equals(contentType.trim())) {
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("Wrapped servlet did not set Content-Type. Using default: "
                             + this.defaultContentType);
            }

            contentType = this.defaultContentType;
        }

        writeResponse(wrappedResponse, contentType);
    }
    
    

    /**
     * Writes the buffer from the wrapped response to the actual
     * response. Sets the HTTP header <code>Content-Length</code> to
     * the size of the buffer in the wrapped response.
     * @param wrappedResponse the wrapped response.
     * @exception Exception if an error occurs.
     */
    protected void writeResponse(BufferedResponseWrapper wrappedResponse, String contentType) 
        throws Exception {
        ServletResponse response = wrappedResponse.getResponse();
        ServletOutputStream outStream = response.getOutputStream();
        byte[] content = wrappedResponse.getContentBuffer();
        if (this.logger.isDebugEnabled()) {
            this.logger.debug("Write response: Content-Length: " + content.length
                         + ", Content-Type: " + contentType);
        }
        response.setContentType(contentType);
        response.setContentLength(content.length);
        outStream.write(content);
        if (this.logger.isDebugEnabled()) {
            this.logger.debug("Wrote " + content.length + " bytes");
        }
        outStream.flush();
        outStream.close();
    }

}

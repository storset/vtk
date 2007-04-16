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
package org.vortikal.web.filter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import org.vortikal.util.io.StreamUtil;


/**
 */
public class XSLTInputStreamFilter extends AbstractRequestFilter
  implements InitializingBean {

    private ResourceLoader loader = new DefaultResourceLoader();

    private static Log logger = LogFactory.getLog(XSLTInputStreamFilter.class);
    private String stylesheet;
    private Templates templates;
    private boolean debug = false;
    private boolean robust = true;
    
    
    public void setStylesheet(String stylesheet) {
        this.stylesheet = stylesheet;
    }
    
    
    public void setDebug(boolean debug) {
        this.debug = debug;
    }
    
    
    public void setRobust(boolean robust) {
        this.robust = robust;
    }
    

    public void afterPropertiesSet() throws Exception {
        if (this.stylesheet == null) {
            throw new BeanInitializationException(
                "JavaBean property 'stylesheet' not set");
        }
        compile();
    }
    
    public HttpServletRequest filterRequest(HttpServletRequest request) {
        return new XSLTContentRequestWrapper(request);
    }
    

    private synchronized void compile() throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("Compiling stylesheet '" + this.stylesheet + "'");
        }

        TransformerFactory factory = TransformerFactory.newInstance();
        
        Resource resource = this.loader.getResource(this.stylesheet);
        Source source = new StreamSource(resource.getInputStream());
        this.templates = factory.newTemplates(source);
    }
    

    private class XSLTContentRequestWrapper extends HttpServletRequestWrapper {
        
        private HttpServletRequest request;
        
        public XSLTContentRequestWrapper(HttpServletRequest request) {
            super(request);
            this.request = request;
        }
        
        public ServletInputStream getInputStream() throws IOException {
            if (debug) {
                try {
                    compile();
                } catch (Exception e) {
                    throw new IOException(e.getMessage());
                }
            }

            ServletInputStream stream = this.request.getInputStream();
            byte[] buffer = StreamUtil.readInputStream(stream);
            ByteArrayInputStream originalStream = new ByteArrayInputStream(buffer);

            StreamSource source = new StreamSource(originalStream);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            StreamResult streamResult = new StreamResult(out);
            try {
                Transformer transformer = templates.newTransformer();
                if (logger.isDebugEnabled()) {
                    logger.debug("Transforming resource using stylesheet: '"
                                 + stylesheet + "', output properties: "
                                 + transformer.getOutputProperties());
                }
                transformer.transform(source, streamResult);
                ByteArrayInputStream result = new ByteArrayInputStream(out.toByteArray());
                return new org.vortikal.util.io.ServletInputStream(result);
            } catch (Exception e) {
                logger.warn("Unable to transform document '" + request.getRequestURI()
                            + "' using stylesheet '" + stylesheet + "'", e);
                if (robust) {
                    // Return the original content
                    originalStream.reset();
                    return new org.vortikal.util.io.ServletInputStream(originalStream);
                }
                throw new IOException(e.getMessage());
            }
        }
    }

}

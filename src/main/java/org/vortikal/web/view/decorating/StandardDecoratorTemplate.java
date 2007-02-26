/* Copyright (c) 2007, University of Oslo, Norway
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
package org.vortikal.web.view.decorating;

import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.web.referencedata.ReferenceDataProvider;
import org.vortikal.web.referencedata.ReferenceDataProviding;
import org.vortikal.web.view.decorating.html.HtmlPage;


public class StandardDecoratorTemplate implements Template, InitializingBean, BeanNameAware  {

    private static final String DEFAULT_DOCTYPE =
        "html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\"";

    private static Log logger = LogFactory.getLog(StandardDecoratorTemplate.class);

    private TemplateParser parser;
    private ComponentInvocation[] fragments;
    private TemplateSource templateSource;
    private String beanName;
    private long lastModified = -1;
    

    public StandardDecoratorTemplate() {
    }
    

    public StandardDecoratorTemplate(String name, TemplateParser parser,
                                     TemplateSource templateSource) throws Exception {
        if (name == null) {
            throw new IllegalArgumentException("Argument 'name' is NULL");
        }
        if (parser == null) {
            throw new IllegalArgumentException("Argument 'parser' is NULL");
        }
        if (name == null) {
            throw new IllegalArgumentException("Argument 'templateSource' is NULL");
        }
        this.beanName = name;
        this.parser = parser;
        this.templateSource = templateSource;
        compile();
    }
    

    public void setTemplateSource(TemplateSource templateSource) {
        this.templateSource = templateSource;
    }
    
    public void setParser(TemplateParser parser) {
        this.parser = parser;
    }
    
    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    public String getName() {
        return this.beanName;
    }
    

    public void afterPropertiesSet() {
        if (this.beanName == null) {
            throw new BeanInitializationException(
                "JavaBean property 'beanName' not specified");
        }
        if (this.parser == null) {
            throw new BeanInitializationException(
                "JavaBean property 'parser' not specified");
        }
        if (this.templateSource == null) {
            throw new BeanInitializationException(
                "JavaBean property 'templateSource' not specified");
        }
        try {
            compile();
        } catch (Exception e) {
            throw new BeanInitializationException("Error compiling template " +
                                                  this.templateSource, e);
        }
    }
    

    public String render(Map model, HtmlPage html, HttpServletRequest request,
                       Locale locale) throws Exception {

        if (this.templateSource.getLastModified() > this.lastModified) {
            compile();
        }
        
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < this.fragments.length; i++) {
            
            try {
                DecoratorComponent c = this.fragments[i].getComponent();
                if (c instanceof ReferenceDataProviding) {
                    ReferenceDataProvider[] providers =
                        ((ReferenceDataProviding) c).getReferenceDataProviders();
                    if (providers != null) {
                        for (int j = 0; j < providers.length; j++) {
                            providers[j].referenceData(model, request);
                        }
                    }
                }

                String doctype = html.getDoctype();
                if (doctype == null) {
                    doctype = DEFAULT_DOCTYPE;
                }
                DecoratorRequest decoratorRequest = new DecoratorRequestImpl(
                    model, html, request, this.fragments[i].getParameters(), doctype, locale);

                String chunk = renderComponent(c, decoratorRequest);
                if (logger.isDebugEnabled()) {
                    logger.debug("Included component: " + this.fragments[i]
                                 + " with result [" + chunk + "]");
                }
                sb.append(chunk);

            } catch (Throwable t) {
                logger.warn("Error including component: " + this.fragments[i], t);
                // Include error message in output:
                String msg = t.getMessage();
                if (msg == null) {
                    msg = t.getClass().getName();
                }
                sb.append(this.fragments[i].getComponent().getName());
                sb.append(": ").append(msg);
            }
        }

        return sb.toString();
    }
    

    private String renderComponent(DecoratorComponent c, DecoratorRequest request)
        throws Exception {
        
        // Default values for decorator responses:
        String defaultResponseDoctype = request.getDoctype();
        String defaultResponseEncoding = "utf-8";
        Locale defaultResponseLocale = Locale.getDefault();

        DecoratorResponseImpl response = new DecoratorResponseImpl(
            defaultResponseDoctype, defaultResponseLocale, defaultResponseEncoding);
        c.render(request, response);
        String result = response.getContentAsString();
        return result;
    }
    

    public synchronized void compile() throws Exception {
        ComponentInvocation[] components = this.parser.parseTemplate(this.templateSource);
        this.fragments = components;
        this.lastModified = templateSource.getLastModified();
    }
    
    public String toString() {
        return this.getClass().getName() + ": " + this.beanName;
    }
    

}



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

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;

import org.vortikal.util.io.StreamUtil;
import org.vortikal.web.referencedata.ReferenceDataProvider;
import org.vortikal.web.referencedata.ReferenceDataProviding;
import org.vortikal.web.servlet.BufferedResponse;


public class StandardDecoratorTemplate implements Template, InitializingBean, BeanNameAware  {

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
    

    public void render(Map model, HttpServletRequest request,
                       HttpServletResponse response) throws Exception {

        if (this.templateSource.getLastModified() > this.lastModified) {
            compile();
        }
        
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < this.fragments.length; i++) {
            
            BufferedResponse bufferedResponse = new BufferedResponse();
            if (this.fragments[i] instanceof ReferenceDataProviding) {
                ReferenceDataProvider[] providers =
                    ((ReferenceDataProviding) this.fragments[i]).
                    getReferenceDataProviders();
                if (providers != null) {

                    for (int j = 0; j < providers.length; j++) {
                        providers[j].referenceData(model, request);
                    }
                }
            }

            try {
                DecoratorComponent c = this.fragments[i].getComponent();
                DecoratorRequest decoratorRequest = new DecoratorRequestImpl(
                    model, request, this.fragments[i].getParameters());

                String chunk = c.getRenderedContent(decoratorRequest);
                if (logger.isDebugEnabled()) {
                    logger.debug("Included component: " + this.fragments[i]
                                 + " with result [" + chunk + "]");
                }
                sb.append(chunk);
            } catch (Throwable t) {
                String msg = t.getMessage();
                if (msg == null) {
                    msg = t.getClass().getName();
                }
                sb.append(msg);
            }
        }

        // XXX: handle character encoding
        byte[] buffer = sb.toString().getBytes("utf-8");
        OutputStream out = response.getOutputStream();
        
        response.setContentType("text/html;charset=utf-8");
        out.write(buffer);
        out.flush();
        out.close();
    }
    

    public synchronized void compile() throws Exception {
        ComponentInvocation[] components = this.parser.parseTemplate(this.templateSource);
        this.fragments = components;
        this.lastModified = templateSource.getLastModified();
    }
    
}



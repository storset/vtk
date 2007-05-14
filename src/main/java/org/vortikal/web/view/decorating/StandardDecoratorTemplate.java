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

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.vortikal.text.html.HtmlPage;


public class StandardDecoratorTemplate implements Template {

    private static final String DEFAULT_DOCTYPE =
        "html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\"";

    private static Log logger = LogFactory.getLog(StandardDecoratorTemplate.class);

    private TemplateParser parser;
    private ComponentInvocation[] fragments;
    private TemplateSource templateSource;
    private long lastModified = -1;
    

    public StandardDecoratorTemplate(TemplateParser parser,
                                     TemplateSource templateSource) throws Exception {
        if (parser == null) {
            throw new IllegalArgumentException("Argument 'parser' is NULL");
        }
        if (templateSource == null) {
            throw new IllegalArgumentException("Argument 'templateSource' is NULL");
        }
        this.parser = parser;
        this.templateSource = templateSource;
        compile();
    }

    public String render(HtmlPage html, HttpServletRequest request,
                       Locale locale) throws Exception {

        if (this.templateSource.getLastModified() > this.lastModified) {
            compile();
        }
        
        StringBuffer sb = new StringBuffer();
        ComponentInvocation[] fragments = this.fragments;        
        for (int i = 0; i < fragments.length; i++) {
            
            try {
                DecoratorComponent c = fragments[i].getComponent();

                String doctype = html.getDoctype();
                if (doctype == null) {
                    doctype = DEFAULT_DOCTYPE;
                }
                DecoratorRequest decoratorRequest = new DecoratorRequestImpl(
                    html, request, fragments[i].getParameters(), doctype, locale);

                String chunk = renderComponent(c, decoratorRequest);
                if (logger.isDebugEnabled()) {
                    logger.debug("Included component: " + fragments[i]
                                 + " with result [" + chunk + "]");
                }
                sb.append(chunk);

            } catch (Throwable t) {
                logger.warn("Error including component: " + fragments[i], t);
                // Include error message in output:
                String msg = t.getMessage();
                if (msg == null) {
                    msg = t.getClass().getName();
                }
                sb.append(fragments[i].getComponent().getName());
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
    

    private synchronized void compile() throws Exception {
        if (this.lastModified == templateSource.getLastModified())
            return;

        this.fragments = this.parser.parseTemplate(
            this.templateSource.getTemplateReader());

        this.lastModified = templateSource.getLastModified();
    }
    
    public String toString() {
        return this.getClass().getName() + ": " + this.templateSource;
    }
    

}



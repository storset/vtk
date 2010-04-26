/* Copyright (c) 2010, University of Oslo, Norway
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
package org.vortikal.web.decorating;

import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.vortikal.resourcemanagement.view.tl.ComponentInvokerNodeFactory;
import org.vortikal.text.html.HtmlPage;
import org.vortikal.text.tl.Context;
import org.vortikal.text.tl.DirectiveNodeFactory;
import org.vortikal.text.tl.NodeList;
import org.vortikal.text.tl.ParseResult;
import org.vortikal.text.tl.Parser;


public class DynamicDecoratorTemplate implements Template {

    private Parser parser;
    private ParseResult compiledTemplate;
    private ComponentResolver componentResolver;
    private TemplateSource templateSource;
    private long lastModified = -1;
    private Map<String, DirectiveNodeFactory> directiveHandlers;
    
    private static final String CR_REQ_ATTR = "__component_resolver__";
    private static final String HTML_REQ_ATTR = "__html_page__";

    public static class ComponentSupport implements ComponentInvokerNodeFactory.ComponentSupport {

        public ComponentResolver getComponentResolver(Context context) {
            return (ComponentResolver) context.get(CR_REQ_ATTR);
        }

        @Override
        public HtmlPage getHtmlPage(Context context) {
            return (HtmlPage) context.get(HTML_REQ_ATTR);
        }
        
    }
    
    public DynamicDecoratorTemplate(TemplateSource templateSource,
                                     ComponentResolver componentResolver,
                                     Map<String, DirectiveNodeFactory> directiveHandlers) throws InvalidTemplateException {
        if (templateSource == null) {
            throw new IllegalArgumentException("Argument 'templateSource' is NULL");
        }
        if (componentResolver == null) {
            throw new IllegalArgumentException("Argument 'componentResolver' is NULL");
        }
        if (directiveHandlers == null) {
            throw new IllegalArgumentException("Argument 'directiveHandlers' is NULL");
        }
        this.templateSource = templateSource;
        this.componentResolver = componentResolver;
        this.directiveHandlers = directiveHandlers;
        try {
            compile();
        } catch (Exception e) {
            throw new InvalidTemplateException("Unable to compile template " 
                    + templateSource, e);
        }
    }

    public class Execution implements TemplateExecution {
        private HtmlPageContent content;
        private ParseResult compiledTemplate;
        private ComponentResolver componentResolver;
        private HttpServletRequest request;
        private Map<Object, Object> model;

        public Execution(HtmlPageContent content, ParseResult compiledTemplate, 
                ComponentResolver componentResolver, HttpServletRequest request,
                Map<Object, Object> model) {
            this.content = content;
            this.componentResolver = componentResolver;
            this.compiledTemplate = compiledTemplate;
            this.request = request;
            this.model = model;
        }
        
        public void setComponentResolver(ComponentResolver componentResolver) {
            this.componentResolver = componentResolver;
        }
        
        public ComponentResolver getComponentResolver() {
            return this.componentResolver;
        }

        public PageContent render() throws Exception {
            HtmlPage html = this.content.getHtmlContent();
            Locale locale = this.request.getLocale(); // XXX
            Context context = new Context(locale);
            context.define(CR_REQ_ATTR, this.componentResolver, true);
            context.define(HTML_REQ_ATTR, html, true);
            Writer writer = new StringWriter();
            NodeList nodeList = this.compiledTemplate.getNodeList();
            nodeList.render(context, writer);
            return new ContentImpl(writer.toString(), this.content.getOriginalCharacterEncoding());
        }
    }
    
    public TemplateExecution newTemplateExecution(
            HtmlPageContent html, HttpServletRequest request,
            Map<Object, Object> model) throws Exception {

        if (this.templateSource.getLastModified() > this.lastModified) {
            compile();
        }
        return new Execution(html, this.compiledTemplate, this.componentResolver, request, model);
    }

    
    private synchronized void compile() throws Exception {
       if (this.compiledTemplate != null 
                && (this.lastModified == this.templateSource.getLastModified())) {
            return;
        }
        
        Reader reader = new InputStreamReader(
                this.templateSource.getInputStream(), 
                this.templateSource.getCharacterEncoding());

        this.parser = new Parser(reader, this.directiveHandlers);
        this.compiledTemplate = this.parser.parse();
        this.lastModified = templateSource.getLastModified();
    }
    
    public String toString() {
        return this.getClass().getName() + ": " + this.templateSource;
    }
}

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

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.vortikal.resourcemanagement.view.tl.ComponentInvokerNodeFactory;
import org.vortikal.text.html.HtmlPage;
import org.vortikal.text.html.HtmlPageParser;
import org.vortikal.text.tl.Context;
import org.vortikal.text.tl.DirectiveNodeFactory;
import org.vortikal.text.tl.Node;
import org.vortikal.text.tl.NodeList;
import org.vortikal.text.tl.ParseResult;
import org.vortikal.text.tl.Parser;


public class DynamicDecoratorTemplate implements Template {
    private static Log logger = LogFactory.getLog(DynamicDecoratorTemplate.class);
    
    private Parser parser;
    private ParseResult compiledTemplate;
    private ComponentResolver componentResolver;
    private TemplateSource templateSource;
    private long lastModified = -1;
    private Map<String, DirectiveNodeFactory> directiveHandlers;
    private HtmlPageParser htmlParser;
    
    private static final String CR_REQ_ATTR = "__component_resolver__";
    private static final String HTML_REQ_ATTR = "__html_page__";
    private static final String PARAMS_REQ_ATTR = "__template_params__";

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
                                     Map<String, DirectiveNodeFactory> directiveHandlers, 
                                     HtmlPageParser htmlParser) throws InvalidTemplateException {
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
        this.htmlParser = htmlParser;
        try {
            compile();
        } catch (Exception e) {
            throw new InvalidTemplateException("Unable to compile template " 
                    + templateSource, e);
        }
    }

    static Object getTemplateParam(HttpServletRequest request, String name) {
        Object attr = request.getAttribute(PARAMS_REQ_ATTR);
        if (attr == null) {
            return null;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> parameters = (Map<String, Object>) attr;

        return parameters.get(name);
    }
    
    public class Execution implements TemplateExecution {
        private HtmlPageContent content;
        private ParseResult compiledTemplate;
        private ComponentResolver componentResolver;
        private HttpServletRequest request;
        private Map<String, Object> templateParameters;

        public Execution(HtmlPageContent content, ParseResult compiledTemplate, 
                ComponentResolver componentResolver, HttpServletRequest request,
                Map<String, Object> templateParameters) {
            this.content = content;
            this.componentResolver = componentResolver;
            this.compiledTemplate = compiledTemplate;
            this.request = request;
            this.templateParameters = templateParameters;
        }
        
        public void setComponentResolver(ComponentResolver componentResolver) {
            this.componentResolver = componentResolver;
        }
        
        @Override
        public ComponentResolver getComponentResolver() {
            return this.componentResolver;
        }

        @Override
        public PageContent render() throws Exception {
            HtmlPage html = this.content.getHtmlContent();

            Locale locale = 
                new org.springframework.web.servlet.support.RequestContext(this.request).getLocale();

            Context context = new Context(locale);
            for (String name : this.templateParameters.keySet()) {
                context.define(name, this.templateParameters.get(name), true);
            }
            context.define(CR_REQ_ATTR, this.componentResolver, true);
            context.define(HTML_REQ_ATTR, html, true);
            this.request.setAttribute(PARAMS_REQ_ATTR, this.templateParameters);
            StringWriter writer = new StringWriter();
            NodeList nodeList = this.compiledTemplate.getNodeList();
            nodeList.render(context, writer);
            
            if (htmlParser == null) {
                return new ContentImpl(writer.toString(), this.content.getOriginalCharacterEncoding());
            }
            HtmlPage page = htmlParser.parse(
                    new ByteArrayInputStream(writer.toString().getBytes("utf-8")), "utf-8");
            return new HtmlPageContentImpl(page.getCharacterEncoding(), page);
        }
    }
    
    @Override
    public TemplateExecution newTemplateExecution(
            HtmlPageContent html, HttpServletRequest request,
            Map<String, Object> model, Map<String, Object> templateParameters) throws Exception {

        if (this.templateSource.getLastModified() > this.lastModified) {
            compile();
        }
        return new Execution(html, this.compiledTemplate, this.componentResolver, request, templateParameters);
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
        try {
            this.compiledTemplate = this.parser.parse();
            if (logger.isDebugEnabled()) {
                logger.debug("Successfully compiled template " + this);
            }
        } catch (Throwable t) {
            logger.warn("Failed to compile template " + this, t);
            this.compiledTemplate = getErrorTemplate(t);
        } finally {
            reader.close();
        }
        this.lastModified = templateSource.getLastModified();
    }
    
    public String toString() {
        return this.getClass().getName() + ": " + this.templateSource;
    }
    
    private ParseResult getErrorTemplate(final Throwable t) {
        final String message = t.getMessage();
        final TemplateSource template = this.templateSource;
        NodeList nodeList = new NodeList();
        nodeList.add(new Node() {
            public boolean render(Context ctx, Writer out) throws Exception {
                out.write("Error compiling template " + template.getID() + ": " + message);
                return true;
            }
        });
        return new ParseResult(nodeList);
    }
}

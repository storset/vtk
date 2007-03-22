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

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Locale;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.vortikal.web.view.decorating.html.HtmlElement;
import org.vortikal.web.view.decorating.html.HtmlNodeFilter;
import org.vortikal.web.view.decorating.html.HtmlPage;
import org.vortikal.web.view.decorating.html.HtmlPageParser;

public class TemplateDecorator implements Decorator {

    private static Log logger = LogFactory.getLog(TemplateDecorator.class);
    
    private HtmlPageParser htmlParser;
    private DecorationResolver decorationResolver;
    
    private HtmlNodeFilter htmlNodeFilter;    


    public void decorate(Map model, HttpServletRequest request, Content content)
        throws Exception, UnsupportedEncodingException, IOException {

        Locale locale = 
            new org.springframework.web.servlet.support.RequestContext(request).getLocale();
        
        DecorationDescriptor descriptor = resolveDecorationDescriptor(request, locale);
        if (!descriptor.decorate()) {
            return;
        }

        boolean filter = descriptor.parse();

        HtmlPage html = parseHtml(content.getContent(), filter);
        if (logger.isDebugEnabled()) {
            logger.debug("Parsed document [root element: " + html.getRootElement() + " "
                         + ", doctype: "+ html.getDoctype() + "]");
        }

        Template template = descriptor.getTemplate();
        if (template == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("No template resolved for request " + request);
            }
            replaceContentFromPage(content, html, descriptor.tidy());
            return;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Rendering request for " + request.getRequestURI()
                         + " using template '" + template + "'");
        }

        HtmlElement rootElement = html.getRootElement();
        if (rootElement != null && "frameset".equals(rootElement.getName())) {
            // Framesets are not decorated:
            replaceContentFromPage(content, html, descriptor.tidy());
            return;
        } 
        content.setContent(template.render(html, request, locale));
        if (descriptor.tidy()) {
            tidyContent(content);
        }
    }


    protected void replaceContentFromPage(Content content, HtmlPage page,
                                          boolean tidy) throws Exception {
        if (page.getRootElement() == null) {
            return;
        }
        content.setContent(page.getRootElement().getEnclosedContent());
        if (tidy) {
            tidyContent(content);
        }
    }
    

    protected void tidyContent(Content content) throws Exception {
        java.io.ByteArrayInputStream inStream = new java.io.ByteArrayInputStream(
            content.getContent().getBytes("utf-8"));

        org.w3c.tidy.Tidy tidy = new org.w3c.tidy.Tidy();
        tidy.setTidyMark(false);
        tidy.setMakeClean(false);
        tidy.setShowWarnings(false);
        tidy.setQuiet(true);
        tidy.setXHTML(true);
        tidy.setDocType("transitional"); 
        tidy.setCharEncoding(org.w3c.tidy.Configuration.UTF8);

        org.w3c.dom.Document document = tidy.parseDOM(inStream, null);
        java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
        tidy.pprint(document, outputStream);
            
        content.setContent(new String(outputStream.toByteArray(), "utf-8"));
    }
    
    protected DecorationDescriptor resolveDecorationDescriptor(
        HttpServletRequest request, Locale locale) throws Exception {
        return this.decorationResolver.resolve(request, locale);
    }
    

    protected HtmlPage parseHtml(String content, boolean filter) throws Exception {
        long before = System.currentTimeMillis();

        // XXX: encoding
        String encoding = "utf-8";
        InputStream stream = new java.io.ByteArrayInputStream(content.getBytes(encoding));
        HtmlPage html = null;
        if (filter && this.htmlNodeFilter != null) {
            html = this.htmlParser.parse(stream, encoding, this.htmlNodeFilter);
        } else {
            html = this.htmlParser.parse(stream, encoding);
        }

        long duration = System.currentTimeMillis() - before;
        if (logger.isDebugEnabled()) {
            logger.debug("Parsing document took " + duration + " ms");
        }
        return html;
    }
    

    public void setHtmlNodeFilter(HtmlNodeFilter htmlNodeFilter) {
        this.htmlNodeFilter = htmlNodeFilter;
    }

    public void setDecorationResolver(DecorationResolver decorationResolver) {
        this.decorationResolver = decorationResolver;
    }

    public void setHtmlParser(HtmlPageParser htmlParser) {
        this.htmlParser = htmlParser;
    }


    

    
}

/* Copyright (c) 2007, 2008, University of Oslo, Norway
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.vortikal.text.html.HtmlElement;
import org.vortikal.text.html.HtmlNodeFilter;
import org.vortikal.text.html.HtmlPage;
import org.vortikal.text.html.HtmlPageParser;

public class TemplateDecorator implements Decorator {

    private final static String EMPTY_DOCUMENT_START = 
        "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\"\n"
        + "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n"
        + "<html><head></head><body>";

    private final static String EMPTY_DOCUMENT_END = "</body></html>";

    private final static String EMPTY_DOCUMENT = EMPTY_DOCUMENT_START + EMPTY_DOCUMENT_END;
        
    private static Log logger = LogFactory.getLog(TemplateDecorator.class);
    
    private HtmlPageParser htmlParser;
    private DecorationResolver decorationResolver;
    boolean tidyXhtml = true;
    
    private List<HtmlNodeFilter> htmlNodeFilters;    

    private static final String DECORATION_DESCRIPTOR_REQ_ATTR = 
        TemplateDecorator.class.getName() + ".DecorationDescriptor";
    
    public boolean match(HttpServletRequest request, HttpServletResponse response) throws Exception {
        DecorationDescriptor descriptor = resolveDecorationDescriptor(request, response);
        if (descriptor != null) {
        	request.setAttribute(DECORATION_DESCRIPTOR_REQ_ATTR, descriptor);
        	return descriptor.decorate();
        }
        return false;
    }
    
    @SuppressWarnings("unchecked")
    public void decorate(Map model, HttpServletRequest request, HttpServletResponse response, Content content)
        throws Exception, UnsupportedEncodingException, IOException {

        DecorationDescriptor descriptor = (DecorationDescriptor) request.getAttribute(DECORATION_DESCRIPTOR_REQ_ATTR);
        if (logger.isDebugEnabled()) {
            logger.debug("Resolved decorator descriptor for request " 
                    + request + ": " + descriptor);
        }
        if (descriptor == null || !descriptor.decorate()) {
            return;
        }

        boolean filter = descriptor.parse();
        HtmlPage html = parseHtml(content, filter);
        if (logger.isDebugEnabled()) {
            logger.debug("Parsed document [root element: " + html.getRootElement() + " "
                         + ", doctype: "+ html.getDoctype() + "]");
        }

        Template template = descriptor.getTemplate();
        if (template == null) {
            replaceContentFromPage(content, html, descriptor.tidy());
            return;
        }

        if (isFrameset(html)) {
            // Framesets are not decorated:
            replaceContentFromPage(content, html, false);
            return;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Rendering request for " + request.getRequestURI()
                         + " using template '" + template + "'");
        }

        content.setContent(template.render(html, request, model));
        if (descriptor.tidy()) {
            tidyContent(content);
        }
    }

    protected boolean isFrameset(HtmlPage page) {
        HtmlElement rootElement = page.getRootElement();
        if (rootElement != null) {
            HtmlElement[] children = rootElement.getChildElements("frameset");
            if (children != null && children.length > 0) {
                return true;
            }
        } 
        return false;
    }
    

    protected void replaceContentFromPage(Content content, HtmlPage page,
                                          boolean tidy) throws Exception {
        if (page.getRootElement() == null) {
            return;
        }
        content.setContent(page.getStringRepresentation());
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
        tidy.setXHTML(tidyXhtml);
        tidy.setDocType("transitional"); 
        tidy.setCharEncoding(org.w3c.tidy.Configuration.UTF8);

        org.w3c.dom.Document document = tidy.parseDOM(inStream, null);
        java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
        tidy.pprint(document, outputStream);
            
        content.setContent(new String(outputStream.toByteArray(), "utf-8"));
    }
    
    protected DecorationDescriptor resolveDecorationDescriptor(
        HttpServletRequest request, HttpServletResponse response) throws Exception {
        return this.decorationResolver.resolve(request, response);
    }
    

    protected HtmlPage parseHtml(Content content, boolean filter) throws Exception {
        long before = System.currentTimeMillis();
        String encoding = content.getOriginalCharacterEncoding();
        String source = content.getContent();

        // Best-effort attempt to parse empty and "markup-less"
        // documents:
        if (source == null || "".equals(source.trim())) {
            source = EMPTY_DOCUMENT;
        } else if (!source.trim().startsWith("<")) {
            StringBuilder sb = new StringBuilder();
            sb.append(EMPTY_DOCUMENT_START).append(source).append(EMPTY_DOCUMENT_END);
            source = sb.toString();
        }

        InputStream stream = new ByteArrayInputStream(source.getBytes(encoding));
        HtmlPage html = null;
        if (filter && this.htmlNodeFilters != null) {
            html = this.htmlParser.parse(stream, encoding, this.htmlNodeFilters);
        } else {
            html = this.htmlParser.parse(stream, encoding);
        }

        long duration = System.currentTimeMillis() - before;
        if (logger.isDebugEnabled()) {
            logger.debug("Parsing document took " + duration + " ms");
        }
        return html;
    }
    

    public void setHtmlNodeFilters(List<HtmlNodeFilter> htmlNodeFilters) {
        this.htmlNodeFilters = htmlNodeFilters;
    }

    public void setDecorationResolver(DecorationResolver decorationResolver) {
        this.decorationResolver = decorationResolver;
    }

    public void setHtmlParser(HtmlPageParser htmlParser) {
        this.htmlParser = htmlParser;
    }

    public void setTidyXhtml(boolean tidyXhtml) {
        this.tidyXhtml = tidyXhtml;
    }

}

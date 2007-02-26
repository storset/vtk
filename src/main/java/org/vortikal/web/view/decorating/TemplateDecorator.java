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
    private TemplateResolver templateResolver;
    
    private HtmlNodeFilter htmlNodeFilter;    


    public void decorate(Map model, HttpServletRequest request, Content content)
        throws Exception, UnsupportedEncodingException, IOException {

        org.springframework.web.servlet.support.RequestContext ctx =
            new org.springframework.web.servlet.support.RequestContext(request);

        Template[] templates = resolveTemplates(model, request, ctx.getLocale());
            
        if (templates == null || templates.length == 0) {
            if (logger.isDebugEnabled()) {
                logger.debug("Unable to resolve template for request " + request);
            }
            return;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Rendering template sequence "
                         + java.util.Arrays.asList(templates));
        }


        for (int i = 0; i < templates.length; i++) {
            HtmlPage html = parseHtml(content.getContent());
            if (logger.isDebugEnabled()) {
                logger.debug("Parsed document [root element: " + html.getRootElement() + " "
                             + ", doctype: "+ html.getDoctype() + "]");
            }
            HtmlElement rootElement = html.getRootElement();
            if (rootElement != null && "frameset".equals(rootElement.getName())) {
                // Framesets are not decorated:
                continue;
            } 
            content.setContent(templates[i].render(model, html, request, ctx.getLocale()));
        }
    }

    protected HtmlPage parseHtml(String content) throws Exception {
        long before = System.currentTimeMillis();

        // XXX: encoding
        String encoding = "utf-8";        
        InputStream stream = new java.io.ByteArrayInputStream(content.getBytes(encoding));
        HtmlPage html = null;
        if (this.htmlNodeFilter != null) {
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
    
    protected Template[] resolveTemplates(Map model, HttpServletRequest request,
            Locale locale) throws Exception {
        return this.templateResolver.resolveTemplates(model, request, locale);

    }

    public void setHtmlNodeFilter(HtmlNodeFilter htmlNodeFilter) {
        this.htmlNodeFilter = htmlNodeFilter;
    }

    public void setTemplateResolver(TemplateResolver templateResolver) {
        this.templateResolver = templateResolver;
    }

    public void setHtmlParser(HtmlPageParser htmlParser) {
        this.htmlParser = htmlParser;
    }


    

    
}

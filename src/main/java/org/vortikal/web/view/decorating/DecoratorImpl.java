package org.vortikal.web.view.decorating;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.vortikal.util.repository.ContentTypeHelper;
import org.vortikal.util.text.HtmlUtil;
import org.vortikal.web.service.Assertion;
import org.vortikal.web.servlet.BufferedResponse;
import org.vortikal.web.view.decorating.html.HtmlElement;
import org.vortikal.web.view.decorating.html.HtmlNodeFilter;
import org.vortikal.web.view.decorating.html.HtmlPage;
import org.vortikal.web.view.decorating.html.HtmlPageParser;

public class DecoratorImpl implements Decorator {

    private static Log logger = LogFactory.getLog(DecoratorImpl.class);
    
    private boolean guessCharacterEncodingFromContent = false;

    private HtmlPageParser htmlParser;
    private TemplateResolver templateResolver;
    
    private HtmlNodeFilter htmlNodeFilter;    

    private String forcedOutputEncoding;
    private boolean appendCharacterEncodingToContentType = true;
    private Assertion[] assertions;

    public void decorate(Map model, HttpServletRequest request, BufferedResponse bufferedResponse)
        throws Exception, UnsupportedEncodingException, IOException {

        byte[] contentBuffer = bufferedResponse.getContentBuffer();

        String characterEncoding = null;
        String contentType = bufferedResponse.getContentType();
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        contentType = contentType.trim();
        if (contentType.indexOf("charset") != -1
                && contentType.indexOf(";") != -1) {
            contentType = contentType.substring(0, contentType.indexOf(";"));
            characterEncoding = bufferedResponse.getCharacterEncoding();
        } else if (this.guessCharacterEncodingFromContent) {
            characterEncoding = HtmlUtil
                    .getCharacterEncodingFromBody(contentBuffer);
        }

        if (!ContentTypeHelper.isHTMLContentType(contentType)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Unable to decorate response " + bufferedResponse
                             + " for requested URL " + request.getRequestURL()
                             + ": unsupported content type: " + contentType);

            }
            return;
        }

        if (characterEncoding == null) {
            characterEncoding = bufferedResponse.getCharacterEncoding();
        }

        if (!Charset.isSupported(characterEncoding)) {
            if (logger.isInfoEnabled()) {
                logger.info("Unable to perform content filtering on response  "
                        + bufferedResponse + " for requested URL "
                        + request.getRequestURL() + ": character encoding '"
                        + characterEncoding
                        + "' is not supported on this system");
            }
            return;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Reading buffered content using character encoding "
                    + characterEncoding);
        }

        String content = new String(contentBuffer, characterEncoding);
        
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
            // Reset response:
            bufferedResponse.reset();
            
            HtmlPage html = parseHtml(content);
            if (logger.isDebugEnabled()) {
                logger.debug("Parsed document [root element: " + html.getRootElement() + " "
                             + ", doctype: "+ html.getDoctype() + "]");
            }
            HtmlElement rootElement = html.getRootElement();
            if (rootElement != null && "frameset".equals(rootElement.getName())) {
                // Framesets are not decorated:
                continue;
            } else {
                templates[i].render(model, html, request, ctx.getLocale(), bufferedResponse);
                content = new String(bufferedResponse.getContentBuffer(),
                                     bufferedResponse.getCharacterEncoding());
            }
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

    public void setAppendCharacterEncodingToContentType(
            boolean appendCharacterEncodingToContentType) {
        this.appendCharacterEncodingToContentType = appendCharacterEncodingToContentType;
    }

    public void setAssertions(Assertion[] assertions) {
        this.assertions = assertions;
    }

    public void setForcedOutputEncoding(String forcedOutputEncoding) {
        this.forcedOutputEncoding = forcedOutputEncoding;
    }

    public void setGuessCharacterEncodingFromContent(
            boolean guessCharacterEncodingFromContent) {
        this.guessCharacterEncodingFromContent = guessCharacterEncodingFromContent;
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

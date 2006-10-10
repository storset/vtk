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
package org.vortikal.web.view.wrapper;

import com.opensymphony.module.sitemesh.HTMLPage;
import com.opensymphony.module.sitemesh.parser.FastPageParser;

import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.View;

import org.vortikal.util.repository.ContentTypeHelper;
import org.vortikal.util.text.HtmlUtil;
import org.vortikal.web.referencedata.ReferenceDataProvider;
import org.vortikal.web.referencedata.ReferenceDataProviding;
import org.vortikal.web.service.Assertion;
import org.vortikal.web.servlet.BufferedResponse;



/**
 */
public class DecoratingViewWrapper implements ViewWrapper, ReferenceDataProviding {

    protected Log logger = LogFactory.getLog(this.getClass());

    private String coreHtmlModelName = "core";

    private TemplateResolver templateResolver;
    
    private boolean propagateExceptions = true;
    private String forcedOutputEncoding;
    private boolean guessCharacterEncodingFromContent = false;
    private boolean appendCharacterEncodingToContentType = true;
    private ReferenceDataProvider[] referenceDataProviders;
    private Assertion[] assertions;
    


    public void setTemplateResolver(TemplateResolver templateResolver) {
        this.templateResolver = templateResolver;
    }
    

    public void setForcedOutputEncoding(String forcedOutputEncoding) {
        this.forcedOutputEncoding = forcedOutputEncoding;
    }


    public void setGuessCharacterEncodingFromContent(
            boolean guessCharacterEncodingFromContent) {
        this.guessCharacterEncodingFromContent = guessCharacterEncodingFromContent;
    }


    public void setAppendCharacterEncodingToContentType(
            boolean appendCharacterEncodingToContentType) {
        this.appendCharacterEncodingToContentType = appendCharacterEncodingToContentType;
    }


    public void setPropagateExceptions(boolean propagateExceptions) {
        this.propagateExceptions = propagateExceptions;
    }


    public ReferenceDataProvider[] getReferenceDataProviders() {
        return this.referenceDataProviders;
    }


    public void setReferenceDataProviders(
        ReferenceDataProvider[] referenceDataProviders) {
        this.referenceDataProviders = referenceDataProviders;
    }
    
    public void setAssertions(Assertion[] assertions) {
        this.assertions = assertions;
    }
    

    public void renderView(View view, Map model, HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        RequestWrapper requestWrapper = new RequestWrapper(request, "GET");
        BufferedResponse responseWrapper = new BufferedResponse();


        preRender(model, request, responseWrapper);

        if (this.propagateExceptions) {

            view.render(model, requestWrapper, responseWrapper);

        } else {

            try {
                view.render(model, requestWrapper, responseWrapper);
            } catch (Throwable t) {
                throw new ViewWrapperException(
                        "An error occurred while rendering the wrapped view",
                        t, model, view);
            }
        }
        postRender(model, request, responseWrapper, response);

    }


    public void preRender(Map model, HttpServletRequest request,
            BufferedResponse bufferedResponse) throws Exception {
    }


    public void postRender(Map model, HttpServletRequest request,
                           BufferedResponse bufferedResponse,
                           HttpServletResponse servletResponse) throws Exception {

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
//             throw new IllegalArgumentException(
//                 "Unable to decorate response " + bufferedResponse
//                 + " for requested URL " + request.getRequestURL()
//                 + ": unsupported content type: " + contentType);
            writeResponse(bufferedResponse, servletResponse);
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
            writeResponse(bufferedResponse, servletResponse);
            return;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Reading buffered content using character encoding "
                    + characterEncoding);
        }


        String content = new String(contentBuffer, characterEncoding);
        FastPageParser parser = new FastPageParser();
        HTMLPage html = (HTMLPage) parser.parse(new StringReader(content));

        Map coreHtml = new HashMap();
        coreHtml.put("title", html.getTitle());
        coreHtml.put("head", html.getHead());
        coreHtml.put("body", html.getBody());
        model.put(this.coreHtmlModelName, coreHtml);
        
        Template template = this.templateResolver.resolveTemplate(
            model, request, bufferedResponse);
        
        if (template == null) {
            writeResponse(bufferedResponse, servletResponse);
            return;
        }


        BufferedResponse templateResponse = new BufferedResponse();
        template.render(model, request, templateResponse);
        
        System.out.println("T_RESP: " + new String(templateResponse.getContentBuffer()));

        writeResponse(templateResponse, servletResponse, "text/html");
    }
    

    /**
     * Writes the buffer from the wrapped response to the actual
     * response. Sets the HTTP header <code>Content-Length</code> to
     * the size of the buffer in the wrapped response.
     * 
     * @param responseWrapper the wrapped response.
     * @exception Exception if an error occurs.
     */
    protected void writeResponse(BufferedResponse responseWrapper,
                                 ServletResponse response)
            throws Exception {

        //ServletResponse response = responseWrapper.getResponse();
        ServletOutputStream outStream = response.getOutputStream();
        byte[] content = responseWrapper.getContentBuffer();
        if (logger.isDebugEnabled()) {
            logger.debug("Write response: Content-Length: " + content.length
                    + ", unspecified content type");
        }
        response.setContentLength(content.length);
        response.setContentType(responseWrapper.getContentType());
        outStream.write(content);
        outStream.flush();
        outStream.close();
    }


    protected void writeResponse(BufferedResponse responseWrapper,
                                 ServletResponse response, String contentType)
            throws Exception {
        //ServletResponse response = responseWrapper.getResponse();
        byte[] content = responseWrapper.getContentBuffer();
        ServletOutputStream outStream = response.getOutputStream();

        if (logger.isDebugEnabled()) {
            logger.debug("Write response: Content-Length: " + content.length
                    + ", Content-Type: " + contentType);
        }
        response.setContentType(contentType);
        response.setContentLength(content.length);
        outStream.write(content);
        outStream.flush();
        outStream.close();
    }


    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(this.getClass().getName()).append(":");
        sb.append("]");
        return sb.toString();
    }

}

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

import java.nio.charset.Charset;
import java.util.Arrays;
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
import org.vortikal.web.servlet.BufferedResponseWrapper;

/**
 * FIXME... Provides response wrapping
 * for content (and header) manipulation.
 *
 * <p>Configurable JavaBean properties:
 * <ul>
 *   <li><code>propagateExceptions</code> a boolean specifying whether
 *   or not to let runtime exceptions that occur while rendering the
 *   wrapped view propagate all the way up to the caller. When set to
 *   <code>false</code>, all exceptions are caught and re-thrown
 *   wrapped inside a {@link ViewWrapperException}. The default value
 *   is <code>true</code>.
 * </ul>
 */
public class FilteringViewWrapper implements ViewWrapper, ReferenceDataProviding {

    private static Log logger = LogFactory.getLog(FilteringViewWrapper.class);

    private boolean propagateExceptions = true;
    private TextContentFilter[] contentFilters;
    private ReferenceDataProvider[] referenceDataProviders;

    public boolean guessCharacterEncodingFromContent = false;

    private boolean appendCharacterEncodingToContentType = true;

    public void setPropagateExceptions(boolean propagateExceptions) {
        this.propagateExceptions = propagateExceptions;
    }

    public void setContentFilters(TextContentFilter[] contentFilters) {
        this.contentFilters = contentFilters;
    }

    public void setGuessCharacterEncodingFromContent(
            boolean guessCharacterEncodingFromContent) {
        this.guessCharacterEncodingFromContent = guessCharacterEncodingFromContent;
    }

    public void setAppendCharacterEncodingToContentType(
            boolean appendCharacterEncodingToContentType) {
        this.appendCharacterEncodingToContentType = appendCharacterEncodingToContentType;
    }

    public void setReferenceDataProviders(ReferenceDataProvider[] referenceDataProviders) {
        this.referenceDataProviders = referenceDataProviders;
    }
    

    public ReferenceDataProvider[] getReferenceDataProviders() {
        return this.referenceDataProviders;
    }

    public void renderView(View view, Map model, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        BufferedResponseWrapper wrappedResponse = new BufferedResponseWrapper(
                response);

        preRender(model, request, wrappedResponse);

        if (this.propagateExceptions) {

            view.render(model, request, wrappedResponse);

        } else {

            try {
                view.render(model, request, wrappedResponse);
            } catch (Throwable t) {
                throw new ViewWrapperException(
                        "An error occurred while rendering the wrapped view",
                        t, model, view);
            }
        }
        postRender(model, request, wrappedResponse);

    }

    public void preRender(Map model, HttpServletRequest request,
            BufferedResponseWrapper bufferedResponse) throws Exception {
    }

    public void postRender(Map model, HttpServletRequest request,
            BufferedResponseWrapper bufferedResponse) throws Exception {

        if (logger.isDebugEnabled()) {
            logger.debug("About to process buffered content, content type: "
                    + bufferedResponse.getContentType()
                    + ", character encoding: "
                    + bufferedResponse.getCharacterEncoding());
        }

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
            writeResponse(bufferedResponse);
            return;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Reading buffered content using character encoding "
                    + characterEncoding);
        }

        String content = new String(contentBuffer, characterEncoding);

        if (contentFilters != null) {
            for (int i = 0; i < contentFilters.length; i++) {
                content = contentFilters[i].process(model, request, content);
                if (logger.isDebugEnabled()) {
                    logger.debug("Ran content filter " + contentFilters[i]
                            + ", content length after = " + content.length());
                }
            }
        }

        if (this.appendCharacterEncodingToContentType
                && ContentTypeHelper.isXMLContentType(contentType)) {

            contentType = contentType + ";charset=" + characterEncoding;
        }

        writeResponse(content.getBytes(characterEncoding), bufferedResponse,
                contentType);
    }

    /**
     * Writes the buffer from the wrapped response to the actual response. Sets
     * the HTTP header <code>Content-Length</code> to the size of the buffer
     * in the wrapped response.
     * 
     * @param wrappedResponse
     *            the wrapped response.
     * @exception Exception
     *                if an error occurs.
     */
    protected void writeResponse(BufferedResponseWrapper wrappedResponse)
            throws Exception {
        ServletResponse response = wrappedResponse.getResponse();
        ServletOutputStream outStream = response.getOutputStream();
        byte[] content = wrappedResponse.getContentBuffer();
        if (logger.isDebugEnabled()) {
            logger.debug("Write response: Content-Length: " + content.length
                    + ", unspecified content type");
        }
        response.setContentLength(content.length);
        outStream.write(content);
        outStream.flush();
        outStream.close();
    }

    protected void writeResponse(byte[] content,
            BufferedResponseWrapper wrappedResponse, String contentType)
            throws Exception {
        ServletResponse response = wrappedResponse.getResponse();
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

    // FIXME!
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(this.getClass().getName()).append(": [");
        sb.append(", contentFilters = ").append(
                Arrays.asList(this.contentFilters));
        sb.append("]");
        return sb.toString();
    }

}

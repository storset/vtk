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
package org.vortikal.web.view.wrapper;

import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.View;
import org.vortikal.web.servlet.BufferedResponse;
import org.vortikal.web.view.decorating.Decorator;


/**
 * 
 */
public class DecoratingViewWrapper implements ViewWrapper {

    protected Log logger = LogFactory.getLog(this.getClass());

    private Decorator decorator;
    private TextContentFilter textContentFilter;

    
    private boolean propagateExceptions = true;


    public void setPropagateExceptions(boolean propagateExceptions) {
        this.propagateExceptions = propagateExceptions;
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
        if (logger.isDebugEnabled()) {
            logger.debug("Rendered view " + view + ", proceeding to postRender step");
        }

        postRender(model, request, responseWrapper, response);
    }


    protected void preRender(Map model, HttpServletRequest request,
            BufferedResponse bufferedResponse) throws Exception {
    }


    protected void postRender(Map model, HttpServletRequest request,
                              BufferedResponse bufferedResponse,
                              HttpServletResponse servletResponse) throws Exception {

        
        
        if (this.textContentFilter != null) {
            try {
                String content = new String(bufferedResponse.getContentBuffer(), bufferedResponse.getCharacterEncoding());
                String result = this.textContentFilter.process(model, request, content);
                bufferedResponse.resetBuffer();
                PrintWriter writer = bufferedResponse.getWriter();
                writer.write(result);
            } catch (UnsupportedEncodingException e) {
            }
        }
        if (this.decorator != null) {
            this.decorator.decorate(model, request, bufferedResponse);
        }
        writeResponse(bufferedResponse, servletResponse,
                bufferedResponse.getContentType());
    }

    /**
     * Writes the buffer from the wrapped response to the actual
     * response. Sets the HTTP header <code>Content-Length</code> to
     * the size of the buffer in the wrapped response.
     * 
     * @param responseWrapper the wrapped response.
     * @param response the real servlet response.
     * @param contentType the content type of the response.
     * @exception Exception if an error occurs.
     */
    protected void writeResponse(BufferedResponse responseWrapper,
                                 ServletResponse response, String contentType)
            throws Exception {
        byte[] content = responseWrapper.getContentBuffer();
        
        ServletOutputStream outStream = response.getOutputStream();

        if (logger.isDebugEnabled()) {
            logger.debug("Write response: Content-Length: " + content.length
                    + ", Content-Type: " + contentType);
        }
        if (contentType.indexOf("charset") == -1) {
            response.setContentType(contentType  + ";charset=utf-8");
        } else {
            response.setContentType(contentType);
        }
        response.setContentLength(content.length);

        // Make sure content is not cached:
        if (response instanceof HttpServletResponse) {
            HttpServletResponse httpServletResponse = (HttpServletResponse) response;
            httpServletResponse.setHeader("Cache-Control", "no-cache, no-store, must-revalidate, max-age=0");
            httpServletResponse.setHeader("Pragma", "no-cache");
        }
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


    public void setDecorator(Decorator decorator) {
        this.decorator = decorator;
    }


    public void setTextContentFilter(TextContentFilter textContentFilter) {
        this.textContentFilter = textContentFilter;
    }


}

/* Copyright (c) 2004, University of Oslo, Norway
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
package org.vortikal.web.view;

import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.servlet.View;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.vortikal.web.servlet.BufferedResponseWrapper;


/**
 * Abstract base class for view wrappers. Provides response wrapping
 * for content (and header) manipulation.
 */
public abstract class AbstractViewWrapper
  extends AbstractReferenceDataProvidingWithChildrenView implements InitializingBean {


    protected Log logger = LogFactory.getLog(this.getClass());


    private View wrappedView = null;


    public void setWrappedView(View wrappedView) {
        this.wrappedView = wrappedView;
    }
    

    public View getWrappedView() {
        return this.wrappedView;
    }
    
    public View[] getViews() {
        return new View[] {this.wrappedView};
    }


    public void afterPropertiesSet() throws Exception {
        if (this.wrappedView == null) {
            throw new BeanInitializationException(
                "Required property 'wrappedView' not set");
        }
    }



    /**
     * The actual rendering. First obtains the wrapped servlet
     * response via <code>getWrappedResponse()</code>, then invokes
     * <code>render()</code> on the wrapped view object.
     *
     * @param model the Spring MVC model
     * @param request the servlet request
     * @param response the servlet response
     * @exception Exception if an error occurs
     */
    public final void renderMergedOutputModel(Map model, HttpServletRequest request,
                                              HttpServletResponse response)
        throws Exception {
        BufferedResponseWrapper wrappedResponse = new BufferedResponseWrapper(response);

        preRender(model, request, wrappedResponse);
        wrappedView.render(model, request, wrappedResponse);
        postRender(model, request, wrappedResponse);
    }
    
    

    /**
     * Writes the buffer from the wrapped response to the actual
     * response. Sets the HTTP header <code>Content-Length</code> to
     * the size of the buffer in the wrapped response.
     * @param wrappedResponse the wrapped response.
     * @exception Exception if an error occurs.
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
                                 BufferedResponseWrapper wrappedResponse, 
                                 String contentType) throws Exception {
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

    protected abstract void preRender(Map model, HttpServletRequest request, 
                                      BufferedResponseWrapper bufferedResponse) throws Exception;
    

    protected abstract void postRender(Map model, HttpServletRequest request, 
                                       BufferedResponseWrapper bufferedResponse) throws Exception;

    
}

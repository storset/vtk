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

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.View;
import org.vortikal.web.servlet.BufferedResponse;
import org.vortikal.web.view.wrapper.RequestWrapper;
import org.vortikal.web.view.wrapper.ViewWrapper;
import org.vortikal.web.view.wrapper.ViewWrapperException;


/**
 * 
 */
public class DecoratingViewWrapper implements ViewWrapper {

    protected Log logger = LogFactory.getLog(this.getClass());

    private Decorator decorator;
    private ResponseFilter responseFilter;

    
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

        if (this.responseFilter != null) {
            this.responseFilter.process(model, request, bufferedResponse);
        }
        if (this.decorator != null) {
            this.decorator.decorate(model, request, bufferedResponse, servletResponse);
        }
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


    public void setResponseFilter(ResponseFilter responseFilter) {
        this.responseFilter = responseFilter;
    }


}

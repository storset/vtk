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

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.AbstractView;

import org.vortikal.web.referencedata.ReferenceDataProvider;
import org.vortikal.web.referencedata.ReferenceDataProviding;
import org.vortikal.web.servlet.BufferedResponse;

/**
 * View implementation that takes a list of other views, concatenating
 * their output. The content type and character encoding can either be
 * configured statically, or decided at runtime, which means that the
 * content type of the last view in the chain takes precedence. The
 * HTTP status code set is always that of the last view.
 *
 * <p>Configurable properties (in addition to those defined by the
 *  {@link AbstractView superclass}):
 * <ul>
 *   <li><code>viewList</code> - the array of {@link View views} to
 *   invoke (in sequence)
 * </ul>
 * 
 * <p>TODO: what about other headers (Last-Modified, etc.)?
 *
 */
public class CompositeView extends AbstractView
  implements ReferenceDataProviding, InitializingBean {

    private static Log logger = LogFactory.getLog(CompositeView.class);
    
    private View[] views = null;
    private String contentType = null;
    private ReferenceDataProvider[] referenceDataProviders;
    
    public void setReferenceDataProviders(ReferenceDataProvider[] referenceDataProviders) {
        this.referenceDataProviders = referenceDataProviders;
    }


    public void setViews(View[] views) {
        this.views = views;
    }
    
    
    public void afterPropertiesSet() {        
        if (this.views == null) {
            throw new BeanInitializationException(
                "Bean property 'viewList' must be set");
        }
    }
    
    
    public void renderMergedOutputModel(Map model, HttpServletRequest request,
                           HttpServletResponse response) throws Exception {

        ByteArrayOutputStream bufferStream = new ByteArrayOutputStream();
        
        int sc = HttpServletResponse.SC_OK;
        String contentType = null;

        for (int i = 0; i < views.length; i++) {
            BufferedResponse bufferedResponse = new BufferedResponse();
            views[i].render(model, request, bufferedResponse);
            bufferStream.write(bufferedResponse.getContentBuffer());
            sc = bufferedResponse.getStatus();
            contentType = bufferedResponse.getContentType();
        }

        if (this.contentType != null) {
            contentType = this.contentType;
        }

        byte[] buffer = bufferStream.toByteArray();

        response.setStatus(sc);
        response.setContentType(contentType);
        response.setContentLength(buffer.length);

        OutputStream responseStream = null;

        try {
            if ("HEAD".equals(request.getMethod())) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Request is HEAD, not writing content");
                }
                response.flushBuffer();
            } else {
                responseStream = response.getOutputStream();
                responseStream.write(buffer);
            }

        } finally {
            if (responseStream != null) {
                responseStream.flush();
                responseStream.close();
            }
        }
    }

    /**
     * Gets the set of reference data providers. The list returned is the concatenation
     * of the providers set on this view and all providers for the list of
     * views.
     */
    public ReferenceDataProvider[] getReferenceDataProviders() {
        List providers = new ArrayList();

        if (this.referenceDataProviders != null) {
            providers.addAll(Arrays.asList(this.referenceDataProviders));
        }

        for (int i = 0; i < this.views.length; i++) {
            if (this.views[i] instanceof ReferenceDataProviding) {
                ReferenceDataProvider[] providerList = ((ReferenceDataProviding) this.views[i])
                        .getReferenceDataProviders();
                if (providerList != null && providerList.length > 0) {
                    providers.addAll(Arrays.asList(providerList));
                }
            }
        }

        return (ReferenceDataProvider[]) providers.toArray(new ReferenceDataProvider[providers.size()]);
    }
}

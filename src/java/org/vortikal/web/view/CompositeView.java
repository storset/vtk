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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.servlet.View;
import org.vortikal.web.referencedataprovider.Provider;
import org.vortikal.web.servlet.BufferedResponse;



/**
 * View implementation that takes a list of other views, concatenating
 * their output. The content type and character encoding can either be
 * configured statically, or decided at runtime, which means that the
 * content type of the last view in the chain takes precedence. The
 * HTTP status code set is always that of the last view.
 *
 * <p>TODO: what about other headers (Last-Modified, etc.)?
 *
 * <p>Configurable properties
 * <ul>
 *   <li>viewList - the list of views to run</li>
 *   <li>referenceDataProviders - list of reference data providers</li>
 * </ul>
 */
public class CompositeView implements View, ReferenceDataProviding,
                                      InitializingBean {

    private View[] viewList = null;
    private String contentType = null;
    private Provider[] referenceDataProviders = null;
    
    public void setViewList(View[] viewList) {
        this.viewList = viewList;
    }
    
    public void setReferenceDataProviders(Provider[] referenceDataProviders) {
        this.referenceDataProviders = referenceDataProviders;
    }
    

    public void afterPropertiesSet() {
        if (this.viewList == null) {
            throw new BeanInitializationException(
                "Bean property 'viewList' must be set");
        }
    }
    
    
    /**
     * Gets the set of reference data providers. The list returned is
     * the union of the providers set on this view and all providers
     * for the list of views.
     */
    public Provider[] getReferenceDataProviders() {
        Set providers = new HashSet();
        if (this.referenceDataProviders != null) {
            providers.addAll(Arrays.asList(this.referenceDataProviders));
                        
        }
        for (int i = 0; i < viewList.length; i++) {
            if (viewList[i] instanceof ReferenceDataProviding) {
                Provider[] providerList =
                    ((ReferenceDataProviding) viewList[i]).getReferenceDataProviders();
                if (providerList != null && providerList.length > 0) {
                    providers.addAll(
                        Arrays.asList(providerList));
                }
            }
        }
        return (Provider[]) providers.toArray(new Provider[0]);
    }


    public void render(Map model, HttpServletRequest request,
                           HttpServletResponse response) throws Exception {

        ByteArrayOutputStream bufferStream = new ByteArrayOutputStream();
        
        int sc = HttpServletResponse.SC_OK;
        String contentType = null;
        for (int i = 0; i < viewList.length; i++) {
            BufferedResponse bufferedResponse = new BufferedResponse();
            viewList[i].render(model, request, bufferedResponse);
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
            responseStream = response.getOutputStream();
            responseStream.write(buffer);
        } finally {
            if (responseStream != null) {
                responseStream.flush();
                responseStream.close();
            }
        }
    }
    
}

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.servlet.View;
import org.vortikal.web.referencedata.ReferenceDataProvider;
import org.vortikal.web.referencedata.ReferenceDataProviding;



/**
 * A view that applies a wrapper around the output of another
 * view. The {@link ViewWrapper} interface is utilized for the
 * wrapping.
 *
 * <p>The wrapping view implements {@link ReferenceDataProviding},
 * returning the following concatenated list of providers:
 * <ol>
 *   <li>The reference data providers specified on this view, if any
 *   <li>The providers from the view wrapper, if that wrapper
 *   implements {@link ReferenceDataProviding}
 *   <li>The providers from the view, if that view implements {@link
 *   ReferenceDataProviding}
 * </ol>
 *
 * <p>Configurable JavaBean properties:
 * <ul>
 *   <li><code>view</code> - the {@link View} to wrap around.
 *   <li><code>viewWrapper</code> - the {@link ViewWrapper} that
 *   performs the actual wrapping.
 *   <li><code>referenceDataProviders</code> - the array of {@link
 *   ReferenceDataProvider} objects specified on this view
 * </ul>
 * 
 * @see ViewWrapper
 */
public class WrappingView implements View, InitializingBean, ReferenceDataProviding {

    private View view;
    private ViewWrapper viewWrapper;
    private ReferenceDataProvider[] referenceDataProviders;
    

    public void setView(View view) {
        this.view = view;
    }


    public void setViewWrapper(ViewWrapper viewWrapper) {
        this.viewWrapper = viewWrapper;
    }


    public void setReferenceDataProviders(ReferenceDataProvider[] referenceDataProviders) {
        this.referenceDataProviders = referenceDataProviders;
    }
    


    public void afterPropertiesSet() throws Exception {
        if (this.view == null) {
            throw new BeanInitializationException(
                    "Required property 'view' not set");
        }
        if (this.viewWrapper == null) {
            throw new BeanInitializationException(
                    "Required property 'viewWrapper' not set");
        }
    }
    

    public void render(Map model, HttpServletRequest request,
                       HttpServletResponse response) throws Exception {
        RequestWrapper requestWrapper = new RequestWrapper(request, "GET");
        viewWrapper.renderView(view, model, requestWrapper, response);
    }


    public ReferenceDataProvider[] getReferenceDataProviders() {
        List providersList = new ArrayList();
        
        if (this.referenceDataProviders != null && this.referenceDataProviders.length > 0)
            providersList.addAll(Arrays.asList(this.referenceDataProviders));

        if (this.viewWrapper instanceof ReferenceDataProviding) {
            ReferenceDataProvider[] providers =
                ((ReferenceDataProviding) this.viewWrapper).getReferenceDataProviders();
            if (providers != null && providers.length > 0)
                providersList.addAll(Arrays.asList(providers));
        }

        if (this.view instanceof ReferenceDataProviding) {
            ReferenceDataProvider[] providers =
                ((ReferenceDataProviding) this.view).getReferenceDataProviders();
            if (providers != null && providers.length > 0)
                providersList.addAll(Arrays.asList(providers));
        }

        return (ReferenceDataProvider[]) providersList.toArray(
            new ReferenceDataProvider[providersList.size()]);
    }

}

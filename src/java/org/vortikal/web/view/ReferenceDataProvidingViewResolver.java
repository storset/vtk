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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.vortikal.web.referencedataprovider.Provider;
import org.vortikal.web.view.ReferenceDataProviding;


/**
 * TODO: This must be documented.
 */
public abstract class ReferenceDataProvidingViewResolver implements ViewResolver {

    private static Log logger = LogFactory.getLog(ReferenceDataProvidingViewResolver.class);

    /**
     * @see org.springframework.web.servlet.ViewResolver#resolveViewName(java.lang.String, java.util.Locale)
     */
    public View resolveViewName(String viewName, Locale locale) throws Exception {

        View view = getView(viewName);

        if (view != null && (view instanceof ReferenceDataProviding)) {
            Provider[] providers =
                ((ReferenceDataProviding) view).getReferenceDataProviders();
            
            if ((providers != null) && providers.length > 0) { 
                if (logger.isDebugEnabled()) {
                    logger.debug("Found reference data providers for view " 
                                 + viewName + ": " + Arrays.asList(providers));
                }
                return new ProviderRunningView(view, providers);
            }
        }

        return view;
    }

    protected abstract View getView(String viewName);
    
    /**
     * Wrapper class for the resolved view, running the <code>providers</code>
     * before the wrapped view is run (and the necessary model is available)
     */
    public class ProviderRunningView implements View {

        private Provider[] providers;
        private View view;

        /**
         * @param view - the view to eventually run
         * @param providers - the set of reference data providers for this
         * view
         */
        public ProviderRunningView(View view, Provider[] providers) {
            if (view == null) throw new IllegalArgumentException(
                "The wrapped view cannot be null");

            this.view = view;
            this.providers = providers;
        }
        

        /**
         * @see org.springframework.web.servlet.View#render(java.util.Map, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
         */
        public void render(Map model, HttpServletRequest request,
                           HttpServletResponse response) throws Exception {

            if (providers != null && providers.length > 0) {
                if (model == null) model = new HashMap();

                for (int i = 0; i < providers.length; i++) {
                    Provider provider = providers[i];
                    if (logger.isDebugEnabled()) 
                        logger.debug("Invoking reference data provider '" + 
                                provider + "'");
                    provider.referenceData(model, request);
                }
            }
            view.render(model, request, response);
        }
    }
}

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
package org.vortikal.web;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.vortikal.web.referencedataprovider.Provider;
import org.vortikal.web.view.ReferenceDataProviding;


/**
 * TODO: This must be documented.
 */
public class MappingViewResolver implements ViewResolver {

    private static Log logger = LogFactory.getLog(MappingViewResolver.class);

    private Map views;
    
    public View resolveViewName(String viewName, Locale locale)
        throws Exception {
        View view = (View) views.get(viewName);

        if (logger.isDebugEnabled()) {
            logger.debug("Matched view for viewName '" + viewName
                         + "' is: " + view);
        }

        if (view != null && (view instanceof ReferenceDataProviding)) {
            Provider[] providers =
                ((ReferenceDataProviding) view).getReferenceDataProviders();
            
            if ((providers != null) && providers.length > 0) { 
                if (logger.isDebugEnabled()) {
                    logger.debug("Found reference data providers for view " 
                                 + viewName + ": " + Arrays.asList(providers));
                }
                return new ReferenceDataProvidingView(view, providers);
            }
        }

        return view;
    }

    /**
     * @param views The views to set.
     */
    public void setViews(Map views) {
        this.views = views;
    }
}

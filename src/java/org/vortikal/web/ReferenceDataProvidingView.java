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

import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.vortikal.web.referencedataprovider.Provider;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.View;

/**
 * Wrapper class for views defined in the service tree. On service
 * view resolving the defined view is wrapped by this class, and gets
 * called after this view has run any model builders this has been
 * configured with.
 */
public class ReferenceDataProvidingView implements View {

    private static Log logger = LogFactory.getLog(ReferenceDataProvidingView.class);

    
    private Provider[] providers;
    private View view;

    
    
    /**
     * @param view - the view to eventually run
     * @param providers - the set of reference data providers for this
     * view
     */
    public ReferenceDataProvidingView(View view, Provider[] providers) {
        if (view == null) throw new IllegalArgumentException(
            "The wrapped view cannot be null");

        this.view = view;
        this.providers = providers;
    }
    

    public void render(Map model, HttpServletRequest request,
                       HttpServletResponse response) throws Exception {

        if (model == null) {
            model = new HashMap();
        }

        if (providers != null && providers.length > 0) {
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

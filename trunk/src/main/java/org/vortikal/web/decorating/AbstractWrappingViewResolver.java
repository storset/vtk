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
package org.vortikal.web.decorating;

import java.util.Locale;

import org.springframework.core.Ordered;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.vortikal.web.referencedata.ReferenceDataProvider;


public abstract class AbstractWrappingViewResolver implements ViewResolver, Ordered {

    private ViewWrapper viewWrapper;
    private ReferenceDataProvider[] referenceDataProviders;
    private int order = Integer.MAX_VALUE;


    @Override
    public View resolveViewName(String viewName, Locale locale) throws Exception {
        View view = resolveViewNameInternal(viewName, locale);
        
        if (view == null) {
            return null;
        }

        return new WrappingView(view, this.referenceDataProviders, this.viewWrapper);
    }

    /**
     * Actually resolves the view. Must be implemented by subclasses.
     * 
     * @param viewName the name of the view.
     * @return the resolved view.
     */
    protected abstract View resolveViewNameInternal(String viewName, Locale locale);

    
    public void setReferenceDataProviders(
        ReferenceDataProvider[] referenceDataProviders) {
        this.referenceDataProviders = referenceDataProviders;
    }

    
    public void setViewWrapper(ViewWrapper viewWrapper) {
        this.viewWrapper = viewWrapper;
    }


    public void setOrder(int order) {
        this.order = order;
    }
    

    public int getOrder() {
        return this.order;
    }
    
}

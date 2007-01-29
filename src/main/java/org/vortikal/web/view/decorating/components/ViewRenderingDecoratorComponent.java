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
package org.vortikal.web.view.decorating.components;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.View;

import org.vortikal.web.referencedata.ReferenceDataProvider;
import org.vortikal.web.referencedata.ReferenceDataProviding;
import org.vortikal.web.servlet.BufferedResponse;
import org.vortikal.web.servlet.BufferedResponseWrapper;
import org.vortikal.web.view.decorating.DecoratorComponent;
import org.vortikal.web.view.decorating.DecoratorRequest;


public class ViewRenderingDecoratorComponent extends AbstractDecoratorComponent {
    
    private View view;
    private ReferenceDataProvider[] referenceDataProviders;
    

    public void setView(View view) {
        this.view = view;
    }


    public void setReferenceDataProviders(
        ReferenceDataProvider[] referenceDataProviders) {
        this.referenceDataProviders = referenceDataProviders;
    }


    public ReferenceDataProvider[] getReferenceDataProviders() {
        List providers = new ArrayList();
        if (this.referenceDataProviders != null) {
            providers.addAll(Arrays.asList(this.referenceDataProviders));
        }

        if (this.view instanceof ReferenceDataProviding) {
            ReferenceDataProvider[] viewProviders =
                ((ReferenceDataProviding) this.view).getReferenceDataProviders();
            if (viewProviders != null) {
                providers.addAll(Arrays.asList(viewProviders));
            }
        }

        return (ReferenceDataProvider[]) providers.toArray(
            new ReferenceDataProvider[providers.size()]);
    }
    
    

    public String getRenderedContent(DecoratorRequest request) throws Exception {
        
        Map model = request.getModel();
        HttpServletRequest servletRequest = request.getServletRequest();

        BufferedResponse bufferedResponse = new BufferedResponse();
        this.view.render(model, servletRequest, bufferedResponse);
        return bufferedResponse.getContentString();
    }
    
    
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(this.getClass().getName()).append(": [");
        sb.append("view = ").append(this.view).append("]");
        return sb.toString();
    }
    
}

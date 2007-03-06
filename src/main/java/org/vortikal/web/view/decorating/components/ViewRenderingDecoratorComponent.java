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

import java.io.OutputStream;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.servlet.View;
import org.vortikal.web.referencedata.ReferenceDataProvider;
import org.vortikal.web.referencedata.ReferenceDataProviding;
import org.vortikal.web.servlet.BufferedResponse;
import org.vortikal.web.view.decorating.DecoratorRequest;
import org.vortikal.web.view.decorating.DecoratorResponse;


public class ViewRenderingDecoratorComponent extends AbstractDecoratorComponent {
    
    private View view;

    public void setView(View view) {
        this.view = view;
    }

    public final void render(DecoratorRequest request, DecoratorResponse response)
        throws Exception {
        Map model = new java.util.HashMap();
        processModel(model, request, response);
        renderView(model, request, response);
    }
    
    
    /**
     * Process the model prior to view rendering. The default
     * implementation is to gather all reference data providers (using
     * <code>getReferenceDataProviders()</code>) and invoke them in order.
     *
     * @param model the MVC model
     * @param request the decorator request
     * @param response the decorator response
     * @exception Exception if an error occurs
     */
    protected void processModel(Map model, DecoratorRequest request, DecoratorResponse response)
        throws Exception {

        if (!(this.view instanceof ReferenceDataProviding)) {
            return;
        }

        ReferenceDataProvider[] providers =
                ((ReferenceDataProviding) this.view).getReferenceDataProviders();

        if (providers == null) {
            return;
        }
        
        for (int i = 0; i < providers.length; i++) {
            providers[i].referenceData(model, request.getServletRequest());
        }
    }
    

    private void renderView(Map model, DecoratorRequest request, DecoratorResponse response)
        throws Exception {
        HttpServletRequest servletRequest = request.getServletRequest();
        BufferedResponse bufferedResponse = new BufferedResponse();
        this.view.render(model, servletRequest, bufferedResponse);
        response.setCharacterEncoding(bufferedResponse.getCharacterEncoding());
        OutputStream out = response.getOutputStream();
        out.write(bufferedResponse.getContentBuffer());
        out.close();
    }
    

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(this.getClass().getName()).append(": [");
        sb.append("view = ").append(this.view).append("]");
        return sb.toString();
    }


    protected String getDescriptionInternal() {
        return null;
    }


    protected Map getParameterDescriptionsInternal() {
        return null;
    }
    
}

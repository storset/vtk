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

import org.vortikal.web.service.Service;
import org.vortikal.web.RequestContext;


public class ServiceAwareTemplateResolver implements TemplateResolver {

    private TemplateManager templateManager;
    private Map serviceTemplatesMap;
    
    public void setTemplateManager(TemplateManager templateManager) {
        this.templateManager = templateManager;
    }
    

    public void setServiceTemplatesMap(Map serviceTemplatesMap) {
        this.serviceTemplatesMap = serviceTemplatesMap;
    }
    
    
    public Template resolveTemplate(Map model, HttpServletRequest request,
                                    HttpServletResponse response) throws Exception {
        Service currentService = null;
        RequestContext requestContext = RequestContext.getRequestContext();
        if (requestContext != null) {
            currentService = requestContext.getService();
        }

        if (currentService == null) {
            return null;
        }


        String templateName = null;

        do {
            templateName = (String) this.serviceTemplatesMap.get(currentService);
        } while (templateName == null
                 && (currentService = currentService.getParent()) != null);

        if (templateName == null) {
            throw new RuntimeException(
                "Unable to resolve template for request: " + request);
        }

        return this.templateManager.getTemplate(templateName);
    }
    
    
}

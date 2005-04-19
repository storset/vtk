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
package org.vortikal.web.view.freemarker;

import freemarker.template.Template;
import freemarker.template.TemplateException;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.support.RequestContextUtils;

import org.vortikal.web.referencedataprovider.Provider;
import org.vortikal.web.servlet.BufferedResponseWrapper;
import org.vortikal.web.view.ReferenceDataProviding;


/**
 * An extension of the FreeMarkerView provided in the Spring
 * Framework. Differs from that implementation in that a BeansWrapper
 * is used to wrap the model object before rendering, making
 * controllers independent of the view technology.
 *
 * <p>When the request method is <code>HEAD</code>, only headers is
 * written to the response.
 *
 * <p>Configurable JavaBean properties:
 * <ul>
 *   <li><code>debug</code> - a boolean indicating debug mode. When
 *   set to <code>true</code>, an entry in the model will be added
 *   with key <code>dumpedModel</code>, containing a string dump of
 *   the original model.
 * </ul>
 */
public class FreeMarkerView
  extends org.springframework.web.servlet.view.freemarker.FreeMarkerView 
  implements ReferenceDataProviding {

    private boolean debug = false;
    private Provider[] referenceDataProviders;
    
    public void setDebug(boolean debug)  {
        this.debug = debug;
    }

    protected void doRender(Map model, HttpServletRequest request,
                            HttpServletResponse response) throws Exception {
        BufferedResponseWrapper wrapper = new BufferedResponseWrapper(response);
        super.doRender(model, request, wrapper);

        ServletOutputStream outStream = response.getOutputStream();
        byte[] content = wrapper.getContentBuffer();
        response.setContentLength(content.length);
        if ("HEAD".equals(request.getMethod())) {
            if (logger.isDebugEnabled()) {
                logger.debug("Request is HEAD, not writing content");
            }
            response.flushBuffer();
            return;
        }

        outStream.write(content);
        outStream.flush();
        outStream.close();

        if (logger.isDebugEnabled()) {
            logger.debug("Wrote " + content.length + " bytes to response");
        }
    }


    protected void processTemplate(Template template, Map model,
                                   HttpServletResponse response)
        throws IOException, TemplateException {
        
        if (debug) {
            String debugModel = model.toString();
            model.put("dumpedModel", debugModel);
        }
        model.put("debug", new Boolean(debug));
        super.processTemplate(template, model, response);
    }

    public Provider[] getReferenceDataProviders() {
        return referenceDataProviders;
    }

    public void setReferenceDataProviders(Provider[] referenceDataProviders) {
        this.referenceDataProviders = referenceDataProviders;
    }

    public String toString() {
        return this.getClass().getName() + ":" + this.getUrl();
    }
}

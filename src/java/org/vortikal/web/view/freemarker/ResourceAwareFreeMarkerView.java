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
import java.util.Map;
import javax.servlet.http.HttpServletResponse;
import org.vortikal.repository.Resource;
import org.vortikal.util.web.HttpUtil;


/**
 * An extension of the Vortikal FreeMarker view that is "resource
 * aware". The model is examined for a configurable key containing a
 * {@link Resource} object. If such an object exists, The HTTP header
 * <code>Last-Modified</code> is set to the value of the resource
 * object's {@link Resource#getLastModified getLastModified()}.
 *
 * <p>Otherwise, the behavior is identical to that of the {@link
 * FreeMarkerView superclass}.
 */
public class ResourceAwareFreeMarkerView extends FreeMarkerView {

    private String resourceModelKey = "resource";

    public void setResourceModelKey(String resourceModelKey) {
        this.resourceModelKey = resourceModelKey;
    }
    

    protected void processTemplate(Template template, Map model,
                                   HttpServletResponse response)
        throws IOException, TemplateException {
        
        if (model.containsKey(this.resourceModelKey)) {
            Object o = model.get(this.resourceModelKey);
            if (o instanceof Resource) {
                Resource resource = (Resource) o;
                response.setHeader("Last-Modified", 
                                   HttpUtil.getHttpDateString(resource.getLastModified()));
            }
        }
        super.processTemplate(template, model, response);
    }

}

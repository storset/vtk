/* Copyright (c) 2008, University of Oslo, Norway
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

import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.servlet.support.RequestContext;
import org.vortikal.web.view.decorating.DecoratorRequest;
import org.vortikal.web.view.decorating.DecoratorResponse;

public class LocalizedTextComponent extends AbstractDecoratorComponent 
    implements InitializingBean {

    private static final String DESCRIPTION = 
          "Looks up and displays a localized message from a message bundle "
        + "using the current request's locale.";
    
    
    private static final String PARAMETER_CODE = "code";
    private static final String PARAMETER_CODE_DESC = "The localization code";
    
    private static final String PARAMETER_DEFAULT = "default";
    private static final String PARAMETER_DEFAULT_DESC = "The default message. "
        + "This message is displayed if no translation could be found for the code";

    protected String getDescriptionInternal() {
        return DESCRIPTION;
    }

    protected Map<String, String> getParameterDescriptionsInternal() {
        Map<String, String> map = new LinkedHashMap<String, String>();
        
        map.put(PARAMETER_CODE, PARAMETER_CODE_DESC);
        map.put(PARAMETER_DEFAULT, PARAMETER_DEFAULT_DESC);
        return map;
    }

    public void render(DecoratorRequest request, DecoratorResponse response)
        throws Exception {
        String code = request.getStringParameter(PARAMETER_CODE);
        String defaultValue = request.getStringParameter(PARAMETER_DEFAULT);
        RequestContext rc = new RequestContext(request.getServletRequest());

        String result = null;
        if (defaultValue != null) {
            result = rc.getMessage(code, defaultValue);
        } else {
            result = rc.getMessage(code);
        }
        Writer writer = response.getWriter();
        writer.write(result);
        writer.flush();
        writer.close();
    }
}

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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.View;

/**
 * HTTP status view with parameterizable status code and headers.
 * 
 * <p>Configurable JavaBean properties:
 * <ul>
 * <li><code>statusCode</code> - an integer specifying the HTTP status
 * code. Default is <code>200</code>
 * </li>
 * <li><code>statusMessage</code> - a string specifying the HTTP
 * status message. Default is <code>null</code> (leave it to the
 * servlet container).
 * </li>
 * <li><code>headers</code> - a {@link Map} specifying the HTTP
 * headers to send.
 * </li>
 * </ul></p>
 * 
 * <p>Optional model properties:
 * <ul>
 *  <li><code>statusMessage</code> - a string specifying HTTP status message.
 *  This model property will override any statically configured status message for
 *  a particular request.
 *  </li>
 *  <li><code>statusCode</code> - an <code>Integer</code> specifying the HTTP status code.
 *  This model property will override any statically configured status message for
 *  a particular request.
 *  </li>
 *  <li><code>headers</code> - a {@link Map} specifying the HTTP headers to send. 
 *  This model property will override any statically configured headers
 *  for a particular request.
 *  </li>
 * </ul>
 * </p>
 * 
 */
public class StandardHttpView implements View {

    private int statusCode = HttpServletResponse.SC_OK;
    private String statusMessage = null;
    private Map<String, Object> headers = new HashMap<String, Object>();

    @SuppressWarnings({ "unchecked", "deprecation", "rawtypes" })
    public void render(Map model, HttpServletRequest request,
                       HttpServletResponse response) throws Exception {
        if (model == null) {
            model = new HashMap();
        }
        Integer modelStatusCode = (Integer) model.get("statusCode");
        String modelStatusMessage = (String) model.get("statusMessage");
        Map<String, Object> modelHeaders = (Map<String, Object>) model.get("headers");
        
        int statusCode = modelStatusCode != null ? modelStatusCode.intValue() : this.statusCode;
        String statusMessage = modelStatusMessage != null ? modelStatusMessage : this.statusMessage;
        Map<String, Object> headers = modelHeaders != null ? modelHeaders : this.headers;
        
        if (statusMessage != null) {
            response.setStatus(statusCode, statusMessage);
        } else {
            response.setStatus(statusCode);
        }

        for (String name: headers.keySet()) {
            Object value = headers.get(name);

            if (value instanceof Date) {
                response.setDateHeader(name, ((Date) value).getTime());

            } else if (value instanceof Integer) {
                response.setIntHeader(name, ((Integer) value).intValue());

            } else if (value instanceof String) {
                response.setHeader(name, (String) value);

            } else {
                throw new IllegalStateException(
                    "Header values must be of one of the types: "
                    + "Date, String or Integer");
            }
        }
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }
    
    public void setHeaders(Map<String, Object> headers) {
        this.headers = headers;
    }

    public String getContentType() {
        return null;
    }

}

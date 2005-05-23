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
import java.util.Iterator;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.View;


/**
 * HTTP status view with parameterizable status code and headers.
 * 
 * <p>Configurable JavaBean properties:
 * <ul><code>statusCode</code> - an integer specifying the HTTP status
 * code. Default is <code>200</code>
 * <ul><code>statusMessage</code> - a string specifying the HTTP
 * status message. Default is <code>null</code> (leave it to the
 * servlet container).
 * <ul><code>headers</code> - a {@link Map} specifying the HTTP
 * headers to send.
 */
public class StandardHttpView implements View {

    private int statusCode = HttpServletResponse.SC_OK;
    private String statusMessage = null;
    private Map headers = new HashMap();

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    
    public void setHeaders(Map headers) {
        this.headers = headers;
    }
    

    public void render(Map model, HttpServletRequest request,
                       HttpServletResponse response) throws Exception {

        if (this.statusMessage != null) {
            response.setStatus(this.statusCode, this.statusMessage);
        } else {
            response.setStatus(this.statusCode);
        }

        for (Iterator i = this.headers.keySet().iterator(); i.hasNext();) {
            String name = (String) i.next();
            Object value = this.headers.get(name);

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

}

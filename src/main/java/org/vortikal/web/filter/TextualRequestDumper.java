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
package org.vortikal.web.filter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.vortikal.security.SecurityContext;
import org.vortikal.util.io.StreamUtil;
import org.vortikal.util.repository.ContentTypeHelper;

public class TextualRequestDumper extends AbstractRequestFilter {

    private String logName = this.getClass().getName();
    
    public void setLogName(String logName) {
        this.logName = logName;
    }

    public HttpServletRequest filterRequest(HttpServletRequest request) {
        if (!ContentTypeHelper.isTextContentType(request.getContentType())) {
            return request;
        }
        return new RequestWrapper(request);
    }
    
    private class RequestWrapper extends HttpServletRequestWrapper {
        
        private HttpServletRequest request;
                
        public RequestWrapper(HttpServletRequest request) {
            super(request);
            this.request = request;
        }
        
        public ServletInputStream getInputStream() throws IOException {            

            ServletInputStream stream = this.request.getInputStream();
            Log logger = LogFactory.getLog(logName);
            if (!logger.isDebugEnabled()) {
                return stream;
            }
            
            byte[] buffer = StreamUtil.readInputStream(stream);

            StringBuilder dump = new StringBuilder();
            dump.append("\n--- Begin request: ").append(request.getRequestURI());
            dump.append("\n--- Principal: ").append(SecurityContext.getSecurityContext().getPrincipal());
            dump.append("\n--- Headers:\n");
            Enumeration<?> headers = request.getHeaderNames(); 
            while (headers.hasMoreElements()) {
                String header = (String) headers.nextElement();
                dump.append(header).append(": ").append(request.getHeader(header)).append("\n");
            }                
            dump.append("\n--- Body:\n");
            String encoding = request.getCharacterEncoding();
            if (encoding == null) encoding = "utf-8";
            String body = new String(buffer, encoding);
            dump.append(body);
            dump.append("\n--- End request\n\n");
            logger.debug(dump);
            return new org.vortikal.util.io.ServletInputStream(
                    new ByteArrayInputStream(buffer));

        }
    }

}

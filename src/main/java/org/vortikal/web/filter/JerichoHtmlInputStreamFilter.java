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

import au.id.jericho.lib.html.CharStreamSource;
import au.id.jericho.lib.html.Source;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.vortikal.util.io.StreamUtil;


public class JerichoHtmlInputStreamFilter implements RequestFilter {

    private Log logger = LogFactory.getLog(JerichoHtmlInputStreamFilter.class);
    private int order = Integer.MAX_VALUE;
    
    public void setOrder(int order) {
        this.order = order;
    }

    public int getOrder() {
        return this.order;
    }
    
    public HttpServletRequest filterRequest(HttpServletRequest request) {
        return new JerichoContentRequestWrapper(request);
    }
    
    private class JerichoContentRequestWrapper extends HttpServletRequestWrapper {
        
        private HttpServletRequest request;
        
        public JerichoContentRequestWrapper(HttpServletRequest request) {
            super(request);
            this.request = request;
        }
        
        public ServletInputStream getInputStream() throws IOException {            

            ServletInputStream stream = this.request.getInputStream();
            byte[] buffer = StreamUtil.readInputStream(stream);
            ByteArrayInputStream originalStream = new ByteArrayInputStream(buffer);

            try {
                Source source = new Source(originalStream);
                StringWriter err = new StringWriter();
                source.setLogWriter(err);
                source.fullSequentialParse();
                String indentText = "  ";
                boolean tidyTags = true;
                boolean collapseWhiteSpace = true;
                boolean indentAllElements = false;

                CharStreamSource cs = source.indent(
                    indentText, tidyTags, collapseWhiteSpace, indentAllElements);

                if (logger.isDebugEnabled()) {
                    logger.debug("Transformed document '" + request.getRequestURI()
                                 + "', warnings: '" + err.toString() + "'");
                }

                ByteArrayOutputStream result = new ByteArrayOutputStream();
                cs.writeTo(new OutputStreamWriter(result));
                
                return new org.vortikal.util.io.ServletInputStream(
                    new ByteArrayInputStream(result.toByteArray()));
            } catch (Exception e) {
                logger.warn("Unable to transform document '"
                            + request.getRequestURI() + "', returning original", e);
                return new org.vortikal.util.io.ServletInputStream(originalStream);
            }

        }
    }
    
}

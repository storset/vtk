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
import java.io.InputStream;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.springframework.beans.factory.annotation.Required;

import org.vortikal.text.html.HtmlPage;
import org.vortikal.text.html.HtmlPageParser;


public class HtmlParserInputStreamFilter extends AbstractRequestFilter {

    private HtmlPageParser parser;
    
    @Required public void setParser(HtmlPageParser parser) {
        this.parser = parser;
    }
    
    public HttpServletRequest filterRequest(HttpServletRequest request) {
        try {
            return new HtmlParserRequestWrapper(request);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    private class HtmlParserRequestWrapper extends HttpServletRequestWrapper {
        
        private HttpServletRequest request;
        private HtmlPage page;

        public HtmlParserRequestWrapper(HttpServletRequest request) throws Exception {
            super(request);
            this.request = request;
            this.page = parse();
        }
        
        public ServletInputStream getInputStream() throws IOException {
            return new org.vortikal.util.io.ServletInputStream(
                new ByteArrayInputStream(this.page.getStringRepresentation().getBytes(
                                             this.request.getCharacterEncoding())));
        }

        private HtmlPage parse() throws Exception {
            InputStream is = this.request.getInputStream();
            HtmlPage page = parser.parse(is, this.request.getCharacterEncoding());
            return page;
        }
    }
    
}

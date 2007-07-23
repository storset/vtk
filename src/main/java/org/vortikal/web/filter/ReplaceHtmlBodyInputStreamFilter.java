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
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.springframework.beans.factory.annotation.Required;

import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.SecurityContext;
import org.vortikal.text.html.HtmlContent;
import org.vortikal.text.html.HtmlElement;
import org.vortikal.text.html.HtmlPage;
import org.vortikal.text.html.HtmlPageParser;
import org.vortikal.web.RequestContext;


public class ReplaceHtmlBodyInputStreamFilter extends AbstractRequestFilter {

    private HtmlPageParser parser;
    
    private Repository repository;
    
    @Required public void setParser(HtmlPageParser parser) {
        this.parser = parser;
    }
    
    @Required public void setRepository(Repository repository) {
        this.repository = repository;
    }
    
    
    public HttpServletRequest filterRequest(HttpServletRequest request) {
        try {
            return new ReplaceHtmlBodyContentRequestWrapper(request);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    private class ReplaceHtmlBodyContentRequestWrapper extends HttpServletRequestWrapper {
        
        private HttpServletRequest request;
        private Resource resource;
        
        private HtmlPage originalPage;
        private HtmlPage suppliedPage;
        

        public ReplaceHtmlBodyContentRequestWrapper(HttpServletRequest request) throws Exception {
            super(request);
            this.request = request;
            this.resource = getResource();
            this.originalPage = getOriginalPage();
            this.suppliedPage = getSuppliedPage();

        }
        
        public ServletInputStream getInputStream() throws IOException {
            
            HtmlElement originalRoot = originalPage.getRootElement();
            if (originalRoot.getChildElements("body") == null) {
                throw new RuntimeException(
                    "Unable to find body element of original document");
            }
            HtmlElement originalBody = findBodyTag(originalRoot);
            
            HtmlElement suppliedBody = findBodyTag(suppliedPage.getRootElement());
            List<HtmlContent> newContent = new ArrayList<HtmlContent>();
            for (HtmlContent e: originalRoot.getChildNodes()) {
                if (e != originalBody) {
                    newContent.add(e);
                } else {
                    newContent.add(suppliedBody);
                }
            }

            originalRoot.setChildNodes(newContent.<HtmlContent>toArray(
                                              new HtmlContent[newContent.size()]));
            
            return new org.vortikal.util.io.ServletInputStream(
                new ByteArrayInputStream(originalPage.getStringRepresentation().getBytes(
                                             this.resource.getCharacterEncoding())));
        }


        private Resource getResource() throws Exception {
            RequestContext ctx = RequestContext.getRequestContext();
            String uri = ctx.getResourceURI();
            String token = SecurityContext.getSecurityContext().getToken();
            Resource resource = repository.retrieve(token, uri, false);
            return resource;
        }
        

        private HtmlPage getOriginalPage() throws Exception {
            RequestContext ctx = RequestContext.getRequestContext();
            String uri = ctx.getResourceURI();
            String token = SecurityContext.getSecurityContext().getToken();
            InputStream is = repository.getInputStream(token, uri, false);
            HtmlPage page = parser.parse(is, resource.getCharacterEncoding());
            return page;
        }
        
        private HtmlPage getSuppliedPage() throws Exception {
            return parser.parse(this.request.getInputStream(), this.request.getCharacterEncoding());
        }
        
        private HtmlElement findBodyTag(HtmlElement e) {
            if ("body".equalsIgnoreCase(e.getName())) {
                return e;
            }
            HtmlElement[] children = e.getChildElements();
            for (HtmlElement child: children) {
                HtmlElement body = findBodyTag(child);
                if (body != null) {
                    return body;
                }
            }
            return null;
        }

    }
    
}

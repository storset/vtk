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
package org.vortikal.web.decorating.components;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.context.ServletContextAware;
import org.vortikal.web.decorating.DecoratorRequest;
import org.vortikal.web.servlet.BufferedResponse;
import org.vortikal.web.servlet.VortikalServlet;

import com.sun.syndication.feed.synd.SyndFeed;

public class LocalFeedFetcher implements ServletContextAware {

    private ServletContext servletContext;
    private SyndFeedBuilder feedBuilder;

    public SyndFeed getFeed(String url, DecoratorRequest request) throws Exception {
        InputStream stream = retrieveLocalStream(url, request);
        SyndFeed feed = this.feedBuilder.build(stream);
        return feed;
    }

    private InputStream retrieveLocalStream(String uri, DecoratorRequest request)
        throws Exception {

        HttpServletRequest servletRequest = request.getServletRequest();
        if (servletRequest.getAttribute(IncludeComponent.INCLUDE_ATTRIBUTE_NAME) != null) {
            throw new DecoratorComponentException("Error including URI '" + uri
                    + "': possible include loop detected ");
        }

        // XXX: encode URI?
        RequestWrapper requestWrapper = new RequestWrapper(servletRequest, uri);
        requestWrapper.setAttribute(IncludeComponent.INCLUDE_ATTRIBUTE_NAME, new Object());

        String servletName = (String) servletRequest
                .getAttribute(VortikalServlet.SERVLET_NAME_REQUEST_ATTRIBUTE);

        RequestDispatcher rd = this.servletContext.getNamedDispatcher(servletName);

        if (rd == null) {
            throw new RuntimeException("No request dispatcher for name '"
                    + servletName + "' available");
        }

        BufferedResponse servletResponse = new BufferedResponse();
        rd.forward(requestWrapper, servletResponse);

        requestWrapper.setAttribute(IncludeComponent.INCLUDE_ATTRIBUTE_NAME, null);
        return new ByteArrayInputStream(servletResponse.getContentBuffer());
    }

    @Required public void setFeedBuilder(SyndFeedBuilder feedBuilder) {
        this.feedBuilder = feedBuilder;
    }

    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    private class RequestWrapper extends HttpServletRequestWrapper {

        private String requestUri;
        private String queryString;
        private Map<String, String> params = new HashMap<String, String>();
        
        public RequestWrapper(HttpServletRequest request, String uri) {
            super(request);
            if (uri.indexOf("?") == -1) {
                this.requestUri = uri;
            } else {
                this.requestUri = uri.substring(0, uri.indexOf("?"));
                this.queryString = uri.substring(uri.indexOf("?") + 1);
                StringTokenizer tokenizer = new StringTokenizer(this.queryString, "&");
                while (tokenizer.hasMoreTokens()) {
                    String s = tokenizer.nextToken();
                    if (s.indexOf("=") == -1) {
                        params.put(s, null);
                    } else {
                        params.put(s.substring(0, s.indexOf("=")),
                                   s.substring(s.indexOf("=") + 1));
                    }
                }
            }
        }
        
        public String getRequestURI() {
            return requestUri;
        }

        public String getQueryString() {
            return this.queryString;
        }

        public String getParameter(String name) {
            return this.params.get(name);
        }

        public Map<String, String> getParameterMap() {
            return Collections.unmodifiableMap(this.params);
        }

        public Enumeration<String> getParameterNames() {
            return Collections.enumeration(this.params.keySet());
        }

        public String[] getParameterValues(String name) {
            String value = this.params.get(name);
            if (value == null)
                return null;
            return new String[] {value};
        }
    }

}

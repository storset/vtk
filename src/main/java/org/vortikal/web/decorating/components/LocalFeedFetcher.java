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
import java.util.List;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.context.ServletContextAware;
import org.vortikal.repository.Path;
import org.vortikal.web.decorating.DecoratorRequest;
import org.vortikal.web.service.URL;
import org.vortikal.web.servlet.BufferedResponse;
import org.vortikal.web.servlet.VortikalServlet;

import com.sun.syndication.feed.synd.SyndFeed;

public class LocalFeedFetcher implements ServletContextAware {

    private ServletContext servletContext;
    private SyndFeedBuilder feedBuilder;

    public SyndFeed getFeed(String url, DecoratorRequest request) throws Exception {
        if (url == null || !url.startsWith("/")) {
            throw new IllegalArgumentException("Invalid URL: " + url 
                    + ": must start with '/'");
        }
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
        if (servletResponse.getStatus() != HttpServletResponse.SC_OK) {
            return null;
        }
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
        private URL requestURL;
        
        public RequestWrapper(HttpServletRequest request, String uri) {
            super(request);
            URL url = URL.parse(request.getRequestURL().toString());
            url.clearParameters();
            url.setPath(Path.ROOT);
            if (uri.indexOf("?") == -1) {
                if (uri.endsWith("/") && !uri.equals("/")) {
                    uri = uri.substring(0, uri.length() - 1);
                    url.setCollection(true);
                }
                Path path = Path.fromString(uri);
                url.setPath(path);
            } else {
                String queryString = uri.substring(uri.indexOf("?") + 1);
                Map<String, String[]> query = URL.splitQueryString(queryString);
                String requestPath = uri.substring(0, uri.indexOf("?"));
                if (requestPath.endsWith("/") && !requestPath.equals("/")) {
                    requestPath = requestPath.substring(0, requestPath.length() - 1);
                    url.setCollection(true);
                }
                Path path = Path.fromString(requestPath);
                url.setPath(path);
                for (String param: query.keySet()) {
                    for (String value: query.get(param)) {
                        url.addParameter(param, value);
                    }
                }
            }
            this.requestURL = url;
        }
        
        @Override
        public StringBuffer getRequestURL() {
            return new StringBuffer(this.requestURL.toString());
        }

        @Override
        public String getRequestURI() {
            return this.requestURL.getPathEncoded();
        }

        @Override
        public String getQueryString() {
            String s = this.requestURL.getQueryString();
            if (s != null) {
                return "?" + s;
            }
            return null;
        }

        @Override
        public String getParameter(String name) {
            return this.requestURL.getParameter(name);
        }

        @Override
        public Map<String, String> getParameterMap() {
            Map<String, String> m = new HashMap<String, String>();
            for (String name: this.requestURL.getParameterNames()) {
                m.put(name, this.requestURL.getParameter(name));
            }
            return m;
        }

        @Override
        public Enumeration<String> getParameterNames() {
            return Collections.enumeration(this.requestURL.getParameterNames());
        }

        @Override
        public String[] getParameterValues(String name) {
            List<String> values = this.requestURL.getParameters(name);
            if (values == null) {
                return null;
            }
            return values.toArray(new String[values.size()]);
        }
    }
}

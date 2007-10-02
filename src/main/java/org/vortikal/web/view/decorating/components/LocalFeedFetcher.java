package org.vortikal.web.view.decorating.components;

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

import org.vortikal.web.servlet.BufferedResponse;
import org.vortikal.web.servlet.VortikalServlet;
import org.vortikal.web.view.decorating.DecoratorRequest;

import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;

public class LocalFeedFetcher {

    private ServletContext servletContext;

    public LocalFeedFetcher(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    public SyndFeed getFeed(String url, DecoratorRequest request) throws Exception {
        
        InputStream stream = retrieveLocalStream(url, request);
        XmlReader xmlReader = new XmlReader(stream);
        SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed = input.build(xmlReader);
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

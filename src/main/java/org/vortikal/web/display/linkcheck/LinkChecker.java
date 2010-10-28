/* Copyright (c) 2010, University of Oslo, Norway
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
package org.vortikal.web.display.linkcheck;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URLDecoder;
import java.util.HashSet;
import java.util.Set;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Path;
import org.vortikal.repository.search.ResultSet;
import org.vortikal.repository.search.Search;
import org.vortikal.repository.search.Searcher;
import org.vortikal.repository.search.query.TermOperator;
import org.vortikal.repository.search.query.UriTermQuery;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.service.URL;

public class LinkChecker implements InitializingBean {
    
    private Searcher searcher;
    private CacheManager cacheManager;
    private Cache cache;
    private int connectTimeout = 5000;
    private int readTimeout = 5000;
    private String userAgent = "Link checker";
    private Set<String> localHostNames = new HashSet<String>();
    private boolean optimizeLocalLinks = false;
    
    public static class LinkCheckResult {
        private String link;
        private Status status;
        public LinkCheckResult(String link, Status status) {
            this.link = link;
            this.status = status;
        }
        public String getLink() {
            return this.link;
        }
        public String getStatus() {
            return this.status.toString();
        }
    }
    
    private enum Status {
        OK,
        NOT_FOUND,
        TIMEOUT,
        MALFORMED_URL,
        ERROR;
    }
    
    
    public LinkCheckResult validate(String link, Path base) {
        if (link == null) {
            throw new IllegalArgumentException("Link argument cannot be NULL");
        }
        if (!isWebLink(link)) {
            return new LinkCheckResult(link, validateInternalLink(base, link));
        }
        String cacheKey = link;
        Element cached = this.cache.get(cacheKey);
        if (cached != null) {
            return (LinkCheckResult) cached.getObjectValue();
        }
        URL url;
        try {
            url = getURL(link);
        } catch (Throwable t) {
            return new LinkCheckResult(link, Status.MALFORMED_URL);
        }
        Status status;
        try {
            if (this.optimizeLocalLinks && this.localHostNames.contains(url.getHost())) {
                status = validateInternalLink(base, url.getPath().toString());
                return new LinkCheckResult(link, status);
            }
            status = validateURL(url);
        } catch (Throwable t) {
            t.printStackTrace();
            status = Status.ERROR;
        }
        LinkCheckResult result = new LinkCheckResult(link, status);
        this.cache.put(new Element(link, result));
        return result;
    }
    
    private URL getURL(String link) {
        link = link.trim();
        link = trimTrailingSlash(link);
        if (link.contains("#")) {
            link = link.substring(0, link.indexOf("#"));
        }
        return URL.parse(link);
    }

    private Status validateURL(URL url) {
        // go out on the worldwide web
        HttpURLConnection urlConnection = null;
        try {
            urlConnection = createHeadRequest(url);
            urlConnection.connect();
            int responseCode = urlConnection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_MOVED_PERM 
                    || responseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
                // check if moved location is valid
                responseCode = checkMoved(urlConnection, responseCode);
            }
            if (responseCode == HttpURLConnection.HTTP_NOT_FOUND 
                || responseCode == HttpURLConnection.HTTP_GONE) {
                return Status.NOT_FOUND;
            }
            return Status.OK;
        } catch (SocketTimeoutException e) {
            return Status.TIMEOUT;
        } catch (Exception e) {
            return Status.ERROR;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    private int checkMoved(HttpURLConnection urlConnection, int responseCode) throws IOException {
        int retry = 0;
        // try a maximum of three times
        while (retry < 3
                && (responseCode == HttpURLConnection.HTTP_MOVED_PERM || responseCode == HttpURLConnection.HTTP_MOVED_TEMP)) {
            String location = urlConnection.getHeaderField("Location");
            urlConnection.disconnect();
            if (location == null) {
                return responseCode;
            }
            urlConnection = createHeadRequest(URL.parse(location));
            urlConnection.connect();
            responseCode = urlConnection.getResponseCode();
            retry++;
        }
        return responseCode;
    }
    
    private HttpURLConnection createHeadRequest(URL url) throws IOException {
        java.net.URL location = new java.net.URL(url.toString());
        HttpURLConnection urlConnection = (HttpURLConnection) location.openConnection();
        urlConnection.setRequestMethod("HEAD");
        urlConnection.setConnectTimeout(this.connectTimeout);
        urlConnection.setReadTimeout(this.readTimeout);
        urlConnection.setRequestProperty("User-Agent", this.userAgent);
        return urlConnection;
    }

    private boolean isWebLink(String link) {
        return link.startsWith("http") || link.startsWith("www");
    }

    private Status validateInternalLink(Path base, String link) {
        if (link.contains("?")) {
            link = link.substring(0, link.indexOf("?"));
        }
        if (link.contains("#")) {
            link = link.substring(0, link.indexOf("#"));
        }
        link = trimTrailingSlash(link);
        if (!link.startsWith("/")) {
            try {
                link = base.expand(link).toString();
            } catch (Exception e) {
                return Status.MALFORMED_URL;
            }
        }
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        String token = securityContext.getToken();
        try {
            link = URLDecoder.decode(link, "utf-8");
        } catch (UnsupportedEncodingException e) { }

        UriTermQuery uriQuery = new UriTermQuery(link, TermOperator.EQ);
        Search search = new Search();
        search.setQuery(uriQuery);
        search.setLimit(1);
        ResultSet rs = this.searcher.execute(token, search);
        if (rs.getSize() > 0) {
            return Status.OK;
        }
        return Status.NOT_FOUND;
    }

    private String trimTrailingSlash(String pathString) {
        while (pathString.endsWith("/") && !Path.ROOT.toString().equals(pathString)) {
            pathString = pathString.substring(0, pathString.lastIndexOf("/"));
        }
        return pathString;
    }
    
    @Required
    public void setSearcher(Searcher searcher) {
        this.searcher = searcher;
    }

    @Required
    public void setCacheManager(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }
    
    public void setConnectTimeout(int connectTimeout) {
        if (connectTimeout < 0) {
            throw new IllegalArgumentException("Connect timeout must be an integer >= 0");
        }
        this.connectTimeout = connectTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        if (readTimeout < 0) {
            throw new IllegalArgumentException("Read timeout must be an integer >= 0");
        }
        this.readTimeout = readTimeout;
    }
    
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public void setLocalHostNames(Set<String> localHostNames) {
        this.localHostNames = localHostNames;
    }

    public void setOptimizeLocalLinks(boolean optimizeLocalLinks) {
        this.optimizeLocalLinks = optimizeLocalLinks;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.cache = this.cacheManager.getCache("org.vortikal.LINK_CHECK_CACHE");
    }
    
}

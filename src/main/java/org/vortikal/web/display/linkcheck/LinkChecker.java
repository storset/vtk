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
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.search.ResultSet;
import org.vortikal.repository.search.Search;
import org.vortikal.repository.search.Searcher;
import org.vortikal.repository.search.query.TermOperator;
import org.vortikal.repository.search.query.UriTermQuery;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.service.URL;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

public class LinkChecker {
    
    private Searcher searcher;
    private Cache cache;
    private int connectTimeout = 5000;
    private int readTimeout = 5000;
    private String userAgent = "Link checker";
    
    public static final class LinkCheckResult implements Serializable {
        private String link;
        private Status status;
        private String reason;
        public LinkCheckResult(String link, Status status) {
            this(link, status, null);
        }
        public LinkCheckResult(String link, Status status, String reason) {
            this.link = link;
            this.status = status;
            this.reason = reason;
        }
        public String getLink() {
            return this.link;
        }
        public String getStatus() {
            return this.status.toString();
        }
        public String getReason() {
            return this.reason;
        }
    }
    
    public enum Status {
        OK,
        NOT_FOUND,
        TIMEOUT,
        MALFORMED_URL,
        ERROR;
    }
    
    public LinkCheckResult validate(String href, URL base) {
        if (href == null) {
            throw new IllegalArgumentException("Link argument cannot be NULL");
        }
        boolean internal = !isExternalLink(href);
        URL url;
        try {
            url = base.relativeURL(href);
        } catch (Throwable t) {
            return new LinkCheckResult(href, Status.MALFORMED_URL, t.getMessage());
        }
        if (internal) {
            Status status = indexLookup(url);
            if (status == Status.OK) {
                return new LinkCheckResult(href, status);
            }
        }
        String cacheKey = href;
        Element cached = this.cache.get(cacheKey);
        if (cached != null) {
            return (LinkCheckResult) cached.getValue();
        }
        Status status = null;
        String reason = null;
        try {
            status = validateURL(url);
        } catch (Throwable t) {
            status = Status.ERROR;
            reason = t.getMessage();
        }
        LinkCheckResult result = new LinkCheckResult(href, status, reason);
        this.cache.put(new Element(href, result));
        return result;
    }
    
    private Status indexLookup(URL url) {
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        String token = securityContext.getToken();
        String link = url.getPath().toString();
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

    private boolean isExternalLink(String link) {
        return link.startsWith("http://") || link.startsWith("https://");
    }

    @Required
    public void setSearcher(Searcher searcher) {
        this.searcher = searcher;
    }

    @Required
    public void setCacheManager(CacheManager cacheManager) {
        Cache c = cacheManager.getCache("org.vortikal.LINK_CHECK_CACHE");
        if (c == null) throw new IllegalArgumentException("Provided cache manager has no cache named 'org.vortikal.LINK_CHECK_CACHE'");
        this.cache = c;
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

}

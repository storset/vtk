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

import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.AuthorizationException;
import org.vortikal.repository.Path;
import org.vortikal.repository.Repository;
import org.vortikal.repository.ResourceNotFoundException;
import org.vortikal.repository.search.ResultSet;
import org.vortikal.repository.search.Search;
import org.vortikal.repository.search.query.TermOperator;
import org.vortikal.repository.search.query.UriTermQuery;
import org.vortikal.security.AuthenticationException;
import org.vortikal.security.SecurityContext;

public class LinkChecker implements InitializingBean {
    
    private Repository repository;
    private CacheManager cacheManager;
    private Cache cache;
    private int connectTimeout = 5000;
    private int readTimeout = 5000;
    private String userAgent = "Link checker";
    
    public static class LinkCheckResult {
        private String link;
        private boolean found;
        private Throwable throwable;
        public LinkCheckResult(String link, boolean found) {
            this(link, found, null);
        }
        public LinkCheckResult(String link, boolean found, Throwable t) {
            this.link = link;
            this.found = found;
            this.throwable = t;
        }
        public String getLink() {
            return this.link;
        }
        public boolean isFound() {
            return this.found;
        }
        public Throwable error() {
            return this.throwable;
        }
    }
    
    
    public LinkCheckResult validate(String link, Path base) {
        
        if (!isWebLink(link)) {
            link = getProcessedLink(base, link);
            boolean found = !isBrokenInternal(link);
            return new LinkCheckResult(link, found);
        }
        
        String cacheKey = link;
        Element cached = this.cache.get(cacheKey);
        if (cached != null) {
            System.out.println("__cache_hit: " + link + ", found: " + ((LinkCheckResult) cached.getObjectValue()).isFound());
            return (LinkCheckResult) cached.getObjectValue();
        }

        LinkCheckResult result;
        try {
            boolean found = !isBroken(link, base);
            result  = new LinkCheckResult(link, found);
        } catch (Throwable t) {
            result = new LinkCheckResult(link, false, t);
        }
        System.out.println("__cache_miss: " + link + ", found: " + result.found);
        this.cache.put(new Element(link, result));
        return result;
    }

    private boolean isBroken(String link, Path base) {
        // get the processed link to check
        String processedLink = getProcessedLink(base, link);

        if (!(processedLink.startsWith("http://") || processedLink.startsWith("https://"))) {
            processedLink = "http://" + processedLink;
        }

        // go out on the worldwide web
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(processedLink);
            urlConnection = createHeadRequest(url);
            urlConnection.connect();
            int responseCode = urlConnection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_MOVED_PERM || responseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
                // check if moved location is valid
                responseCode = this.checkMoved(urlConnection, responseCode);
            }
            return responseCode != HttpURLConnection.HTTP_OK;
        } catch (Exception e) {
            return true;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    private int checkMoved(HttpURLConnection urlConnection, int responseCode) throws Exception {
        int retry = 0;
        // try a maximum of three times
        while (retry < 3
                && (responseCode == HttpURLConnection.HTTP_MOVED_PERM || responseCode == HttpURLConnection.HTTP_MOVED_TEMP)) {
            String location = urlConnection.getHeaderField("Location");
            urlConnection.disconnect();
            urlConnection = createHeadRequest(new URL(location));
            urlConnection.connect();
            responseCode = urlConnection.getResponseCode();
            retry++;
        }
        return responseCode;
    }
    
    private HttpURLConnection createHeadRequest(URL location) throws Exception {
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

    private boolean isBrokenInternal(String link) {
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        String token = securityContext.getToken();
        try {
            link = URLDecoder.decode(link, "utf-8");
        } catch (UnsupportedEncodingException e) { }
        UriTermQuery uriQuery = new UriTermQuery(link, TermOperator.EQ);
        Search search = new Search();
        search.setQuery(uriQuery);
        search.setLimit(1);
        ResultSet rs = this.repository.search(token, search);
        boolean broken = rs.getSize() == 0;
        if (broken) {
            try {
                Path path = Path.fromString(link);
                this.repository.retrieve(token, path, false);
                return false;
            } catch (IllegalArgumentException e) {
                return true;
            } catch (ResourceNotFoundException e) {
                return true;
            } catch (AuthorizationException e) {
                return false;
            } catch (AuthenticationException e) {
                return false;
            } catch (Exception e) {
                return true;
            }
        }
        return broken;
    }

    private String getProcessedLink(Path resourceURI, String link) {
        link = trimTrailingSlashInPathString(link);
        if (!isWebLink(link) && !link.startsWith("/")) {
            link = resourceURI.getParent().extend(link).toString();
        }

        if (link.contains("?")) {
            link = link.substring(0, link.indexOf("?"));
        }
        if (link.contains("#")) {
            link = link.substring(0, link.indexOf("#"));
        }
        return link;
    }

    private String trimTrailingSlashInPathString(String pathString) {
        while (pathString.endsWith("/") && !Path.ROOT.toString().equals(pathString)) {
            pathString = pathString.substring(0, pathString.lastIndexOf("/"));
        }
        return pathString;
    }
    
    @Required
    public void setRepository(Repository repository) {
        this.repository = repository;
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

    @Override
    public void afterPropertiesSet() throws Exception {
        this.cache = this.cacheManager.getCache("org.vortikal.LINK_CHECK_CACHE");
    }
    
}

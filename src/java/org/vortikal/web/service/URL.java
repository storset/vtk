/* Copyright (c) 2004, University of Oslo, Norway
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
package org.vortikal.web.service;

import java.util.Map;
import org.vortikal.util.web.URLUtil;
import java.util.Iterator;


/**
 * Class for representing HTTP(s) URLs. Resembles {@link
 * java.net.URL}, except that it has getters/setters for all URL
 * components to achieve easy "on-the-fly" manipulation when
 * constructing URLs.
 *
 * <p>TODO: possibly incorporate authentication information?
 */
public class URL {

    private String protocol = null;
    private String host = null;
    private int port = 80;
    //private String authority = null;
    //private String userInfo = null;
    private String path = null;
    private Map query = null;
    private String ref = null;
    

    public URL(String protocol, String host, String path) {
        if (!("http".equals(protocol) || "https".equals(protocol))) {
            throw new IllegalArgumentException("Unknown protocol: '" + protocol + "'");
        }
        if (host == null || "".equals(host.trim())) {
            throw new IllegalArgumentException("Invalid hostname: '" + host + "'");
        }
        if (path == null || "".equals(path.trim()) || !path.startsWith("/")) {
            throw new IllegalArgumentException("Invalid path: '" + path + "'");
        }

        this.protocol = protocol.trim();
        this.host = host.trim();
        this.path = path;
        if ("http".equals(this.protocol)) {
            this.port = 80;
        } else {
            this.port = 443;
        }
    }
    
    public String getProtocol() {
        return this.protocol;
    }
    

    public void setProtocol(String protocol) {
        if (!("http".equals(protocol) || "https".equals(protocol))) {
            throw new IllegalArgumentException("Unknown protocol: '" + protocol + "'");
        }
        this.protocol = protocol.trim();
    }

    
    public String getHost() {
        return this.host;
    }
    

    public void setHost(String host) {
        if (host == null || "".equals(host.trim())) {
            throw new IllegalArgumentException("Invalid hostname: '" + host + "'");
        }
        this.host = host;
    }
    

    public int getPort() {
        return this.port;
    }
    

    public void setPort(int port) {
        if (port <= 0) {
            throw new IllegalArgumentException("Invalid port number: " + port);
        }
        this.port = port;
    }


    public String getPath() {
        return this.path;
    }
    

    public void setPath(String path) {
        if (path == null || "".equals(path.trim()) || !path.startsWith("/")) {
            throw new IllegalArgumentException("Invalid path: '" + path + "'");
        }
        this.path = path;
    }
    
    
    public Map getQuery() {
        return this.query;
    }
    

    public void setQuery(Map query) {
        this.query = query;
    }
    

    public String getRef() {
        return this.ref;
    }
    

    public void setRef(String ref) {
        this.ref = ref;
    }
    

    public String toString() {
        StringBuffer url = new StringBuffer();
        
        url.append(this.protocol).append("://");
        url.append(this.host);
        if (!(this.port == 80 && "http".equals(this.protocol)
              || (this.port == 443 && "https".equals(this.protocol)))) {
            url.append(":").append(this.port);
        }
        url.append(URLUtil.urlEncode(this.path));
                
        if (this.query != null && !this.query.isEmpty()) {
            url.append("?");
            for (Iterator iter = this.query.keySet().iterator(); iter.hasNext();) {
                String name = (String) iter.next();
                Object value = this.query.get(name);
                url.append(name).append("=").append(value);
                if (iter.hasNext()) url.append("&");
            }
        }

        if (this.ref != null) {
            url.append("#").append(this.ref);
        }

        return url.toString();
    }
    
}

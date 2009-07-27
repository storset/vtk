/* Copyright (c) 2004, 2007, University of Oslo, Norway
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.vortikal.repository.Path;
import org.vortikal.util.web.URLUtil;


/**
 * Class for representing HTTP(s) URLs. Resembles {@link
 * java.net.URL}, except that it has getters/setters for all URL
 * components to achieve easy "on-the-fly" manipulation when
 * constructing URLs.
 */
public class URL {

    private String protocol = null;
    private String host = null;
    private Integer port = null;
    private Path path = null;
    private String characterEncoding = "utf-8";
    private Map<String, List<String>> parameters = new LinkedHashMap<String, List<String>>();
    private String ref = null;
    private boolean pathOnly = false;
    private boolean collection = false;
    
    private static final Integer PORT_80 = new Integer(80);
    private static final Integer PORT_443 = new Integer(443);
    
    private static final String PROTOCOL_HTTP = "http";
    private static final String PROTOCOL_HTTPS = "https";
    
    /**
     * Construct a new <code>URL</code> instance that is a copy
     * if the provided original.
     * 
     * @param original The original <code>URL</code> instance to base the new
     *                 instance on.
     */
    public URL(URL original) {
        this.protocol = original.protocol;
        this.host = original.host;
        this.port = original.port;
        this.path = original.path;
        this.characterEncoding = original.characterEncoding;
        this.ref = original.ref;
        this.pathOnly = original.pathOnly;
        this.collection = original.collection;
        
        // Copy parameter map
        for (Map.Entry<String, List<String>> entry: original.parameters.entrySet()) {
            String key = entry.getKey();
            List<String> values = entry.getValue();
            
            List<String> copiedValues = new ArrayList<String>(values.size());
            for (String value: values) {
                copiedValues.add(value);
            }
            this.parameters.put(key, copiedValues);
        }
    }

    public URL(String protocol, String host, Path path) {
        if (!(PROTOCOL_HTTP.equals(protocol) || PROTOCOL_HTTPS.equals(protocol))) {
            throw new IllegalArgumentException("Unknown protocol: '" + protocol + "'");
        }
        if (host == null || "".equals(host.trim())) {
            throw new IllegalArgumentException("Invalid hostname: '" + host + "'");
        }
        if (path == null) {
            throw new IllegalArgumentException("Path argument cannot be NULL");
        }

        this.protocol = protocol.trim();
        this.host = host.trim();
        this.path = path;
    }
    

    public String getProtocol() {
        return this.protocol;
    }
    

    public void setProtocol(String protocol) {
        if (protocol != null) {
            protocol = protocol.trim();
        }
        if (!(PROTOCOL_HTTP.equals(protocol) || PROTOCOL_HTTPS.equals(protocol))) {
            throw new IllegalArgumentException("Unknown protocol: '" + protocol + "'");
        }

        this.protocol = protocol.trim();

        if (PROTOCOL_HTTP.equals(protocol) && PORT_443.equals(this.port)) {
            this.port = PORT_80;
        } else if (PROTOCOL_HTTPS.equals(protocol) && PORT_80.equals(this.port)) {
            this.port = PORT_443;
        }
    }

    
    public String getHost() {
        return this.host;
    }
    

    public void setHost(String host) {
        if (host == null || "".equals(host.trim())) {
            throw new IllegalArgumentException(
                "Invalid hostname: '" + host + "'");
        }
        this.host = host;
    }
    

    public Integer getPort() {
        return this.port;
    }
    

    public void setPort(Integer port) {
        if (port != null && port.intValue() <= 0) {
            throw new IllegalArgumentException(
                "Invalid port number: " + port.intValue());
        }
        this.port = port;
    }


    public Path getPath() {
        return this.path;
    }
    

    public void setPathOnly(boolean pathOnly) {
        this.pathOnly = pathOnly;
    }

    public boolean isPathOnly() {
        return pathOnly;
    }

    public void setPath(Path path) {
        if (path == null) {
            throw new IllegalArgumentException("Path cannot be NULL");
        }
        this.path = path;
    }
    
    public boolean isCollection() {
        return this.collection;
    }
    
    public void setCollection(boolean collection) {
        this.collection = collection;
    }
    
    public void addParameter(String name, String value) {
        List<String> values = this.parameters.get(name);
        if (values == null) {
            values = new ArrayList<String>();
            this.parameters.put(name, values);
        }
        values.add(value);
    }
    
    public void setParameter(String name, String value) {
        if (this.parameters.containsKey(name)) {
            this.parameters.remove(name);
        }
        List<String> values = new ArrayList<String>();
        values.add(value);
        this.parameters.put(name, values);
    }

    public void removeParameter(String name) {
        this.parameters.remove(name);
    }
    

    public void clearParameters() {
        this.parameters = new LinkedHashMap<String, List<String>>();
    }
    

    public String getParameter(String parameterName) {
        List<String> values = this.parameters.get(parameterName);
        if (values == null || values.size() == 0) {
            return null;
        }
        return values.get(0);
    }
    
    public List<String> getParameters(String parameterName) {
        List<String> values = this.parameters.get(parameterName);
        if (values == null || values.size() == 0) {
            return null;
        }
        return Collections.list(Collections.enumeration(values));
    }
    

    public List<String> getParameterNames() {
        return Collections.list(Collections.enumeration(this.parameters.keySet()));
    }

    public String getQueryString() {
        if (this.parameters.isEmpty()) {
            return null;
        }
        StringBuilder qs = new StringBuilder();
        for (Iterator<String> i = this.parameters.keySet().iterator(); i.hasNext();) {
            String param = i.next();
            String encodedParam = URLUtil.urlEncode(param);
            List<String> values = this.parameters.get(param);
            for (Iterator<String> j = values.iterator(); j.hasNext();) {
                String val = j.next();
                val = URLUtil.urlEncode(val);
                qs.append(encodedParam).append("=").append(val);
                if (j.hasNext()) {
                    qs.append("&");
                }
            }
            if (i.hasNext()) {
                qs.append("&");
            }
        }
        return qs.toString();
    }
    

    public String getRef() {
        return this.ref;
    }
    

    public void setRef(String ref) {
        this.ref = ref;
    }
    

    /**
     * Sets the character encoding used when URL-encoding the path.
     */
    public void setCharacterEncoding(String characterEncoding) {
        if (characterEncoding == null) {
            throw new IllegalArgumentException("Character encoding must be specified");
        }
        java.nio.charset.Charset.forName(characterEncoding);
        this.characterEncoding = characterEncoding;
    }

    public String toString() {
        if (this.pathOnly) return this.getPathRepresentation();
        
        StringBuilder url = new StringBuilder();
        
        url.append(this.protocol).append("://");
        url.append(this.host);
        if (this.port != null) {
            if (!(this.port.equals(PORT_80) && PROTOCOL_HTTP.equals(this.protocol)
                  || (this.port.equals(PORT_443) && PROTOCOL_HTTPS.equals(this.protocol)))) {
                url.append(":").append(this.port.intValue());
            }
        }
        url.append(getPathRepresentation());

        return url.toString();
    }

    
    /**
     * Generates an "absolute path" representation of this URL (a
     * string starting with '/', without protocol, hostname and port
     * information).
     */
    public String getPathRepresentation() {
        StringBuilder sb = new StringBuilder();
        try {
            sb.append(URLUtil.urlEncode(this.path.toString(), this.characterEncoding));
        } catch (java.io.UnsupportedEncodingException e) {
            // Ignore, this.characterEncoding is supposed to be valid.
        }
      
        if (this.collection && !this.path.isRoot()) {
            sb.append("/");
        }
  
        String qs = getQueryString();
        if (qs != null) {
            sb.append("?");
            sb.append(qs);
        }

        if (this.ref != null) {
            sb.append("#").append(this.ref);
        }

        return sb.toString();
    }

    /**
     * Utility method to create a URL from an existing URL. The newly
     * created URL will contain the exact same data as the existing
     * one.
     *
     * @param url the existing URL
     * @return the generated URL
     */
    public static URL create(URL url) {
        URL newURL = new URL(url.getProtocol(), url.getHost(), url.getPath());
        newURL.port = url.port;
        newURL.characterEncoding = url.characterEncoding;
        newURL.parameters = new LinkedHashMap<String, List<String>>(url.parameters);
        newURL.ref = url.ref;
        return newURL;
    }


    /**
     * Utility method to create a URL from a servlet request.
     *
     * @param request the servlet request
     * @return the generated URL
     */
    public static URL create(HttpServletRequest request) {

        String path = request.getRequestURI();
        if (path == null || "".equals(path)) path = "/";

        if (!path.equals("/") && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        
        String host = request.getServerName();
        int port = request.getServerPort();

        URL url = new URL(PROTOCOL_HTTP, host, Path.fromString(path));
        url.setPort(new Integer(port));
        if (request.isSecure()) {
            url.setProtocol(PROTOCOL_HTTPS);
        }

        Map<String, String[]> queryStringMap = URLUtil.splitQueryString(request.getQueryString());

        for (String key: queryStringMap.keySet()) {
            String[] values = queryStringMap.get(key);
            key = URLUtil.urlDecode(key);
            for (String value: values) {
                url.addParameter(key, URLUtil.urlDecode(value));
            }
        }
        return url;
    }
    

//     public static URL parse(String url) {
//         if (url == null || "".equals(url.trim())) {
//             throw new IllegalArgumentException("Illegal URL: " + url);
//         }
//         if (url.indexOf("://") < 1) {
//             throw new IllegalArgumentException("Illegal URL: " + url);
//         }
//         String protocol = url.substring(0, url.indexOf("://"));

//         String host = url.substring(url.indexOf("://") + 3);
//         host = host.substring(0, host.indexOf("/"));
//         if (host.indexOf(":") > 0) {
//             host = host.substring(0, host.indexOf(":"));
//         }
        
//         int pathStartIdx = 0;
//         pathStartIdx = url.indexOf("://");
//         pathStartIdx = url.indexOf("/", pathStartIdx + 1);
//         int pathEndIdx = url.indexOf("?");
//         if (pathEndIdx > 0) {
//             pathEndIdx = url.length();
//         }
//         String path = url.substring(pathStartIdx, pathEndIdx);

//         URL resultURL = new URL(protocol, host, path);

//         Map<String, String[]> queryParams = new HashMap<String, String[]>();
//         if (url.indexOf("?") > 0) {
//             queryParams = URLUtil.splitQueryString(url.substring(url.indexOf("?")));
//         }
//         for (String param: queryParams.keySet()) {
//             String[] values = queryParams.get(param);
//             for (String value: values) {
//                 resultURL.addParameter(param, value);
//             }
//         }
//         return resultURL;
//     }
    

}

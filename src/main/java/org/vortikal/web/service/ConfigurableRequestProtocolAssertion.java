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

import java.util.List;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repository.Path;
import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;

/**
 * Assertion that matches on URI path prefixes.  Uses a {@link
 * Properties} object with entries of type <code>&lt;uri-prefix&gt; =
 * &lt;value&gt;</code>.
 * 
 * <p>Configurable properties:
 * <ul>
 *   <li><code>configuration</code> - the {@link Properties} object
 *   </li>
 * </ul>
 */
public class ConfigurableRequestProtocolAssertion implements Assertion, InitializingBean {

    private final static String PROTO_HTTP = "http";
    private final static String PROTO_HTTPS = "https";

    private Properties configuration;
    private boolean invert = false;
    
    public void setConfiguration(Properties configuration) {
        this.configuration = configuration;
    }
    
    public void setInvert(boolean invert) {
        this.invert = invert;
    }
    
    public void afterPropertiesSet() {
        if (this.configuration == null) throw new IllegalArgumentException(
            "JavaBean property 'configuration' not specified");
    }

    public boolean conflicts(Assertion assertion) {
        return false;
    }

    public void processURL(URL url) {
        Path uri = url.getPath();
        if (this.configuration == null || this.configuration.isEmpty()) {
            url.setProtocol(PROTO_HTTP);
            return;
        }
        
        while (uri != null) {
            String value = this.configuration.getProperty(uri.toString());
            if (value != null) {
                value = value.trim();
                if (PROTO_HTTP.equals(value) || PROTO_HTTPS.equals(value)) {
                    url.setProtocol(invertProtocol(value, this.invert));
                    return;
                }
            }
            uri = uri.getParent();
        }
        url.setProtocol(PROTO_HTTP);
    }


    public boolean processURL(URL url, Resource resource, Principal principal, boolean match) {
        processURL(url);
        return true;
    }


    public boolean matches(HttpServletRequest request, Resource resource,
                           Principal principal) {
        if (this.configuration == null || this.configuration.isEmpty()) {
            return this.invert ? false : true;
        }
        URL url = URL.create(request);
        String protocol = url.getProtocol();
        Path path = url.getPath();
        List<Path> paths = path.getPaths();
        for (int i = paths.size() - 1; i >= 0; i--) {
            String prefix = paths.get(i).toString();
            String value = this.configuration.getProperty(prefix);
            if (value != null) {
                
                boolean match = protocol.equals(value.trim());
                return this.invert ? !match : match;
            }
        }
        return this.invert ? false : true;
    }

    private String invertProtocol(String protocol, boolean invert) {
        if (!invert) {
            return protocol;
        }
        if (PROTO_HTTPS.equals(protocol)) {
            return PROTO_HTTP;
        }
        return PROTO_HTTPS;
    }
}

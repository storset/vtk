/* Copyright (c) 2005, University of Oslo, Norway
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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * Standard request filter.
 * 1) Ensures that the URI is not empty or <code>null</code>.
 * 2) Translates '*' as request URI to '/' (relevant for a host global OPTIONS request).
 */
public class StandardRequestFilter extends AbstractRequestFilter {

    private static Log logger = LogFactory.getLog(StandardRequestFilter.class);

    private Map<Pattern, String> urlReplacements;

    public void setUrlReplacements(Map<String, String> urlReplacements) {
        this.urlReplacements = new LinkedHashMap<Pattern, String>();
        for (String key : urlReplacements.keySet()) {
            Pattern pattern = Pattern.compile(key);
            String replacement = urlReplacements.get(key);
            this.urlReplacements.put(pattern, replacement);
        }
    }

    public HttpServletRequest filterRequest(HttpServletRequest request) {
        return new StandardRequestWrapper(request);
    }

    private class StandardRequestWrapper extends HttpServletRequestWrapper {

        private String uri;

        public StandardRequestWrapper(HttpServletRequest request) {

            super(request);

            String requestURI = request.getRequestURI();
            this.uri = translateUri(requestURI);

            if (logger.isDebugEnabled()) {
                logger.debug("Translated uri: from '" + requestURI + "' to '" + this.uri + "'");
            }
        }

        public String getRequestURI() {
            return this.uri;
        }

        private String translateUri(String requestURI) {
            if (requestURI == null 
                    || "".equals(requestURI)
                    || "*".equals(requestURI)) {
                return "/";
            }

            if (urlReplacements != null) {
                for (Pattern pattern : urlReplacements.keySet()) {
                    String replacement = urlReplacements.get(pattern);
                    requestURI = pattern.matcher(requestURI).replaceAll(replacement);
                }
            }
            return requestURI;
        }
    }

}




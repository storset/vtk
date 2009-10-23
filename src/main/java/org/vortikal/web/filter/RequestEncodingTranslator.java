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

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;


/**
 * Request URI processor that translates the URI (and optionally headers)
 * from one encoding to another.
 *
 * <p>Configurable JavaBean properties:
 * <ul>
 *   <li><code>fromEncoding</code> - the encoding to translate from</li>
 *   <li><code>toEncoding</code> - the encoding to translate to</li>
 *   <li><code>translatedHeaders</code> - a set of header names. 
 *     If <code>*</code> is included in the set, all headers will 
 *     be translated</li>
 * </ul>
 * </p>
 */
public class RequestEncodingTranslator extends AbstractRequestFilter
  implements InitializingBean {

    private String fromEncoding;
    private String toEncoding;
    private Set<String> translatedHeaders = new HashSet<String>();
    private static Log logger = LogFactory.getLog(RequestEncodingTranslator.class);
    

    public void setFromEncoding(String fromEncoding) {
        this.fromEncoding = fromEncoding;
    }

    public void setToEncoding(String toEncoding) {
        this.toEncoding = toEncoding;
    }
    
    public void setTranslatedHeaders(Set<String> translatedHeaders) {
        this.translatedHeaders = translatedHeaders;
    }

    public void afterPropertiesSet() throws Exception {
        if (this.fromEncoding == null) {
            throw new BeanInitializationException(
                "JavaBean property 'fromEncoding' not specified");
        }
        if (this.toEncoding == null) {
            throw new BeanInitializationException(
                "JavaBean property 'toEncoding' not specified");
        }
        Charset.forName(this.fromEncoding);
        Charset.forName(this.toEncoding);
    }
    
    public HttpServletRequest filterRequest(HttpServletRequest request) {
        return new TranslatingRequestWrapper(request, this.fromEncoding, this.toEncoding);
    }
    
    private class TranslatingRequestWrapper extends HttpServletRequestWrapper {

    	private String uri;
    	
        public TranslatingRequestWrapper(HttpServletRequest request,
                                         String fromEncoding, String toEncoding) {
            super(request);
            try {
            	String uri = request.getRequestURI();
            	uri = new String(uri.getBytes(fromEncoding), toEncoding);
            	if (logger.isDebugEnabled()) {
            		logger.debug("Translated uri: from '" + uri 
            				+ "' to '" + uri
            				+ "' using encoding '" + toEncoding + "' (from '"
            				+ fromEncoding + "')");
            	}
            	this.uri = uri;
            } catch (Exception e) {
                logger.warn("Unable to translate uri: " + request.getRequestURI(), e);
                this.uri = request.getRequestURI();
            }
        }
        
        public String getRequestURI() {
        	return this.uri;
        }
        
        @Override
        public String getHeader(String name) {
            String header = super.getHeader(name);
            if (!translatedHeaders.contains(name) 
                    && !translatedHeaders.contains("*")) {
                return header;
            }
            if (header == null) {
                return null;
            }
            try {
                header = new String(header.getBytes(fromEncoding), toEncoding);
            } catch (Exception e) {
                logger.warn("Unable to translate header: " + header, e);
            }
            return header;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Enumeration getHeaders(String name) {
            Enumeration e = super.getHeaders(name);
            if (!translatedHeaders.contains(name) 
                    && !translatedHeaders.contains("*")) {
                return e;
            }
            List<String> headers = new ArrayList<String>();
            while (e.hasMoreElements()) {
                String header = (String) e.nextElement();
                try {
                    header = new String(header.getBytes(fromEncoding), toEncoding);
                } catch (Exception ex) {
                    logger.warn("Unable to translate header: " + header, ex);
                }
                headers.add(header);
            }
            final List<String> result = headers;
            return new Enumeration() {
                int i = 0;
                public boolean hasMoreElements() {
                    return i < result.size();
                }
                public Object nextElement() {
                    if (hasMoreElements()) {
                        return result.get(i++);
                    }
                    throw new NoSuchElementException();
                }
            };
        }

        public String toString() {
            StringBuilder sb = new StringBuilder(this.getClass().getName());
            sb.append(": ").append(this.uri);
            return sb.toString();
        }
    }
}

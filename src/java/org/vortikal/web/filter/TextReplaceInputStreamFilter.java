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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.util.io.StreamUtil;



/**
 * Filter that replaces a part of the request body determined by a
 * regular expression.
 *
 * <p>Configurable JavaBean properties:
 * <ul>
 *   <li><code>pattern</code> - the regular expression to match
 *   <li><code>replacement</code> - the replacement text (may include
 *   substitution variables from the pattern)
 *   <li><code>characterEncoding</code> - the encoding to use when
 *   interpreting the request body as text. This should be an eight
 *   bit encoding, as the byte stream is first decoded and then coded
 *   using the same scheme. The default is <code>iso-8859-1</code>.
 * </ul>
 */
public class TextReplaceInputStreamFilter implements RequestFilter, InitializingBean {

    private Log logger = LogFactory.getLog(this.getClass());

    private String characterEncoding = Charset.forName("iso-8859-1").name();
    private Pattern pattern;
    private String replacement;
    
    private int order = Integer.MAX_VALUE;
    


    public void setCharacterEncoding(String characterEncoding) {
        this.characterEncoding = characterEncoding;
    }

    public void setPattern(String pattern) {
        this.pattern = Pattern.compile(pattern, Pattern.DOTALL);
    }
    
    public void setReplacement(String replacement) {
        this.replacement = replacement;
    }
    
    public void setOrder(int order) {
        this.order = order;
    }
    
    public int getOrder() {
        return this.order;
    }
    

    public void afterPropertiesSet() throws Exception {
        if (this.characterEncoding == null) {
            throw new BeanInitializationException(
                "JavaBean property 'characterEncoding' not specified");
        }
        Charset.forName(this.characterEncoding);
        if (this.pattern == null) {
            throw new BeanInitializationException(
                "JavaBean property 'pattern' not specified");
        }
        if (this.replacement == null) {
            throw new BeanInitializationException(
                "JavaBean property 'replacement' not specified");
        }
    }
    
    public HttpServletRequest filterRequest(HttpServletRequest request) {
        return new TextReplacementRequestWrapper(request, this.pattern,
                                                 this.characterEncoding, this.replacement);
    }
    
    private class TextReplacementRequestWrapper extends HttpServletRequestWrapper {

        private HttpServletRequest request;
        private Pattern pattern;
        private String characterEncoding;
        private String replacement;
        
        public TextReplacementRequestWrapper(HttpServletRequest request,
                                             Pattern pattern,
                                             String characterEncoding,
                                             String replacement) {
            super(request);
            this.request = request;
            this.pattern = pattern;
            this.characterEncoding = characterEncoding;
            this.replacement = replacement;
        }
        
        public ServletInputStream getInputStream() throws IOException {
            byte[] buffer = StreamUtil.readInputStream(request.getInputStream());
            String content = new String(buffer, this.characterEncoding);
            Matcher matcher = pattern.matcher(content);
            StringBuffer sb = new StringBuffer();
            
            if (matcher.find()) {
                matcher.appendReplacement(sb, this.replacement);
            }
            matcher.appendTail(sb);
            buffer = sb.toString().getBytes(this.characterEncoding);
            return new org.vortikal.util.io.ServletInputStream(
                new ByteArrayInputStream(buffer));
        }
    }
    


}

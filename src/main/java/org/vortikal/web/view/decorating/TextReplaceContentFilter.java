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
package org.vortikal.web.view.decorating;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;

/**
 * Text filter that performs regexp matching and replacing.
 *
 * <p>Configurable JavaBean properties:
 * <ul>
 *   <li><code>pattern</code> - the regular expression to match
 *   <li><code>replacement</code> - the replacement string
 * </ul>
 */
public class TextReplaceContentFilter implements Decorator, InitializingBean {

    private Pattern pattern;
    private String replacement;


    public void setPattern(String pattern) {
        this.pattern = Pattern.compile(pattern, Pattern.DOTALL);
    }
    

    public void setReplacement(String replacement) {
        this.replacement = replacement;
    }

    
    public void afterPropertiesSet() throws Exception {
        if (this.pattern == null) {
            throw new BeanInitializationException(
                "JavaBean property 'pattern' not specified");
        }
        if (this.replacement == null) {
            throw new BeanInitializationException(
                "JavaBean property 'replacement' not specified");
        }
    }


    public void decorate(Map model, HttpServletRequest request,
                          Content content) throws Exception {
        
        String s = content.getContent();
        Matcher matcher = this.pattern.matcher(s);
        StringBuffer sb = new StringBuffer();
            
        if (matcher.find()) {
            matcher.appendReplacement(sb, this.replacement);
        }
        matcher.appendTail(sb);
        content.setContent(sb.toString());
    }
}

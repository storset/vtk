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
package org.vortikal.web.service;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;
import org.vortikal.util.io.StreamUtil;

/**
 * Assertion for performing a regexp match on a "chunk" of the
 * contents of a resource starting from the first byte. The byte
 * sequence is interpreted as text in a configurable character
 * encoding before applying the regexp.
 *
 * <p>Configurable JavaBean properties:
 * <ul>
 *   <li><code>repository</code> - the content {@link Repository}
 *   (required)
 *   <li><code>token</code> - a token used for resource
 *   retrieval. Should represent a principal allowed to read all
 *   resources. (Required.)
 *   <li><code>chunkSize</code> - an integer greater than zero
 *   specifying the number of bytes to grab from the resource. The
 *   default value is <code>1024</code>.
 *   <li><code>pattern</code> - the regular expression to match
 *   <li><code>characterEncoding</code> - the character encoding to
 *   use when interpreting the byte sequence as text. The default is
 *   US-ASCII.
 * </ul>
 */
public class ResourceChunkRegexpAssertion extends AbstractRepositoryAssertion
  implements InitializingBean {

    private Log logger = LogFactory.getLog(this.getClass());

    private Repository repository;
    private String token;
    private int chunkSize = 1024;
    private Pattern pattern;
    private Charset characterEncoding = Charset.forName("US-ASCII");
    
    
    public void setChunkSize(int chunkSize) {
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("Chunk size must be a positive integer");
        }
        this.chunkSize = chunkSize;
    }
    

    public void setPattern(String pattern) {
        if (pattern == null) {
            throw new IllegalArgumentException("Pattern cannot be null");
        }
        this.pattern = Pattern.compile(pattern, Pattern.MULTILINE | Pattern.DOTALL);
    }
    

    public void setCharacterEncoding(String characterEncoding) {
        if (characterEncoding == null) {
            throw new IllegalArgumentException("Character encoding cannot be null");
        }
        this.characterEncoding = Charset.forName(characterEncoding);
    }
    

    public void setRepository(Repository repository) {
        this.repository = repository;
    }


    public void setToken(String token) {
        this.token = token;
    }


    public void afterPropertiesSet() throws Exception {
        if (this.repository == null) {
            throw new BeanInitializationException(
                "JavaBean property 'repository' must be set");
        }
    }


    public boolean conflicts(Assertion assertion) {
        return false;
    }


    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(super.toString());
        sb.append("; chunkSize = ").append(this.chunkSize);
        sb.append("; pattern = ").append(this.pattern.pattern());
        sb.append("; characterEncoding = ").append(this.characterEncoding);
        return sb.toString();
    }


    public boolean matches(Resource resource, Principal principal) {

        if (resource.isCollection()) {
            return false;
        }

        try {
            InputStream inputStream = this.repository.getInputStream(
                this.token, resource.getURI(), true);

            byte[] buffer = StreamUtil.readInputStream(inputStream, this.chunkSize);
            String chunk = new String(buffer, this.characterEncoding.name());
            Matcher m = this.pattern.matcher(chunk);
            boolean match = m.matches();
            
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("Got match: " + match + " for regular expression '"
                             + this.pattern.pattern() + "' on resource chunk '"
                             + chunk + "'");
            }
            return match;

        } catch (Exception e) {
            throw new RuntimeException(
                "Unable to perform matching on resource " + resource, e);
        }
    }

}

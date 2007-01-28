/* Copyright (c) 2007, University of Oslo, Norway
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
package org.vortikal.web.view.decorating.ssi;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.vortikal.repository.Repository;
import org.vortikal.repository.RepositoryException;
import org.vortikal.security.AuthenticationException;
import org.vortikal.security.SecurityContext;
import org.vortikal.util.io.StreamUtil;
import org.vortikal.web.RequestContext;

public class RepositoryAccessingSsiProcessor implements SsiProcessor {

    private static Log logger = LogFactory.getLog(RepositoryAccessingSsiProcessor.class);
    
    private static String VIRTUAL = "virtual";
    private static String FILE = "file";
    
    private String identifier = FILE;

    private Repository repository;

    private String characterEncoding = "iso-8859-1";

    public String getIdentifier() {
        return identifier;
    }

    public String resolve(String address) {
        String token = SecurityContext.getSecurityContext().getToken();

        String uri = null;

        if (this.identifier.equals(VIRTUAL)) {
            uri = address;
        } else {
            uri = RequestContext.getRequestContext().getResourceURI();
            uri = uri.substring(0, uri.lastIndexOf("/") + 1) + address;
        }

        String content = null;
        try {
            InputStream is = null;
            is = repository.getInputStream(token, uri, true);
            byte[] bytes = StreamUtil.readInputStream(is);
            content = new String(bytes, characterEncoding);
        } catch (RepositoryException e) {
            // FIXME: Exception handling for ssi processing
            logger.warn("Unhandled exception for resource '" + uri + "'", e);
        } catch (AuthenticationException e) {
            logger.warn("Unhandled exception", e);
        } catch (IOException e) {
            logger.warn("Unhandled exception", e);
        }
        return content;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    /**
     * @param identifier
     *            Either "file" or "virtual". Default is "file".
     */
    public void setIdentifier(String identifier) {
        if (identifier == null || 
                !(FILE.equals(identifier) || VIRTUAL.equals(identifier)))
            throw new IllegalArgumentException(
                    "Java bean property 'identifier' must be '" + FILE + "' or '" +
                    VIRTUAL + "', supplied value is '" + identifier + "'");

        this.identifier = identifier;
    }

}

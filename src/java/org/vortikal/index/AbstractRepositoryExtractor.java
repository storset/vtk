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

package org.vortikal.index;

import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repository.Repository;
import org.vortikal.repository.RepositoryException;
import org.vortikal.repository.Resource;
import org.vortikal.security.AuthenticationException;

/**
 * Common functionality used by index extractors that fetch repository
 * resources.
 *
 * @author oyviste
 */
public abstract class AbstractRepositoryExtractor implements Extractor, InitializingBean {
    
    private static Log logger = LogFactory.getLog(AbstractRepositoryExtractor.class);
    
    /** The system token to use for repository access. */
    private String token;
    
    /** The {@link org.vortikal.repository.Repository} to use. */
    private Repository repository;
    
    public void afterPropertiesSet() throws BeanInitializationException {
        if (token == null) {
            throw new BeanInitializationException("Property 'token' not set.");
        } else if (repository == null) {
            throw new BeanInitializationException("Property 'repository' not set.");
        }
    }
    
    /** Creates a new instance of AbstractRepositoryExtractor */
    public AbstractRepositoryExtractor() {
    }
    
    /**
     * @see {@link org.vortikal.index.Extractor#extract}
     */
    public abstract Object extract(String uri);
    
    /**
     * @see {@link org.vortikal.index.Extractor#getExtractedClass}
     */
    public abstract Class getExtractedClass();

    /* Setter methods */
    public void setRepository(Repository repository) {
        this.repository = repository;
    }
    
    public void setToken(String token) {
        this.token = token;
    }

    /**
     * Get <code>Resource</code> object from repository.
     * @param uri URI of resource to get.
     * @return a <code>Resource</code> object for the corresponding URI, or
     *         <code>null</code> if error.
     */
    public Resource getResource(String uri) {
        Resource resource = null;

        try {
            // Try to get Resource object
            resource = repository.retrieve(this.token, uri, false);
        } catch (IOException io) {
            logger.warn("Got IOException when attempting to get resource from repository: " 
                    + io.getMessage());
        } catch (AuthenticationException ae) {
            logger.warn("Got AuthenticationException when attempting to get resource from repository." +
                        ae.getMessage());
        } catch (RepositoryException repex) {
            logger.warn("Got RepositoryException when attempting to get resource from repository." +
                        repex.getMessage());
        }

        return resource;
    }

    /**
     * Get a resource's data input stream.
     * @param uri
     * @return resource as inputStream, <code>null</code> if unable to get input stream.
     */
    public InputStream getResourceInputStream(String uri) {
        InputStream inputStream = null;

        try {
            inputStream = repository.getInputStream(this.token, uri, false);
        } catch (IOException io) {
            logger.warn("Got IOException when attempting to get resource input stream: " +
                        io.getMessage());
        } catch (AuthenticationException ae) {
            logger.warn("Got AuthenticationException when attempting to get resource input stream:" +
                        ae.getMessage());
        } catch (RepositoryException repex) {
            logger.warn("Got RepositoryException when attempting to get resource input stream:" +
                        repex.getMessage());
        }

        return inputStream;
    }

    protected Repository getRepository() {
        return repository;
    }

    protected String getToken() {
        return token;
    }
}

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
/*
 * Created on 04.aug.2004
 *
 */
package org.vortikal.web.view.freemarker;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.vortikal.repository.Repository;
import org.vortikal.repository.RepositoryException;
import org.vortikal.repository.Resource;

import freemarker.cache.TemplateLoader;


/**
 */
public class RepositoryTemplateLoader implements TemplateLoader {

    private static Log logger = LogFactory.getLog(RepositoryTemplateLoader.class);

    private Repository repository;
    private String trustedToken;
    private String basePath;
    
    /**
     * @see freemarker.cache.TemplateLoader#findTemplateSource(java.lang.String)
     */
    public Object findTemplateSource(String name) throws IOException {
        logger.info("findTemplateSource('" + name + "' called, will resolve" +
                "with basePath = '" + basePath + "'" +
                        " ");
        
        Resource resource = null;
        
        try {
            resource = 
                repository.retrieve(trustedToken, basePath + name, false);
        } catch (RepositoryException ex) {
            return null;
        }
        if (resource == null || resource.isCollection())
            return null;
        
        return name;
    }


    /**
     * @see freemarker.cache.TemplateLoader#getLastModified(java.lang.Object)
     */
    public long getLastModified(Object templateSource) {
        Resource resource = null;
        try {
            resource = repository.retrieve(trustedToken, 
                    basePath + ((String)templateSource), false);
        } catch (IOException ex) {
            return -1;
        }
   
        if (resource == null) return -1;
         
        return resource.getLastModified().getTime();
    }


    /**
     * @see freemarker.cache.TemplateLoader#getReader(java.lang.Object, java.lang.String)
     */
    public Reader getReader(Object templateSource, String encoding) throws IOException {
        String uri = basePath + (String)templateSource;
        Resource resource = 
            repository.retrieve(trustedToken, uri, false);
        
        if (resource == null || resource.isCollection())
            throw new IOException("the template with repository URI '" +
                    uri + "' couldn't be loaded");

        InputStream inputStream = repository.getInputStream(trustedToken, uri, false);
        return new InputStreamReader(inputStream, encoding);
    }


    /**
     * @see freemarker.cache.TemplateLoader#closeTemplateSource(java.lang.Object)
     */
    public void closeTemplateSource(Object templateSource) throws IOException {

    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }
    
    public void setTrustedToken(String trustedToken) {
        this.trustedToken = trustedToken;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }
    
}

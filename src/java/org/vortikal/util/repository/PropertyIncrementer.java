/* Copyright (c) 2006, University of Oslo, Norway
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
package org.vortikal.util.repository;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.Namespace;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;

import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.RepositoryException;
import org.vortikal.repository.Resource;
import org.vortikal.security.AuthenticationException;
import org.vortikal.web.RequestContext;

/**
 * @author Eirik Meland (eirik.meland@usit.uio.no)
 */
public class PropertyIncrementer implements InitializingBean {

    private Log logger = LogFactory.getLog(this.getClass());
    

    private int _increment = 0;

    private org.vortikal.repository.Namespace _namespace;
    
    private String _propertyName;

    private Repository _repository = null;
    
    private String _resourceURI;
    
    private String _token = "";
    
    /** 
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    public void afterPropertiesSet()
            throws Exception {
        logger.debug("Initializing bean");

        // check if all properties are set
        if ( _increment == 0 
                || _namespace == null
                || _propertyName == null
                || _repository == null
                || _resourceURI == null
                || _token == null) {
            throw new BeanInitializationException("Failed to init bean (missing property)");
        }
        
    }

    /**
     * TODO write javadoc
     * @throws IOException
     */
    synchronized public String incrementProperty()
            throws IOException {
        // get Resource
        String uri = RequestContext.getRequestContext().getResourceURI();
        if (!"/".equals(uri)) uri += "/";
        uri += _resourceURI;
        Resource resource = getResource(uri, _token);
        
        // read property
        Property p = resource.getProperty(_namespace, _propertyName);

        if (p == null) {
            p = resource.createProperty(_namespace, _propertyName);
            p.setStringValue("0");
        }
        
        // check that property is a number
        // XXX: add int to value, but this code should be removed alltogether
        int propValue = Integer.parseInt(p.getStringValue());
        
        // increment property
        propValue += _increment;
        
        // write property
        logger.debug("property incremented to " + propValue);
        p.setStringValue(Integer.toString(propValue));

        storeResource(resource);
        return Integer.toString(propValue);
    }

    /**
     * TODO write javadoc
     * @param resource
     * @throws IOException
     */
    private void storeResource(Resource resource)
            throws IOException {
        _repository.store(_token, resource);
    }

    /**
     *
     */
   private Resource getResource(final String url, final String token) {
       Resource r = null;
       try {
           if (_repository.exists(token, url)) {
            // Try to get Resource object
            r = _repository.retrieve(token, url, false);
           } else {
               logger.warn("Resource at " + url + " does not exist in repository.");
           }
       } catch (IOException io) {
           logger.warn("Got IOException when attempting to get resource from repository.", io);
       } catch (AuthenticationException ae) {
           logger.warn("Got AuthenticationException when attempting to get resource from repository.", ae);
       } catch (RepositoryException repex) {
           logger.warn("Got RepositoryException when attempting to get resource from repository.", repex);
       }
       
       return r;
   }

    public void setIncrement(final int increment) {
        _increment = increment;
    }
    
    public void setNamespace(final org.vortikal.repository.Namespace namespace) {
        _namespace = namespace;
    }
    
    public void setPropertyName(final String propertyName) {
        _propertyName = propertyName;
    }
    
    public void setRepository(Repository repository) {
        _repository = repository;
    }
    
    public void setResourceURI(final String resourceURI) {
        _resourceURI = resourceURI;
    }
    
    public void setToken(final String token) {
        _token = token;
    }
}

/*
 * $Id$
 */
package org.vortikal.util.repository;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;

import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.RepositoryException;
import org.vortikal.repository.Resource;
import org.vortikal.security.AuthenticationException;

/**
 * @author Eirik Meland (eirik.meland@usit.uio.no)
 */
public class PropertyIncrementer implements InitializingBean {

    private Log logger = LogFactory.getLog(this.getClass());
    

    private int _increment = 0;

    private String _namespace;
    
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
        
        incrementProperty();
    }

    /**
     * TODO write javadoc
     * @throws IOException
     */
    synchronized private void incrementProperty()
            throws IOException {
        // get Resource
        Resource resource = getResource(_resourceURI, _token);
        
        // read property
        Property p = resource.getProperty(_namespace, _propertyName);

        // check that property is a number
        int propValue = Integer.parseInt(p.getValue());
        
        // increment property
        propValue += _increment;
        
        // write property
        logger.debug("property incremented to " + propValue);
        p.setValue(Integer.toString(propValue));

        resource.setProperty(p);
        storeResource(resource);
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
    
    public void setNamespace(final String namespace) {
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

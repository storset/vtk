package org.vortikal.repositoryimpl.query;

import org.vortikal.repository.PropertySet;

/**
 * Interface for retrieving property sets from a <code>PropertySetIndex</code> 
 * by URI or UUID in a random access manner.
 * 
 * Must be closed after usage.
 * 
 * @author oyviste
 *
 */
public interface PropertySetIndexRandomAccessor {
   
    /**
     * Check if one or more property sets exists for the given URI.
     * 
     * @param uri
     * @return
     * @throws IndexException
     */
    public boolean exists(String uri) throws IndexException;
    
    /**
     * Count number of existing property set instances for the given URI. This can be used
     * to detect inconsistencies.
     * 
     * @param uri
     * @return
     * @throws IndexException
     */
    public int countInstances(String uri) throws IndexException;
    
    /**
     * Get a property set by URI
     * @param uri
     * @return
     * @throws IndexException
     */
    public PropertySet getPropertySetByURI(String uri) throws IndexException;
    
    /**
     * Get a property set by UUID
     * @param uuid
     * @return
     * @throws IndexException
     */
    public PropertySet getPropertySetByUUID(String uuid) throws IndexException;
    
    /**
     * This method should be called after usage to free index resources.
     * @throws IndexException
     */
    public void close() throws IndexException;
    
}

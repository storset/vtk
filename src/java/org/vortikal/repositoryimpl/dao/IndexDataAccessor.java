package org.vortikal.repositoryimpl.dao;

import java.io.IOException;

import org.vortikal.repository.PropertySet;

/**
 * 
 * @author oyviste
 *
 */
public interface IndexDataAccessor {
    
    /**
     * Get iterator over <em>all</em> existing property sets in repository.
     * 
     * @return
     * @throws IOException
     */
    public ResultSetIterator getPropertySetIterator() throws IOException;
    
    /**
     * Get an ordered <code>PropertySet</code> iterator, starting from the given 
     * URI. The URI order should be lexicographic.
     * Might be useful for incremental synchronization/re-indexing.
     * 
     * @param startURI
     * @return
     * @throws IOException
     */
    public ResultSetIterator getOrderedPropertySetIterator(String startURI) throws IOException;
    
    /**
     * Returns a single <code>PropertySet</code> for the given URI.
     * 
     * @param uri
     * @return
     * @throws IOException
     */
    public PropertySet getPropertySetForURI(String uri) throws IOException;
    
    
    
}

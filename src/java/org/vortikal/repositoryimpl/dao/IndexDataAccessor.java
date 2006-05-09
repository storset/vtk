package org.vortikal.repositoryimpl.dao;

import java.io.IOException;
import java.util.List;

import org.vortikal.repository.PropertySet;

/**
 * 
 * @author oyviste
 *
 */
public interface IndexDataAccessor {
    
    /**
     * Get iterator over <em>all</em> existing property sets in repository.
     * The URI order should be lexicographic.
     * Might be useful for incremental synchronization/re-indexing.
     * 
     * @return
     * @throws IOException
     */
    public ResultSetIterator getOrderedPropertySetIterator() throws IOException;
    
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
     * Get iterator over all <code>PropertySet</code>s from URIs in the given 
     * <code>List</code>.
     *  
     * @param uris
     * @return
     * @throws IOException
     */
    public ResultSetIterator getPropertySetIteratorForURIs(List uris) throws IOException;
    
    /**
     * Returns a single <code>PropertySet</code> for the given URI.
     * 
     * @param uri
     * @return
     * @throws IOException
     */
    public PropertySet getPropertySetForURI(String uri) throws IOException;

}

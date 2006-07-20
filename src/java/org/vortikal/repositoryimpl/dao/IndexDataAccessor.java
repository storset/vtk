package org.vortikal.repositoryimpl.dao;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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
    public Iterator getOrderedPropertySetIterator() throws IOException;
    
    /**
     * Get an ordered <code>PropertySet</code> iterator, starting from the given 
     * URI. The URI order should be lexicographic.
     * Might be useful for incremental synchronization/re-indexing.
     * 
     * @param startURI
     * @return
     * @throws IOException
     */
    public Iterator getOrderedPropertySetIterator(String startURI) throws IOException;
    
    /**
     * Get iterator over all <code>PropertySet</code>s from URIs in the given 
     * <code>List</code>.
     *  
     * @param uris
     * @return
     * @throws IOException
     */
    public Iterator getPropertySetIteratorForURIs(List uris) throws IOException;
    
    /**
     * Returns a single <code>PropertySet</code> for the given URI.
     * 
     * @param uri
     * @return
     * @throws IOException
     */
    public PropertySet getPropertySetForURI(String uri) throws IOException;
    
    /**
     * Close an {@link java.util.Iterator} instance obtained with any of the
     * following methods:
     * <ul>
     *  <li>{@link #getOrderedPropertySetIterator()}</li>
     *  <li>{@link #getOrderedPropertySetIterator(String)}</li>
     *  <li>{@link #getPropertySetIteratorForURIs(List)}</li>
     * </ul>
     * 
     * This should always be done to free database resources.
     * 
     * @param iterator
     * @throws IOException
     */
    public void close(Iterator iterator) throws IOException;
    
    /**
     * Process list of <code>ResultSecurityInfo</code> instances
     * against list of given user/group names.
     * 
     * @param principalNames
     * @param resultSecurityInfo
     * @throws IOException
     */
    public void processQueryResultsAuthorization(Set principalNames, 
                                                 List resultSecurityInfo)
        throws IOException;

}

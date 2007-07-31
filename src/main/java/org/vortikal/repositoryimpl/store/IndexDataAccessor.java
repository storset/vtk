/* Copyright (c) 2006, 2007, University of Oslo, Norway
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
package org.vortikal.repositoryimpl.store;


import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.vortikal.repository.PropertySet;
import org.vortikal.repositoryimpl.search.query.security.ResultSecurityInfo;

/**
 * Repository data accessor for indexing system.
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
     */
    public Iterator<PropertySet> getOrderedPropertySetIterator() throws DataAccessException;

    /**
     * Get an ordered <code>PropertySet</code> iterator, starting from the given 
     * URI. The URI order should be lexicographic.
     * Might be useful for incremental synchronization/re-indexing.
     * 
     * @param startUri
     */
    public Iterator<PropertySet> getOrderedPropertySetIterator(String startUri) throws DataAccessException;
    
    /**
     * Get iterator over all <code>PropertySet</code>s from URIs in the given 
     * <code>List</code>.
     *  
     * @param uris
     */
    public Iterator<PropertySet> getPropertySetIteratorForUris(List uris) throws DataAccessException;

    /**
     * Returns a single <code>PropertySet</code> for the given URI.
     * 
     * @param uri
     */
    public PropertySet getPropertySetByUri(String uri) throws DataAccessException;

    /**
     * Returns a single <code>PropertySet</code> for the given ID.
     * @param id The repository ID of the property set.
     * @return A single <code>PropertySet</code> for the given ID, or <code>null</code> if not found.
     */
    public PropertySet getPropertySetByID(int id) throws DataAccessException;
    
    /**
     * Close an {@link java.util.Iterator} instance obtained with any of the
     * following methods:
     * <ul>
     *  <li>{@link #getOrderedPropertySetIterator()}</li>
     *  <li>{@link #getOrderedPropertySetIterator(String)}</li>
     *  <li>{@link #getPropertySetIteratorForUris(List)}</li>
     * </ul>
     * 
     * This should always be done to free database resources.
     * 
     * @param iterator
     * @throws IOException
     */
    public void close(Iterator<PropertySet> iterator) throws DataAccessException;

    /**
     * Process list of <code>ResultSecurityInfo</code> instances
     * against list of given user/group names.
     * 
     * @param principalNames
     * @param resultSecurityInfo
     */
    public void processQueryResultsAuthorization(
        Set<String> principalNames,  List<ResultSecurityInfo> resultSecurityInfo) throws DataAccessException;

    
}

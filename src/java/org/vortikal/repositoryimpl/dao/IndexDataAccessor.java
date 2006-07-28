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

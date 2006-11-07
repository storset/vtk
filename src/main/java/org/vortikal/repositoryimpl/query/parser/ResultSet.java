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
package org.vortikal.repositoryimpl.query.parser;

import java.util.Iterator;
import java.util.List;

/**
 * Contains the result set of a repository query.
 * 
 * FIXME: Not final, comitted for peer review.
 * 
 * TODO: Result object type should be 
 * <code>org.vortikal.repository.PropertySet</code>,
 * when it's ready.
 * 
 * @author oyviste
 *
 */
public interface ResultSet {

    /**
     * Get the result at a given index position in the
     * result set.  
     * @param index The position of the desired result.
     *              First result is at position zero (0), 
     *              last result is at position n-1, where n is
     *              the total number of results in the result set.
     *        
     * @return The result object at the given position.
     */
    public Object getResult(int index);
    
    /**
     * Get all the results up to, but not including, the result
     * at position <code>maxIndex</code>. 
     * Example:
     * List tenFirstResults = resultSet.getResults(10);
     * List allResults = resultSet.getResults
     * 
     * @param maxIndex 
     * @return A <code>List</code> of the results 
     */
    public List getResults(int maxIndex);
    
    /**
     * Get a subset of the result-set (fromIndex inclusive, toIndex exclusive).
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    public List getResults(int fromIndex, int toIndex)
        throws IndexOutOfBoundsException;
    
    /**
     * Get all the results in the result set, as a 
     * <code>List</code>
     * 
     * @return <code>List</code> with all the results in the
     *         result set.
     */
    public List getAllResults();
    
    /**
     * Get the size of the result set (number of query hits).
     * 
     * @return Size of the result set.
     */
    public int getSize();
 
    /**
     * Iterate over results.
     * @return
     */
    public Iterator iterator();
    
    /**
     * Get total number of hits the actual query produced, regardless of the result set size or
     * cursor+maxresults.
     */
    public int getTotalHits();
}

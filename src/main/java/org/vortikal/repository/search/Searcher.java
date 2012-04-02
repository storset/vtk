/* Copyright (c) 2007, University of Oslo, Norway
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
package org.vortikal.repository.search;

import org.vortikal.repository.PropertySet;


/**
 * Simple search interface
 *
 */
public interface Searcher {

    /**
     * Callback interface that should be implemented by client code for match iteration
     * API.
     */
    public interface MatchCallback {
        /**
         * Called once for each matching <code>PropertySet</code>.
         * 
         * @param propertySet 
         * @return Return <code>false</code> to stop matching iteration, <code>true</code> to continue.
         */
        boolean matching(PropertySet propertySet) throws Exception;
    }
    
    /**
     * Execute a regular search returning a <code>ResultSet</code>.
     * 
     * @param token
     * @param search
     * @return
     * @throws QueryException 
     */
    public ResultSet execute(String token, Search search) throws QueryException;
 
    /**
     * Execute an iteration of all property sets that match the criteria in the
     * provided <code>Search</code>.  There are no limits on the number of matching
     * PropertySet instances that can be included in the iteration.
     * 
     * Note however that the following limitations do apply:
     * <ul>
     *   <li>The iteration can support simple ASCENDING NON-LOCALE-SENSITIVE ordering on
     *       a single field/property only. If provided sorting specification violates
     *       these constraints, an exception will be thrown.
     * 
     *   <li>If sorting on a field, then ONLY docs which have the property/field
     *       are included in the matching iteration. If you want to make sure you
     *       get all docs for which your query matches, then sort on a property/field
     *       which is guaranteed to be present in all docs, or don't sort at all
     *       (Search.setSorting(null)).
     * </ul>
     * 
     * <em>
     * It is most efficient to remove the sorting criterium from <code>Search</code> 
     * unless it is specifically required !
     * </em>
     * 
     * Since an iteration can potentially load a lot of documents, client code
     * should take care to set a proper field/property-selector in <code>Search</code> for
     * better efficiency.
     *
     * @param token
     * @param search A <code>Search</code> instance, encapsulating all aspects
     *        of the index search. The search query itself may be null, in which case
     *        everything will match.
     * 
     * @param callback a provided <code>MatchCallback</code> that will be
     *        used to process matching results.
     */
    public void iterateMatching(String token, Search search, MatchCallback callback) throws QueryException;
    
}

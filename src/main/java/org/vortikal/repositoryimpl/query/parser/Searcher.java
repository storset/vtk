/* Copyright (c) 2004, University of Oslo, Norway
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

import org.vortikal.repository.search.query.PropertySelect;
import org.vortikal.repository.search.query.Query;
import org.vortikal.repository.search.query.Sorting;

/**
 * Simple search interface
 *
 */
public interface Searcher {

    /**
     * Executes a query on repository resources with a hard limit on how
     * many results that should be returned, in addition to a cursor. 
     * 
     * At any given time, the <code>Query</code> alone will produce a complete result
     * set. The <code>cursor</code> and <code>maxResults</code> parameters
     * can be used to fetch subsets of this result set. Useful for implementing
     * paging when browsing large result sets.
     * 
     * The implementation must take into consideration what happens
     * when the complete result set changes between queries with 
     * cursor/maxResults. 
     * 
     * @param token The security token associated with the principal 
     *              executing the query.
     * @param query The <code>Query</code> object, containing the query
     *              conditions.
     * @param maxResults Number of results to include from (and including) cursor position.
     * @param sorting the {@link Sorting} to use
     * 
     * @return      A <code>ResultSet</code> containing a subset of the results.
     * 
     * @throws QueryException If the query could not be executed.
     * 
     * @see Query, ResultSet
     */
    public ResultSet execute(String token, Query query, Sorting sorting, 
                             int maxResults) throws QueryException;
    
    /**
     * Executes a query on repository resources with a hard limit on how
     * many results that should be returned, in addition to a cursor. 
     * 
     * At any given time, the <code>Query</code> alone will produce a complete result
     * set. The <code>cursor</code> and <code>maxResults</code> parameters
     * can be used to fetch subsets of this result set. Useful for implementing
     * paging when browsing large result sets.
     * 
     * The implementation must take into consideration what happens
     * when the complete result set changes between queries with 
     * cursor/maxResults. 
     * 
     * @param token The security token associated with the principal 
     *              executing the query.
     * @param query The <code>Query</code> object, containing the query
     *              conditions.
     * @param maxResults Number of results to include from (and including) cursor position.
     * @param sorting the {@link Sorting} to use
     * @param selectedProperties the {@link PropertySelecy properties} queried for
     * 
     * @return      A {@link ResultSet} containing a subset of the results.
     * 
     * @throws QueryException If the query could not be executed.
     * 
     * @see Query, ResultSet
     */
    public ResultSet execute(String token, Query query, Sorting sorting, 
                             int maxResults, PropertySelect selectedProperties)
        throws QueryException;

    /**
     * Executes a query on repository resources with a hard limit on how
     * many results that should be returned, in addition to a cursor. 
     * 
     * At any given time, the <code>Query</code> alone will produce a complete result
     * set. The <code>cursor</code> and <code>maxResults</code> parameters
     * can be used to fetch subsets of this result set. Useful for implementing
     * paging when browsing large result sets.
     * 
     * The implementation must take into consideration what happens
     * when the complete result set changes between queries with 
     * cursor/maxResults. 
     * 
     * @param token The security token associated with the principal 
     *              executing the query.
     * @param query The <code>Query</code> object, containing the query
     *              conditions.
     * @param sorting the {@link Sorting} to use
     * @param maxResults Number of results to include from (and including) cursor position.
     * @param cursor     Positition to start in the query result set (zero-based).
     * 
     * 
     * @return      A <code>ResultSet</code> containing a subset of the results.
     * 
     * @throws QueryException If the query could not be executed.
     * 
     * @see Query, ResultSet
     */
    public ResultSet execute(String token, Query query, Sorting sorting,
            int maxResults, int cursor) throws QueryException;

    


    /**
     * Executes a query on repository resources with a hard limit on how
     * many results that should be returned, in addition to a cursor. 
     * 
     * At any given time, the <code>Query</code> alone will produce a complete result
     * set. The <code>cursor</code> and <code>maxResults</code> parameters
     * can be used to fetch subsets of this result set. Useful for implementing
     * paging when browsing large result sets.
     * 
     * The implementation must take into consideration what happens
     * when the complete result set changes between queries with 
     * cursor/maxResults. 
     * 
     * @param token The security token associated with the principal 
     *              executing the query.
     * @param query The <code>Query</code> object, containing the query
     *              conditions.
     * @param maxResults Number of results to include from (and including) cursor position.
     * @param cursor     Positition to start in the query result set (zero-based).
     * 
     * @param sorting the {@link Sorting} to use
     * @param selectedProperties the {@link PropertySelecy properties} queried for
     * 
     * @return      A <code>ResultSet</code> containing a subset of the results.
     * 
     * @throws QueryException If the query could not be executed.
     * 
     * @see Query, ResultSet
     */
    public ResultSet execute(String token, Query query, Sorting sorting,
                             int maxResults, int cursor, 
                             PropertySelect selectedProperties) throws QueryException;
}

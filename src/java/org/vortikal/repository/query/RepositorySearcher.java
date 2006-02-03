package org.vortikal.repository.query;

/**
 * Interface for performing queries on repository 
 * resources.
 * 
 * TODO: Differentiate between methods for doing full resource
 * queries, or just queries on a subset/namespace of properties for resources.
 * 
 * @author oyviste
 *
 */
public interface RepositorySearcher {

    /**
     * Perform a query on repository resources.
     * 
     * @param token The security token associated with the principal 
     *              executing the query.
     * @param query The <code>Query</code> object, containing the query
     *              conditions.
     * @return      A <code>ResultSet</code> containing the results.
     * 
     * @see org.vortikal.repository.query.Query
     * @see org.vortikal.repository.query.ResultSet
     *              
     * @throws QueryException If the query could not be executed.
     */
    public ResultSet query(String token, Query query)
        throws QueryException;
    
    /**
     * Perform a query on repository resources with a hard limit on how
     * many results that should be returned.
     * 
     * @param token The security token associated with the principal 
     *              executing the query.
     * @param query The <code>Query</code> object, containing the query
     *              conditions.
     * @return      A <code>ResultSet</code> containing the results.
     * 
     * @param maxResults Maximum number of desired results in the returned
     *                   result set. If the query produces more hits than
     *                   this limit, the overflowing results are discarded.
     * 
     * @return      A <code>ResultSet</code> containing the results.
     *
     * @throws QueryException If the query could not be executed.
     */
    public ResultSet query(String token, Query query, int maxResults)
        throws QueryException;

   /**
    * Perform a query on repository resources with a hard limit on how
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
    * @param maxResults Number of results to include from cursor position.
    * @param cursor     Positition to start in the query result set.
    * 
    * @return      A <code>ResultSet</code> containing a subset of the results.
    * 
    * @throws QueryException If the query could not be executed.
    */
    public ResultSet query(String token, Query query, int maxResults, int cursor)
        throws QueryException;
    
}

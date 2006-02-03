package org.vortikal.repository.query;

/**
 * Interface for performing repository resource queries.
 * 
 * @author oyviste
 *
 */
public interface RepositorySearcher {

    public ResultSet query(String token, Query query)
        throws QueryException;
    
    public ResultSet query(String token, Query query, int maxResults)
        throws QueryException;
    
    public ResultSet query(String token, Query query, int maxResults, 
                                int cursor)
        throws QueryException;
    
}

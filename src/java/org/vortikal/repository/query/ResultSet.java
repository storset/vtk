package org.vortikal.repository.query;

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
    
}

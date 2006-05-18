package org.vortikal.repositoryimpl.query;

import org.vortikal.repositoryimpl.query.query.Sorting;

/**
 * 
 * @author oyviste
 *
 */
public interface SortBuilder {
    
    public org.apache.lucene.search.Sort buildSort(Sorting sort)
        throws SortBuilderException;
    
}

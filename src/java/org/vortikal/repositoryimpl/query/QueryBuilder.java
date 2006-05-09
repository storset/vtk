package org.vortikal.repositoryimpl.query;


/**
 * Interface for implementations that build different Lucene queries.
 * 
 * @author oyviste
 *
 */
public interface  QueryBuilder {

    public org.apache.lucene.search.Query buildQuery();
    
}

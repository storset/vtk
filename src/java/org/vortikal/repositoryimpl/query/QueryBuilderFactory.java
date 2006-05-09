package org.vortikal.repositoryimpl.query;

import org.vortikal.repositoryimpl.query.builders.NameTermQueryBuilder;
import org.vortikal.repositoryimpl.query.builders.PropertyRangeQueryBuilder;
import org.vortikal.repositoryimpl.query.builders.PropertyTermQueryBuilder;
import org.vortikal.repositoryimpl.query.query.NameTermQuery;
import org.vortikal.repositoryimpl.query.query.PropertyRangeQuery;
import org.vortikal.repositoryimpl.query.query.PropertyTermQuery;
import org.vortikal.repositoryimpl.query.query.Query;

public final class QueryBuilderFactory {

    public static QueryBuilder getBuilder(Query query) {
        
       if (query instanceof NameTermQuery) {
           return new NameTermQueryBuilder((NameTermQuery)query);
       }
       
       if (query instanceof PropertyTermQuery) {
           return new PropertyTermQueryBuilder((PropertyTermQuery)query);
       }
       
       if (query instanceof PropertyRangeQuery) {
           return new PropertyRangeQueryBuilder((PropertyRangeQuery)query);
       }
       
       throw new QueryBuilderException("Unsupported query type: " + query);
    }

}

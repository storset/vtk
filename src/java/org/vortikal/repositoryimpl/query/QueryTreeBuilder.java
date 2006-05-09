package org.vortikal.repositoryimpl.query;

import java.util.Iterator;
import java.util.List;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.vortikal.repositoryimpl.query.query.AndQuery;
import org.vortikal.repositoryimpl.query.query.OrQuery;
import org.vortikal.repositoryimpl.query.query.Query;


/**
 * 
 * @author oyviste
 *
 */
public class QueryTreeBuilder implements QueryBuilder {

    Query query;
    
    public QueryTreeBuilder(Query query) {
        this.query = query;
    }

    public org.apache.lucene.search.Query buildQuery() {
        return buildInternal(this.query);
    }
    
    private org.apache.lucene.search.Query buildInternal(Query query) {
        
        if (query instanceof AndQuery) {
            AndQuery andQ = (AndQuery)query;
            List subQueries = andQ.getQueries();
            
            BooleanQuery bq = new BooleanQuery(true);
            for (Iterator i = subQueries.iterator(); i.hasNext();) {
                bq.add(buildInternal((Query)i.next()), BooleanClause.Occur.MUST);
            }

            return bq;
        } else if (query instanceof OrQuery) {
            OrQuery orQ = (OrQuery)query;
            List subQueries = orQ.getQueries();
            
            BooleanQuery bq = new BooleanQuery(true);
            for (Iterator i = subQueries.iterator(); i.hasNext();) {
                bq.add(buildInternal((Query)i.next()), BooleanClause.Occur.SHOULD);
            }

            return bq;
            
        } else {
            
            return QueryBuilderFactory.getBuilder(query).buildQuery();
        }
    }
}

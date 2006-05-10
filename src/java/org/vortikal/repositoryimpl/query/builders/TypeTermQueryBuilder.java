package org.vortikal.repositoryimpl.query.builders;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.vortikal.repositoryimpl.query.DocumentMapper;
import org.vortikal.repositoryimpl.query.QueryBuilder;
import org.vortikal.repositoryimpl.query.QueryBuilderException;
import org.vortikal.repositoryimpl.query.query.TypeOperator;
import org.vortikal.repositoryimpl.query.query.TypeTermQuery;

/**
 * 
 * @author oyviste
 *
 */
public class TypeTermQueryBuilder implements QueryBuilder {

    private TypeTermQuery ttq;
    
    public TypeTermQueryBuilder(TypeTermQuery ttq) {
        this.ttq = ttq;
    }

    public Query buildQuery() {
        
        
        // TypeOperator.IN must be handled specially, as this information is 
        // currently not contained in index.
        //
        // Soltions: 1) handle before making Lucene query tree (delegate to "other" query processor), 
        //              then combine results
        //           2) store ancestor resource types in index, and make it searchable.
        //           3) ..?
        if (ttq.getOperator() != TypeOperator.EQ) {
            throw new QueryBuilderException("Only the 'EQ' TermOperator is currently not implemented.");
        }
        
        String typeTerm = ttq.getTerm();
        
        return new TermQuery(new Term(DocumentMapper.RESOURCETYPE_FIELD_NAME, typeTerm));
    }

}

package org.vortikal.repositoryimpl.query.builders;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;
import org.vortikal.repositoryimpl.query.DocumentMapper;
import org.vortikal.repositoryimpl.query.QueryBuilder;
import org.vortikal.repositoryimpl.query.QueryBuilderException;
import org.vortikal.repositoryimpl.query.query.NameTermQuery;
import org.vortikal.repositoryimpl.query.query.TermOperator;

public class NameTermQueryBuilder implements QueryBuilder {

    NameTermQuery ntq;
    
    public NameTermQueryBuilder(NameTermQuery q) {
        this.ntq = q;
    }
    
    public org.apache.lucene.search.Query buildQuery() {
        
        if (ntq.getOperator() != TermOperator.EQ) {
            throw new QueryBuilderException("Only the 'EQ' TermOperator is currently implemented");
        }
        
        TermQuery tq = new TermQuery(new Term(DocumentMapper.NAME_FIELD_NAME,
                    ntq.getTerm()));
        
        return tq;
    }

}

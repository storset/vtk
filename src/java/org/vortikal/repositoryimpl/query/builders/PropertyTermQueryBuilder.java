package org.vortikal.repositoryimpl.query.builders;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repositoryimpl.query.DocumentMapper;
import org.vortikal.repositoryimpl.query.FieldMapper;
import org.vortikal.repositoryimpl.query.QueryBuilder;
import org.vortikal.repositoryimpl.query.QueryBuilderException;
import org.vortikal.repositoryimpl.query.query.PropertyTermQuery;
import org.vortikal.repositoryimpl.query.query.TermOperator;

public class PropertyTermQueryBuilder implements QueryBuilder {

    private PropertyTermQuery ptq;
    
    public PropertyTermQueryBuilder(PropertyTermQuery ptq) {
        this.ptq = ptq;
    }

    public org.apache.lucene.search.Query buildQuery() {

        if (ptq.getOperator() != TermOperator.EQ) {
            throw new QueryBuilderException("Only the 'EQ' TermOperator is currently implemented");
        }
        
        PropertyTypeDefinition propDef = ptq.getPropertyDefinition();
        
        String nsPrefix = propDef.getNamespace().getPrefix();
        String fieldName = nsPrefix != null ? nsPrefix + ":" + propDef.getName() : propDef.getName();

        String fieldValue = FieldMapper.encodeIndexFieldValue(ptq.getTerm(), propDef.getType());
        
        TermQuery tq = new TermQuery(new Term(fieldName, fieldValue));
        
        return tq;
    }

}

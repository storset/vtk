package org.vortikal.repositoryimpl.query.builders;

import org.apache.lucene.search.ConstantScoreRangeQuery;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repositoryimpl.query.FieldMapper;
import org.vortikal.repositoryimpl.query.QueryBuilder;
import org.vortikal.repositoryimpl.query.query.PropertyRangeQuery;

/**
 * 
 * @author oyviste
 *
 */
public class PropertyRangeQueryBuilder implements QueryBuilder {

    PropertyRangeQuery prq;
    
    public PropertyRangeQueryBuilder(PropertyRangeQuery prq) {
        this.prq = prq;
    }

    public org.apache.lucene.search.Query buildQuery() {

        String from = this.prq.getFromTerm();
        String to = this.prq.getToTerm();
        PropertyTypeDefinition def = prq.getPropertyDefinition();
        
        String fromEncoded = FieldMapper.encodeIndexFieldValue(from, def.getType());
        String toEncoded = FieldMapper.encodeIndexFieldValue(to, def.getType());
        
        String nsPrefix = def.getNamespace().getPrefix();
        String fieldName = nsPrefix != null ? nsPrefix + ":" + def.getName() : def.getName();
        
        ConstantScoreRangeQuery csrq = new ConstantScoreRangeQuery(fieldName, fromEncoded, 
                toEncoded, true, true);
        
        return csrq;
    }

}

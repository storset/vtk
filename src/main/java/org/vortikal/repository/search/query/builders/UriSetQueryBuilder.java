package org.vortikal.repository.search.query.builders;

import java.util.Set;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.TermsFilter;
import org.vortikal.repository.index.mapping.FieldNameMapping;
import org.vortikal.repository.search.query.QueryBuilder;
import org.vortikal.repository.search.query.QueryBuilderException;
import org.vortikal.repository.search.query.UriSetQuery;
import org.vortikal.repository.search.query.filter.InversionFilter;

public class UriSetQueryBuilder implements QueryBuilder {

    private UriSetQuery usQuery;
    
    public UriSetQueryBuilder(UriSetQuery query) {
        this.usQuery = query;
    }
    
    public org.apache.lucene.search.Query buildQuery() throws QueryBuilderException {

        Set<String> uris = this.usQuery.getUris();
        
        TermsFilter tf = new TermsFilter();
        for (String uri: uris) {
            tf.addTerm(new Term(FieldNameMapping.URI_FIELD_NAME, uri));
        }

        switch (usQuery.getOperator()) {
        case IN:
            return new ConstantScoreQuery(tf);
        case NI:
            return new ConstantScoreQuery(new InversionFilter(tf));
        default:
            throw new QueryBuilderException(
                        "Operator '" + usQuery.getOperator() + "' not legal for UriSetQuery.");
        }

    }

}

package org.vortikal.repositoryimpl.query.builders;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
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
    private Map typeDescendantNames;
    
    public TypeTermQueryBuilder(Map typeDescendantNames, TypeTermQuery ttq) {
        this.ttq = ttq;
        this.typeDescendantNames = typeDescendantNames; 
    }

    public Query buildQuery() {
        String typeTerm = ttq.getTerm();
        
        if (ttq.getOperator() == TypeOperator.EQ) {
            return new TermQuery(new Term(DocumentMapper.RESOURCETYPE_FIELD_NAME, typeTerm));
        } else if (ttq.getOperator() == TypeOperator.IN) {
            
            BooleanQuery bq = new BooleanQuery(true);
            bq.add(new TermQuery(new Term(DocumentMapper.RESOURCETYPE_FIELD_NAME, typeTerm)),
                                BooleanClause.Occur.SHOULD);
            List descendantNames = (List)typeDescendantNames.get(typeTerm);
            
            if (descendantNames != null) {
                for (Iterator i = descendantNames.iterator();i.hasNext();) {
                    Term t = new Term(DocumentMapper.RESOURCETYPE_FIELD_NAME, (String)i.next());
                    bq.add(new TermQuery(t),  BooleanClause.Occur.SHOULD);
                }
            }
            
            return bq;
        } else throw new QueryBuilderException("Unsupported type operator: " + ttq.getOperator());

    }

}

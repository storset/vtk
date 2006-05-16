package org.vortikal.repositoryimpl.query.builders;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.vortikal.repositoryimpl.query.DocumentMapper;
import org.vortikal.repositoryimpl.query.QueryBuilder;
import org.vortikal.repositoryimpl.query.QueryBuilderException;

/**
 * 
 * @author oyviste
 */
public class UriPrefixQueryBuilder implements QueryBuilder {

    private Term idTerm;
    
    /**
     * 
     * @param idTerm The <code>Term</code> containing the special id of the property set
     *        that represents the uri prefix.
     */
    public UriPrefixQueryBuilder(Term idTerm) {
        this.idTerm = idTerm;
    }
    
    public Query buildQuery() throws QueryBuilderException {
        
        BooleanQuery bq = new BooleanQuery();
        
        TermQuery uriTermq = new TermQuery(idTerm);
        bq.add(uriTermq, BooleanClause.Occur.SHOULD);
        
        // Use ancestor ids field from index to det all descendants
        TermQuery uriDescendants = 
            new TermQuery(
                    new Term(DocumentMapper.ANCESTORIDS_FIELD_NAME, idTerm.text()));
        
        bq.add(uriDescendants, BooleanClause.Occur.SHOULD);
        
        return bq;
    }

}

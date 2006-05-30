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
    private String uri;
    
    /**
     * 
     * @param idTerm The <code>Term</code> containing the special id of the property set
     *        that represents the URI prefix (the ancestor).
     */
    public UriPrefixQueryBuilder(String uri, Term idTerm) {
        this.idTerm = idTerm;
        this.uri = uri;
    }
    
    public Query buildQuery() throws QueryBuilderException {
        // Use ancestor ids field from index to get all descendants
        TermQuery uriDescendants = 
            new TermQuery(
                    new Term(DocumentMapper.ANCESTORIDS_FIELD_NAME, idTerm.text()));

        if (uri.endsWith("/")) {
            // Don't include parent
            // XXX: Note that the root URI '/' is a special case, it will not be included
            //      as part of URI prefix query results (only the children).
            //      If we need to differentiate between the "include-self or not"-case
            //      for the root resource, this info has to be explicitly available in query class.
            return uriDescendants;
        } else {
            // Include the parent URI as well
            BooleanQuery bq = new BooleanQuery();
            TermQuery uriTermq = new TermQuery(idTerm);
            bq.add(uriTermq, BooleanClause.Occur.SHOULD);
            bq.add(uriDescendants, BooleanClause.Occur.SHOULD);
            
            return bq;
        }
    }

}

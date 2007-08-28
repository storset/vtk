package org.vortikal.repositoryimpl.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TopDocs;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.search.PropertySelect;
import org.vortikal.repository.search.QueryException;
import org.vortikal.repository.search.ResultSet;
import org.vortikal.repository.search.Search;
import org.vortikal.repository.search.Searcher;
import org.vortikal.repository.search.Sorting;
import org.vortikal.repository.search.query.Query;
import org.vortikal.repositoryimpl.index.LuceneIndexManager;
import org.vortikal.repositoryimpl.index.mapping.DocumentMapper;
import org.vortikal.repositoryimpl.search.query.QueryBuilderFactory;
import org.vortikal.repositoryimpl.search.query.SortBuilder;
import org.vortikal.repositoryimpl.search.query.SortBuilderImpl;
import org.vortikal.repositoryimpl.search.query.security.LuceneResultSecurityInfo;
import org.vortikal.repositoryimpl.search.query.security.QueryResultAuthorizationManager;
import org.vortikal.repositoryimpl.search.query.security.ResultSecurityInfo;

public class NewSearcherImpl implements Searcher {

    private static final Log LOG = LogFactory.getLog(SearcherImpl.class);

    private LuceneIndexManager indexAccessor;
    private DocumentMapper documentMapper;
    private QueryResultAuthorizationManager queryResultAuthorizationManager;
    private QueryBuilderFactory queryBuilderFactory;
    
    private final SortBuilder sortBuilder = new SortBuilderImpl();

    /**
     * The internal maximum number of hits allowed for any
     * query <em>before</em> processing of the results by layers above Lucene.
     * This limit includes unauthorized hits that are <em>not</em> supplied to client.
     */
    private int luceneSearchLimit = 60000;
    
    public void afterPropertiesSet() throws BeanInitializationException {
        if (this.luceneSearchLimit <= 0) {
            throw new BeanInitializationException(
             "Property 'luceneHitLimit' must be an integer greater than zero.");
        }
    }
    
    public ResultSet execute(String token, Search search) throws QueryException {

        Query query = search.getQuery();
        Sorting sorting = search.getSorting();
        int clientLimit = search.getLimit();
        int clientCursor = search.getCursor();
        PropertySelect selectedProperties = search.getPropertySelect();

        org.apache.lucene.search.Query luceneQuery =
            this.queryBuilderFactory.getBuilder(query).buildQuery();

        Sort luceneSort = sorting != null ? 
                this.sortBuilder.buildSort(sorting) : null;
        
        FieldSelector selector = selectedProperties != null ?
                this.documentMapper.getDocumentFieldSelector(selectedProperties) : null;
        
        if (LOG.isDebugEnabled()) {
            LOG.debug("Built Lucene query '" 
                    + luceneQuery + "' from query '" + query.dump("") + "'");
            
            LOG.debug("Built Lucene sorting '" + luceneSort + "' from sorting '"
                    + sorting + "'");
        }

        IndexSearcher searcher = null;
        try {
            searcher = this.indexAccessor.getIndexSearcher();
            IndexReader reader = searcher.getIndexReader();
            
            int need = clientCursor + clientLimit;
            int have = 0;
            
            // Perform searches until we have enough authorized results
            int searchLimit = need;
            if (searchLimit > this.luceneSearchLimit) {
                searchLimit = this.luceneSearchLimit;
            }
            int scoreDocPos = 0;
            List<Document> authorizedDocs = new ArrayList<Document>(need);
            
            System.out.println("Starting search interations ..");
            System.out.println("clientCursor = " + clientCursor + ", clientLimit = " + clientLimit);
            System.out.println("need = " + need + ", have = " + have);
            int round = 0;
            int totalHits = -1;
            while (have < need) {
                System.out.println("Searching with search limit: " + searchLimit + ", round =" + round);
                TopDocs topDocs = performLuceneQuery(searcher, luceneQuery, 
                                                     searchLimit, luceneSort);
                
                ScoreDoc[] docs = topDocs.scoreDocs;
                totalHits = topDocs.totalHits;
                System.out.println("Got " + docs.length + " Lucene hits, totalHits = " + totalHits);
                
                have += authorizeScoreDocs(docs, scoreDocPos,
                                           authorizedDocs,
                                           reader, token, selector);
                
                System.out.println("have = " + have + " after authorization");
                
                if (totalHits == docs.length 
                              || searchLimit == this.luceneSearchLimit) {
                    // We already have all available hits, no need to continue ..
                    System.out.println("Breaking out because totalHits == docs.length || searchLimit reaced max");
                    break;
                }  
                
                scoreDocPos = docs.length;
                searchLimit = Math.min(searchLimit * 2, this.luceneSearchLimit);
                System.out.println("Preparing for next round with new searchLimit = " + searchLimit);
                System.out.println("New scoreDocPos = " + scoreDocPos);
                System.out.println("-------");
                ++round;
            }
            
            System.out.println();
            System.out.println("Finished with search iterations, needed " + round + " rounds.");
            System.out.println("authorizedDocs.size() == " + authorizedDocs.size());
            System.out.println("have == " + have);
            
            ResultSetImpl rs = new ResultSetImpl();
            rs.setTotalHits(totalHits);
            if (clientCursor < have) {
                int end = Math.min(need, have);
                for (Document doc: authorizedDocs.subList(clientCursor, end)) {
                    rs.addResult(this.documentMapper.getPropertySet(doc));
                }
            }
            
            return rs;
            
        } catch (IOException io) {
            LOG.warn("IOException while performing query on index", io);
            throw new QueryException("IOException while performing query on index", io);
        } finally {
            try {
                this.indexAccessor.releaseIndexSearcher(searcher);                
            } catch (IOException io) {
                LOG.warn("IOException while releasing index searcher", io);
            }
        }
    }
    
    private TopDocs performLuceneQuery(IndexSearcher searcher, 
            org.apache.lucene.search.Query query, int limit, Sort sort)
        throws IOException {

        if (sort != null) {
            return searcher.search(query, null, limit, sort);
        } else {
            return searcher.search(query, null, limit);
        }
    }

    private int authorizeScoreDocs(ScoreDoc[] docs, int scoreDocPos,
            List<Document> authorizedDocs, IndexReader reader, String token,
            FieldSelector fieldSelector) throws IOException {

        List<ResultSecurityInfo> rsiList = 
            new ArrayList<ResultSecurityInfo>(docs.length-scoreDocPos);
        
        for (int i = scoreDocPos; i < docs.length; i++) {
            Document doc = reader.document(docs[i].doc, fieldSelector);
            rsiList.add(new LuceneResultSecurityInfo(doc));
        }

        this.queryResultAuthorizationManager.authorizeQueryResults(token, rsiList);

        int authorizedCount = 0;
        for (ResultSecurityInfo rsi : rsiList) {
            if (rsi.isAuthorized()) {
                authorizedDocs.add(((LuceneResultSecurityInfo) rsi).getDocument());
                ++authorizedCount;
            }
        }

        return authorizedCount;
    }

    @Required
    public void setDocumentMapper(DocumentMapper documentMapper) {
        this.documentMapper = documentMapper;
    }

    @Required
    public void setIndexAccessor(LuceneIndexManager indexAccessor) {
        this.indexAccessor = indexAccessor;
    }

    @Required
    public void setQueryBuilderFactory(QueryBuilderFactory queryBuilderFactory) {
        this.queryBuilderFactory = queryBuilderFactory;
    }
    
    public int getLuceneSearchLimit() {
        return luceneSearchLimit;
    }

    public void setLuceneSearchLimit(int luceneSearchLimit) {
        this.luceneSearchLimit = luceneSearchLimit;
    }
    
    @Required
    public void setQueryResultAuthorizationManager(QueryResultAuthorizationManager
                                                   queryResultAuthorizationManager) {
        this.queryResultAuthorizationManager = queryResultAuthorizationManager;
    }    
}

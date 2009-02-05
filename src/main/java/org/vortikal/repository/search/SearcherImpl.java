/* Copyright (c) 2007, University of Oslo, Norway
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 
 *  * Neither the name of the University of Oslo nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *      
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.vortikal.repository.search;

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
import org.vortikal.repository.index.LuceneIndexManager;
import org.vortikal.repository.index.mapping.DocumentMapper;
import org.vortikal.repository.index.mapping.FieldValueMapper;
import org.vortikal.repository.search.query.DumpQueryTreeVisitor;
import org.vortikal.repository.search.query.Query;
import org.vortikal.repository.search.query.QueryBuilderFactory;
import org.vortikal.repository.search.query.SortBuilder;
import org.vortikal.repository.search.query.SortBuilderImpl;
import org.vortikal.repository.search.query.security.LuceneResultSecurityInfo;
import org.vortikal.repository.search.query.security.QueryResultAuthorizationManager;
import org.vortikal.repository.search.query.security.ResultSecurityInfo;

@Deprecated
public class SearcherImpl implements Searcher {

    private static final Log LOG = LogFactory.getLog(SearcherImpl.class);

    private LuceneIndexManager indexAccessor;
    private DocumentMapper documentMapper;
    private QueryResultAuthorizationManager queryResultAuthorizationManager;
    private QueryBuilderFactory queryBuilderFactory;
    
    private FieldValueMapper fieldValueMapper;
    
    private final SortBuilder sortBuilder = new SortBuilderImpl();
    
    private static final int MIN_INITIAL_SEARCHLIMIT_UPSCALE = 500;

    /**
     * Log a warning if any query takes longer complete than 
     * this time (milliseconds). 15 seconds is the default.
     */
    private long totalQueryTimeWarnThreshold = 15000;

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

        IndexSearcher searcher = null;
        try {
            searcher = this.indexAccessor.getIndexSearcher();
            IndexReader reader = searcher.getIndexReader();
            
            org.apache.lucene.search.Query luceneQuery = this.queryBuilderFactory
                    .getBuilder(query, reader).buildQuery();

            Sort luceneSort = sorting != null ? this.sortBuilder
                    .buildSort(sorting) : null;

            FieldSelector selector = selectedProperties != null ? this.documentMapper
                    .getDocumentFieldSelector(selectedProperties)
                    : null;

            if (LOG.isDebugEnabled()) {
                LOG.debug("Built Lucene query '" + luceneQuery
                        + "' from query '"
                        + query.accept(new DumpQueryTreeVisitor(), null) + "'");

                LOG.debug("Built Lucene sorting '" + luceneSort
                        + "' from sorting '" + sorting + "'");
            }
        
            int need = clientCursor + clientLimit;
            int have = 0;
            
            // Perform searches until we have enough authorized results
            int searchLimit = Math.min(this.luceneSearchLimit, need);
            int scoreDocPos = 0;
            List<Document> authorizedDocs = new ArrayList<Document>(need);
            
            if (LOG.isDebugEnabled()) {
                LOG.debug("Starting search iterations ..");
                LOG.debug("clientCursor = " + clientCursor + ", clientLimit = " + clientLimit);
                LOG.debug("need = " + need + ", have = " + have);
            }
            
            int round = 0;
            long totalLuceneQueryTime = 0L;
            long queryAuthorizationTime = 0L;
            int totalHits = -1;
            while (have < need) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Searching with search limit = " 
                                            + searchLimit + ", round = " + round);
                }
                long start = System.currentTimeMillis();
                TopDocs topDocs = performLuceneQuery(searcher, luceneQuery, 
                                                     searchLimit, luceneSort);
                long finished = System.currentTimeMillis();
                if (LOG.isDebugEnabled()) {
                    if (luceneSort != null) {
                        LOG.debug("Sorted Lucene query with searchLimit = " 
                                + searchLimit + " took " 
                                + (finished-start) + "ms");
                    } else {
                        LOG.debug("Unsorted Lucene query with searchLimit = " 
                                + searchLimit + " took " 
                                + (finished-start) + "ms");
                    }
                }
                totalLuceneQueryTime += (finished-start);
                
                ScoreDoc[] docs = topDocs.scoreDocs;
                totalHits = topDocs.totalHits;
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Got " + docs.length + " Lucene hits, totalHits = " + totalHits);
                }
                
                start = System.currentTimeMillis();
                have += authorizeScoreDocs(docs, scoreDocPos,
                                           authorizedDocs,
                                           reader, token, selector);
                finished = System.currentTimeMillis();
                queryAuthorizationTime += (finished - start);
                
                
                if (LOG.isDebugEnabled()) {
                    LOG.debug("have = " + have + " results after authorization");
                }

                ++round;

                if (totalHits == docs.length 
                              || searchLimit == this.luceneSearchLimit) {
                    // We already have all available hits, no need to continue ..
                    LOG.debug("Breaking out because totalHits == docs.length || searchLimit reached max");
                    break;
                }  
                
                scoreDocPos = docs.length;
                searchLimit = Math.min(
                                Math.max(searchLimit * 2, MIN_INITIAL_SEARCHLIMIT_UPSCALE),
                                                                            this.luceneSearchLimit);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Preparing for next round with new searchLimit = " + searchLimit);
                    LOG.debug("New scoreDocPos = " + scoreDocPos);
                }
            }
            
            if (totalLuceneQueryTime > this.totalQueryTimeWarnThreshold) {
                // Log a warning, query took too long to complete.
                StringBuilder msg = 
                    new StringBuilder("Total execution time for Lucene query '");
                msg.append(luceneQuery).append("'");
                msg.append(", with a final search limit of ").append(searchLimit);
                msg.append(", was ").append(totalLuceneQueryTime).append("ms. ");
                msg.append("This exceeds the warning threshold of ");
                msg.append(this.totalQueryTimeWarnThreshold);
                msg.append("ms. Search limit was upscaled ").append(round-1);
                msg.append(" time(s).");
                LOG.warn(msg);
            }
            
            if (LOG.isDebugEnabled()) {
                LOG.debug("Finished with search iterations, needed " + round + " rounds.");
                
                LOG.debug("Total time spent with Lucene queries: " + totalLuceneQueryTime + "ms");
                LOG.debug("Total time spent with result authorization: " + queryAuthorizationTime + "ms");
                
                LOG.debug("authorizedDocs.size() = " + authorizedDocs.size());
                LOG.debug("have = " + have);
            }
            
            ResultSetImpl rs = new ResultSetImpl();
            rs.setTotalHits(totalHits);
            if (clientCursor < have) {
                int end = Math.min(need, have);
                
                long start = System.currentTimeMillis();
                for (Document doc: authorizedDocs.subList(clientCursor, end)) {
                    rs.addResult(this.documentMapper.getPropertySet(doc));
                }
                long finished = System.currentTimeMillis();
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Document mapping took " + (finished-start) + "ms");
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
        } 

        return searcher.search(query, null, limit);

    }

    private int authorizeScoreDocs(ScoreDoc[] docs, int scoreDocPos,
            List<Document> authorizedDocs, IndexReader reader, String token,
            FieldSelector fieldSelector) throws IOException {

        List<ResultSecurityInfo> rsiList = 
            new ArrayList<ResultSecurityInfo>(docs.length-scoreDocPos);
        
        for (int i = scoreDocPos; i < docs.length; i++) {
            Document doc = reader.document(docs[i].doc, fieldSelector);
            rsiList.add(new LuceneResultSecurityInfo(doc, this.fieldValueMapper));
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
    
    @Required
    public void setQueryResultAuthorizationManager(QueryResultAuthorizationManager
                                                   queryResultAuthorizationManager) {
        this.queryResultAuthorizationManager = queryResultAuthorizationManager;
    }    

    public int getLuceneSearchLimit() {
        return luceneSearchLimit;
    }

    public void setLuceneSearchLimit(int luceneSearchLimit) {
        this.luceneSearchLimit = luceneSearchLimit;
    }

    public void setTotalQueryTimeWarnThreshold(long totalQueryTimeWarnThreshold) {
        if (totalQueryTimeWarnThreshold <= 0) {
            throw new IllegalArgumentException("Argument cannot be zero or negative");
        }
        
        this.totalQueryTimeWarnThreshold = totalQueryTimeWarnThreshold;
    }

    @Required
    public void setFieldValueMapper(FieldValueMapper fieldValueMapper) {
        this.fieldValueMapper = fieldValueMapper;
    }
    
}

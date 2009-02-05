/* Copyright (c) 2009, University of Oslo, Norway
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TopDocs;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.index.LuceneIndexManager;
import org.vortikal.repository.index.mapping.DocumentMapper;
import org.vortikal.repository.search.query.DumpQueryTreeVisitor;
import org.vortikal.repository.search.query.Query;
import org.vortikal.repository.search.query.QueryBuilderFactory;
import org.vortikal.repository.search.query.SortBuilder;
import org.vortikal.repository.search.query.SortBuilderImpl;
import org.vortikal.repository.search.query.security.QueryAuthorizationFilterFactory;

/**
 * 
 *
 */
public class SearcherImplACL implements Searcher {

    private final Log logger = LogFactory.getLog(SearcherImplACL.class);

    private LuceneIndexManager indexAccessor;
    private DocumentMapper documentMapper;
    private QueryBuilderFactory queryBuilderFactory;
    private QueryAuthorizationFilterFactory queryAuthorizationFilterFactory;
    private final SortBuilder sortBuilder = new SortBuilderImpl();
    
    private long totalQueryTimeWarnThreshold = 15000; // Warning threshold in milliseconds
    
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

            org.apache.lucene.search.Query luceneQuery = this.queryBuilderFactory
                    .getBuilder(query, searcher.getIndexReader()).buildQuery();

            Sort luceneSort = sorting != null ? this.sortBuilder
                    .buildSort(sorting) : null;

            FieldSelector selector = selectedProperties != null ? this.documentMapper
                    .getDocumentFieldSelector(selectedProperties)
                    : null;

            if (logger.isDebugEnabled()) {
                logger.debug("Built Lucene query '" + luceneQuery
                        + "' from query '"
                        + query.accept(new DumpQueryTreeVisitor(), null) + "'");

                logger.debug("Built Lucene sorting '" + luceneSort
                        + "' from sorting '" + sorting + "'");
            }

            int need = clientCursor + clientLimit;
            long totalTime = 0;
            
            int searchLimit = Math.min(this.luceneSearchLimit, need);

            long startTime = System.currentTimeMillis();
            TopDocs topDocs = doACLFilteredTopDocsQuery(searcher, luceneQuery, 
                                             searchLimit, luceneSort, token);
            long endTime = System.currentTimeMillis();
            
            if (logger.isDebugEnabled()){
                logger.debug("ACL filtered lucene query took " + (endTime-startTime) + "ms");
            }
            
            totalTime += (endTime - startTime);

            ScoreDoc[] scoreDocs = topDocs.scoreDocs;
            
            ResultSetImpl rs = new ResultSetImpl();
            rs.setTotalHits(topDocs.totalHits);
            if (clientCursor < scoreDocs.length) {
                int end = Math.min(need, scoreDocs.length);
                
                startTime = System.currentTimeMillis();
                for (int i=clientCursor; i < end; i++) {
                    int scoreDocId = scoreDocs[i].doc;
                    Document doc = searcher.doc(scoreDocId, selector);
                    PropertySet propSet = this.documentMapper.getPropertySet(doc);
                    rs.addResult(propSet);
                }
                endTime = System.currentTimeMillis();
            
                totalTime += (endTime - startTime);
                
                if (logger.isDebugEnabled()){
                    logger.debug("Document mapping took " + (endTime-startTime) + "ms");
                }
            }
            
            if (totalTime > this.totalQueryTimeWarnThreshold) {
                // Log a warning, query took too long to complete.
                StringBuilder msg = 
                    new StringBuilder("Total execution time for Lucene query '");
                msg.append(luceneQuery).append("'");
                msg.append(", with a search limit of ").append(searchLimit);
                msg.append(", was ").append(totalTime).append("ms. ");
                msg.append("This exceeds the warning threshold of ");
                msg.append(this.totalQueryTimeWarnThreshold).append("ms");
                logger.warn(msg);
            }
            
            return rs;
            
        } catch (IOException io) {
            logger.warn("IOException while performing query on index", io);
            throw new QueryException("IOException while performing query on index", io);
        } finally {
            try {
                this.indexAccessor.releaseIndexSearcher(searcher);                
            } catch (IOException io) {
                logger.warn("IOException while releasing index searcher", io);
            }
        }
    }
    
    private TopDocs doACLFilteredTopDocsQuery(IndexSearcher searcher, 
                                              org.apache.lucene.search.Query query,
                                              int limit,
                                              Sort sort,
                                              String token)
        throws IOException {

        Filter aclFilter = 
            this.queryAuthorizationFilterFactory.authorizationQueryFilter(token, 
                                                       searcher.getIndexReader());
        
        if (logger.isDebugEnabled()){
            if (aclFilter == null) {
                logger.debug("ACL filter null for token: " + token);
            } else {
                logger.debug("ACL filter: " +  aclFilter + " (token = " + token + ")");
            }
        }
        
        if (sort != null) {
            return searcher.search(query, aclFilter, limit, sort);
        } 

        return searcher.search(query, aclFilter, limit);
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
    public void setQueryAuthorizationFilterFactory(QueryAuthorizationFilterFactory factory) {
        this.queryAuthorizationFilterFactory = factory;
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

}

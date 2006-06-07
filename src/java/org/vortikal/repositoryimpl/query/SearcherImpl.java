/* Copyright (c) 2006, University of Oslo, Norway
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
package org.vortikal.repositoryimpl.query;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.HitCollector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TopDocs;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repository.AuthorizationException;
import org.vortikal.repository.query.QueryException;
import org.vortikal.repositoryimpl.query.parser.ResultSet;
import org.vortikal.repositoryimpl.query.parser.ResultSetImpl;
import org.vortikal.repositoryimpl.query.parser.Searcher;
import org.vortikal.repositoryimpl.query.query.Query;
import org.vortikal.repositoryimpl.query.query.Sorting;
import org.vortikal.repositoryimpl.query.security.QueryAuthorizationManager;
import org.vortikal.security.AuthenticationException;

/**
 * @author oyviste
 *
 *  TODO: As efficient ACL filtering as possible
 *  TODO: Define behavoir when results are removed because of permissions
 *        wrt. to expected number of maxResults, etc.
 *        
 *  <p>Configurable bean properties:</p>
 *  <ul>
 *      <li><code>maxAllowedHitsPerQuery</code>
 *      The internal maximum number of hits allowed for any
 *      query <em>before</em> any post-processing of the results.
 *      This is a low-level hard system limit, and no query result will ever be 
 *      bigger than this value.
 *      </li>
 *      
 *      <li><code>indexAccessor</code>
 *      The low-level Lucene index accessor instance that this searcher should use</li>
 *      
 *      <li><code>queryAuthorizationManager</code>
 *      Manager used to authorize results in queries based on the security token
 *      of the client performing the query. Security filtering will not be 
 *      applied to results if this bean is not configured.</li>
 *      
 *      <li><code>queryBuilderFactory</code>
 *      Factory for building Lucene queries (required)</li>
 *  </ul>
 *
 */
public class SearcherImpl implements Searcher, InitializingBean {

    Log logger = LogFactory.getLog(SearcherImpl.class);
    
    private LuceneIndex indexAccessor;
    private DocumentMapper documentMapper;
    private QueryAuthorizationManager queryAuthorizationManager;
    private QueryBuilderFactory queryBuilderFactory;
    
    private SortBuilder sortBuilder = new SortBuilderImpl();
    
    /**
     * The maximum number of hits allowed for any
     * query <em>before</em> processing of the results by layers above Lucene.
     */
    private int maxAllowedHitsPerQuery = 50000;
    
    public void afterPropertiesSet() throws BeanInitializationException {
        if (indexAccessor == null) {
            throw new BeanInitializationException("Property 'indexAccessor' not set.");
        } else if (documentMapper == null) {
            throw new BeanInitializationException("Property 'documentMapper' not set.");
        } else if (queryBuilderFactory == null) {
            throw new BeanInitializationException("Property 'queryBuilderFactory' not set.");
        } else if (this.maxAllowedHitsPerQuery <= 0) {
            throw new BeanInitializationException("Property 'maxAllowedHitsPerQuery'" 
                                         + " must be an integer greater than zero.");
        }
        
        if (this.queryAuthorizationManager == null) {
            logger.warn("No authorization manager configured, queries will not be filtered with regard to"
                    + " permissions.");
        }
    }
    
    /* (non-Javadoc)
     * @see org.vortikal.repositoryimpl.queryparser.Searcher#execute(java.lang.String, org.vortikal.repositoryimpl.query.query.Query)
     */
    public ResultSet execute(String token, Query query) throws QueryException {
        
        return executeQuery(token, query, null, this.maxAllowedHitsPerQuery, 0);
        
    }

    /* (non-Javadoc)
     * @see org.vortikal.repositoryimpl.queryparser.Searcher#execute(java.lang.String, org.vortikal.repositoryimpl.query.query.Query, int)
     */
    public ResultSet execute(String token, Query query, int maxResults)
            throws QueryException {
        
        return executeQuery(token, query, null, maxResults, 0);
        
    }
    
    /* (non-Javadoc)
     * @see org.vortikal.repositoryimpl.queryparser.Searcher#execute(java.lang.String, org.vortikal.repositoryimpl.query.query.Query, int, int)
     */
    public ResultSet execute(String token, Query query, int maxResults, int cursor)
            throws QueryException {
        
        return executeQuery(token, query, null, maxResults, cursor);
        
    }
    
    public ResultSet execute(String token, Query query, Sorting sorting) 
        throws QueryException {

        return executeQuery(token, query, sorting, this.maxAllowedHitsPerQuery, 0);
        
    }

    public ResultSet execute(String token, Query query, Sorting sorting,
        int maxResults) throws QueryException {
    
        return executeQuery(token, query, sorting, maxResults, 0);
        
    }

    public ResultSet execute(String token, Query query, Sorting sorting,
        int maxResults, int cursor) throws QueryException {
    
        return executeQuery(token, query, sorting, maxResults, cursor);
        
    }

    
    private ResultSet executeQuery(String token, Query query, Sorting sorting,
                int maxResults, int cursor) throws QueryException {
        
        org.apache.lucene.search.Query q =
            this.queryBuilderFactory.getBuilder(query).buildQuery();

        if (logger.isDebugEnabled()) {
            logger.debug("Built lucene query '" + q + "' from query" + query.dump(""));
        }

        IndexSearcher searcher = null;
        try {
            searcher = indexAccessor.getIndexSearcher();
            
            // The n parameter is the upper maximum number of results we allow
            // Lucene to fetch.
            int n = cursor + maxResults;
            if (n > this.maxAllowedHitsPerQuery) n = this.maxAllowedHitsPerQuery;
            
            long start = System.currentTimeMillis();
            TopDocs topDocs = null;
            if (sorting != null) {
                Sort sort = sortBuilder.buildSort(sorting);
                topDocs = searcher.search(q, null, n, sort);
            } else {
                topDocs = searcher.search(q, null, n);
            }
            
            if (logger.isDebugEnabled()) {
                logger.debug("Got " + topDocs.totalHits 
                                        + " total hits for lucene query: " + q);
                
                if (sorting != null) {
                    logger.debug("Sorted lucene query took: " 
                            + (System.currentTimeMillis()-start) + " ms");
                } else {
                    logger.debug("Unsorted (index-ordered) lucene query took: " 
                            + (System.currentTimeMillis()-start) + " ms");
                }
            }
            
            ResultSet rs = buildResultSet(topDocs.scoreDocs, searcher.getIndexReader(), 
                                          token, maxResults, cursor);
            
            if (logger.isDebugEnabled()) {
                logger.debug("Total query time: " + (System.currentTimeMillis()-start) + " ms");
            }
            
            return rs;
            
        } catch (IOException io) {
            logger.warn("IOException while performing query on index", io);
            throw new QueryException("IOException while performing query on index", io);
        } finally {
            try {
                indexAccessor.releaseIndexSearcher(searcher);                
            } catch (IOException io) {}
        }
    }

    private ResultSetImpl buildResultSet(ScoreDoc[] docs, IndexReader reader, 
                                         String token, int maxResults,
                                         int cursor) throws IOException {
        
        ResultSetImpl rs = new ResultSetImpl();
        
        int length = docs.length;
        
        if (length == 0 || cursor >= length) {
            return rs; // Empty result set
        }
        
        if (maxResults < 0) maxResults = 0;
        
        int end = (cursor + maxResults) < length ? 
                cursor + maxResults : length;
        
        for (int i=cursor; i < end; i++) {
            Document doc = reader.document(docs[i].doc);
            
            try {
                authorizeQueryResult(token, doc);
            } catch (AuthorizationException e) {
                continue;
            } catch (AuthenticationException e) {
                continue;
            }
            
            rs.addResult(this.documentMapper.getPropertySet(doc));
        }
        
        return rs;
    }

//    private ResultSetImpl buildResultSet(Hits hits, String token, int maxResults, 
//                                                                  int cursor)
//        throws IOException {
//        
//        ResultSetImpl rs = new ResultSetImpl();
//        
//        int length = hits.length();
//        
//        if (length == 0 || cursor >= length) {
//            return rs; // Empty result set
//        }
//
//        if (maxResults < 0) maxResults = 0;
//        
//        int end = (cursor + maxResults) < length ? 
//                                        cursor + maxResults : length;
//
//        for (int i=cursor; i < end; i++) {
//            Document doc = hits.doc(i);
//
//                try {
//                    authorizeQueryResult(token, doc);
//                } catch (AuthorizationException e) {
//                    continue;
//                } catch (AuthenticationException e) {
//                    continue;
//                }
//            
//            PropertySet propSet = this.documentMapper.getPropertySet(doc);
//            rs.addResult(propSet);
//        }
//        
//        return rs;
//    }
    
//    private ResultSetImpl buildResultSet(SimpleHitCollector collector, 
//            IndexReader reader, String token, int maxResults, int cursor) 
//    throws IOException {
//
//        ResultSetImpl rs = new ResultSetImpl();
//
//        int length = collector.length();
//
//        if (length == 0 || cursor >= length) {
//            return rs; // Empty result set
//        }
//
//        if (maxResults < 0) maxResults = 0;
//
//        int end = (cursor + maxResults) < length ? cursor + maxResults : length;
//
//        for (int i = cursor; i < end; i++) {
//            Document doc = reader.document(collector.doc(i));
//
//            try {
//                authorizeQueryResult(token, doc);
//            } catch (AuthorizationException e) {
//                continue;
//            } catch (AuthenticationException e) {
//                continue;
//            }
//
//            PropertySet propSet = this.documentMapper.getPropertySet(doc);
//            rs.addResult(propSet);
//        }
//
//        return rs;
//
//    }

//  private ResultSet executeUnsortedQuery(String token, Query query, 
//  int maxResults, int cursor) throws QueryException {
//
//org.apache.lucene.search.Query q =
//  this.queryBuilderFactory.getBuilder(query).buildQuery();
//
//if (logger.isDebugEnabled()) {
//  logger.debug("Built lucene query '" + q + "' from query" + query.dump(""));
//}
//
//IndexSearcher searcher = null;
//try {
//  searcher = indexAccessor.getIndexSearcher();
//  
//  SimpleHitCollector collector = new SimpleHitCollector();
//  
//  long start = System.currentTimeMillis();
//  searcher.search(q, collector);
//  if (logger.isDebugEnabled()) {
//      logger.debug("Got " + collector.length() + " hits for lucene query: " + q);
//      logger.debug("Lucene query took: " + (System.currentTimeMillis()-start) + " ms");
//  }
//
//  ResultSet rs = buildResultSet(collector, searcher.getIndexReader(), 
//                                          token, maxResults, cursor);
//  
//  if (logger.isDebugEnabled()) {
//      logger.debug("Total query time: " + (System.currentTimeMillis() - start) + " ms");
//  }
//  
//  return rs;
//  
//} catch (IOException io) {
//  logger.warn("IOException while performing query on index", io);
//  throw new QueryException("IOException while performing query on index", io);
//} finally {
//  try {
//      indexAccessor.releaseIndexSearcher(searcher);                
//  } catch (IOException io) {}
//}
//
//}

//private ResultSet executeSortedQuery(String token, Query query, Sorting sorting,
//  int maxResults, int cursor) throws QueryException {
//
//org.apache.lucene.search.Query q =
//  this.queryBuilderFactory.getBuilder(query).buildQuery();
//
//if (logger.isDebugEnabled()) {
//  logger.debug("Built lucene query '" + q + "' from query" + query.dump(""));
//}
//
//IndexSearcher searcher = null;
//try {
//  searcher = indexAccessor.getIndexSearcher();
//  
//  Sort sort = sortBuilder.buildSort(sorting);
//  
//  Hits hits = searcher.search(q, sort);
//  if (logger.isDebugEnabled()) {
//      logger.debug("Got " + hits.length() + " hits for lucene query: " + q);
//  }
//  
//  return buildResultSet(hits, token, maxResults, cursor);
//  
//} catch (IOException io) {
//  logger.warn("IOException while performing query on index", io);
//  throw new QueryException("IOException while performing query on index", io);
//} finally {
//  try {
//      indexAccessor.releaseIndexSearcher(searcher);                
//  } catch (IOException io) {}
//}
//
//}
    private void authorizeQueryResult(String token, Document doc)
        throws AuthorizationException, AuthenticationException {
        if (this.queryAuthorizationManager != null) {
                this.queryAuthorizationManager.authorizeQueryResult(token, 
                 FieldValueMapper.getIntegerFromUnencodedField(
                         doc.getField(DocumentMapper.ID_FIELD_NAME)),
                 FieldValueMapper.getIntegerFromUnencodedField(
                         doc.getField(DocumentMapper.ACL_INHERITED_FROM_FIELD_NAME)));
        }
    }
    
    public void setDocumentMapper(DocumentMapper documentMapper) {
        this.documentMapper = documentMapper;
    }

    public void setIndexAccessor(LuceneIndex indexAccessor) {
        this.indexAccessor = indexAccessor;
    }

    public void setQueryAuthorizationManager(QueryAuthorizationManager queryAuthorizationManager) {
        this.queryAuthorizationManager = queryAuthorizationManager;
    }

    public void setQueryBuilderFactory(QueryBuilderFactory queryBuilderFactory) {
        this.queryBuilderFactory = queryBuilderFactory;
    }
    
    public void setMaxAllowedHitsPerQuery(int maxAllowedHitsPerQuery) {
        this.maxAllowedHitsPerQuery = maxAllowedHitsPerQuery;
    }

    public int getMaxAllowedHitsPerQuery() {
        return maxAllowedHitsPerQuery;
    }

    /**
     * Collect all document numbers that match a query, discard all scores.
     * Can only be used in a no-sorting scenario, as Lucene uses its own
     * special collectors for sorting, internally.
     */
    private class SimpleHitCollector extends HitCollector {
        
        List docNums = new ArrayList(SearcherImpl.this.maxAllowedHitsPerQuery);
        
        public void collect(int doc, float score) {
            if (docNums.size() > SearcherImpl.this.maxAllowedHitsPerQuery) {
                // Max number of hits exceeded, don't add anything more
                return;
            }
            
            docNums.add(new Integer(doc));
        }
        
        public int doc(int index) {
            return ((Integer)docNums.get(index)).intValue();
        }
        
        public int length() {
            return docNums.size();
        }
        
    }

}

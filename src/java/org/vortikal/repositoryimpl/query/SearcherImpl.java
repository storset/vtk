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
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Sort;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repository.AuthorizationException;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.query.QueryException;
import org.vortikal.repositoryimpl.query.query.Query;
import org.vortikal.repositoryimpl.query.query.Sorting;
import org.vortikal.repositoryimpl.query.security.QueryAuthorizationManager;
import org.vortikal.repositoryimpl.queryparser.ResultSet;
import org.vortikal.repositoryimpl.queryparser.ResultSetImpl;
import org.vortikal.repositoryimpl.queryparser.Searcher;
import org.vortikal.security.AuthenticationException;

/**
 * @author oyviste
 *
 * TODO: support sorting when our own sorting interface is finished.
 */
public class SearcherImpl implements Searcher, InitializingBean {

    Log logger = LogFactory.getLog(SearcherImpl.class);
    
    private LuceneIndex indexAccessor;
    private DocumentMapper documentMapper;
    private QueryAuthorizationManager queryAuthorizationManager;
    private QueryBuilderFactory queryBuilderFactory;
    
    private SortBuilder sortBuilder = new SortBuilderImpl();
    
    public void afterPropertiesSet() throws BeanInitializationException {
        if (indexAccessor == null) {
            throw new BeanInitializationException("Property 'indexAccessor' not set.");
        } else if (documentMapper == null) {
            throw new BeanInitializationException("Property 'documentMapper' not set.");
        } else if (queryBuilderFactory == null) {
            throw new BeanInitializationException("Property 'queryBuilderFactory' not set.");
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
        
        return executeUnsortedQuery(token, query, Integer.MAX_VALUE, 0);
    }

    /* (non-Javadoc)
     * @see org.vortikal.repositoryimpl.queryparser.Searcher#execute(java.lang.String, org.vortikal.repositoryimpl.query.query.Query, int)
     */
    public ResultSet execute(String token, Query query, int maxResults)
            throws QueryException {
        
        return executeUnsortedQuery(token, query, maxResults, 0);
        
    }
    
    /* (non-Javadoc)
     * @see org.vortikal.repositoryimpl.queryparser.Searcher#execute(java.lang.String, org.vortikal.repositoryimpl.query.query.Query, int, int)
     */
    public ResultSet execute(String token, Query query, int maxResults, int cursor)
            throws QueryException {
        
        return executeUnsortedQuery(token, query, maxResults, cursor);
    }
    
    public ResultSet execute(String token, Query query, Sorting sorting) 
        throws QueryException {

        return executeSortedQuery(token, query, sorting, Integer.MAX_VALUE, 0);
    }

    public ResultSet execute(String token, Query query, Sorting sorting,
        int maxResults) throws QueryException {
    
        return executeSortedQuery(token, query, sorting, maxResults, 0);
    }

    public ResultSet execute(String token, Query query, Sorting sorting,
        int maxResults, int cursor) throws QueryException {
    
        return executeSortedQuery(token, query, sorting, maxResults, cursor);
    }

    private ResultSet executeUnsortedQuery(String token, Query query, 
            int maxResults, int cursor) throws QueryException {
        
        org.apache.lucene.search.Query q = this.queryBuilderFactory.getBuilder(query).buildQuery();
        
        IndexSearcher searcher = null;
        try {
            searcher = indexAccessor.getIndexSearcher();
            
            SimpleHitCollector collector = new SimpleHitCollector();
            
            searcher.search(q, collector);
            
            return buildResultSet(collector, searcher.getIndexReader(), 
                                                    token, maxResults, cursor);
        } catch (IOException io) {
            logger.warn("IOException while performing query on index", io);
            throw new QueryException("IOException while performing query on index", io);
        } finally {
            try {
                indexAccessor.releaseIndexSearcher(searcher);                
            } catch (IOException io) {}
        }
        
    }
    
    private ResultSet executeSortedQuery(String token, Query query, Sorting sorting,
            int maxResults, int cursor) throws QueryException {

        org.apache.lucene.search.Query q = this.queryBuilderFactory.getBuilder(query).buildQuery();
        
        IndexSearcher searcher = null;
        try {
            searcher = indexAccessor.getIndexSearcher();
            
            Sort sort = sortBuilder.buildSort(sorting);
            
            Hits hits = searcher.search(q, sort);
            
            return buildResultSet(hits, token, maxResults, cursor);
            
        } catch (IOException io) {
            logger.warn("IOException while performing query on index", io);
            throw new QueryException("IOException while performing query on index", io);
        } finally {
            try {
                indexAccessor.releaseIndexSearcher(searcher);                
            } catch (IOException io) {}
        }
        
    }
    
    private ResultSetImpl buildResultSet(SimpleHitCollector collector, 
                IndexReader reader, String token, int maxResults, int cursor) 
        throws IOException {
        
        ResultSetImpl rs = new ResultSetImpl();
        
        int length = collector.length();
        
        if (length == 0 || cursor >= length) {
            return rs; // Empty result set
        }

        if (maxResults < 0) maxResults = 0;
        
        int end = (cursor + maxResults) < length ? 
                                        cursor + maxResults : length;
        
        for (int i=cursor; i < end; i++) {
            Document doc = reader.document(collector.doc(i));

                try {
                    authorizeQueryResult(token, doc);
                } catch (AuthorizationException e) {
                    continue;
                } catch (AuthenticationException e) {
                    continue;
                }
            
            PropertySet propSet = this.documentMapper.getPropertySet(doc);
            rs.addResult(propSet);
        }
        
        return rs;
        
    }
            

    private ResultSetImpl buildResultSet(Hits hits, String token, int maxResults, 
                                                                  int cursor)
        throws IOException {
        
        ResultSetImpl rs = new ResultSetImpl();
        
        int length = hits.length();
        
        if (length == 0 || cursor >= length) {
            return rs; // Empty result set
        }

        if (maxResults < 0) maxResults = 0;
        
        int end = (cursor + maxResults) < length ? 
                                        cursor + maxResults : length;

        for (int i=cursor; i < end; i++) {
            Document doc = hits.doc(i);

                try {
                    authorizeQueryResult(token, doc);
                } catch (AuthorizationException e) {
                    continue;
                } catch (AuthenticationException e) {
                    continue;
                }
            
            PropertySet propSet = this.documentMapper.getPropertySet(doc);
            rs.addResult(propSet);
        }
        
        return rs;
    }
    
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
    
    /**
     * Collect all document numbers that match a query, discard all scores.
     * Can only be used in a no-sorting scenario, as Lucene uses its own
     * special collectors for sorting, internally.
     */
    private static class SimpleHitCollector extends HitCollector {
        
        List docNums = new ArrayList();
        
        public void collect(int doc, float score) {
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

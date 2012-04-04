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
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.util.OpenBitSet;
import org.apache.lucene.util.OpenBitSetDISI;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.index.LuceneIndexManager;
import org.vortikal.repository.index.mapping.DocumentMapper;
import org.vortikal.repository.search.query.DumpQueryTreeVisitor;
import org.vortikal.repository.search.query.LuceneQueryBuilder;
import org.vortikal.repository.search.query.Query;

/**
 * Implementation of {@link org.vortikal.repository.search.Searcher} based on Lucene.
 */
public class SearcherImpl implements Searcher {

    private final Log logger = LogFactory.getLog(SearcherImpl.class);

    private LuceneIndexManager indexAccessor;
    private DocumentMapper documentMapper;
    private LuceneQueryBuilder queryBuilder;

    private int unauthenticatedQueryMaxDirtyAge = 0;
    private long totalQueryTimeWarnThreshold = 15000; // Warning threshold in milliseconds
    
    /**
     * The internal maximum number of hits allowed for any
     * query <em>before</em> processing of the results by layers above Lucene.
     * A <code>ResultSet</code> set will never be larger than this, no matter
     * what client code requests.
     * 
     * Iteration-style matching API is <em>not</em> affected by this limit.
     */
    private int luceneSearchLimit = 60000;
    
    public void afterPropertiesSet() throws BeanInitializationException {
        if (this.luceneSearchLimit <= 0) {
            throw new BeanInitializationException(
             "Property 'luceneHitLimit' must be an integer greater than zero.");
        }
    }

    @Override
    public ResultSet execute(String token, Search search) throws QueryException {
        Query query = search.getQuery();
        Sorting sorting = search.getSorting();
        int clientLimit = search.getLimit();
        int clientCursor = search.getCursor();
        PropertySelect selectedProperties = search.getPropertySelect();

        IndexSearcher searcher = null;
        try {
            if (token == null && this.unauthenticatedQueryMaxDirtyAge > 0) {
                // Accept higher dirty age for speedier queries when token is null
                searcher = this.indexAccessor.getIndexSearcher(this.unauthenticatedQueryMaxDirtyAge);
            } else {
                // Authenticated query, no dirty age acceptable.
                searcher = this.indexAccessor.getIndexSearcher();
            }

            // Build Lucene query
            org.apache.lucene.search.Query luceneQuery =
                this.queryBuilder.buildQuery(query, searcher.getIndexReader());

            // Should include ACL filter combined with any other necessary filters ..
            org.apache.lucene.search.Filter luceneFilter =
                this.queryBuilder.buildSearchFilter(token, search, searcher.getIndexReader());

            // Build Lucene sorting
            org.apache.lucene.search.Sort luceneSort = 
                this.queryBuilder.buildSort(sorting);

            // Get Lucene document field selector
            FieldSelector selector = selectedProperties != null ? 
                  this.documentMapper.getDocumentFieldSelector(selectedProperties) : null;

            if (logger.isDebugEnabled()) {
                logger.debug("Built Lucene query '" + luceneQuery
                        + "' from query '"
                        + query.accept(new DumpQueryTreeVisitor(), null) + "'");

                logger.debug("Built Lucene sorting '" + luceneSort
                        + "' from sorting '" + sorting + "'");
                
                logger.debug("Built Lucene filter: " + luceneFilter);
            }

            int need = clientCursor + clientLimit;
            long totalTime = 0;
            
            int searchLimit = Math.min(this.luceneSearchLimit, need);

            long startTime = System.currentTimeMillis();
            TopDocs topDocs = doTopDocsQuery(searcher, luceneQuery, 
                                             luceneFilter, luceneSort, searchLimit);
            long endTime = System.currentTimeMillis();
            
            if (logger.isDebugEnabled()){
                logger.debug("Filtered lucene query took " + (endTime-startTime) + "ms");
            }
            
            totalTime += (endTime - startTime);

            ScoreDoc[] scoreDocs = topDocs.scoreDocs;
            
            ResultSetImpl rs;
            if (clientCursor < scoreDocs.length) {
                int end = Math.min(need, scoreDocs.length);
                rs = new ResultSetImpl(end-clientCursor);
                
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
            } else {
                rs = new ResultSetImpl(0);
            }
            rs.setTotalHits(topDocs.totalHits);
            
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
    
    private TopDocs doTopDocsQuery(IndexSearcher searcher, 
                                   org.apache.lucene.search.Query query,
                                   org.apache.lucene.search.Filter filter,
                                   org.apache.lucene.search.Sort sort,
                                   int limit)
        throws IOException {

        if (sort != null) {
            return searcher.search(query, filter, limit, sort);
        } 

        return searcher.search(query, filter, limit);
    }
    
    @Override
    public void iterateMatching(String token, Search search, MatchCallback callback) throws QueryException {

        IndexSearcher searcher = null;
        try {
            if (token == null && this.unauthenticatedQueryMaxDirtyAge > 0) {
                // Accept higher dirty age for speedier queries when token is null
                searcher = this.indexAccessor.getIndexSearcher(this.unauthenticatedQueryMaxDirtyAge);
            } else {
                // Authenticated query, no dirty age acceptable.
                searcher = this.indexAccessor.getIndexSearcher();
            }

            // Build iteration filter (may be null)
            Filter iterationFilter = this.queryBuilder.buildIterationFilter(
                    token, search, searcher.getIndexReader());

            // Build Lucene sorting (may be null)
            org.apache.lucene.search.Sort luceneSort = 
                this.queryBuilder.buildSort(search.getSorting());

            // Get Lucene document field selector (may be null)
            PropertySelect selectedProperties = search.getPropertySelect();
            FieldSelector selector = selectedProperties != null ? 
                  this.documentMapper.getDocumentFieldSelector(selectedProperties) : null;
            
            String iterationField = null;
            if (luceneSort != null) {
                org.apache.lucene.search.SortField[] sf = luceneSort.getSort();
                if (sf.length != 1) {
                    throw new UnsupportedOperationException("Only ONE sort field supported for iteration: " + search.getSorting());
                }
                if (sf[0].getReverse()) {
                    throw new UnsupportedOperationException("Only ascending sort supported for iteration: " + search.getSorting());
                }
                
                iterationField = sf[0].getField();
            }

            if (iterationField != null) {
                iterateOnField(iterationField,
                               iterationFilter,
                               searcher.getIndexReader(),
                               selector, search.getCursor(), search.getLimit(), callback);
            } else {
                iterate(iterationFilter,
                        searcher.getIndexReader(),
                        selector, search.getCursor(), search.getLimit(), callback);
            }
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException)e;
            }
            throw new QueryException("Exception while performing match iteration on index", e);
        } finally {
            try {
                this.indexAccessor.releaseIndexSearcher(searcher);                
            } catch (IOException io) {
                logger.warn("IOException while releasing index searcher", io);
            }
        }
    }

    /**
     * Iterator all docs matching filter in index order. Filter may be <code>null></code>
     * , in which case all non-deleted docs are iterated.
     */
    private void iterate(org.apache.lucene.search.Filter iterationFilter,
                        IndexReader reader,
                        FieldSelector luceneSelector,
                        int cursor,
                        int limit,
                        MatchCallback callback) throws Exception {

        if (limit <= 0) return;

        int matchDocCounter = 0;
        int callbackCounter = 0;
            
        if (iterationFilter == null) {
            // Iteration without filter
            final int maxDoc = reader.maxDoc();
            for (int docID = 0; docID < maxDoc; docID++) {
                if (!reader.isDeleted(docID)) {
                    if (matchDocCounter++ < cursor) {
                        continue;
                    }
                    Document doc = reader.document(docID, luceneSelector);
                    PropertySet ps = this.documentMapper.getPropertySet(doc);
                    boolean continueIteration = callback.matching(ps);
                    if (++callbackCounter == limit || !continueIteration) {
                        break;
                    }
                }
            }
            return;
        }
        
        // Iteration with filter
        DocIdSet allowedDocs = iterationFilter.getDocIdSet(reader);
        if (allowedDocs == null || allowedDocs == DocIdSet.EMPTY_DOCIDSET || limit <= 0) return;
        
        DocIdSetIterator iterator = allowedDocs.iterator();
        int docID;
        while ((docID = iterator.nextDoc()) != DocIdSetIterator.NO_MORE_DOCS) {
            if (matchDocCounter++ < cursor) continue;

            Document doc = reader.document(docID, luceneSelector);
            PropertySet ps = this.documentMapper.getPropertySet(doc);
            boolean continueIteration = callback.matching(ps);
            if (++callbackCounter == limit || !continueIteration) break;
        }
    }
    
    /**
     * Iteration all docs with given field in lexicographic order.
     * Filter may be null.
     */
    private void iterateOnField(final String luceneField,
                        org.apache.lucene.search.Filter iterationFilter,
                        IndexReader reader,
                        FieldSelector luceneSelector,
                        int cursor,
                        int limit,
                        MatchCallback callback) throws Exception {

        if (limit <= 0) return;

        OpenBitSet obs = null;
        if (iterationFilter != null) {
            DocIdSet allowedDocs = iterationFilter.getDocIdSet(reader);
            if (allowedDocs == null || allowedDocs == DocIdSet.EMPTY_DOCIDSET) return;
            
            if (allowedDocs instanceof OpenBitSet) {
                obs = (OpenBitSet) allowedDocs;
            } else {
                DocIdSetIterator iterator = allowedDocs.iterator();
                obs = new OpenBitSetDISI(iterator, reader.maxDoc());
            }
        }
        
        TermEnum te = null;
        TermDocs td = null;
        int matchDocCounter = 0;
        int callbackCounter = 0;
        try {
            te = reader.terms(new Term(luceneField, ""));
            if (! (te.term() != null && te.term().field() == luceneField)) { // Interned string comparison OK
                return;
            }
            
            td = reader.termDocs(new Term(luceneField, ""));
            do {
                td.seek(te);
                while (td.next()) {
                    int docID = td.doc();
                    if (obs != null && !obs.fastGet(docID)) continue;
                    
                    if (matchDocCounter++ < cursor) continue;

                    Document doc = reader.document(docID, luceneSelector);
                    PropertySet ps = this.documentMapper.getPropertySet(doc);
                    boolean continueIteration = callback.matching(ps);

                    if (++callbackCounter == limit || !continueIteration) return;
                }
            } while (te.next() && te.term().field() == luceneField); // Interned string comparison OK
        } finally {
            if (te != null) {
                te.close();
            }
            if (td != null) {
                td.close();
            }
        }
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
    public void setQueryBuilder(LuceneQueryBuilder queryBuilder) {
        this.queryBuilder = queryBuilder;
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

    /**
     * Set maximum acceptable dirty age when acquiring index searcher for
     * unauthenticated queries. Value is in seconds.
     *
     * To get any effect, it should be equal or higher than value set in
     * {@link org.vortikal.repository.index.LuceneIndexManager#setAgingReadOnlyReaderThreshold(int)}
     */
    public void setUnauthenticatedQueryMaxDirtyAge(int unauthenticatedQueryMaxDirtyAge) {
        this.unauthenticatedQueryMaxDirtyAge = unauthenticatedQueryMaxDirtyAge;
    }

}

/* Copyright (c) 2005, University of Oslo, Norway
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
package org.vortikal.repositoryimpl.index.lucene;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repository.query.AbstractSearcher;
import org.vortikal.repository.query.And;
import org.vortikal.repository.query.Condition;
import org.vortikal.repository.query.Not;
import org.vortikal.repository.query.Or;
import org.vortikal.repository.query.ParseException;
import org.vortikal.repository.query.ParsedQueryCondition;
import org.vortikal.repository.query.Query;
import org.vortikal.repository.query.QueryException;
import org.vortikal.repository.query.Searcher;
import org.vortikal.repository.query.Sort;
import org.vortikal.repository.query.SortField;
import org.vortikal.repository.query.TermQueryCondition;
import org.vortikal.repository.query.UnsupportedConditionException;
import org.vortikal.repositoryimpl.index.IndexConstants;
import org.vortikal.repositoryimpl.index.ModifiableResults;
import org.vortikal.repositoryimpl.index.Results;

/**
 * New searcher implementation based on searcher interface from vortikal sandbox.
 * TODO: Support more conditions, but make sure we keep most of the flexibility
 *       of Lucene's own query classes.
 *       
 * TODO: Integrate queries which can utilize parent resource ids for greatly improving efficiency
 *       when listing contents based on URI (URIQuery). Such queries can be used for directory-like
 *       listing of resources.
 * 
 * @author oyviste
 *
 */
public class SearcherImpl extends AbstractSearcher 
    implements InitializingBean, Searcher {

    private static Log logger = LogFactory.getLog(SearcherImpl.class);
    
    private LuceneIndex index;
    private String defaultField = IndexConstants.URI_FIELD;
    
    public void afterPropertiesSet() {
        if (index == null) {
            throw new BeanInitializationException("Required property 'index' not set.");
        } 
    }
    
    public Results execute(String token, Query query) 
        throws QueryException {
        return execute(token, query, Integer.MAX_VALUE, 0);
    }
    
    public Results execute(String token, Query query, int maxResults) 
        throws QueryException {
        return execute(token, query, maxResults, 0);
    }
    
    public Results execute(String token, Query query, int maxResults, int cursor) 
        throws QueryException {
        IndexReader reader = null;
        try {
            reader = index.getReadOnlyIndexReader();
            IndexSearcher searcher = new IndexSearcher(reader);

            Sort sort = query.getSort();
            org.apache.lucene.search.Query luceneQuery = prepareLuceneQuery(
                    query.getCondition(), reader);

            Hits hits = null;
            if (sort != null) {
                org.apache.lucene.search.Sort luceneSort = null;

                SortField[] sortFields = sort.getFields();

                if (sortFields != null && sortFields.length > 0) {

                    org.apache.lucene.search.SortField[] luceneSortFields = new org.apache.lucene.search.SortField[sortFields.length];

                    for (int i = 0; i < sortFields.length; i++) {
                        luceneSortFields[i] = new org.apache.lucene.search.SortField(
                                sortFields[i].getField(), sortFields[i].isInvert());
                    }
                    luceneSort = new org.apache.lucene.search.Sort(luceneSortFields);
                }
                hits = searcher.search(luceneQuery, null, luceneSort);
            } else {
                hits = searcher.search(luceneQuery);
            }
            
            ModifiableResults results = new CachedLuceneResults(hits, index.getIndexBeanClass(), maxResults, cursor);

            int rawSize = results.getSize();

            super.applySecurityFilter(results, token);
            
            if (logger.isDebugEnabled()) {
                logger.debug("Searched for query: '" + query + "': raw size: "
                        + rawSize + ", after processing: " + results.getSize());
            }

            return results;
        } catch (IOException io) {
            throw new QueryException("IOException while performing query: " + io.getMessage());
        } catch (BooleanQuery.TooManyClauses tmc) {
            throw new QueryException("Overflow in wildcard or boolean " + 
                          "query (too many clauses), please rephrase: " + tmc.getMessage());
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException io) {
                    logger.warn("IOException while closing index reader: " + io.getMessage());
                }
            }
        }
    }

    /**
     * Construct an {@link org.apache.lucene.search.Query} object tree from a
     * {@link org.vortikal.repository.query.Condition}-tree.
     * @param condition
     * @param reader
     * @return An <code>org.apache.lucene.search.Query</code> object tree which
     *         can be passed directly to a Lucene searcher.
     * @throws IOException
     * @throws UnsupportedConditionException If an unsupported <code>Condition</code> type
     *         is encountered in the tree.
     */
    private org.apache.lucene.search.Query prepareLuceneQuery(Condition condition, 
                                                              IndexReader reader) 
        throws IOException, UnsupportedConditionException {
        if (condition == null) {
            throw new IllegalArgumentException("Condition is null.");
        }
        
        if (condition instanceof ParsedQueryCondition) {
            ParsedQueryCondition pqc = (ParsedQueryCondition)condition;
            String expression = pqc.getExpression();
            if (expression == null) {
                throw new IllegalArgumentException("Query expression cannot be null.");
            }
            
            try {
                if (pqc.isMultiField()) {
                    String[] fields = (String[]) reader.getFieldNames(true).toArray(new String[]{});
                    return MultiFieldQueryParser.parse(expression, fields, 
                            index.getAnalyzer());
                } else {
                    return QueryParser.parse(expression, this.defaultField, 
                            index.getAnalyzer()); 
                }
            } catch (org.apache.lucene.queryParser.ParseException pe) {
                throw new ParseException("Query parse error: " + pe.getMessage());
            }
        } else if (condition instanceof TermQueryCondition) {
            TermQueryCondition tc = ((TermQueryCondition)condition);
            String field = tc.getField();
            if (field == null) {
                field = this.defaultField;
            }
            String text = tc.getText();
            if (text == null) {
                throw new IllegalArgumentException("Term text cannot be null.");
            }
            
            return new TermQuery(new Term(field, text));
        } else if (condition instanceof Not) {
            BooleanQuery bq = new BooleanQuery();
            bq.add(prepareLuceneQuery(((Not)condition).getCondition(), reader), false, true);
            return bq;
        } else if (condition instanceof Or) {
            // Process left and right conditions
            org.apache.lucene.search.Query left = 
                  prepareLuceneQuery(((Or)condition).getLeftCondition(), reader);
            org.apache.lucene.search.Query right =
                  prepareLuceneQuery(((Or)condition).getRightCondition(), reader);

            // 'OR' the left and right conditions
            BooleanQuery bq = new BooleanQuery();
            bq.add(left, false, false);
            bq.add(right, false, false);
            return bq;
        } else if (condition instanceof And) {
            // 'AND' left and right conditions
            org.apache.lucene.search.Query left = 
                  prepareLuceneQuery(((And)condition).getLeftCondition(), reader);
            org.apache.lucene.search.Query right =
                  prepareLuceneQuery(((And)condition).getRightCondition(), reader);

            BooleanQuery bq = new BooleanQuery();
            bq.add(left, true, false);
            bq.add(right, true, false);
            return bq;
        }
        
        throw new UnsupportedConditionException("Condition not supported.", condition);
    }
    
    public void setDefaultField(String defaultField) {
        this.defaultField = defaultField;
    }
    
    public void setIndex(LuceneIndex index) {
        this.index = index;
    }
}

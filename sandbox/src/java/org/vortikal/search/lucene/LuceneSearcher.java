/* Copyright (c) 2004, University of Oslo, Norway
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
package org.vortikal.search.lucene;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.RangeQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repository.Repository;
import org.vortikal.search.And;
import org.vortikal.search.Condition;
import org.vortikal.search.Contains;
import org.vortikal.search.Not;
import org.vortikal.search.Or;
import org.vortikal.search.PropertyComparator;
import org.vortikal.search.Result;
import org.vortikal.search.ResultEntry;
import org.vortikal.search.SearchException;
import org.vortikal.search.Searcher;
import org.vortikal.search.UnsupportedConditionException;


public class LuceneSearcher implements Searcher, InitializingBean {

    private int maxResults = 50;
    private String luceneIndex = null;
    private File index = null;    
    private Repository repository = null;    
    private Log logger = LogFactory.getLog(this.getClass());
    

    public void setLuceneIndex(String luceneIndex) {
        this.luceneIndex = luceneIndex;
    }


    public void setRepository(Repository repository) {
        this.repository = repository;
    }


    public void afterPropertiesSet() {
        if (luceneIndex == null) {
            throw new BeanInitializationException(
                "No property `luceneIndex' set, can't continue.");
        }

        index = new File(luceneIndex.trim());
        if (!index.exists()) {
            try {
                index.mkdir();
            } catch (Exception e) {
                BeanInitializationException be = new BeanInitializationException(
                    "Creating lucene index directory: " +
                    index.getAbsolutePath() + " failed: " +
                    e.getMessage());
                be.fillInStackTrace();
                throw be;
            }
        }

        if (!index.isDirectory()) {
            throw new BeanInitializationException("Property `luceneIndex' does not " +
                                                  "point to a directory");
        }

        if (repository == null) {
            throw new BeanInitializationException("Property `repository' not set ");
        }
    }

    
    

    public Result execute(String token, org.vortikal.search.Query query,
                          int maxResults) throws SearchException {
        return execute(token, query, maxResults, 0);
    }


    public Result execute(String token,
                          org.vortikal.search.Query query,
                          int maxResults, int cursor) throws SearchException {
        if (query == null) {
            throw new IllegalArgumentException("No query given: null");
        }

        if (maxResults <= 0) {
            throw new IllegalArgumentException(
                "Invalid maxResults specified: " + maxResults);
        }

        IndexSearcher searcher = null;
        try {

            long start = System.currentTimeMillis();

            Condition rootCondition = query.getConditions();
            org.apache.lucene.search.Query luceneQuery =
                prepareLuceneQuery(rootCondition);


            searcher = new IndexSearcher(this.luceneIndex);
            Hits hits = searcher.search(luceneQuery);

            ResultEntry[] results = prepareResults(token, hits, cursor, maxResults);
            if (logger.isDebugEnabled()) {
                logger.debug("Execute: " + query);
                logger.debug("Translated to lucene query: " + luceneQuery);
                logger.debug("Total results: " + hits.length());
                logger.debug("Actual returned results: " + results.length);
                logger.debug("Search took " + (System.currentTimeMillis() - start) + " ms");
            }

            return new LuceneResult(results, hits.length());

        } catch (IOException e) {
            throw new SearchException("I/O error", e);

        } finally {
            try {
                if (searcher != null) {
                    searcher.close();
                }
            } catch (IOException e) {
                logger.warn("Unable to close lucene searcher", e);
            }
        }
    }
    


    public void setMaxResults(int maxResults) {
        this.maxResults = maxResults;
    }
    


    public int getMaxResults() throws SearchException {
        return this.maxResults;
    }
    


    private ResultEntry[] prepareResults(String token, Hits hits,
                                         int start, int maxResults) throws IOException {

        if (hits.length() < start) {
            throw new IllegalArgumentException(
                "Invalid start index: " + start + " (number of hits was: " +
                hits.length() + ")");
        }

        List results = new ArrayList();
        for (int i = start; i < hits.length() && i < (start + maxResults); i++) {
	    Document doc = hits.doc(i);
	    String uri = doc.get("uri");
            String summary = doc.get("summary");
            LuceneResultEntry result = new LuceneResultEntry(uri, summary);
            try {
                repository.retrieve(token, uri, true);
                results.add(result);
            } catch (Throwable t) { }
        }

        return (ResultEntry[]) results.toArray(new ResultEntry[results.size()]);
    }

    

    private org.apache.lucene.search.Query prepareLuceneQuery(Condition condition) {

        if (condition == null) {
            throw new IllegalArgumentException("Condition is null");
        }

        if (condition instanceof And) {

            BooleanQuery luceneQuery = new BooleanQuery();
            luceneQuery.add(prepareLuceneQuery(((And) condition).getLeftCondition()), BooleanClause.Occur.MUST);
            luceneQuery.add(prepareLuceneQuery(((And) condition).getRightCondition()), BooleanClause.Occur.MUST);
            return luceneQuery;

        } else if (condition instanceof Or) {

            BooleanQuery luceneQuery = new BooleanQuery();
            luceneQuery.add(prepareLuceneQuery(((Or) condition).getLeftCondition()), BooleanClause.Occur.SHOULD);
            luceneQuery.add(prepareLuceneQuery(((Or) condition).getRightCondition()), BooleanClause.Occur.SHOULD);
            return luceneQuery;

        } else if (condition instanceof Not) {
            // FIXME: currently not working
            throw new UnsupportedConditionException(
                "`Not' queries are currently not working for some strange reason. " +
                "Please check back later.", condition);

//             BooleanQuery luceneQuery = new BooleanQuery();
//             luceneQuery.add(prepareLuceneQuery(((Not) condition).getCondition()), false, true);
//             return luceneQuery;

        } else if (condition instanceof Contains) {

            String term = ((Contains) condition).getTerm();
            if (term == null) {
                throw new IllegalArgumentException(
                    "Null value specified in search term");
            }
            if (!term.endsWith("*")) term += "*";
            //return new TermQuery(new Term("contents", term));
            //return new FuzzyQuery(new Term("contents", term));
            return new WildcardQuery(new Term("contents", term));

        } else if (condition instanceof PropertyComparator) {

            String prop = ((PropertyComparator) condition).getName();
            if (prop == null || prop.trim().equals("")) {
                
                throw new IllegalArgumentException(
                    "No property name specified in property comparator");
            }
            String val = prepareLucenePropertyValue(
                ((PropertyComparator) condition).getValue());

            int operator = ((PropertyComparator) condition).getOperator();

            switch (operator) {
                case PropertyComparator.EQ:
                {
                    
                    Term term = new Term(prop, val);
                    return new TermQuery(term);
                }
                
                case PropertyComparator.LIKE:
                {
                    Term term = new Term(prop, val + "*");
                    return new WildcardQuery(term);
                }
                
                case PropertyComparator.LT:
                case PropertyComparator.LTE:
                {
                    Term lower =
                        // FIXME: define waterproof absolute lower bound:
                        new Term(prop, "000000000000");
                    Term upper = new Term(prop, val);
                    boolean inclusive = (operator == PropertyComparator.LTE);
                    return new RangeQuery(lower, upper, inclusive);
                    
                }
                
                case PropertyComparator.GT:
                case PropertyComparator.GTE: 
                {
                    
                    Term lower = new Term(prop, val);
                    Term upper =
                        // FIXME: define waterproof absolute upper bound:
                        new Term(prop, "ZZZZZZZZZZZZZZ");
                    boolean inclusive = (operator == PropertyComparator.GTE);
                    return new RangeQuery(lower, upper, inclusive);
                }
                
                default:
                    throw new UnsupportedConditionException(
                        "Operator " + operator + " not supported", condition);
            }

            
        } else {
            throw new UnsupportedConditionException(
                "Unsupported condition", condition);
        }
    }
    



    private String prepareLucenePropertyValue(Object val) {
        if (val == null) {
            throw new IllegalArgumentException(
                "Null reference given for value object");
        }

        if (val instanceof String) {
            return (String) val;
        }
        if (val instanceof java.util.Date) {
//            return org.apache.lucene.document.DateField.dateToString(
//                (java.util.Date) val);
            
            return DateTools.timeToString(((java.util.Date)val).getTime(), 
                    DateTools.Resolution.SECOND);
        }
        
        return val.toString();
    }
    
}

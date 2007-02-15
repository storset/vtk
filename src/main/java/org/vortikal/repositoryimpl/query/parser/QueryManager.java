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
package org.vortikal.repositoryimpl.query.parser;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;

import org.vortikal.repositoryimpl.query.query.PropertySelect;
import org.vortikal.repositoryimpl.query.query.Query;
import org.vortikal.repositoryimpl.query.query.Sorting;


public class QueryManager implements InitializingBean {

    private Log logger = LogFactory.getLog(this.getClass());

    private QueryParserFactory parserFactory;
    private Searcher searcher;
    private QueryStringProcessor queryStringProcessor;
    

    public void setParserFactory(QueryParserFactory parserFactory) {
        this.parserFactory = parserFactory;
    }

    public void setSearcher(Searcher searcher) {
        this.searcher = searcher;
    }
    
    public void setQueryStringProcessor(QueryStringProcessor queryStringProcessor)  {
        this.queryStringProcessor = queryStringProcessor;
    }
    
    public void afterPropertiesSet() throws Exception {
        if (this.parserFactory == null) {
            throw new BeanInitializationException("JavaBean property 'parserFactory' not set");
        }
        if (this.searcher == null) {
            throw new BeanInitializationException("JavaBean property 'searcher' not set");
        }
        if (this.queryStringProcessor == null) {
            throw new BeanInitializationException(
                "JavaBean property 'queryStringProcessor' not set");
        }

    }

    public ResultSet execute(String token, String queryString) throws QueryException {
        Parser parser = this.parserFactory.getParser();
        Query q = parser.parse(queryString);
        return execute(token, q); 
    }
    
    public ResultSet execute(String token, String queryString,
                             Sorting sorting, int maxResults, PropertySelect select)
        throws QueryException {
        queryString = this.queryStringProcessor.processQueryString(queryString);
        Parser parser = this.parserFactory.getParser();
        Query q = parser.parse(queryString);
        return execute(token, q, sorting, maxResults, select); 
    }
    
    public ResultSet execute(String token, Query query) throws QueryException {
        validateQuery(query);
        long start = System.currentTimeMillis();
        ResultSet result = this.searcher.execute(token, query);
        if (this.logger.isDebugEnabled()) {
            long now = System.currentTimeMillis();
            this.logger.debug("Query for '" + query.dump(" ") + "' (" + result.getSize()
                         + " hits) took " + (now - start) + " ms");
        }
        return result;
    }
    
    public ResultSet execute(String token, Query query, Sorting sorting, int maxResults)
        throws QueryException {
        validateQuery(query);
        long start = System.currentTimeMillis();
        ResultSet result = this.searcher.execute(token, query, sorting, maxResults);
        if (this.logger.isDebugEnabled()) {
            long now = System.currentTimeMillis();
            this.logger.debug("Query for '" + query.dump(" ") + "' (" + result.getSize()
                         + " hits) took " + (now - start) + " ms");
        }
        return result;
    }
    
    public ResultSet execute(String token, Query query, Sorting sorting,
                             int maxResults, PropertySelect select)
        throws QueryException {
        validateQuery(query);
        long start = System.currentTimeMillis();
        ResultSet result = this.searcher.execute(token, query, sorting, maxResults, select);
        if (this.logger.isDebugEnabled()) {
            long now = System.currentTimeMillis();
            this.logger.debug("Query for '" + query.dump(" ") + "' (" + result.getSize()
                         + " hits) took " + (now - start) + " ms");
        }
        return result;
    }

    private void validateQuery(Query query) throws QueryException{
        
    }
    
}

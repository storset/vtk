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

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repositoryimpl.PropertyManager;
import org.vortikal.repositoryimpl.query.query.Query;
import org.vortikal.repositoryimpl.query.query.Sorting;

public class QueryManager implements InitializingBean {

    private Parser parser;
    private Searcher searcher;
    private PropertyManager propertyManager;
    private QueryStringProcessor queryStringProcessor;
    

    public void setParser(Parser parser) {
        this.parser = parser;
    }

    public void setSearcher(Searcher searcher) {
        this.searcher = searcher;
    }
    
    public void setPropertyManager(PropertyManager propertyManager) {
        this.propertyManager = propertyManager;
    }

    public void setQueryStringProcessor(QueryStringProcessor queryStringProcessor)  {
        this.queryStringProcessor = queryStringProcessor;
    }
    
    public void afterPropertiesSet() throws Exception {
        if (this.parser == null) {
            throw new BeanInitializationException("JavaBean property 'parser' not set");
        }
        if (this.searcher == null) {
            throw new BeanInitializationException("JavaBean property 'searcher' not set");
        }
        if (this.propertyManager == null) {
            throw new BeanInitializationException(
                "JavaBean property 'propertyManager' not set");
        }
        if (this.queryStringProcessor == null) {
            throw new BeanInitializationException(
                "JavaBean property 'queryStringProcessor' not set");
        }

    }

    public ResultSet execute(String token, String queryString) throws QueryException {
        Query q = parser.parse(queryString);
        return execute(token, q); 
    }
    
    public ResultSet execute(String token, String queryString,
                             Sorting sorting, int maxResults)
        throws QueryException {
        queryString = this.queryStringProcessor.processQueryString(queryString);
        Query q = parser.parse(queryString);
        return execute(token, q, sorting, maxResults); 
    }
    
    public ResultSet execute(String token, Query query) throws QueryException {
        validateQuery(query);
        return searcher.execute(token, query);
    }
    
    public ResultSet execute(String token, Query query, Sorting sorting, int maxResults)
        throws QueryException {
        validateQuery(query);
        return searcher.execute(token, query, sorting, maxResults);
    }
    
    private void validateQuery(Query query) throws QueryException{
        
    }



    
}

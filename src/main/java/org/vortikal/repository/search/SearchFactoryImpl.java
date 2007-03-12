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
package org.vortikal.repository.search;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repository.search.query.Query;
import org.vortikal.repository.search.query.QueryParserFactory;
import org.vortikal.repository.search.query.QueryStringPreProcessor;


/**
 * XXX: Missing lots of validation of search objects...
 */
public class SearchFactoryImpl implements InitializingBean, SearchFactory {

    private Log logger = LogFactory.getLog(this.getClass());

    private QueryParserFactory parserFactory;
    private QueryStringPreProcessor queryStringPreProcessor;
    

    public void setParserFactory(QueryParserFactory parserFactory) {
        this.parserFactory = parserFactory;
    }

    public void setQueryStringPreProcessor(QueryStringPreProcessor queryStringPreProcessor)  {
        this.queryStringPreProcessor = queryStringPreProcessor;
    }
    
    public void afterPropertiesSet() throws Exception {
        if (this.parserFactory == null) {
            throw new BeanInitializationException("JavaBean property 'parserFactory' not set");
        }
        if (this.queryStringPreProcessor == null) {
            throw new BeanInitializationException(
                "JavaBean property 'queryStringPreProcessorImpl' not set");
        }

    }

    public Search createSearch(String queryString) throws QueryException {
        Search search = new SearchImpl();
        queryString = this.queryStringPreProcessor.processQueryString(queryString);
        Query query = this.parserFactory.getParser().parse(queryString);
        search.setQuery(query);
        
        return search;
    }

    public Search createSearch(Query query) throws QueryException {
        Search search = new SearchImpl();
        search.setQuery(query);

        return search;
    }
    
    private class SearchImpl implements Search {

        public final static int MAX_LIMIT = 50000; 
        
        private PropertySelect propertySelect = new WildcardPropertySelect();
        private Query query;
        private Sorting sorting;
        private int limit = 40000;
        private int cursor = 0;
        
        public SearchImpl() {
            SortingImpl defaultSorting = new SortingImpl();
            defaultSorting.addSortField(new SimpleSortField("uri"));
            this.sorting = defaultSorting;
        }

        public int getCursor() {
            return cursor;
        }
        
        public void setCursor(int cursor) {
            this.cursor = cursor;
        }
        
        public int getLimit() {
            return limit;
        }
        
        public void setLimit(int limit) {
            if (limit > MAX_LIMIT)
                this.limit = MAX_LIMIT;
            else
                this.limit = limit;
        }
        
        public PropertySelect getPropertySelect() {
            return propertySelect;
        }
        
        public void setPropertySelect(PropertySelect propertySelect) {
            this.propertySelect = propertySelect;
        }
        
        public Query getQuery() {
            return query;
        }
        
        public void setQuery(Query query) {
            this.query = query;
        }
        
        public Sorting getSorting() {
            return sorting;
        }
        
        public void setSorting(Sorting sorting) {
            this.sorting = sorting;
        }
        
    }

}

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
package org.vortikal.repository.search.query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repository.search.QueryException;


/**
 * XXX: Missing lots of validation of search objects...
 */
public class PreprocessingQueryParser implements Parser, InitializingBean {

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

    public Query parse(String query) {
        query = this.queryStringPreProcessor.processQueryString(query);
        return this.parserFactory.getParser().parse(query);
    }

}

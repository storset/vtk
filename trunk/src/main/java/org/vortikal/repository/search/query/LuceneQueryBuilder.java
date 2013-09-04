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

import org.apache.lucene.index.IndexReader;
import org.vortikal.repository.search.Search;
import org.vortikal.repository.search.Sorting;


/**
 * Build instances of {@link org.apache.lucene.search.Query} and
 * {@link org.apache.lucene.search.Filter} from our own query types.
 */
public interface LuceneQueryBuilder {

    /**
     * Build a Lucene {@link org.apache.lucene.search.Query} 
     * for a given <code>{@link org.vortikal.repository.search.query.Query}</code>.
     * 
     * @param query
     * @param reader
     * @return
     * @throws QueryBuilderException
     */
    public org.apache.lucene.search.Query buildQuery(Query query, IndexReader reader) 
        throws QueryBuilderException;
    
    /**
     * Build a {@link org.apache.lucene.search.Filter} that should be applied for the given
     * search and query.
     * 
     * May return <code>null</code> if no filter should be applied for the given search.  
     */
    public org.apache.lucene.search.Filter buildSearchFilter(String token, Search search, IndexReader reader)
        throws QueryBuilderException;
    
    /**
     * Build iteration filter based on search query, token and options.
     * @param token
     * @param search
     * @param reader
     * @return 
     */
    public org.apache.lucene.search.Filter buildIterationFilter(String token, Search search, IndexReader reader);


    /**
     * Build a {@link org.apache.lucene.search.Sort} from given 
     * {@link org.vortikal.repository.search.Sorting}.
     * 
     * @param sort
     * @return
     */
    public org.apache.lucene.search.Sort buildSort(Sorting sort);

}

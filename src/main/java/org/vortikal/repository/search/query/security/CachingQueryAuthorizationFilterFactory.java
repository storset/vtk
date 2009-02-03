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
package org.vortikal.repository.search.query.security;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.CachingWrapperFilter;
import org.apache.lucene.search.Filter;

/**
 * A filter-factory which does caching of filters.
 * 
 * Currently, caching is only done for ACL read for all filter. The cached
 * filter bits are keyed on IndexReader instance, so old bitsets are automatically
 * discarded when a new index reader instance is used. This is
 * done in {@link CachingWrapperFilter}. A <code>Map</code> with weak keys
 * is used internally, so it does not leak old <code>IndexReader</code> references.
 * 
 */
public class CachingQueryAuthorizationFilterFactory extends
        SimpleQueryAuthorizationFilterFactory {
    
    private Filter cachingAclReadForAllFilter = new CachingWrapperFilter(
                   SimpleQueryAuthorizationFilterFactory.ACL_READ_FOR_ALL_FILTER);
    
    @Override
    public Filter authorizationQueryFilter(String token, IndexReader reader) {
        if (token == null) {
            return this.cachingAclReadForAllFilter;
        } else {
            return super.authorizationQueryFilter(token, reader);
        }
    }

}

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

import org.vortikal.repositoryimpl.query.builders.NameRangeQueryBuilder;
import org.vortikal.repositoryimpl.query.builders.NameTermQueryBuilder;
import org.vortikal.repositoryimpl.query.builders.PropertyRangeQueryBuilder;
import org.vortikal.repositoryimpl.query.builders.PropertyTermQueryBuilder;
import org.vortikal.repositoryimpl.query.builders.QueryTreeBuilder;
import org.vortikal.repositoryimpl.query.query.AbstractMultipleQuery;
import org.vortikal.repositoryimpl.query.query.NameRangeQuery;
import org.vortikal.repositoryimpl.query.query.NameTermQuery;
import org.vortikal.repositoryimpl.query.query.PropertyRangeQuery;
import org.vortikal.repositoryimpl.query.query.PropertyTermQuery;
import org.vortikal.repositoryimpl.query.query.Query;

public final class QueryBuilderFactory {

    public static QueryBuilder getBuilder(Query query) {
        
       if (query instanceof AbstractMultipleQuery) {
           return new QueryTreeBuilder((AbstractMultipleQuery)query);
       }
        
       if (query instanceof NameTermQuery) {
           return new NameTermQueryBuilder((NameTermQuery)query);
       }

       if (query instanceof NameRangeQuery) {
           return new NameRangeQueryBuilder((NameRangeQuery)query);
       }
       
       if (query instanceof PropertyTermQuery) {
           return new PropertyTermQueryBuilder((PropertyTermQuery)query);
       }
       
       if (query instanceof PropertyRangeQuery) {
           return new PropertyRangeQueryBuilder((PropertyRangeQuery)query);
       }

       
       throw new QueryBuilderException("Unsupported query type: " + query);
    }

}

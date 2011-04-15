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
package org.vortikal.repository.search.query.builders;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.WildcardTermEnum;
import org.vortikal.repository.index.mapping.FieldNameMapping;
import org.vortikal.repository.search.query.NameWildcardQuery;
import org.vortikal.repository.search.query.QueryBuilder;
import org.vortikal.repository.search.query.QueryBuilderException;
import org.vortikal.repository.search.query.filter.InversionFilter;
import org.vortikal.repository.search.query.filter.WildcardTermFilter;

/**
 * 
 * @author oyviste
 *
 */
public class NameWildcardQueryBuilder implements QueryBuilder {

    private NameWildcardQuery nwq;
    private Filter deletedDocsFilter;

    public NameWildcardQueryBuilder(NameWildcardQuery nwq) {
        this.nwq = nwq;
    }

    public NameWildcardQueryBuilder(NameWildcardQuery nwq, Filter deletedDocs) {
        this.nwq = nwq;
        this.deletedDocsFilter = deletedDocs;
    }

    @Override
    public Query buildQuery() throws QueryBuilderException {
        
        String wildcard = this.nwq.getTerm();
        
        if (wildcard.indexOf(WildcardTermEnum.WILDCARD_CHAR) == -1
                && wildcard.indexOf(WildcardTermEnum.WILDCARD_STRING) == -1) {
            throw new QueryBuilderException("The search term '" 
                    + wildcard + "' does not have any wildcard characters (?,*) !");
        }
        
        Term wTerm = new Term(FieldNameMapping.NAME_FIELD_NAME, wildcard);
        
        Filter filter = new WildcardTermFilter(wTerm);

        if (this.nwq.isInverted()) {
            filter = new InversionFilter(filter, this.deletedDocsFilter);
        }
        
        return new ConstantScoreQuery(filter);
        
    }

}

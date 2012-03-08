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
import org.vortikal.repository.index.mapping.FieldNameMapping;
import org.vortikal.repository.resourcetype.PropertyType.Type;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.search.query.PropertyWildcardQuery;
import org.vortikal.repository.search.query.QueryBuilder;
import org.vortikal.repository.search.query.QueryBuilderException;
import org.vortikal.repository.search.query.TermOperator;
import org.vortikal.repository.search.query.filter.InversionFilter;
import org.vortikal.repository.search.query.filter.WildcardTermFilter;

/**
 * 
 * @author oyviste
 *
 */
public class PropertyWildcardQueryBuilder implements QueryBuilder {

    private PropertyWildcardQuery query;
    private Filter deletedDocsFilter;
    
    public PropertyWildcardQueryBuilder(PropertyWildcardQuery query) {
        this.query = query;
    }

    public PropertyWildcardQueryBuilder(PropertyWildcardQuery query, Filter deletedDocs) {
        this(query);
        this.deletedDocsFilter = deletedDocs;
    }

    @Override
    public Query buildQuery() throws QueryBuilderException {
        
        PropertyTypeDefinition def = this.query.getPropertyDefinition();
        String wildcard = this.query.getTerm();

        if (! (def.getType() == Type.PRINCIPAL ||
                def.getType() == Type.STRING ||
                def.getType() == Type.HTML ||
                def.getType() == Type.JSON)) {
             throw new QueryBuilderException("Wildcard queries are only supported for "
                 + "property types PRINCIPAL, STRING, HTML and JSON w/attribute specifier. "
                 + "Use range queries for dates and numbers.");
         }

        TermOperator op = query.getOperator();

        boolean ignorecase = (op == TermOperator.EQ_IGNORECASE || op == TermOperator.NE_IGNORECASE);
        boolean invert = (op == TermOperator.NE || op == TermOperator.NE_IGNORECASE);
        
        String fieldName = FieldNameMapping.getSearchFieldName(def, ignorecase);
        if (def.getType() == Type.JSON && query.getComplexValueAttributeSpecifier() != null) {
            fieldName = FieldNameMapping.getJSONSearchFieldName(def,
                    query.getComplexValueAttributeSpecifier(), ignorecase);
        }

        Term wTerm = new Term(fieldName, (ignorecase ? wildcard.toLowerCase() : wildcard));

        Filter filter = new WildcardTermFilter(wTerm);
        if (invert) {
            filter = new InversionFilter(filter, this.deletedDocsFilter);
        }
        
        return new ConstantScoreQuery(filter);
    }

}

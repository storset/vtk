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

import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermRangeFilter;
import org.vortikal.repository.index.mapping.FieldNames;
import org.vortikal.repository.index.mapping.FieldValueMapper;
import org.vortikal.repository.resourcetype.PropertyType.Type;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.search.query.PropertyRangeQuery;
import org.vortikal.repository.search.query.QueryBuilder;
import org.vortikal.repository.search.query.QueryBuilderException;

/**
 * 
 * @author oyviste
 */
public class PropertyRangeQueryBuilder implements QueryBuilder {

    private PropertyRangeQuery prq;
    private FieldValueMapper fieldValueMapper;
    
    public PropertyRangeQueryBuilder(PropertyRangeQuery prq, FieldValueMapper mapper) {
        this.prq = prq;
        this.fieldValueMapper = mapper;
    }

    @Override
    public Query buildQuery() throws QueryBuilderException {
        
        String from = this.prq.getFromTerm();
        String to = this.prq.getToTerm();
        PropertyTypeDefinition def = this.prq.getPropertyDefinition();

        String fromEncoded, toEncoded, fieldName, cva = prq.getComplexValueAttributeSpecifier();
        if (cva == null) {
            fieldName = FieldNames.getSearchFieldName(def, false);
            fromEncoded = this.fieldValueMapper.encodeIndexFieldValue(from, def.getType(), false);
            toEncoded = this.fieldValueMapper.encodeIndexFieldValue(to, def.getType(), false);
        } else {
            Type dataType = FieldValueMapper.getJsonFieldDataType(prq.getPropertyDefinition(), cva);
            fieldName = FieldNames.getJsonSearchFieldName(def, cva, false);
            fromEncoded = this.fieldValueMapper.encodeIndexFieldValue(from, dataType, false);
            toEncoded = this.fieldValueMapper.encodeIndexFieldValue(to, dataType, false);
        }

        TermRangeFilter trFilter = new TermRangeFilter(fieldName, fromEncoded,
                toEncoded, this.prq.isInclusive(), this.prq.isInclusive());

        return new ConstantScoreQuery(trFilter);

    }
}

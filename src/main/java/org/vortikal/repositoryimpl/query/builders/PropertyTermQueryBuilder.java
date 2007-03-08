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
package org.vortikal.repositoryimpl.query.builders;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.ConstantScoreRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.search.query.PropertyTermQuery;
import org.vortikal.repository.search.query.TermOperator;
import org.vortikal.repositoryimpl.index.mapping.DocumentMapper;
import org.vortikal.repositoryimpl.index.mapping.FieldValueMapper;
import org.vortikal.repositoryimpl.query.QueryBuilder;
import org.vortikal.repositoryimpl.query.QueryBuilderException;

/**
 * 
 * @author oyviste
 *
 */
public class PropertyTermQueryBuilder implements QueryBuilder {

    private PropertyTermQuery ptq;
    public PropertyTermQueryBuilder(PropertyTermQuery ptq) {
        this.ptq = ptq;
    }

    public Query buildQuery() throws QueryBuilderException {
        
        TermOperator op = this.ptq.getOperator();
        PropertyTypeDefinition propDef = this.ptq.getPropertyDefinition();
        
        String fieldName = DocumentMapper.getFieldName(propDef);

        String fieldValue = FieldValueMapper.encodeIndexFieldValue(this.ptq.getTerm(), 
                                                            propDef.getType());
        
        if (op == TermOperator.EQ) {
            return new TermQuery(new Term(fieldName, fieldValue));
        }
        
        boolean includeLower = false;
        boolean includeUpper = false;
        String upperTerm = null;
        String lowerTerm = null;
        
        if (op == TermOperator.GE) {
            lowerTerm = fieldValue;
            includeLower = true;
            includeUpper = true;
        } else if (op == TermOperator.GT) {
            lowerTerm = fieldValue;
            includeUpper = true;
        } else if (op == TermOperator.LE) {
            upperTerm = fieldValue;
            includeUpper = true;
            includeLower = true;
        } else if (op == TermOperator.LT) {
            upperTerm = fieldValue;
            includeLower = true;
        } else if (op == TermOperator.NE) {
            throw new QueryBuilderException("Term operator 'NE' not yet supported.");
        } else {
            throw new QueryBuilderException("Unknown term operator"); 
        }
        
        return new ConstantScoreRangeQuery(fieldName, lowerTerm, upperTerm, 
                                                    includeLower, includeUpper);
    }

}

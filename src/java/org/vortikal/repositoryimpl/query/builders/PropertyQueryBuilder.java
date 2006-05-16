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
import org.apache.lucene.search.CachingWrapperFilter;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.ConstantScoreRangeQuery;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.TermQuery;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repositoryimpl.query.DocumentMapper;
import org.vortikal.repositoryimpl.query.FieldMapper;
import org.vortikal.repositoryimpl.query.QueryBuilder;
import org.vortikal.repositoryimpl.query.QueryBuilderException;
import org.vortikal.repositoryimpl.query.SimplePrefixTermFilter;
import org.vortikal.repositoryimpl.query.query.AbstractPropertyQuery;
import org.vortikal.repositoryimpl.query.query.PropertyPrefixQuery;
import org.vortikal.repositoryimpl.query.query.PropertyRangeQuery;
import org.vortikal.repositoryimpl.query.query.PropertyTermQuery;
import org.vortikal.repositoryimpl.query.query.TermOperator;

/**
 * Builder for property queries.
 * 
 * @author oyviste
 *
 */
public class PropertyQueryBuilder implements QueryBuilder {

    private AbstractPropertyQuery query;
    public PropertyQueryBuilder(AbstractPropertyQuery query) {
        this.query = query;
    }
    
    public org.apache.lucene.search.Query buildQuery() throws QueryBuilderException {
        if (this.query instanceof PropertyTermQuery) {
            return buildPropertyTermQuery((PropertyTermQuery)this.query);
        }
        
        if (this.query instanceof PropertyRangeQuery) {
            return buildPropertyRangeQuery((PropertyRangeQuery)this.query);
        }
        
        if (this.query instanceof PropertyPrefixQuery) {
            return buildPropertyPrefixQuery((PropertyPrefixQuery)this.query);
        }
        
        throw new QueryBuilderException("Unsupported property query type: " 
                                       + this.query.getClass().getSimpleName());
    }
    
    private org.apache.lucene.search.Query buildPropertyTermQuery(
                                                    PropertyTermQuery ptq) 
        throws QueryBuilderException {
        
        // TODO: Use ConstantScoreRangeQuery to support
        //       other operators
        if (ptq.getOperator() != TermOperator.EQ) {
            throw new QueryBuilderException("Only the 'EQ' TermOperator is currently implemented");
        }
        
        PropertyTypeDefinition propDef = ptq.getPropertyDefinition();
        
        String fieldName = getPropertyFieldName(propDef);

        String fieldValue = FieldMapper.encodeIndexFieldValue(ptq.getTerm(), 
                                                            propDef.getType());
        
        TermQuery tq = new TermQuery(new Term(fieldName, fieldValue));
        
        return tq;
        
    }
    
    private org.apache.lucene.search.Query buildPropertyRangeQuery(
                                                      PropertyRangeQuery prq)  {
        
        String from = prq.getFromTerm();
        String to = prq.getToTerm();
        PropertyTypeDefinition def = prq.getPropertyDefinition();
        
        String fromEncoded = FieldMapper.encodeIndexFieldValue(from, def.getType());
        String toEncoded = FieldMapper.encodeIndexFieldValue(to, def.getType());
        
        String fieldName = getPropertyFieldName(def);
        
        ConstantScoreRangeQuery csrq = new ConstantScoreRangeQuery(fieldName, 
                fromEncoded, toEncoded, prq.isInclusive(), prq.isInclusive());
        
        return csrq;
        
    }
    
    private org.apache.lucene.search.Query buildPropertyPrefixQuery(
            PropertyPrefixQuery ppq) throws QueryBuilderException {
 
        PropertyTypeDefinition def = ppq.getPropertyDefinition();
        String term = ppq.getTerm();
        
        if (! (def.getType() == PropertyType.TYPE_PRINCIPAL ||
               def.getType() == PropertyType.TYPE_STRING)) {
            throw new QueryBuilderException("Prefix queries are only supported for "
                + "property types 'String' and 'Principal'. " 
                + "Use range queries for dates and numbers.");
        }
        
        Filter filter = new SimplePrefixTermFilter(
                                new Term(getPropertyFieldName(def), term));
        
        return new ConstantScoreQuery(new CachingWrapperFilter(filter));
    }
    
    private String getPropertyFieldName(PropertyTypeDefinition def) {
        String nsPrefix = def.getNamespace().getPrefix();
        return nsPrefix != null ? 
                nsPrefix + DocumentMapper.FIELD_NAMESPACEPREFIX_NAME_SEPARATOR 
                + def.getName() : def.getName();
    }

}

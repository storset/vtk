/* Copyright (c) 2006, 2007, 2009 University of Oslo, Norway
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

import java.util.List;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermsFilter;
import org.vortikal.repository.HierarchicalVocabulary;
import org.vortikal.repository.search.query.QueryBuilder;
import org.vortikal.repository.search.query.QueryBuilderException;
import org.vortikal.repository.search.query.TermOperator;
import org.vortikal.repository.search.query.filter.InversionFilter;

public class HierarchicalTermQueryBuilder<T> implements QueryBuilder {

    private HierarchicalVocabulary<T> hierarchicalVocabulary;
    private final TermOperator operator;
    private final String fieldName;
    private final T term;
    private Filter deletedDocsFilter;
    
    public HierarchicalTermQueryBuilder(HierarchicalVocabulary<T> hierarchicalVocabulary,
                                        TermOperator operator, String fieldName, T term) {
        this.hierarchicalVocabulary = hierarchicalVocabulary;
        this.operator = operator;
        this.fieldName = fieldName;
        this.term = term; 
    }

    public HierarchicalTermQueryBuilder(HierarchicalVocabulary<T> hierarchicalVocabulary,
                                        TermOperator operator, String fieldName, T term,
                                        Filter deletedDocs) {
        this(hierarchicalVocabulary, operator, fieldName, term);
        this.deletedDocsFilter = deletedDocs;
    }

    @Override
    public Query buildQuery() {
        if (this.operator == TermOperator.IN) {
            return new ConstantScoreQuery(getInFilter());
        } else if (this.operator == TermOperator.NI) {
            Filter filter = new InversionFilter(getInFilter(), this.deletedDocsFilter);
            return new ConstantScoreQuery(filter);
        } else {
            throw new QueryBuilderException("Unsupported type operator: " + this.operator);
        }
    }

    private Filter getInFilter() {
        TermsFilter tf = new TermsFilter();
        tf.addTerm(new Term(this.fieldName, this.term.toString()));

        List<T> descendantNames = this.hierarchicalVocabulary.getDescendants(this.term);
        if (descendantNames != null && !descendantNames.isEmpty()) {
            for (T descendantName: descendantNames) {
                tf.addTerm(new Term(this.fieldName, descendantName.toString()));
            }
        }
        
        return tf;
    }
}

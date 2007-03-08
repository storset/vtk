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

import java.util.Iterator;
import java.util.List;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.vortikal.repository.ResourceTypeTree;
import org.vortikal.repository.query.TypeOperator;
import org.vortikal.repository.query.TypeTermQuery;
import org.vortikal.repositoryimpl.index.DocumentMapper;
import org.vortikal.repositoryimpl.query.QueryBuilder;
import org.vortikal.repositoryimpl.query.QueryBuilderException;

/**
 * 
 * @author oyviste
 *
 */
public class TypeTermQueryBuilder implements QueryBuilder {

    private TypeTermQuery ttq;
    private ResourceTypeTree resourceTypeTree;
    
    public TypeTermQueryBuilder(ResourceTypeTree resourceTypeTree, TypeTermQuery ttq) {
        this.ttq = ttq;
        this.resourceTypeTree = resourceTypeTree; 
    }

    public Query buildQuery() {
        String typeTerm = this.ttq.getTerm();
        
        if (this.ttq.getOperator() == TypeOperator.EQ) {
            return new TermQuery(new Term(DocumentMapper.RESOURCETYPE_FIELD_NAME, typeTerm));
        } else if (this.ttq.getOperator() == TypeOperator.IN) {
            
            BooleanQuery bq = new BooleanQuery(true);
            bq.add(new TermQuery(new Term(DocumentMapper.RESOURCETYPE_FIELD_NAME, typeTerm)),
                                BooleanClause.Occur.SHOULD);

            List descendantNames = this.resourceTypeTree.getResourceTypeDescendantNames(typeTerm);
            
            if (descendantNames != null) {
                for (Iterator i = descendantNames.iterator();i.hasNext();) {
                    Term t = new Term(DocumentMapper.RESOURCETYPE_FIELD_NAME, (String)i.next());
                    bq.add(new TermQuery(t),  BooleanClause.Occur.SHOULD);
                }
            }
            
            return bq;
        } else throw new QueryBuilderException("Unsupported type operator: " + this.ttq.getOperator());

    }

}

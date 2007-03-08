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
package org.vortikal.repositoryimpl.search.query;

import java.util.Iterator;

import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.search.query.PropertySortField;
import org.vortikal.repository.search.query.SimpleSortField;
import org.vortikal.repository.search.query.SortField;
import org.vortikal.repository.search.query.SortFieldDirection;
import org.vortikal.repository.search.query.Sorting;
import org.vortikal.repositoryimpl.index.mapping.DocumentMapper;

/**
 * 
 * @author oyviste
 *
 */
public class SortBuilderImpl implements SortBuilder {

    public org.apache.lucene.search.Sort buildSort(Sorting sort) 
        throws SortBuilderException {

        if (sort == null) {
            return new org.apache.lucene.search.Sort(
                new org.apache.lucene.search.SortField[0]);
        }

        org.apache.lucene.search.SortField[] luceneSortFields =
            new org.apache.lucene.search.SortField[sort.getSortFields().size()];
        
        int j=0;
        for (Iterator i = sort.getSortFields().iterator(); i.hasNext(); j++) {
            SortField f = (SortField)i.next();
            
            if (f instanceof SimpleSortField) {
                luceneSortFields[j]= buildSimpleSortField((SimpleSortField)f);
            } else if (f instanceof PropertySortField) {
                luceneSortFields[j] = buildPropertySortField((PropertySortField)f);
            } else {
                throw new SortBuilderException("Unknown sorting type");
            }
            
        }
        
        return new org.apache.lucene.search.Sort(luceneSortFields);
    }
    
    private org.apache.lucene.search.SortField buildSimpleSortField(
            SimpleSortField ssf) {
        
        org.apache.lucene.search.SortField sortField = 
            new org.apache.lucene.search.SortField(ssf.getName(), 
                    org.apache.lucene.search.SortField.STRING,
                    (ssf.getDirection() == SortFieldDirection.ASC ? false : true));
        
        return sortField;
        
    }
    
    private org.apache.lucene.search.SortField buildPropertySortField(
            PropertySortField f) {
     
        PropertyTypeDefinition def = f.getDefinition();
        SortFieldDirection d = f.getDirection();
        String fieldName = DocumentMapper.getFieldName(def);
        
        // We do our own type->string encoding, and must use Lucene's generic string sorting type.
        org.apache.lucene.search.SortField sortField =
            new org.apache.lucene.search.SortField(fieldName,
                    org.apache.lucene.search.SortField.STRING, 
                    (d == SortFieldDirection.ASC ? false : true));
                    
        return sortField;
    }

}

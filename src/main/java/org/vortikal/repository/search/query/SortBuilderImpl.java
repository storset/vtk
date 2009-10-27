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
package org.vortikal.repository.search.query;

import java.util.Iterator;

import org.vortikal.repository.PropertySet;
import org.vortikal.repository.index.mapping.FieldNameMapping;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.search.PropertySortField;
import org.vortikal.repository.search.SortField;
import org.vortikal.repository.search.SortFieldDirection;
import org.vortikal.repository.search.Sorting;
import org.vortikal.repository.search.TypedSortField;

/**
 * 
 * @author oyviste
 * 
 */
public class SortBuilderImpl implements SortBuilder {

//    private SortComparatorSource sortComparatorSource = null; 
    
//    public SortBuilderImpl() throws SortBuilderException {
//        try {
//            sortComparatorSource = new CustomSortComparatorSource();
//        } catch (IOException e) {
//            throw new SortBuilderException("Couldn't create custom sort comparator source", e);
//        } catch (ParseException e) {
//            throw new SortBuilderException("Couldn't create custom sort comparator source", e);
//        }
//    }

    public org.apache.lucene.search.Sort buildSort(Sorting sort)
            throws SortBuilderException {

        if (sort == null) {
            return new org.apache.lucene.search.Sort(
                    new org.apache.lucene.search.SortField[0]);
        }

        org.apache.lucene.search.SortField[] luceneSortFields =
            new org.apache.lucene.search.SortField[sort.getSortFields().size()];

        int j = 0;
        for (Iterator<SortField> i = sort.getSortFields().iterator(); i.hasNext(); j++) {
            SortField f = i.next();
            String fieldName = null;
            boolean reverse = f.getDirection() != SortFieldDirection.ASC;

            if (f instanceof TypedSortField) {
                TypedSortField tsf =  (TypedSortField)f;
                if (PropertySet.TYPE_IDENTIFIER.equals(tsf.getType())) {
                    fieldName = FieldNameMapping.RESOURCETYPE_FIELD_NAME;
                } else if (PropertySet.URI_IDENTIFIER.equals(tsf.getType())) {
                    fieldName = FieldNameMapping.URI_FIELD_NAME;
                } else if (PropertySet.NAME_IDENTIFIER.equals(tsf.getType())) {
                    fieldName = FieldNameMapping.NAME_FIELD_NAME;
                } else {
                    throw new SortBuilderException("Unknown typed sort field type: " + tsf.getType());
                }
                
                // Special fields, do locale-sensitive lexicographic sorting (uri, name or type) 
                luceneSortFields[j] = new org.apache.lucene.search.SortField(
                                            fieldName, f.getLocale(), reverse);
            } else if (f instanceof PropertySortField) {
                PropertySortField psf = (PropertySortField)f;
                PropertyTypeDefinition def = psf.getDefinition();

                fieldName = FieldNameMapping.getSearchFieldName(def, false);

                switch (def.getType()) {
                case JSON:
                    String cva = psf.getComplexValueAttributeSpecifier();
                    if (cva != null) {
                        fieldName = FieldNameMapping.getJSONSearchFieldName(def, cva, false);
                    }
                case DATE:
                case TIMESTAMP:
                case BOOLEAN:
                case INT:
                case LONG:
                case PRINCIPAL:
                case IMAGE_REF:

                    // These types are all encoded as lexicographically sortable strings,
                    // and there is no need to do locale-sensitive sorting on any of them.
                    luceneSortFields[j] = new org.apache.lucene.search.SortField(
                            fieldName, org.apache.lucene.search.SortField.STRING, reverse);
                    break;

                default:
                    // Sort field according to locale, typically STRING properties (slower)
                    luceneSortFields[j] = new org.apache.lucene.search.SortField(
                            fieldName, f.getLocale(), reverse);
                }
            } else {
                throw new SortBuilderException("Unknown sorting type "
                        + f.getClass().getName());
            }

            //luceneSortFields[j] = new org.apache.lucene.search.SortField(fieldName, sortComparatorSource, direction);
        }

        return new org.apache.lucene.search.Sort(luceneSortFields);
    }
}

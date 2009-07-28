/* Copyright (c) 2009, University of Oslo, Norway
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
package org.vortikal.repository.index;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.document.FieldSelectorResult;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanFilter;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.FilterClause;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.OpenBitSet;
import org.codehaus.plexus.util.FastMap;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.index.mapping.DocumentMapper;
import org.vortikal.repository.index.mapping.FieldNameMapping;
import org.vortikal.repository.index.mapping.FieldValueMapper;
import org.vortikal.repository.reporting.DataReportException;
import org.vortikal.repository.reporting.Pair;
import org.vortikal.repository.reporting.PropertyValueFrequencyQuery;
import org.vortikal.repository.reporting.UriScope;
import org.vortikal.repository.reporting.ValueFrequencyPairComparator;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.repository.search.ConfigurablePropertySelect;
import org.vortikal.repository.search.query.TermExistsFilter;
import org.vortikal.repository.search.query.security.QueryAuthorizationFilterFactory;
import org.vortikal.repository.store.DataReportDAO;

/**
 * Experimental data-report DAO using system index instead of database.
 * 
 * An advantage with this is that we get complete ACL filtering nearly for free.
 * The SqlMapDataReportDAO does not have any ACL-handling ..
 *
 * It might also be the case that this is actually faster than going
 * the database-route, even for large repositories. But that's speculation, it
 * needs to be tested .. Hopefully, we can completely replace the database-DAO
 * with this one.
 * 
 */
public class IndexDataReportDAO implements DataReportDAO {

    private LuceneIndexManager systemIndexAccessor;
    private QueryAuthorizationFilterFactory queryAuthFilterFactory;
    private DocumentMapper documentMapper;
    private FieldValueMapper fieldValueMapper;
    
    private static final Filter MATCH_NOTHING_FILTER = new MatchNothingFilter();
    
    @SuppressWarnings("unchecked")
    public List<Pair<Value, Integer>> executePropertyFrequencyValueQuery(
            String token, PropertyValueFrequencyQuery query) 
            throws DataReportException {

        IndexReader reader = null;
        try {
            reader = systemIndexAccessor.getReadOnlyIndexReader();

            PropertyTypeDefinition def = query.getPropertyTypeDefintion();
            
            if (def == null) {
                throw new IllegalArgumentException("Property type definition cannot be null");
            }

            BooleanFilter mainFilter = new BooleanFilter();
            
            Filter propertyExistsFilter = getPropertyExistsFilter(def);
            Filter aclFilter = getAclFilter(token, reader);
            Filter uriScopeFilter = getUriScopeFilter(query.getUriScope(), reader);
            
            mainFilter.add(new FilterClause(propertyExistsFilter, BooleanClause.Occur.MUST));

            if (uriScopeFilter != null) {
                mainFilter.add(new FilterClause(uriScopeFilter, BooleanClause.Occur.MUST));
            }
            
            if (aclFilter != null) {
                mainFilter.add(new FilterClause(aclFilter, BooleanClause.Occur.MUST));
            }
            
            ConfigurablePropertySelect selector = new ConfigurablePropertySelect();
            selector.addPropertyDefinition(def);
            FieldSelector fieldSelector = this.documentMapper.getDocumentFieldSelector(selector);
            
            FastMap valFreqMap = new FastMap(reader.numDocs() / 3); 
            
            DocIdSet allowedDocs = mainFilter.getDocIdSet(reader);
            DocIdSetIterator iterator = allowedDocs.iterator();
            while (iterator.next()) {
                Document doc = reader.document(iterator.doc(), fieldSelector);
                Field[] fields = doc.getFields(FieldNameMapping.getStoredFieldName(def));
                
                if (fields.length == 0) { // This check should be unnecessary now, since we're always using term
                                          // exists filter, but keep it for safety.
                    continue;
                }
                
                if (def.isMultiple()) {
                    Value[] values = this.fieldValueMapper.getValuesFromStoredBinaryFields(Arrays.asList(fields), def.getType());
                    for (Value value: values) {
                        addValue(valFreqMap, value);
                    }
                } else {
                    if (fields.length != 1) {
                        throw new DataReportException("Index error (multiple values for single valued property)");
                    }
                    addValue(valFreqMap, this.fieldValueMapper.getValueFromStoredBinaryField(fields[0], def.getType()));
                }
            }
            
            int minFreq = query.getMinValueFrequency();
            List<Pair<Value, Integer>> retval = new ArrayList<Pair<Value,Integer>>(valFreqMap.size());
            for (Object o: valFreqMap.entrySet()) {
                Map.Entry entry = (Map.Entry)o;

                Integer freq = (Integer)entry.getValue();
                Value value = (Value)entry.getKey();
                
                if (freq.intValue() >= minFreq) {
                    retval.add(new Pair<Value, Integer>(value, freq));
                }
            }
            
            // Sort
            if (query.getOrdering() != PropertyValueFrequencyQuery.Ordering.NONE) {
                Collections.sort(retval, new ValueFrequencyPairComparator(query.getOrdering()));
            }

            // Apply any limit to number of results
            if (query.getLimit() != PropertyValueFrequencyQuery.LIMIT_UNLIMITED) {
                int limit = Math.min(query.getLimit(), retval.size());
                retval = retval.subList(0, limit);
            }
            
            return retval;
        } catch (IOException io) {
            throw new DataReportException(io);
        } finally {
            if (reader != null) {
                try {
                    this.systemIndexAccessor.releaseReadOnlyIndexReader(reader);
                } catch (IOException io){ }
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    private void addValue(Map valFreqMap, Value value) {
        Integer currentFreq = (Integer)valFreqMap.get(value);
        if (currentFreq == null) {
            currentFreq = new Integer(1);
        } else {
            currentFreq = new Integer(currentFreq.intValue()+1);
        }
        valFreqMap.put(value, currentFreq);
    }

    private Filter getAclFilter(String token, IndexReader reader) {
        return this.queryAuthFilterFactory.authorizationQueryFilter(token, reader);
    }
    
    private Filter getPropertyExistsFilter(PropertyTypeDefinition def) {
        String fieldName = FieldNameMapping.getSearchFieldName(def, false);
        return new TermExistsFilter(fieldName);
    }
    
    private Filter getUriScopeFilter(UriScope scope, IndexReader reader) 
        throws IOException {
        
        if (scope != null && !scope.getUri().toString().equals("/")) {
            String uri = scope.getUri().toString();
            TermDocs tdocs = reader.termDocs(new Term(FieldNameMapping.URI_FIELD_NAME, uri));
            try {
                if (tdocs.next()) {
                    Document doc = reader.document(tdocs.doc(), new FieldSelector() {
                        private static final long serialVersionUID = 2294209998307991707L;

                        public FieldSelectorResult accept(String name) {
                            if (FieldNameMapping.STORED_ID_FIELD_NAME == name) { // Interned string comparison
                                return FieldSelectorResult.LOAD;
                            }
                            
                            return FieldSelectorResult.NO_LOAD;
                        }
                    });
                    
                    String idValue = 
                        Integer.toString(
                                this.fieldValueMapper.getIntegerFromStoredBinaryField(
                                        doc.getField(FieldNameMapping.STORED_ID_FIELD_NAME)));
                    
                    BooleanQuery bq = new BooleanQuery(true);
                    bq.add(new TermQuery(
                            new Term(
                              FieldNameMapping.ANCESTORIDS_FIELD_NAME, idValue)), BooleanClause.Occur.SHOULD);
                    bq.add(new TermQuery(
                            new Term(
                              FieldNameMapping.ID_FIELD_NAME, idValue)), BooleanClause.Occur.SHOULD);
    
                    return new QueryWrapperFilter(bq);
                } else {
                    return MATCH_NOTHING_FILTER; // Invalid URI scope results in no matches (URI not found in index).
                }
            } finally {
                tdocs.close();
            }
        }

        // Return null, which signals no uri scope filter (all)
        return null;
    }
    
    private static final class MatchNothingFilter extends Filter {
        private static final long serialVersionUID = 5225305691186860115L;

        @Override
        public DocIdSet getDocIdSet(IndexReader reader) {
            return new OpenBitSet(reader.maxDoc());
        }
        
        @Override
        public BitSet bits(IndexReader reader){
            return new BitSet(reader.maxDoc());
        }
    }
    
    @Required
    public void setSystemIndexAccessor(LuceneIndexManager systemIndexAccessor) {
        this.systemIndexAccessor = systemIndexAccessor;
    }

    @Required
    public void setQueryAuthFilterFactory(
            QueryAuthorizationFilterFactory queryAuthFilterFactory) {
        this.queryAuthFilterFactory = queryAuthFilterFactory;
    }

    @Required
    public void setDocumentMapper(DocumentMapper documentMapper) {
        this.documentMapper = documentMapper;
    }

    @Required
    public void setFieldValueMapper(FieldValueMapper fieldValueMapper) {
        this.fieldValueMapper = fieldValueMapper;
    }
    

}

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
import java.util.List;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.document.FieldSelectorResult;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanFilter;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.FilterClause;
import org.apache.lucene.search.TermsFilter;
import org.apache.lucene.util.OpenBitSet;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Path;
import org.vortikal.repository.index.mapping.FieldNameMapping;
import org.vortikal.repository.index.mapping.FieldValueMapper;
import org.vortikal.repository.reporting.ReportScope;
import org.vortikal.repository.reporting.ResourcePropertyValueScope;
import org.vortikal.repository.reporting.ResourceReadableACLScope;
import org.vortikal.repository.reporting.ResourceTypeScope;
import org.vortikal.repository.reporting.UriPrefixScope;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.ResourceTypeDefinition;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.repository.search.query.security.QueryAuthorizationFilterFactory;

/**
 * Generate Lucene filters for misc data report resource scopings.
 *
 *
 * @author oyviste
 */
public class IndexDataReportScopeFilterFactoryImpl
                    implements IndexDataReportScopeFilterFactory {
    
    private static final Filter MATCH_NOTHING_FILTER = new MatchNothingFilter();

    private QueryAuthorizationFilterFactory queryAuthorizationFilterFactory;
    private FieldValueMapper fieldValueMapper;

    @Override
    public Filter getScopeFilter(List<ReportScope> scoping, IndexReader reader) throws IOException {

        if (scoping.isEmpty()) return null;

        BooleanFilter mainFilter = new BooleanFilter();

        // Assemble boolean filter chain with base AND scoping logic and optional inversion/prohibition
        for (ReportScope scope: scoping) {

            Filter filter = null;
            if (scope instanceof ResourceReadableACLScope) {
                
                filter = getAclFilter(((ResourceReadableACLScope)scope).getToken(), reader);

            } else if (scope instanceof ResourcePropertyValueScope) {

                ResourcePropertyValueScope rpscope =
                        (ResourcePropertyValueScope)scope;

                filter = getPropertyValueFilter(rpscope.getDef(), rpscope.getValues());

            } else if (scope instanceof ResourceTypeScope) {

                filter = getResourceTypeFilter(((ResourceTypeScope)scope).getTypes());

            } else if (scope instanceof UriPrefixScope) {

                UriPrefixScope upscope = (UriPrefixScope)scope;

                filter = getUriScopeFilter(upscope.getUriPrefixes(), reader);
                
            } else {
                
                throw new UnsupportedOperationException("Unsupported scope type: " + scope.getClass());
            }


            if (filter != null) {
                if (scope.isProhibited()) {
                    mainFilter.add(new FilterClause(filter, BooleanClause.Occur.MUST_NOT));
                } else {
                    mainFilter.add(new FilterClause(filter, BooleanClause.Occur.MUST));
                }
            }
        }

        return mainFilter;
    }

    // FIXME no hierarchical type support (the world is flat after all)
    private Filter getResourceTypeFilter(Set<ResourceTypeDefinition> types) {
        TermsFilter tf = new TermsFilter();
        for (ResourceTypeDefinition type: types) {
            tf.addTerm(new Term(FieldNameMapping.RESOURCETYPE_FIELD_NAME, type.getName()));
        }

        return tf;
    }

    private Filter getPropertyValueFilter(PropertyTypeDefinition def, Set<Value> values) {
        TermsFilter tf = new TermsFilter();
        String searchFieldName = FieldNameMapping.getSearchFieldName(def, false);

        for (Value value: values) {
            String searchFieldValue = this.fieldValueMapper.encodeIndexFieldValue(value, false);
            Term term = new Term(searchFieldName, searchFieldValue);
            tf.addTerm(term);
        }

        return tf;
    }

    private Filter getAclFilter(String token, IndexReader reader) {
        return this.queryAuthorizationFilterFactory.authorizationQueryFilter(token, reader);
    }

    private Filter getUriScopeFilter(List<Path> uris, IndexReader reader) throws IOException {

        if (uris.isEmpty()) {
            // If a UriPrefixScope is defined, but it contains no URIs, then it matches nothing.
            return MATCH_NOTHING_FILTER;
        }

        TermsFilter tf = new TermsFilter();
        int validUris = 0;
        for (Path path : uris) {
            if (path.isRoot()) {
                return null; // Signal that scope doesn't restrict anything.
            }

            TermDocs tdocs = reader.termDocs(
                    new Term(FieldNameMapping.URI_FIELD_NAME, path.toString()));
            try {
                if (tdocs.next()) {
                    Document doc = reader.document(tdocs.doc(), new FieldSelector() {

                        private static final long serialVersionUID = 2294209998307991707L;

                        public FieldSelectorResult accept(String name) {
                            // Interned string comparison
                            if (FieldNameMapping.STORED_ID_FIELD_NAME == name) {
                                return FieldSelectorResult.LOAD;
                            }

                            return FieldSelectorResult.NO_LOAD;
                        }
                    });

                    String idValue = Integer.toString(
                            this.fieldValueMapper.getIntegerFromStoredBinaryField(
                                                  doc.getField(FieldNameMapping.STORED_ID_FIELD_NAME)));

                    BooleanQuery bq = new BooleanQuery(true);

                    tf.addTerm(new Term(FieldNameMapping.ANCESTORIDS_FIELD_NAME, idValue));
                    tf.addTerm(new Term(FieldNameMapping.ID_FIELD_NAME, idValue));
                    ++validUris;
                } 
            } finally {
                tdocs.close();
            }
        }

        if (validUris == 0) {
            return MATCH_NOTHING_FILTER; // No valid URIs means this scope matches nothing.
        }

        return tf;
    }

    /**
     * @param queryAuthFilterFactory the queryAuthFilterFactory to set
     */
    @Required
    public void setQueryAuthorizationFilterFactory(QueryAuthorizationFilterFactory queryAuthorizationFilterFactory) {
        this.queryAuthorizationFilterFactory = queryAuthorizationFilterFactory;
    }

    /**
     * @param fieldValueMapper the fieldValueMapper to set
     */
    @Required
    public void setFieldValueMapper(FieldValueMapper fieldValueMapper) {
        this.fieldValueMapper = fieldValueMapper;
    }

    private static final class MatchNothingFilter extends Filter {
        private static final long serialVersionUID = 5225305691186860115L;

        @Override
        public DocIdSet getDocIdSet(IndexReader reader) {
            return new OpenBitSet(reader.maxDoc());
        }

    }

}

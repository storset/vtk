/* Copyright (c) 2014, University of Oslo, Norway
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
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SearcherFactory;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopFieldDocs;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.index.mapping.FieldNames;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.search.Search;
import org.vortikal.repository.search.query.LuceneQueryBuilder;
import org.vortikal.repository.search.query.PropertyExistsQuery;

/**
 *
 */
public class WarmupSearcherFactory extends SearcherFactory {

    private PropertyTypeDefinition lastModifiedPropDef;
    private PropertyTypeDefinition hiddenPropDef;
    private LuceneQueryBuilder luceneQueryBuilder;

    @Override
    public IndexSearcher newSearcher(IndexReader reader) throws IOException {
        IndexSearcher searcher = super.newSearcher(reader);
        warmup(searcher);
        return searcher;
    }

    private void warmup(IndexSearcher searcher) throws IOException {

        // Do a simple warmup first with basic query
        Query luceneQuery = getWarmupQuery();
        Sort luceneSorting = getWarmupSorting();
        Filter luceneFilter = null;
        TopFieldDocs docs = searcher.search(luceneQuery, luceneFilter, 2500, luceneSorting);
        int max = Math.min(500, docs.scoreDocs.length);
        for (int i = 0; i < max; i++) {
            searcher.doc(docs.scoreDocs[i].doc);
        }
        docs = null; // Garbage

        // Implicitly warm up query builder internal filter caching for the new reader instance.
        // This should warm up:
        // 1. Inverted hidden-prop existence query filter cache
        // 2. ACL filter cache for anonymous user
        // Enabling pre-building of all this caching should be very good for performance of new reader
        // after warmup.
        
        Search search = new Search();
        search.setLimit(2500);
        PropertyExistsQuery peq = new PropertyExistsQuery(this.hiddenPropDef, true);
        search.setQuery(peq);
        luceneQuery = this.luceneQueryBuilder.buildQuery(search.getQuery(), searcher);
        luceneFilter = this.luceneQueryBuilder.buildSearchFilter(null, search, searcher);
        searcher.search(luceneQuery, luceneFilter, search.getLimit());
    }

    private Query getWarmupQuery() {
        BooleanQuery bq = new BooleanQuery();
        bq.add(new TermQuery(new Term(FieldNames.URI_ANCESTORS_FIELD_NAME, "/")), BooleanClause.Occur.SHOULD);
        bq.add(new TermQuery(new Term(FieldNames.RESOURCETYPE_FIELD_NAME, "collection")), BooleanClause.Occur.SHOULD);
        return bq;
    }

    private Sort getWarmupSorting() {
        SortField[] fields = new SortField[2];
        fields[0] = new SortField(FieldNames.sortFieldName(this.lastModifiedPropDef), SortField.Type.STRING, true);
        fields[1] = new SortField(FieldNames.URI_SORT_FIELD_NAME, SortField.Type.STRING);
        return new Sort(fields);
    }

    @Required
    public void setLastModifiedPropDef(PropertyTypeDefinition lastModifiedPropDef) {
        this.lastModifiedPropDef = lastModifiedPropDef;
    }

    @Required
    public void setHiddenPropDef(PropertyTypeDefinition hiddenPropDef) {
        this.hiddenPropDef = hiddenPropDef;
    }

    @Required
    public void setLuceneQueryBuilder(LuceneQueryBuilder luceneQueryBuilder) {
        this.luceneQueryBuilder = luceneQueryBuilder;
    }
    
}

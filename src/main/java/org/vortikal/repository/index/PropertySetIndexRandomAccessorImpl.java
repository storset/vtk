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
package org.vortikal.repository.index;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.queries.TermFilter;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.NumericUtils;

import org.vortikal.repository.Path;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.index.mapping.DocumentMapper;
import org.vortikal.repository.index.mapping.FieldNames;

/**
 * Random access (lookup by id) implementation for property set index.
 */
class PropertySetIndexRandomAccessorImpl implements PropertySetIndexRandomAccessor {

    private final DocumentMapper mapper;
    private final Lucene4IndexManager index;
    private final IndexSearcher searcher;
    
    public PropertySetIndexRandomAccessorImpl(Lucene4IndexManager index, DocumentMapper mapper) 
        throws IOException {
        this.mapper = mapper;
        this.index = index;
        this.searcher = index.getIndexSearcher();
    }
    
    @Override
    public int countInstances(Path uri) throws IndexException {

        Term term = new Term(FieldNames.URI_FIELD_NAME, uri.toString());
        try {
            List<Document> docs = lookupDocs(term, Collections.<String>emptySet());
            return docs.size();
        } catch (IOException io) {
            throw new IndexException(io);
        }
    }
    
    @Override
    public PropertySet getPropertySetByURI(Path uri) throws IndexException {
        PropertySet propSet = null;

        try {
            List<Document> docs = lookupDocs(new Term(FieldNames.URI_FIELD_NAME, uri.toString()), null);
            if (docs.size() > 0) {
                Document doc = docs.get(0); // Just pick first in case of duplicates
                propSet = mapper.getPropertySet(doc);
            }
        } catch (IOException io) {
            throw new IndexException(io);
        }
        
        return propSet;
    }

    @Override
    public PropertySet getPropertySetByUUID(String uuid) throws IndexException {
        PropertySet propSet = null;
        try {
            List<Document> docs = lookupDocs(new Term(FieldNames.ID_FIELD_NAME, uuid), null);
            if (docs.size() > 0) {
                Document doc = docs.get(0); // Just pick first in case of duplicates
                propSet = mapper.getPropertySet(doc);
            }
        } catch (IOException io) {
            throw new IndexException(io);
        }
        
        return propSet;
    }
    
    @Override
    public PropertySetInternalData getPropertySetInternalData(final Path uri) throws IndexException {

        try {
            Set<String> fields = new HashSet<String>();
            fields.add(FieldNames.URI_FIELD_NAME);
            fields.add(FieldNames.RESOURCETYPE_FIELD_NAME);
            fields.add(FieldNames.ID_FIELD_NAME);
            fields.add(FieldNames.ACL_INHERITED_FROM_FIELD_NAME);
            fields.add(FieldNames.ACL_READ_PRINCIPALS_FIELD_NAME);
            
            List<Document> docs = lookupDocs(new Term(FieldNames.URI_FIELD_NAME, uri.toString()), fields);
            if (docs.isEmpty()) return null;

            Document doc = docs.get(0); // Just pick first in case of duplicates
            final String rt = doc.get(FieldNames.RESOURCETYPE_FIELD_NAME);
            final int id = mapper.getResourceId(doc);
            final int aclInheritedFrom = mapper.getAclInheritedFrom(doc);
            final Set<String> aclReadPrincipalNames = mapper.getACLReadPrincipalNames(doc);

            return new PropertySetInternalData() {
                @Override
                public Path getURI() {
                    return uri;
                }

                @Override
                public String getResourceType() {
                    return rt;
                }

                @Override
                public int getResourceId() {
                    return id;
                }

                @Override
                public int getAclInheritedFromId() {
                    return aclInheritedFrom;
                }

                @Override
                public Set<String> getAclReadPrincipalNames() {
                    return aclReadPrincipalNames;
                }
            };
        } catch (IOException io) {
            throw new IndexException(io);
        }
        
    }
    
    private List<Document> lookupDocs(Term term, Set<String> loadFields) throws IOException {

        final List<Document> documents = new ArrayList<Document>();
        final TermFilter tf = new TermFilter(term);
        try {
            for (AtomicReaderContext arc : searcher.getIndexReader().leaves()) {
                AtomicReader ar = arc.reader();
                Bits liveDocs = ar.getLiveDocs();
                DocIdSet docSet = tf.getDocIdSet(arc, liveDocs);
                if (docSet != null) {
                    DocIdSetIterator disi = docSet.iterator();
                    if (disi != null) {
                        int docId;
                        while ((docId = disi.nextDoc()) != DocIdSetIterator.NO_MORE_DOCS) {
                            Document doc = ar.document(docId, loadFields);
                            documents.add(doc);
                        }
                    }
                }
            }
        } catch (IOException io) {
            throw new IndexException(io);
        }
        
        return documents;
    }
    
    @Override
    public void close() throws IndexException {
        try {
            index.releaseIndexSearcher(searcher);
        } catch (IOException io) {
            throw new IndexException(io);
        }
    }

}

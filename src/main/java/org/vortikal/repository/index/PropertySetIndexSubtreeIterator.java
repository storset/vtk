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
import java.util.NoSuchElementException;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.index.mapping.DocumentMapper;
import org.vortikal.repository.index.mapping.FieldNames;

class PropertySetIndexSubtreeIterator extends  AbstractDocumentFieldPrefixIterator {

    private DocumentMapper mapper;
    private PropertySet rootUriPropset = null;
    private boolean first = true;
    
    public PropertySetIndexSubtreeIterator(IndexReader reader, DocumentMapper mapper, String rootUri)
            throws IOException {
        super(reader, FieldNames.URI_FIELD_NAME, ("/".equals(rootUri) ? "/" : rootUri + "/"));
        this.mapper = mapper;
        if (!"/".equals(rootUri)) {
            this.rootUriPropset = rootUriPropSet(reader, rootUri);
        } else {
            this.first = false;
        }
    }

    private PropertySet rootUriPropSet(IndexReader reader, String rootUri) throws IOException {
        TermDocs tdocs = reader.termDocs(new Term(FieldNames.URI_FIELD_NAME, rootUri));
        try {
            if (tdocs.next()) {
                return this.mapper.getPropertySet(reader.document(tdocs.doc()));
            }
        } finally {
            tdocs.close();
        }

        return null;
    }
    
    @Override
    public boolean hasNext() {
        if (first) {
            return this.rootUriPropset != null;
        } else {
            return super.hasNext();
        }
    }
    
    @Override
    public Object next() {
        Object retval = null;
        if (first) {
            if (this.rootUriPropset == null) {
                throw new NoSuchElementException("No more elements");
            }

            retval = this.rootUriPropset;
            first = false;
        } else {
            retval = super.next();
        }
        
        return retval;
    }

    protected Object getObjectFromDocument(Document doc) throws Exception {
        return mapper.getPropertySet(doc);
    }

}

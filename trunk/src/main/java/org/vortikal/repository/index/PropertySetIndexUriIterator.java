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

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermEnum;
import org.vortikal.repository.Path;
import org.vortikal.repository.index.mapping.FieldNames;

/**
 * Simple URI-only iterator, lexicographic order.
 * 
 * @author oyviste
 *
 */
public class PropertySetIndexUriIterator implements CloseableIterator<Object> {

    private Path next = null;
    private TermEnum te;
    private TermDocs td;
    private IndexReader reader;
    private String iteratorField = FieldNames.URI_FIELD_NAME;
        
    public PropertySetIndexUriIterator(IndexReader reader) throws IOException {
        this.te = reader.terms(new Term(this.iteratorField, ""));
        this.td = reader.termDocs(new Term(this.iteratorField, ""));
        this.reader = reader;
        
        if (te.term() != null && te.term().field() == iteratorField) {
            td.seek(te);
            next = nextUri();
        }
    }
    
    // Next non-deleted URI _including_ any multiples
    private Path nextUri() throws IOException {
        while (td.next()) {
            if (! reader.isDeleted(td.doc())) {
                return Path.fromString(te.term().text());
            }
        }
        
        // No more docs for current term, seek to next
        while (te.next() && te.term().field() == iteratorField) {
            td.seek(te);
            while (td.next()) {
                if (! reader.isDeleted(td.doc())) {
                    return Path.fromString(te.term().text());
                }
            }
        }
        
        return null;
    }
    
    public boolean hasNext() {
        return (next != null);
    }

    public Object next() {
        if (next == null) {
            throw new IllegalStateException("No more elements");
        }
        
        Path retVal = next;
        
        try {
            next = nextUri();
        } catch (IOException e) {
            next = null;
        }
        
        return retVal;
    }

    public void remove() {
        throw new UnsupportedOperationException("Iterator does not support elment removal");
    }
    
    public void close() throws Exception {
        te.close();
        td.close();
    }

}

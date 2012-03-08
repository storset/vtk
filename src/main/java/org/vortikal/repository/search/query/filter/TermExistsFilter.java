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
package org.vortikal.repository.search.query.filter;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.Filter;
import org.apache.lucene.util.OpenBitSet;

/**
 * Experimental, might be slow for common terms in indexes with many
 * documents. Avoid if you can.
 * 
 * @author oyviste
 *
 */
public class TermExistsFilter extends Filter {
    
    private static final long serialVersionUID = 6676434194690479831L;
    private String fieldName;

    /**
     * Construct a filter for the given fieldName.
     * 
     * @param fieldName The Lucene Document field to check for existence on.
     */
    public TermExistsFilter(String fieldName) {
        this.fieldName = fieldName;
    }
    
    @Override
    public DocIdSet getDocIdSet(final IndexReader reader) throws IOException {
        final OpenBitSet bits = new OpenBitSet(reader.maxDoc());
        new ExistsIdGenerator(this.fieldName) {
            @Override
            public void handleDoc(int doc) {
                bits.fastSet(doc);
            }
        }.generate(reader);

        return bits;
    }

    private static abstract class ExistsIdGenerator {
        private Term exists;

        ExistsIdGenerator(String fieldName) {
            this.exists = new Term(fieldName, "");
        }

        void generate(final IndexReader reader) throws IOException {
            TermEnum tenum = reader.terms(this.exists);
            TermDocs tdocs = reader.termDocs(this.exists);
            String existsFieldName = this.exists.field();
            try {
                do {
                    Term t = tenum.term();
                    if (t != null && t.field() == existsFieldName) { // Interned string comparison
                        // Add the docs
                        tdocs.seek(tenum);
                        while (tdocs.next()) {
                            handleDoc(tdocs.doc());
                        }
                    } else {
                        break;
                    }
                } while (tenum.next());
            } finally {
                tenum.close();
                tdocs.close();
            }
        }
        
        abstract void handleDoc(int doc);

    }

}

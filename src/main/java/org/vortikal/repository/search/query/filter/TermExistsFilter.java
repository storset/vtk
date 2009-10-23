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
import java.util.BitSet;

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
 * <p>      
 * Not thread safe. A new instance should be created for every query.
 *      
 * @author oyviste
 *
 */
public class TermExistsFilter extends Filter {
    
    private static final long serialVersionUID = 6676434194690479831L;
    private String fieldName;
    private BitSet bits;
    private DocIdSet docIdSet;
    
    /**
     * Construct a filter for the given fieldName.
     * 
     * @param fieldName The Lucene Document field to check for existence on.
     */
    public TermExistsFilter(String fieldName) {
        this.fieldName = fieldName;
    }
    
    public BitSet bits(IndexReader reader) throws IOException {
        if (this.bits == null) {
            BitSet bits = new BitSet(reader.maxDoc());
            Term term = new Term(this.fieldName, "");
            String termField = term.field();
            
            TermEnum tenum = reader.terms(term);
            TermDocs tdocs = reader.termDocs(term);
            try {
                do {
                    if (tenum.term() != null && tenum.term().field() == termField) { // Interned string comparison
                        // Add the docs
                        tdocs.seek(tenum);
                        while (tdocs.next()) {
                            bits.set(tdocs.doc());
                        }
                    } else break;
                    
                } while (tenum.next());
            } finally {
                tenum.close();
                tdocs.close();
            }
            
            this.bits = bits;
        }

        return this.bits;
    }
    
    public DocIdSet getDocIdSet(IndexReader reader) throws IOException {
       if (this.docIdSet == null) {
           OpenBitSet openBits = new OpenBitSet(reader.maxDoc());
           Term term = new Term(this.fieldName, "");
           String fieldName = term.field();
           
           TermEnum tenum = reader.terms(term);
           TermDocs tdocs = reader.termDocs(term);
           try {
               do {
                   Term t = tenum.term();
                   if (t != null && t.field() == fieldName) {
                       // Add the docs
                       tdocs.seek(tenum);
                       while (tdocs.next()) {
                           openBits.fastSet(tdocs.doc());
                       }
                   } else break;
                   
               } while (tenum.next());
           } finally {
               tenum.close();
               tdocs.close();
           }
           
           this.docIdSet = openBits;
       }

       return this.docIdSet;
    }

}

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
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Filter;
import org.apache.lucene.util.OpenBitSet;

/**
 * A {@link org.apache.lucene.search.Filter} that inverts the result of 
 * another <code>Filter</code>.
 * <p>
 * 
 * It basically flips all bits provided by the wrapped filter, while
 * making sure that bits for deleted documents are not set.
 * <p>
 * 
 * It is a non-thread safe, per-query dynamic filter. It will directly alter the 
 * <code>BitSet</code> provided by the wrapped filter to avoid double 
 * memory allocation and copying. Beware of this if wrapping
 * re-usable (long-lived) filters that cache their own bitset and expect it
 * not to change.
 * <p>
 * 
 * NOTE: It may be more efficient to code inversion-logic directly into
 * filter implementations (depends).
 * 
 * @author oyviste
 *
 */
public class InversionFilter extends Filter {

    private static final long serialVersionUID = -8133303000593033686L;

    private Filter wrappedFilter;
    private BitSet bits;
    private DocIdSet docIdSet;
    
    public InversionFilter(Filter wrappedFilter) {
        this.wrappedFilter = wrappedFilter;
    }
    
    @Override
    @Deprecated
    public BitSet bits(IndexReader reader) throws IOException {
        if (bits == null) {
            bits = this.wrappedFilter.bits(reader);
            bits.flip(0, reader.maxDoc());
            for (int i = bits.nextSetBit(0); i >= 0; i = bits.nextSetBit(i+1)) {
                if (reader.isDeleted(i)) {
                    bits.clear(i);
                }
            }
        }
        
        return bits;
    }
    
    @Override
    public DocIdSet getDocIdSet(IndexReader reader) throws IOException {
        if (this.docIdSet == null) {
            int maxDoc = reader.maxDoc();
            OpenBitSet docIdSet = new OpenBitSet(maxDoc);
        
            DocIdSetIterator iterator = this.wrappedFilter.getDocIdSet(reader).iterator();
            int prevDocId = -1;
            while(iterator.next()) {
                int currentDocId = iterator.doc();
                for (int i=prevDocId+1; i<currentDocId; i++) {
                    if (!reader.isDeleted(i)) { 
                        docIdSet.fastSet(i);
                    }
                }
                prevDocId = currentDocId;
            }
            
            // Flip the rest of the bits (last doc id+1 to maxdoc())
            for (int i=prevDocId+1; i<maxDoc; i++) {
                if (!reader.isDeleted(i)) {
                    docIdSet.fastSet(i);
                }
            }
            
            this.docIdSet = docIdSet;
        }
        
        return this.docIdSet;
    }
    
}

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
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.Filter;
import org.apache.lucene.util.OpenBitSet;
import org.apache.lucene.util.OpenBitSetDISI;

/**
 * A {@link org.apache.lucene.search.Filter} that inverts the result of 
 * another <code>Filter</code>.
 * <p>
 * 
 * It basically flips all bits provided by the wrapped filter, while
 * making sure that bits for deleted documents are not set.
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
    private Filter deletedDocsFilter;
    
    public InversionFilter(Filter wrappedFilter) {
        this.wrappedFilter = wrappedFilter;
    }

    /**
     * Constructor with provided filter for deleted docs. This filter can be
     * cached and if pre-built makes the inversion go faster.
     *
     * @param wrappedFilter
     * @param deletedDocsFilter
     */
    public InversionFilter(Filter wrappedFilter, Filter deletedDocsFilter) {
        this.wrappedFilter = wrappedFilter;
        this.deletedDocsFilter = deletedDocsFilter;
    }

    @Override
    public DocIdSet getDocIdSet(IndexReader reader) throws IOException {

        final int maxDoc = reader.maxDoc();
        final OpenBitSetDISI inverted = new OpenBitSetDISI(maxDoc);
        final DocIdSet wrappedSet = this.wrappedFilter.getDocIdSet(reader);
        if (wrappedSet instanceof OpenBitSet) {
            // Optimized case for OpenBitSet
            inverted.or((OpenBitSet)wrappedSet);
        } else {
            inverted.inPlaceOr(wrappedSet.iterator());
        }

        inverted.flip(0, maxDoc);

        // Filters used as part of query tree cannot return deleted docs, so
        // we need to remove any set bits for such doc ids
        if (reader.hasDeletions()) {
            DocIdSet deletedSet;
            if (this.deletedDocsFilter != null) {
                // Use provided and possibly cached filter for better efficiency
                deletedSet = this.deletedDocsFilter.getDocIdSet(reader);
            } else {
                deletedSet = new DeletedDocsFilter().getDocIdSet(reader);
            }

            if (deletedSet instanceof OpenBitSet) {
                // Optimized case for OpenBitSet
                inverted.andNot((OpenBitSet) deletedSet);
            } else {
                inverted.inPlaceNot(deletedSet.iterator());
            }
        }

        return inverted;
    }
}

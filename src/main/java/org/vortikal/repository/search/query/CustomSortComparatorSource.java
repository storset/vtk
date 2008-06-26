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
package org.vortikal.repository.search.query;

import java.io.IOException;
import java.io.InputStream;
import java.text.Collator;
import java.text.ParseException;
import java.text.RuleBasedCollator;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.FieldCache;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.ScoreDocComparator;
import org.apache.lucene.search.SortComparatorSource;

public class CustomSortComparatorSource implements SortComparatorSource {

    private static final long serialVersionUID = -8586536787705536907L;
    
    private Collator collator;
    private String fileName = "custom-rules.txt";

    public CustomSortComparatorSource() throws IOException, ParseException {
        InputStream input = getClass().getResourceAsStream(fileName);
        String rules = inputStreamToString(input);

        this.collator = new RuleBasedCollator(rules);
    }

    public static String inputStreamToString(InputStream in) throws IOException {
        StringBuilder out = new StringBuilder();
        byte[] b = new byte[4096];
        for (int n; (n = in.read(b)) != -1;) {
            out.append(new String(b, 0, n, "UTF-8"));
        }
        return out.toString();
    }

    /**
     * Creates a comparator for the field in the given index.
     * 
     * @param reader
     *            Index to create comparator for.
     * @param fieldname
     *            Fieldable to create comparator for.
     * @return Comparator of ScoreDoc objects.
     * @throws IOException
     *             If an error occurs reading the index.
     */
    public ScoreDocComparator newComparator(IndexReader reader,
            String fieldname) throws IOException {
        final String field = fieldname.intern();
        final String[] index = FieldCache.DEFAULT.getStrings(reader, field);
        return new ScoreDocComparator() {
            public final int compare(final ScoreDoc i, final ScoreDoc j) {
                String is = index[i.doc];
                String js = index[j.doc];
                if (is == js) {
                    return 0;
                } else if (is == null) {
                    return -1;
                } else if (js == null) {
                    return 1;
                } else {
                    return collator.compare(is, js);
                }
            }

            @SuppressWarnings("unchecked")
            public Comparable sortValue(final ScoreDoc i) {
                return index[i.doc];
            }

            public int sortType() {
                return org.apache.lucene.search.SortField.STRING;
            }
        };
    }

    public Collator getCollator() {
        return collator;
    }
}

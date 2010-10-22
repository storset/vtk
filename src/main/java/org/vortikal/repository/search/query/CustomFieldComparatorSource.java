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
import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.FieldComparatorSource;


/**
 * XXX Completely untested after upgrade to Lucene 3.
 *     Might be utterly broken. We don't currently use it for anything, though..
 */
public class CustomFieldComparatorSource extends FieldComparatorSource {

    private static final long serialVersionUID = -8586536787705536907L;
    
    private Collator collator;
    private String fileName = "custom-rules.txt";

    public CustomFieldComparatorSource() throws IOException, ParseException {
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

    public Collator getCollator() {
        return collator;
    }

    @Override
    public FieldComparator newComparator(final String fieldname,
                                         final int numHits,
                                         final int sortPos,
                                         final boolean reversed) throws IOException {

        return new FieldComparator() {
            private final String[] values = new String[numHits];
            private String[] currentReaderValues;
            private final String field = fieldname;
            final Collator collator = this.collator;
            private String bottom;

            @Override
            public int compare(int slot1, int slot2) {
                final String val1 = values[slot1];
                final String val2 = values[slot2];
                if (val1 == null) {
                    if (val2 == null) {
                        return 0;
                    }
                    return -1;
                } else if (val2 == null) {
                    return 1;
                }
                return collator.compare(val1, val2);
            }

            @Override
            public int compareBottom(int doc) {
                final String val2 = currentReaderValues[doc];
                if (bottom == null) {
                    if (val2 == null) {
                        return 0;
                    }
                    return -1;
                } else if (val2 == null) {
                    return 1;
                }
                return collator.compare(bottom, val2);
            }

            @Override
            public void copy(int slot, int doc) {
                values[slot] = currentReaderValues[doc];
            }

            @Override
            public void setNextReader(IndexReader reader, int docBase) throws IOException {
                currentReaderValues = FieldCache.DEFAULT.getStrings(reader, field);
            }

            @Override
            public void setBottom(final int bottom) {
                this.bottom = values[bottom];
            }

            @Override
            public Comparable value(int slot) {
                return values[slot];
            }

        };

    }
}

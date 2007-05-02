package org.vortikal.repositoryimpl.search.query;

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

            public Comparable sortValue(final ScoreDoc i) {
                return index[i.doc];
            }

            public int sortType() {
                return org.apache.lucene.search.SortField.STRING;
            }
        };
    }
}

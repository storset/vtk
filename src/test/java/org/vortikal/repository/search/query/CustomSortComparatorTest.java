package org.vortikal.repository.search.query;

import java.io.IOException;
import java.text.Collator;
import java.text.ParseException;

import junit.framework.TestCase;
import edu.emory.mathcs.backport.java.util.Arrays;

public class CustomSortComparatorTest extends TestCase {

    public void testCollator() throws IOException, ParseException {
        Collator collator = new CustomSortComparatorSource().getCollator();

        assertEquals(-1, collator.compare("Aa", "c"));
        assertEquals(-1, collator.compare("A", "aa"));
        assertEquals(-1, collator.compare("ä", "Ø"));
        assertEquals(1, collator.compare("ä", "Æ"));
        assertEquals(-1, collator.compare("V", "W"));
        assertEquals(1, collator.compare("ö", "Ø"));
        assertEquals(1, collator.compare("Ö", "Ø"));
        assertEquals(-1, collator.compare("Ö", "Å"));

        String list[] = { "Aa", "c", "C", "Ca", "Caa", "CAa", "CAA", "A", "aa", "Ø", "Æ", "Va", "Wa", "å", "Å", "a" };

        Arrays.sort(list, collator);

        for (int i = 0; i < list.length; i++) {
            System.out.println(list[i]);
        }
    }
}

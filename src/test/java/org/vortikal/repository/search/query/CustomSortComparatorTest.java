package org.vortikal.repository.search.query;

import java.io.IOException;
import java.text.Collator;
import java.text.ParseException;

import junit.framework.TestCase;

public class CustomSortComparatorTest extends TestCase {

    public void testCollator() throws IOException, ParseException {
        Collator collator = new CustomFieldComparatorSource().getCollator();

        assertEquals(-1, collator.compare("Aa", "c"));
        assertEquals(-1, collator.compare("A", "aa"));
        assertEquals(-1, collator.compare("ä", "Ø"));
        assertEquals(1, collator.compare("ä", "Æ"));
        assertEquals(-1, collator.compare("V", "W"));
        assertEquals(1, collator.compare("ö", "Ø"));
        assertEquals(1, collator.compare("Ö", "Ø"));
        assertEquals(-1, collator.compare("Ö", "Å"));
      
    }
}   

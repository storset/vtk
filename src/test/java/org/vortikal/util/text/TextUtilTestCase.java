package org.vortikal.util.text;

import junit.framework.TestCase;

public class TextUtilTestCase extends TestCase {

    // Test the removeDuplicates method in TextUtil class.
    public void testRemoveDuplicatesIgnoreCase() {

        String testTags = "Forskning, Røed Ødegård, forskning, FoRsKning, forskNING";
        String expectedTags = "forskning, røed ødegård";

        String test = TextUtils.removeDuplicatesIgnoreCase(testTags, ",");

        assertEquals(expectedTags, test);
    }
}

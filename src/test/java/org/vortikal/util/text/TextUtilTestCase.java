package org.vortikal.util.text;

import junit.framework.TestCase;

public class TextUtilTestCase extends TestCase {

    // Test the removeDuplicates method in TextUtil class.
    public void testRemoveDuplicatesIgnoreCase() {

        String testTags = "Forskning, Røed Ødegård, forskning, FoRsKning, forskNING";

        String expectedTagsWithSpaces = "forskning, røed ødegård";
        String expectedTagsWithoutSpaces = "forskning,røed ødegård";
        String expectedTagsWithoutDelimiter = "forskning røed ødegård";
        String expectedTagsWithoutSpacesAndDelimiter = "forskningrøed ødegård";

        String test0 = TextUtils.removeDuplicatesIgnoreCase(testTags, ",");
        assertEquals(expectedTagsWithSpaces, test0);

        String test1 = TextUtils.removeDuplicatesIgnoreCase(testTags, ",", false, false);
        assertEquals(expectedTagsWithSpaces, test1);

        String test2 = TextUtils.removeDuplicatesIgnoreCase(testTags, ",", true, false);
        assertEquals(expectedTagsWithoutSpaces, test2);

        String test3 = TextUtils.removeDuplicatesIgnoreCase(testTags, ",", false, true);
        assertEquals(expectedTagsWithoutDelimiter, test3);

        String test4 = TextUtils.removeDuplicatesIgnoreCase(testTags, ",", true, true);
        assertEquals(expectedTagsWithoutSpacesAndDelimiter, test4);
    }
}

package org.vortikal.util.text;

import junit.framework.TestCase;

public class TextUtilTestCase extends TestCase {

    public void testRemoveDuplicates() {

        String testTags = "Forskning, Røed Ødegård, forskning, FoRsKning, forskNING";
        String expectedTags = "røed ødegård, forskning";

        String test = TextUtils.removeDuplicates(testTags, ", ");

        assertEquals(expectedTags, test);

    }
}

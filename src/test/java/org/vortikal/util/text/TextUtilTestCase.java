package org.vortikal.util.text;

import junit.framework.TestCase;

public class TextUtilTestCase extends TestCase {

    public void testRemoveDuplicatesIgnoreCase() {

        String testData = "Forskning, Røed Ødegård, forskning, FoRsKning, forskNING";

        String expectedData = "Forskning, Røed Ødegård";
        String expectedDataNoSpaces = "Forskning,Røed Ødegård";
        String expectedDataNoDelimiter = "Forskning Røed Ødegård";
        String expectedDataNoSpacesAndDelimiter = "ForskningRøed Ødegård";

        // Test noSpaces, noDelimiter boolean configurations with capitalizeWords
        String test = TextUtils.removeDuplicatesIgnoreCase(testData, ",", false, false, true);
        assertEquals(expectedData, test);

        test = TextUtils.removeDuplicatesIgnoreCase(testData, ",", true, false, true);
        assertEquals(expectedDataNoSpaces, test);

        test = TextUtils.removeDuplicatesIgnoreCase(testData, ",", false, true, true);
        assertEquals(expectedDataNoDelimiter, test);

        test = TextUtils.removeDuplicatesIgnoreCase(testData, ",", true, true, true);
        assertEquals(expectedDataNoSpacesAndDelimiter, test);

        // Single test without capitalizeWords
        String expectedDataNoneCapitalized = "forskning, røed ødegård";

        test = TextUtils.removeDuplicatesIgnoreCase(testData, ",", false, false, false);
        assertEquals(expectedDataNoneCapitalized, test);

    }


    public void testCapitalizeString() {

        String expectedCapitalized = "These Words Needs To Be Capitalized";

        String test = TextUtils.capitalizeString("these words needs to be capitalized");
        assertEquals(expectedCapitalized, test);
    }
}

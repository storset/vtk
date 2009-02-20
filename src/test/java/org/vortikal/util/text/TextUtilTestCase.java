package org.vortikal.util.text;

import junit.framework.TestCase;

public class TextUtilTestCase extends TestCase {

    public void testRemoveDuplicatesIgnoreCase() {

        String testTags = "Forskning, Røed Ødegård, forskning, FoRsKning, forskNING";

        String expectedTagsWithSpacesNoCapitalize = "forskning, røed ødegård";
        String expectedTagsWithSpaces = "Forskning, Røed Ødegård";
        String expectedTagsWithoutSpaces = "Forskning,Røed Ødegård";
        String expectedTagsWithoutDelimiter = "Forskning Røed Ødegård";
        String expectedTagsWithoutSpacesAndDelimiter = "ForskningRøed Ødegård";

        // Overload methods
        String test = TextUtils.removeDuplicatesIgnoreCase(testTags, ",");
        assertEquals(expectedTagsWithSpacesNoCapitalize, test);

        test = TextUtils.removeDuplicatesIgnoreCaseCapitalized(testTags, ",");
        assertEquals(expectedTagsWithSpaces, test);

        // Overloaded method
        test = TextUtils.removeDuplicatesIgnoreCase(testTags, ",", false, false, true);
        assertEquals(expectedTagsWithSpaces, test);

        test = TextUtils.removeDuplicatesIgnoreCase(testTags, ",", true, false, true);
        assertEquals(expectedTagsWithoutSpaces, test);

        test = TextUtils.removeDuplicatesIgnoreCase(testTags, ",", false, true, true);
        assertEquals(expectedTagsWithoutDelimiter, test);

        test = TextUtils.removeDuplicatesIgnoreCase(testTags, ",", true, true, true);
        assertEquals(expectedTagsWithoutSpacesAndDelimiter, test);

        // Capitalize alone
        String expectedCapitalized = "These Words Needs To Be Capitalized";

        test = TextUtils.capitalizeString("these words needs to be capitalized");
        assertEquals(expectedCapitalized, test);
    }
}

package org.vortikal.integration.webtests.view;

import org.vortikal.integration.webtests.BaseWebTest;

public class TagsTest extends BaseWebTest {

    public void testTagK1() {
        clickLink("k1");
        assertElementPresent("vrtx-tagview");
        assertLinkPresentWithText("title k1");
        assertLinkPresentWithText("title k1k2");
    }


    public void testTagK2() {
        clickLink("k2");
        assertElementPresent("vrtx-tagview");
        assertLinkPresentWithText("title k2");
        assertLinkPresentWithText("title k1k2");
    }


    public void testList() {
        clickLink("list");
        assertElementPresent("vrtx-tagview");
        assertTextPresent("No tags specified");
    }
}

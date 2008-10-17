/* Copyright (c) 2008, University of Oslo, Norway
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
package org.vortikal.integration.webtests.view;

import org.vortikal.integration.webtests.BaseWebTest;

public class TagsTest extends BaseWebTest {

    public void testTagK1() {
        clickLink("k1");
        assertElementPresent("vrtx-tagview");
        assertLinkPresentWithText("title k1");
        assertLinkPresentWithText("title k1k2");
        assertLinkPresentWithText("title k1-in-subfolder");
        // TODO We should not show unpublished articles
        assertLinkPresentWithText("title unpublished article");

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


    public void testTagK1InSubfolderScopeFolder() {
        clickLink("k1-subfolder-scope-folder");
        assertElementPresent("vrtx-tagview");
        assertLinkPresentWithText("title k1-in-subfolder");
    }


    public void testTagK2InSubfolderScopeFolder() {
        clickLink("k1-subfolder-scope-folder");
        assertElementPresent("vrtx-tagview");
        assertLinkNotPresentWithText("title k2-in-subfolder ");
        assertTextNotPresent("No resources tagged with tagstest-k2.");
    }


    public void testTagK1InSubfolderNoScope() {
        clickLink("k1-subfolder-no-scope");
        assertElementPresent("vrtx-tagview");
        assertLinkPresentWithText("title k1");
        assertLinkPresentWithText("title k1k2");
        assertLinkPresentWithText("title k1-in-subfolder");
    }


    public void testArticleIntroductionImagePresent() {
        clickLink("k1");
        assertImagePresent("/automatedtestresources/tagstest/article/omuios1.gif",
                "IMG for 'title unpublished article'");

    }
}

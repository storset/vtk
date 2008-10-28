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
        invokeTagsService("resources with keyword1");
        assertLinksPresentWithExactText("title document-with-keyword1", "title document-with-keyword1-and-keyword2",
        		"title document-with-keyword1-in-subfolder");
        assertLinkNotPresentWithExactText("title unpublished-article-with-keyword1");
    }

    public void testTagK2() {
        invokeTagsService("resources with keyword2");
        assertLinksPresentWithExactText("title document-with-keyword2", "title document-with-keyword1-and-keyword2");
    }

    public void testList() {
        invokeTagsService("list tags");
        assertTextPresent("No tags specified");
    }

    public void testTagK1InSubfolderScopeFolder() {
        invokeTagsService("resources in subfolder with keyword1 scope current folder");
        assertLinkPresentWithExactText("title document-with-keyword1-in-subfolder");
    }

    public void testTagK2InSubfolderScopeFolder() {
        invokeTagsService("resources in subfolder with keyword2 scope current folder");
        assertLinkNotPresent("document-with-keyword1-in-subfolder.html");
        assertTextNotPresent("No resources tagged with tagstest-k2.");
    }

    public void testTagK1InSubfolderNoScope() {
        invokeTagsService("resources in subfolder with keyword1 no scope");
        assertLinksPresentWithExactText("title document-with-keyword1", "title document-with-keyword1-and-keyword2",
        		"title document-with-keyword1-in-subfolder");
    }

    private void invokeTagsService(String tagsLink) {
    	clickLinkWithExactText(tagsLink);
        assertElementPresent("vrtx-tagview");
    }
    
    private void assertLinksPresentWithExactText(String... links) {
    	for (int i = 0; i < links.length; i++) {
			assertLinkPresentWithExactText(links[i]);
		}
    }

}

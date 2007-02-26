/* Copyright (c) 2004, University of Oslo, Norway
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
package org.vortikal.web.view.decorating;

import java.util.HashMap;

import junit.framework.TestCase;

import org.springframework.mock.web.MockHttpServletRequest;
import org.vortikal.web.view.MockStringView;
import org.vortikal.web.view.decorating.ContentImpl;
import org.vortikal.web.view.decorating.HtmlHeadDecorator;


public class HtmlHeadContentFilterTest extends TestCase {


    protected void setUp() throws Exception {
        super.setUp();
    }


    public void testEmptyDocument() throws Exception{
        String document = "";
        String headContent = "<title>my title</title>";
        String expectedResult = "";

        String result = runFilter(document, headContent);
        assertEquals(expectedResult, result);
    }
    

    public void testSimpleDocument() throws Exception {
        String document = "<html><head></head><body></body></html>";
        String headContent = "<title>my title</title>";
        String expectedResult =
            "<html><head><title>my title</title></head><body></body></html>";

        String result = runFilter(document, headContent);
        assertEquals(expectedResult, result);
    }
    

    public void testMissingHeadElement() throws Exception{
        String document = "<html><body></body></html>";
        String headContent = "<title>my title</title>";
        String expectedResult = "<html><body></body></html>";

        String result = runFilter(document, headContent);
        assertEquals(expectedResult, result);
    }
    

    public void testBrokenHeadElement() throws Exception{
        String document = "<html><head></head<body></body></html>";
        String headContent = "<title>my title</title>";
        String expectedResult = "<html><head></head<body></body></html>";

        String result = runFilter(document, headContent);
        assertEquals(expectedResult, result);
    }
    

    public void testDoubleHeadElement() throws Exception{
        String document = "<html><head></head><head></head><body></body></html>";
        String headContent = "<title>my title</title>";
        String expectedResult =
            "<html><head><title>my title</title></head><head></head><body></body></html>";

        String result = runFilter(document, headContent);
        assertEquals(expectedResult, result);
    }


    public void testRemoveCharsetElement() throws Exception{
        String document = "<html><head><meta http-equiv=\"Content-Type\" "
            + "content=\"text/html;charset=iso-8859-1\"></head><body></body></html>";
        String headContent = "<title>my title</title>";
        String expectedResult =
            "<html><head><title>my title</title></head><body></body></html>";

        String result = runFilter(document, headContent);
        assertEquals(expectedResult, result);
    }


    public void testRemoveTitleElement() throws Exception{
        String document = "<html><head><title>my first title</title></head>"
            + "<body></body></html>";
        String headContent = "<title>my second title</title>";
        String expectedResult =
            "<html><head><title>my second title</title></head><body></body></html>";
        String result = runFilter(document, headContent);
        assertEquals(expectedResult, result);
    }


    public void testRemoveTitleAndCharsetElement() throws Exception{
        String document = "<html><head><meta http-equiv=\"Content-Type\" "
            + "content=\"text/html;charset=iso-8859-1\">"
            + "<title>my first title</title></head><body></body></html>";
        String headContent = "<title>my second title</title>";
        String expectedResult =
            "<html><head><title>my second title</title></head><body></body></html>";

        String result = runFilter(document, headContent);
        assertEquals(expectedResult, result);
    }


    public void testKeepCharsetElement() throws Exception{
        String document = "<html><head><meta http-equiv=\"Content-Type\" "
            + "content=\"text/html;charset=iso-8859-1\">"
            + "<title>my first title</title></head><body></body></html>";
        String headContent = "<title>my second title</title>";
        String expectedResult =
            "<html><head><meta http-equiv=\"Content-Type\" "
            + "content=\"text/html;charset=iso-8859-1\">"
            + "<title>my second title</title></head><body></body></html>";

        String result = runFilter(document, headContent, true, false);
        assertEquals(expectedResult, result);
    }


    public void testKeepTitleElement() throws Exception{
        String document = "<html><head><title>my first title</title></head>"
            + "<body></body></html>";
        String headContent = "<title>my second title</title>";
        String expectedResult =
            "<html><head><title>my first title</title>"
            + "<title>my second title</title></head><body></body></html>";

        String result = runFilter(document, headContent, false, false);
        assertEquals(expectedResult, result);
    }


    public void testKeepTitleAndCharsetElement() throws Exception{
        String document = "<html><head><meta http-equiv=\"Content-Type\" "
            + "content=\"text/html;charset=iso-8859-1\"><title>my first "
            + "title</title></head><body></body></html>";
        String headContent = "<title>my second title</title>";
        String expectedResult =
            "<html><head><meta http-equiv=\"Content-Type\" "
            + "content=\"text/html;charset=iso-8859-1\"><title>my first "
            + "title</title><title>my second title</title></head><body></body></html>";

        String result = runFilter(document, headContent, false, false);
        assertEquals(expectedResult, result);
    }


    private String runFilter(String document, String headContent,
                             boolean removeTitles, boolean removeCharsets) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();

        MockStringView view = new MockStringView(headContent);
        HtmlHeadDecorator decorator = new HtmlHeadDecorator();
        decorator.setView(view);
        decorator.setRemoveCharsets(removeCharsets);
        decorator.setRemoveTitles(removeTitles);

        decorator.afterPropertiesSet();
        ContentImpl content = new ContentImpl(document);
        decorator.decorate(new HashMap(), request, content);
        return content.getContent();

    }
    

    private String runFilter(String document, String headContent) throws Exception {
        return runFilter(document, headContent, true, true);
    }
    
}

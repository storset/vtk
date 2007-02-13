/* Copyright (c) 2007, University of Oslo, Norway
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
package org.vortikal.web.view.decorating.htmlparser;

import junit.framework.TestCase;
import org.vortikal.web.view.decorating.HtmlElement;
import org.vortikal.web.view.decorating.HtmlPage;


public class SiteMeshHtmlPageParserTestCase extends TestCase {

    private static final String SIMPLE_HTML_PAGE =
        "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">"
        + "<html>"
        + "  <head attr1=\"foo\" attr2=\"bar\">"
        + "    <object>foobar</object>"
        + "    <object>foobbar</object>"
        + "    <object>syt</object>"
        + "    <object><miz><under>foo</under></miz></object>"
        + "    <meta name=\"keywords\" content=\"My keywords\"/>"
        + "    <title>My title</title>"
        + "  </head>"
        + "  <body>The body</body>"
        + "</html>";

    public void testSimpleHtmlPage() throws Exception {
        SiteMeshHtmlPageParser parser = new SiteMeshHtmlPageParser();
        HtmlPage page = parser.parse(SIMPLE_HTML_PAGE.toCharArray());
        assertEquals("html", page.getHtmlElement().getName());
        assertEquals("head", page.getHtmlElement().getChildElements()[0].getName());
        
        assertEquals("My title", page.getTitleElement().getContent());

        assertEquals("My title", page.getHtmlElement().getChildElements()[0]
                     .getChildElements()[5].getContent());

        assertEquals("bar", page.getHtmlElement().getChildElements()[0]
                     .getAttributes()[1].getValue());
    }
    
}

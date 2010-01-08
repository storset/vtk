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
package org.vortikal.util.text;

import junit.framework.TestCase;

public class HtmlUtilTest extends TestCase {
    
    private final static String XHTML_10_TRANS =
        "html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" "
        + "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\"";
    
    private final static String HTML_401_TRANS =
        "HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\""
        + "\"http://www.w3.org/TR/REC-html40/loose.dtd\"";

    public void testGetDocTypeFromBody() throws Exception {
        String html = "<!DOCTYPE " + XHTML_10_TRANS + ">"
            + "<html xmlns=\"http://www.w3.org/1999/xhtml\">"
            + "<head><title>My title</title></head>"
            + "<body>My Body</body></html>";
        String doctype = HtmlUtil.getDoctypeFromBody(html.getBytes("iso-8859-1"));
        assertEquals(XHTML_10_TRANS, doctype);

        html = "<!DOCTYPE " + HTML_401_TRANS + ">"
            + "<html><head><title>My title</title></head>"
            + "<body>My Body</body></html>";

        doctype = HtmlUtil.getDoctypeFromBody(html.getBytes("iso-8859-1"));
        assertEquals(HTML_401_TRANS, doctype);


        html = "<!DOCTYPE some garbage>"
            + "<html><head><title>My title</title></head>"
            + "<body>My Body</body></html>";

        doctype = HtmlUtil.getDoctypeFromBody(html.getBytes("iso-8859-1"));
        assertEquals("some garbage", doctype);
    }
    
    public void testGetCharacterEncodingFromBody() throws Exception {
        String html = "<!DOCTYPE " + XHTML_10_TRANS + ">"
            + "<html xmlns=\"http://www.w3.org/1999/xhtml\">"
            + "<head><title>My title</title>"
            + "<meta http-equiv=\"Content-Type\" value=\"text/html;charset=utf-8\"/>"
            + "</head>"
            + "<body>My Body</body></html>";
        
        String characterEncoding = HtmlUtil.getCharacterEncodingFromBody(html.getBytes("utf-8"));
        assertEquals("utf-8", characterEncoding);

        html = "<!DOCTYPE " + HTML_401_TRANS + ">"
            + "<html><head><title>My title</title></head>"
            + "<body>My Body</body></html>";
        characterEncoding = HtmlUtil.getCharacterEncodingFromBody(html.getBytes("utf-8"));
        assertNull(characterEncoding);
    }
    
}


/* Copyright (c) 2009, University of Oslo, Norway
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
package org.vortikal.web.service;

import java.util.Map;

import junit.framework.TestCase;

import org.vortikal.repository.Path;

public class URLTest extends TestCase {

    public void testParse() {
        try {
            String s = null;
            URL.parse(s);
            fail("Parsed malformed URL: " + s);
        } catch (Exception e) { }
        try {
            String s = "/";
            URL.parse(s);
            fail("Parsed malformed URL: " + s);
        } catch (Exception e) { }
        try {
            String s = "http;//";
            URL.parse(s);
            fail("Parsed malformed URL: " + s);
        } catch (Exception e) { }
        try {
            String s = "http:///";
            URL.parse(s);
            fail("Parsed malformed URL: " + s);
        } catch (Exception e) { }
 
        URL url = URL.parse("http://foo");
        assertEquals(Integer.valueOf(80), url.getPort());
        assertEquals(Path.ROOT, url.getPath());
        assertTrue(url.isCollection());
        assertEquals("http://foo/", url.toString());
        
        url = URL.parse("http://ExamPLE.COM");
        assertEquals("example.com", url.getHost());

        url = URL.parse("http://foo?param=value");
        assertEquals(Path.ROOT, url.getPath());
        assertEquals("value", url.getParameter("param"));
        assertTrue(url.isCollection());
        
        url = URL.parse("http://foo/?param=value");
        assertEquals(Path.ROOT, url.getPath());
        assertEquals("value", url.getParameter("param"));
        assertTrue(url.isCollection());

        url = URL.parse("http://foo//bar");
        
        url = URL.parse("http://foo.bar:8080");
        assertEquals(Path.ROOT, url.getPath());
        
        url = URL.parse("http://127.0.0.1:8080?param=val");
        assertEquals("127.0.0.1", url.getHost());
        
        url = URL.parse("http://abc_0-foo.xx0.yy0.zz1:8080?param=val");
        assertEquals("abc_0-foo.xx0.yy0.zz1", url.getHost());

        url = URL.parse("http://foo.bar:8080/baz");
        assertEquals("foo.bar", url.getHost());
        assertEquals(Integer.valueOf(8080), url.getPort());
        assertEquals("http", url.getProtocol());
        assertEquals(Path.fromString("/baz"), url.getPath());
        assertFalse(url.isCollection());

        url = URL.parse("http://foo.bar:8080/baz/?param1&param2=abc");
        assertEquals("", url.getParameter("param1"));
        assertEquals("abc", url.getParameter("param2"));
        assertEquals("http://foo.bar:8080/baz/?param1&param2=abc", url.toString());
        
        url = URL.parse("http://foo.bar:8080/baz/?xyz=abc");
        assertEquals(Integer.valueOf(8080), url.getPort());
        assertEquals("abc", url.getParameter("xyz"));
        
        url = URL.parse("http://foo.bar:8080/baz/?xyz=abc#ref");
        assertEquals(Path.fromString("/baz"), url.getPath());
        assertEquals("ref", url.getRef());
        
        
        url = URL.parse("rtmp://foo");
        assertEquals(Integer.valueOf(80), url.getPort());
        assertEquals(Path.ROOT, url.getPath());
        assertTrue(url.isCollection());
        assertEquals("rtmp://foo/", url.toString());
        
        url = URL.parse("rtmp://foo");
        assertEquals(Integer.valueOf(80), url.getPort());
        assertEquals(Path.ROOT, url.getPath());
        assertTrue(url.isCollection());
        assertEquals("rtmp://foo/", url.toString());
        
        url = URL.parse("rtmp://foo:9322");
        assertEquals(Integer.valueOf(9322), url.getPort());
        assertEquals(Path.ROOT, url.getPath());
        assertTrue(url.isCollection());
        assertEquals("rtmp://foo:9322/", url.toString());
        
        // real rtmp URL
        String rtmpUrl = "rtmp://stream-prod01.uio.no/vod/mp4:uio/intermedia/rektor/rektor_jub.mp4";
        url = URL.parse(rtmpUrl);
        assertEquals(rtmpUrl, url.toString());
    }

    public void testSplitQueryString() {
        String testQueryString = "?key1=val1&key1=val2&key2=val%203";
        Map<String, String[]> splitValues = URL.splitQueryString(testQueryString);
        String[] param1 = splitValues.get("key1");
        assertEquals(2, param1.length);
        assertEquals("val1", param1[0]);
        assertEquals("val2", param1[1]);

        String[] param2 = splitValues.get("key2");
        assertEquals(1, param2.length);
        assertEquals("val%203", param2[0]);
    }

    
    public void testEncode() {
        assertEquals("%21", URL.encode("!"));
        //assertEquals("%2A", URL.encode("*"));
        assertEquals("%22", URL.encode("\""));
        assertEquals("%27", URL.encode("'"));
        assertEquals("%28", URL.encode("("));
        assertEquals("%29", URL.encode(")"));
        assertEquals("%3B", URL.encode(";"));
        assertEquals("%3A", URL.encode(":"));
        assertEquals("%40", URL.encode("@"));
        assertEquals("%26", URL.encode("&"));
        assertEquals("%3D", URL.encode("="));
        assertEquals("%2B", URL.encode("+"));
        assertEquals("%24", URL.encode("$"));
        assertEquals("%2C", URL.encode(","));
        assertEquals("%2F", URL.encode("/"));
        assertEquals("%3F", URL.encode("?"));
        assertEquals("%25", URL.encode("%"));
        assertEquals("%23", URL.encode("#"));
        assertEquals("%5B", URL.encode("["));
        assertEquals("%5D", URL.encode("]"));
        
        URL url = new URL("http", "foo.bar", Path.fromString("/ "));
        assertEquals("/%20", url.getPathEncoded());
        url.setPath(Path.fromString("/)"));
        assertEquals("/%29", url.getPathEncoded());

        url = new URL("http", "foo.bar", Path.fromString("/%20"));
        assertEquals("http://foo.bar/%2520", url.toString());
    }

    public void testDecode() {
        assertEquals("\u2664", URL.decode("%E2%99%A4"));
        URL url = URL.parse("http://foo.bar/abc%28def%29?xyz=2%2B2%3D3");
        assertEquals(Path.fromString("/abc(def)"), url.getPath());
        assertEquals("2+2=3", url.getParameter("xyz"));
        url = URL.parse("http://foo.bar/%2520");
        assertEquals("/%20", url.getPath().toString());
    }
    
    public void testRelative() {
        URL url = URL.parse("http://a/b/c/d?q#f");
                
        assertEquals("http://a/b/c/g", url.relativeURL("g").toString());
        assertEquals("http://a/b/c/g", url.relativeURL("./g").toString());
        assertEquals("http://a/b/c/g/", url.relativeURL("g/").toString());
        assertEquals("http://a/g", url.relativeURL("/g").toString());
        assertEquals("http://a/g/", url.relativeURL("/g/").toString());
        assertEquals("http://g/", url.relativeURL("//g").toString());
        assertEquals("http://g/?y", url.relativeURL("//g/?y").toString());
        assertEquals("http://a/b/c/d?y", url.relativeURL("?y").toString());
        assertEquals("http://a/b/c/d?y", url.relativeURL("?y").toString());
        assertEquals("http://a/b/c/d?q#s", url.relativeURL("#s").toString());
        assertEquals("http://a/b/c/", url.relativeURL(".").toString());
        assertEquals("http://a/b/c/", url.relativeURL("./").toString());
        assertEquals("http://a/b/", url.relativeURL("..").toString());
        assertEquals("http://a/b/", url.relativeURL("../").toString());
        assertEquals("http://a/b/g", url.relativeURL("../g").toString());
        assertEquals("http://a/", url.relativeURL("../..").toString());
        assertEquals("http://a/", url.relativeURL("../../").toString());
        assertEquals("http://a/g", url.relativeURL("../../g").toString());
        assertEquals("http://a/b/c/d?q#f", url.relativeURL("").toString());
        
        url = URL.parse("http://a/b/");
        assertEquals("http://a/b/?x=y", url.relativeURL("./?x=y").toString());

        url = URL.parse("http://a/b/");
        assertEquals("http://a/c/", url.relativeURL("        ../c/     ").toString());
        assertEquals("http://a/b/%c3%a6", url.relativeURL("%c3%a6").toString().toLowerCase());
    }
    
    public void testIsRelative() {
        assertFalse(URL.isRelativeURL("http://foo.bar"));
        assertFalse(URL.isRelativeURL("mailto:xyz@example.com?subject=foo&body=bar"));
        assertFalse(URL.isRelativeURL("ftp://ftp.example.com/"));
        assertTrue(URL.isRelativeURL("a/b/c"));
        assertTrue(URL.isRelativeURL("test.html?foo=bar:baaz"));
    }
    
    public void testIsEncoded() {
        assertFalse(URL.isEncoded("http://www.uio.no/detteÆerikkeencoda"));
        assertFalse(URL.isEncoded("http://www.uio.no/dette%erikkeencoda"));
        assertTrue(URL.isEncoded("http://www.uio.no/dette%20erencoda"));
        assertTrue(URL.isEncoded("http://www.uio.no/?"));
        assertFalse(URL.isEncoded("http://www.uio.no/er dette encoda?"));
    }
    
}

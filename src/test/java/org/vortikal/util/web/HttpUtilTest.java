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
package org.vortikal.util.web;

import junit.framework.TestCase;

public class HttpUtilTest extends TestCase {


    public void testExtractHeaderField() {

        String testHeader = 
            "Authorization: Digest username=\"user@localhost\", " 
            + "   realm=\"Vortex\", " 
            + "nonce=\"MjAwNy0wNC0xMCAxMTowNDozNGU2ZTIwYjcxOWQwNmRkYzNhYzVhZTJlZGUzOGM5NzVm\", " 
            + "uri=\"/test/?vrtx=admin\", " 
            + "algorithm=MD5, " 
            + "response=\"26b9036106ed75b0b649f67c9d918e11\" , " 
            + "opaque=\"0c629b3f-8188-496c-adcf-9cc6a4d012f9\", " 
            + "qop=auth, nc=0000000d, cnonce=\"e01111b3d155e483\"";

        String testHeaderValue= testHeader.substring(
                                            "Authorization: Digest".length(), 
                                            testHeader.length());

        String username = HttpUtil.extractHeaderField(testHeaderValue, "username");
        String realm = HttpUtil.extractHeaderField(testHeaderValue, "realm");
        String nonce = HttpUtil.extractHeaderField(testHeaderValue, "nonce");
        String uri = HttpUtil.extractHeaderField(testHeaderValue, "uri");
        String algorithm = HttpUtil.extractHeaderField(testHeaderValue, "algorithm");
        String response = HttpUtil.extractHeaderField(testHeaderValue, "response");
        String opaque = HttpUtil.extractHeaderField(testHeaderValue, "opaque");
        String qop = HttpUtil.extractHeaderField(testHeaderValue, "qop");
        String nc = HttpUtil.extractHeaderField(testHeaderValue, "nc");
        String cnonce = HttpUtil.extractHeaderField(testHeaderValue, "cnonce");

        assertEquals("user@localhost", username);
        assertEquals("Vortex", realm);
        assertEquals("MjAwNy0wNC0xMCAxMTowNDozNGU2ZTIwYjcxOWQwNmRkYzNhYzVhZTJlZGUzOGM5NzVm", nonce);
        assertEquals("/test/?vrtx=admin", uri);
        assertEquals("MD5", algorithm);
        assertEquals("26b9036106ed75b0b649f67c9d918e11", response);
        assertEquals("0c629b3f-8188-496c-adcf-9cc6a4d012f9", opaque);
        assertEquals("auth", qop);
        assertEquals("0000000d", nc);
        assertEquals("e01111b3d155e483", cnonce);
        
        testHeaderValue = " bar=baz";
        String value = HttpUtil.extractHeaderField(testHeaderValue, "bar");
        assertEquals("baz", value);
        
        testHeaderValue = "bar=baz";
        value = HttpUtil.extractHeaderField(testHeaderValue, "bar");
        assertEquals("baz", value);
        
        testHeaderValue = "bar=baz";
        value = HttpUtil.extractHeaderField(testHeaderValue, "baz");
        assertNull(value);
        
        testHeaderValue = " r=test";
        value = HttpUtil.extractHeaderField(testHeaderValue, "r");
        assertEquals("test", value);
        
        testHeaderValue = "";
        value = HttpUtil.extractHeaderField(testHeaderValue, "foo");
        assertNull(value);
        
        testHeaderValue = "nano=\" pico, pico, pico \", pico=2";
        assertEquals(" pico, pico, pico ", HttpUtil.extractHeaderField(testHeaderValue, "nano"));
        assertEquals("2", HttpUtil.extractHeaderField(testHeaderValue, "pico"));

    }
}

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
package org.vortikal.web.filter;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

public class StandardRequestFilterTest {

    @Test
    public void testFilterRequest() {

        StandardRequestFilter filter = new StandardRequestFilter();
        Map<String, String> replacements = new HashMap<String, String>();
        replacements.put("\\+", "%2B");
        filter.setUrlReplacements(replacements);
        
        HttpServletRequest filtered = filter.filterRequest(new MockHttpServletRequest("GET", "/foo/bar"));
        assertEquals("/foo/bar", filtered.getRequestURI());

        filtered = filter.filterRequest(new MockHttpServletRequest("GET", "/%20"));
        assertEquals("/", filtered.getRequestURI());

        filtered = filter.filterRequest(new MockHttpServletRequest("GET", "/foo/bar/file+2.txt"));
        assertEquals("/foo/bar/file%2B2.txt", filtered.getRequestURI());
        
        filtered = filter.filterRequest(new MockHttpServletRequest("GET", "/foo/bar/i am a file with spaces.txt"));
        assertEquals("/foo/bar/i%20am%20a%20file%20with%20spaces.txt", filtered.getRequestURI());

        filtered = filter.filterRequest(new MockHttpServletRequest("GET", "/"));
        assertEquals("/", filtered.getRequestURI());

        filtered = filter.filterRequest(new MockHttpServletRequest("GET", ""));
        assertEquals("/", filtered.getRequestURI());
        
//        filtered = filter.filterRequest(new MockHttpServletRequest("OPTIONS", "*"));
//        assertEquals("/", filtered.getRequestURI());
//        assertEquals("OPTIONS", filtered.getMethod());

        try {
            filtered = filter.filterRequest(new MockHttpServletRequest("OPTIONS", "%")); // Invalid request
            throw new IllegalStateException("Should not pass");
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

}

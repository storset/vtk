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
package org.vortikal.web.decorating;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import junit.framework.TestCase;

import org.vortikal.repository.Path;
import org.vortikal.web.decorating.PathMappingConfig.ConfigEntry;

public class PathMappingConfigTest extends TestCase {

    private static final String TEST_CONFIG = 
 
        "/ = root\n"
        + "/a = value-a\n"
        + "/a[d:e,f:g] = value-a2\n"
        + "# comment\n"
        + "\t\t# comment\n"
        + "/b = value-b # comment\n"
        + "/c = value-c\n"
        + "/c/d# = value d\n"
        + "/c/d/e/f/g/ = foo\n"
        + "/c/d/e/f/g/h = bar\n";
       
    
    public void testConfig() throws Exception {
        InputStream is = new ByteArrayInputStream(TEST_CONFIG.getBytes("utf-8"));
        PathMappingConfig config = new PathMappingConfig(is);

        assertNotNull(config.get(Path.fromString("/")));
        assertNull(config.get(Path.fromString("/unknown")));
        
        List<ConfigEntry> entries = config.get(Path.fromString("/a"));
        assertEquals(2, entries.size());
        assertEquals("value-a", entries.get(0).getValue());
        assertEquals(0, entries.get(0).getPredicates().size());
        assertEquals("value-a2", entries.get(1).getValue());
        assertEquals(2, entries.get(1).getPredicates().size());
        assertEquals("d", entries.get(1).getPredicates().get(0).getName());
        assertEquals("e", entries.get(1).getPredicates().get(0).getValue());
        assertEquals("f", entries.get(1).getPredicates().get(1).getName());
        assertEquals("g", entries.get(1).getPredicates().get(1).getValue());

        assertNotNull(config.get(Path.fromString("/c")));
        assertNull(config.get(Path.fromString("/c/d")));
        assertNull(config.get(Path.fromString("/c/d/e")));
        assertNull(config.get(Path.fromString("/c/d/e/f")));

        assertNotNull(config.get(Path.fromString("/c/d/e/f/g")));
        assertTrue(config.get(Path.fromString("/c/d/e/f/g")).get(0).isExact());
        assertNotNull(config.get(Path.fromString("/c/d/e/f/g/h")));
        assertFalse(config.get(Path.fromString("/c/d/e/f/g/h")).get(0).isExact());

        assertNull(config.get(Path.fromString("/c/d/e/f/g/h/i")));
        
    }

}

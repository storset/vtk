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
package org.vortikal.util.text;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import junit.framework.TestCase;

import org.vortikal.repository.Path;
import org.vortikal.util.text.PathMappingConfig.ConfigEntry;


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
        + "/c/d/e/f/g/ = exact\n"
        + "/c/d/e/f/g/h = bar\n"
        + "/x/y/z = xxx\n"
        + "/x/y/z[a:b,c:d]/ = exact\n"
        + "/esc/rhs = \\a\\b\\=";
    
    public void testGet() throws Exception {
        InputStream is = new ByteArrayInputStream(TEST_CONFIG.getBytes("utf-8"));
        PathMappingConfig config = new PathMappingConfig(is);

        assertNotNull(config.get(Path.fromString("/")));
        assertNull(config.get(Path.fromString("/unknown")));
        
        List<ConfigEntry> entries = config.get(Path.fromString("/a"));
        assertEquals(2, entries.size());
        assertEquals("value-a", entries.get(0).getValue());
        assertEquals(0, entries.get(0).getQualifiers().size());
        assertEquals("value-a2", entries.get(1).getValue());
        assertEquals(2, entries.get(1).getQualifiers().size());
        assertEquals("d", entries.get(1).getQualifiers().get(0).getName());
        assertEquals("e", entries.get(1).getQualifiers().get(0).getValue());
        assertEquals("f", entries.get(1).getQualifiers().get(1).getName());
        assertEquals("g", entries.get(1).getQualifiers().get(1).getValue());

        assertNotNull(config.get(Path.fromString("/c")));
        assertNull(config.get(Path.fromString("/c/d")));
        assertNull(config.get(Path.fromString("/c/d/e")));
        assertNull(config.get(Path.fromString("/c/d/e/f")));

        assertNotNull(config.get(Path.fromString("/c/d/e/f/g")));
        assertTrue(config.get(Path.fromString("/c/d/e/f/g")).get(0).isExact());
        assertNotNull(config.get(Path.fromString("/c/d/e/f/g/h")));
        assertFalse(config.get(Path.fromString("/c/d/e/f/g/h")).get(0).isExact());

        assertNull(config.get(Path.fromString("/c/d/e/f/g/h/i")));
        
        assertNotNull(config.get(Path.fromString("/x/y/z")));
        assertTrue(config.get(Path.fromString("/x/y/z")).get(1).isExact());
        
        entries = config.get(Path.fromString("/esc/rhs"));
        assertEquals(1, entries.size());
        assertEquals("\\a\\b=", entries.get(0).getValue());
    }
    
    public void testGetMatchAncestors() throws Exception {
        String testConfig = 
                  "// = The Root Resource exactly\n"
                + "/[lunarPhase:full moon]/ = Full Moon\n"
                + "/ = Default\n"
                + "/a[] =   The A area  \n"
                + "/a # no effect\n"
                + "/a/b[x:y]/ = The AB value in case of x=y\n"
                + "/a/b/[z:1] = The AB value in case of z=1\n"
                + "/E/ = Exactly E\n"
                + "/a/b/c = The ABC area\n"
                + "/a/b/c[foo\\:bar:baz] = The ABC area in case of foo:bar=baz\n"
                + "/a/b/c/d/ = Exactly ABCD\n";

        InputStream is = new ByteArrayInputStream(testConfig.getBytes("utf-8"));
        PathMappingConfig config = new PathMappingConfig(is);
        
        // For "/"
        List<ConfigEntry> entries = config.getMatchAncestor(Path.fromString("/"));
        assertEquals(3, entries.size());
        assertTrue(entries.get(0).isExact());
        assertEquals("The Root Resource exactly", entries.get(0).getValue());
        assertEquals(Path.ROOT, entries.get(0).getPath());
        assertTrue(entries.get(0).getQualifiers().isEmpty());
        
        assertTrue(entries.get(1).isExact());
        assertEquals("Full Moon", entries.get(1).getValue());
        assertEquals(Path.ROOT, entries.get(1).getPath());
        assertEquals(1, entries.get(1).getQualifiers().size());
        assertEquals("lunarPhase", entries.get(1).getQualifiers().get(0).getName());
        assertEquals("full moon", entries.get(1).getQualifiers().get(0).getValue());

        assertFalse(entries.get(2).isExact());
        assertEquals("Default", entries.get(2).getValue());
        assertEquals(Path.ROOT, entries.get(2).getPath());
        assertTrue(entries.get(2).getQualifiers().isEmpty());
        
        // For "/unknown"
        entries = config.getMatchAncestor(Path.fromString("/unknown"));
        assertEquals(1, entries.size());
        assertFalse(entries.get(0).isExact());
        assertEquals("Default", entries.get(0).getValue());
        assertEquals(Path.ROOT, entries.get(0).getPath());
        assertTrue(entries.get(0).getQualifiers().isEmpty());
        
        // For "/a"
        entries = config.getMatchAncestor(Path.fromString("/a"));
        assertEquals(1, entries.size());
        assertFalse(entries.get(0).isExact());
        assertEquals("The A area", entries.get(0).getValue());
        assertEquals(Path.fromString("/a"), entries.get(0).getPath());
        assertTrue(entries.get(0).getQualifiers().isEmpty());

        // For "/a/1/2/3/unknown"
        entries = config.getMatchAncestor(Path.fromString("/a/1/2/3/unknown"));
        assertEquals(1, entries.size());
        assertFalse(entries.get(0).isExact());
        assertEquals("The A area", entries.get(0).getValue());
        assertEquals(Path.fromString("/a"), entries.get(0).getPath());
        assertTrue(entries.get(0).getQualifiers().isEmpty());

        // For "/a/b"
        entries = config.getMatchAncestor(Path.fromString("/a/b"));
        assertEquals(2, entries.size());
        assertTrue(entries.get(0).isExact());
        assertEquals("The AB value in case of x=y", entries.get(0).getValue());
        assertEquals(Path.fromString("/a/b"), entries.get(0).getPath());
        assertEquals(1, entries.get(0).getQualifiers().size());
        assertEquals("x", entries.get(0).getQualifiers().get(0).getName());
        assertEquals("y", entries.get(0).getQualifiers().get(0).getValue());
        
        assertTrue(entries.get(1).isExact());
        assertEquals("The AB value in case of z=1", entries.get(1).getValue());
        assertEquals(Path.fromString("/a/b"), entries.get(1).getPath());
        assertEquals(1, entries.get(1).getQualifiers().size());
        assertEquals("z", entries.get(1).getQualifiers().get(0).getName());
        assertEquals("1", entries.get(1).getQualifiers().get(0).getValue());
        
        // For "/E"
        entries = config.getMatchAncestor(Path.fromString("/E"));
        assertEquals(1, entries.size());
        assertTrue(entries.get(0).isExact());
        assertEquals("Exactly E", entries.get(0).getValue());
        assertEquals(Path.fromString("/E"), entries.get(0).getPath());
        assertTrue(entries.get(0).getQualifiers().isEmpty());

        // For "/E/x" (gets config from "/", skips exact config for "/E")
        entries = config.getMatchAncestor(Path.fromString("/E/x"));
        assertEquals(1, entries.size());
        assertFalse(entries.get(0).isExact());
        assertEquals("Default", entries.get(0).getValue());
        assertEquals(Path.fromString("/"), entries.get(0).getPath());
        assertTrue(entries.get(0).getQualifiers().isEmpty());
        
        // For /a/b/c/f (should inherit from /a/b/c)
        entries = config.getMatchAncestor(Path.fromString("/a/b/c/f"));
        assertEquals(2, entries.size());
        assertFalse(entries.get(0).isExact());
        assertEquals("The ABC area", entries.get(0).getValue());
        assertEquals(Path.fromString("/a/b/c"), entries.get(0).getPath());
        assertTrue(entries.get(0).getQualifiers().isEmpty());
        
        assertFalse(entries.get(1).isExact());
        assertEquals("The ABC area in case of foo:bar=baz", entries.get(1).getValue());
        assertEquals(Path.fromString("/a/b/c"), entries.get(1).getPath());
        assertEquals(1, entries.get(1).getQualifiers().size());
        assertEquals("foo:bar", entries.get(1).getQualifiers().get(0).getName());
        assertEquals("baz", entries.get(1).getQualifiers().get(0).getValue());
        
        // For /a/b/c/d
        entries = config.getMatchAncestor(Path.fromString("/a/b/c/d"));
        assertEquals(1, entries.size());
        assertTrue(entries.get(0).isExact());
        assertEquals("Exactly ABCD", entries.get(0).getValue());
        assertEquals(Path.fromString("/a/b/c/d"), entries.get(0).getPath());
        assertTrue(entries.get(0).getQualifiers().isEmpty());
        
    }

}

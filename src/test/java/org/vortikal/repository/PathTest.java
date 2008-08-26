package org.vortikal.repository;

import junit.framework.TestCase;

public class PathTest extends TestCase {

    public void testPaths() {

        assertInvalid(null);
        assertInvalid("");
        assertInvalid(" ");
        assertInvalid("invalid.path");
        assertInvalid("/invalid.path/..");
        assertInvalid("/invalid.path/.");
        assertInvalid("/invalid//path");
        assertInvalid("/invalid/../path");
        assertInvalid("/invalid.path/");
        assertInvalid(getString("i", 1500));
        
        Path p = Path.fromString("/");
        assertEquals(p.toString(), "/");
        assertTrue(p.isRoot());
        assertEquals(p, Path.ROOT);
        assertEquals("/", p.getName());
        assertEquals(0, p.getDepth());
        assertNull(p.getParent());
        assertEquals(0, p.getAncestors().size());
        
        p = Path.fromString("/a/b/c/d");
        assertEquals(p.toString(), "/a/b/c/d");
        assertFalse(p.isRoot());
        assertEquals("d", p.getName());
        assertEquals(5, p.getElements().size());
        assertEquals(5, p.getPaths().size());
        assertEquals(4, p.getDepth());

        assertEquals(4, p.getAncestors().size());

        Path p2 = Path.fromString("/a/b/c/d");
        assertEquals(p, p2);
        assertEquals(p.hashCode(), p2.hashCode());

        p2 = Path.fromString("/b/c/d");
        assertTrue(p2.compareTo(p) > 0);
        
        assertEquals("/", p.getElements().get(0));
        assertEquals("a", p.getElements().get(1));
        assertEquals("b", p.getElements().get(2));
        assertEquals("c", p.getElements().get(3));
        assertEquals("d", p.getElements().get(4));

        assertEquals("c", p.getParent().getElements().get(3));
        
        assertTrue(p.isAncestorOf(Path.fromString("/a/b/c/d/e")));
        assertFalse(p.isAncestorOf(Path.fromString("/a/b/c/e")));
 
        assertEquals(p.extend("e"), Path.fromString("/a/b/c/d/e"));
        assertEquals(p.extend("e/f/g"), Path.fromString("/a/b/c/d/e/f/g"));
    }
    
    private void assertInvalid(String path) {
        try {
            Path.fromString(path);
            fail("Invalid path constructed: '" + path + "'");
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }
    
    private String getString(String chr, int length) {
        StringBuilder sb = new StringBuilder("/");
        for (int i = 1; i < length; i++) {
            sb.append(chr);
        }
        return sb.toString();
    }

    

}

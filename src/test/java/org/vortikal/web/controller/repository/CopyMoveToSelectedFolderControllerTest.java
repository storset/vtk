package org.vortikal.web.controller.repository;

import org.vortikal.repository.Path;

import junit.framework.TestCase;

public class CopyMoveToSelectedFolderControllerTest extends TestCase {

    public void testAppendCopySuffix() {
        Path path = Path.fromString("/lala.html");
        String expectedString = "/lala(1).html";
        Path expected = Path.fromString(expectedString);
        Path copy = CopyMoveToSelectedFolderController.appendCopySuffix(path);
        assertEquals(expectedString, copy.toString());
        assertEquals(expected, copy);
    }
    
}

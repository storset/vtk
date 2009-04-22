package org.vortikal.web.controller.repository;

import junit.framework.TestCase;

import org.vortikal.repository.Path;

public class CopyMoveToSelectedFolderControllerTest extends TestCase {
    
    private CopyMoveToSelectedFolderController controller;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        controller = new CopyMoveToSelectedFolderController();
    }

    public void testAppendCopySuffix() {
        Path original = Path.fromString("/lala.html");
        
        Path firstCopy = controller.appendCopySuffix(original, 1);
        assertEquals(firstCopy, Path.fromString("/lala(1).html"));
        
        Path secondCopy = controller.appendCopySuffix(firstCopy, 1);
        assertEquals(secondCopy, Path.fromString("/lala(2).html"));
        
        Path thirdCopy = controller.appendCopySuffix(secondCopy, 1);
        assertEquals(thirdCopy, Path.fromString("/lala(3).html"));
        
        Path parenthesisName = Path.fromString("/test/te(3)st.xml");
        Path copiedParantis = controller.appendCopySuffix(parenthesisName, 3);
        assertEquals(copiedParantis, Path.fromString("/test/te(3)st(3).xml"));
    }
    
}

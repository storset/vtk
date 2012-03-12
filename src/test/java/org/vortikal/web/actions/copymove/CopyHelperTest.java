package org.vortikal.web.actions.copymove;

import junit.framework.TestCase;

import org.vortikal.repository.Path;

public class CopyHelperTest extends TestCase {
    
    private CopyHelper copyHelper;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        copyHelper = new CopyHelper();
    }

    public void testAppendCopySuffix() {
        Path original = Path.fromString("/lala.html");
        
        Path firstCopy = copyHelper.appendCopySuffix(original, 1, false);
        assertEquals(firstCopy, Path.fromString("/lala(1).html"));
        
        Path secondCopy = copyHelper.appendCopySuffix(firstCopy, 1, false);
        assertEquals(secondCopy, Path.fromString("/lala(2).html"));
        
        Path thirdCopy = copyHelper.appendCopySuffix(secondCopy, 1, false);
        assertEquals(thirdCopy, Path.fromString("/lala(3).html"));
        
        Path parenthesisName = Path.fromString("/test/te(3)st.xml");
        Path copiedParenthesis = copyHelper.appendCopySuffix(parenthesisName, 3, false);
        assertEquals(copiedParenthesis, Path.fromString("/test/te(3)st(3).xml"));
        
        Path copyToDoubleDigit = Path.fromString("/foo(9).html");
        Path firstToDoubleDigitCopy = copyHelper.appendCopySuffix(copyToDoubleDigit, 1, false);
        assertEquals(firstToDoubleDigitCopy, Path.fromString("/foo(10).html"));
    }
    
}

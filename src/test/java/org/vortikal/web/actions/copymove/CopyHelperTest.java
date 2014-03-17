package org.vortikal.web.actions.copymove;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import org.vortikal.repository.Path;

public class CopyHelperTest {
    
    private CopyHelper copyHelper;
    
    @Before
    public void setUp() throws Exception {
        copyHelper = new CopyHelper();
    }

    @Test
    public void appendCopySuffix() {
        Path original = Path.fromString("/lala.html");
        
        Path firstCopy = copyHelper.appendCopySuffix(original, 1);
        assertEquals(firstCopy, Path.fromString("/lala(1).html"));
        
        Path secondCopy = copyHelper.appendCopySuffix(firstCopy, 1);
        assertEquals(secondCopy, Path.fromString("/lala(2).html"));
        
        Path thirdCopy = copyHelper.appendCopySuffix(secondCopy, 1);
        assertEquals(thirdCopy, Path.fromString("/lala(3).html"));
        
        Path parenthesisName = Path.fromString("/test/te(3)st.xml");
        Path copiedParenthesis = copyHelper.appendCopySuffix(parenthesisName, 3);
        assertEquals(copiedParenthesis, Path.fromString("/test/te(3)st(3).xml"));
        
        Path copyToDoubleDigit = Path.fromString("/foo(9).html");
        Path firstToDoubleDigitCopy = copyHelper.appendCopySuffix(copyToDoubleDigit, 1);
        assertEquals(firstToDoubleDigitCopy, Path.fromString("/foo(10).html"));
    }
    
}

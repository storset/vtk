package org.vortikal.repositoryimpl.query;

import java.io.IOException;
import java.io.StringReader;

import junit.framework.TestCase;

import org.apache.lucene.analysis.Token;

public class EscapedMultiValueFieldTokenizerTestCase extends TestCase {
    
    private char splitChar = ';';
    
    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testTokenizer() throws IOException {
        
        String testString = 
            ";;value1;value2;value3;;;;value4;;two escaped semicolons: \\;\\;;lastValue";
        
        EscapedMultiValueFieldTokenizer tokenizer = 
            new EscapedMultiValueFieldTokenizer(new StringReader(testString), splitChar);
        
        Token t = tokenizer.next();
        assertEquals("value1", t.termText());
        assertEquals(2, t.startOffset());
        assertEquals(8, t.endOffset());
        
        t = tokenizer.next();
        assertEquals("value2", t.termText());
        assertEquals(9, t.startOffset());
        assertEquals(15, t.endOffset());
        
        t = tokenizer.next();
        assertEquals("value3", t.termText());
        assertEquals(16, t.startOffset());
        assertEquals(22, t.endOffset());
        
        t = tokenizer.next();
        assertEquals("value4", t.termText());
        assertEquals(26, t.startOffset());
        assertEquals(32, t.endOffset());
        
        t = tokenizer.next();
        assertEquals("two escaped semicolons: ;;", t.termText());
        assertEquals(34, t.startOffset());
        assertEquals(62, t.endOffset());

        t = tokenizer.next();
        assertEquals("lastValue", t.termText());
        assertEquals(63, t.startOffset());
        assertEquals(72, t.endOffset());

        t = tokenizer.next();
        assertNull(t);
        
    }
    
    public void testEmptyInput() throws IOException {

        EscapedMultiValueFieldTokenizer tokenizer = 
            new EscapedMultiValueFieldTokenizer(new StringReader(""), splitChar);        
        
        Token t = tokenizer.next();
        assertNull(t);
    }
    
    public void testOnlySplitCharsInput() throws IOException {
        
        EscapedMultiValueFieldTokenizer tokenizer = 
            new EscapedMultiValueFieldTokenizer(new StringReader(";;;;;;;;;;;"), splitChar);        
        
        Token t = tokenizer.next();
        assertNull(t);
        
        
    }
    
}

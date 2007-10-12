package org.vortikal.repository.index.mapping;

import junit.framework.TestCase;

import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.vortikal.repository.index.mapping.StringArrayTokenStream;

public class StringArrayTokenStreamTestCase extends TestCase {

    public void testMultipleValues() throws Exception {
        
        String[] values = new String[]{ "foo", "bar", "hepp", "hopp" };
        TokenStream stream = new StringArrayTokenStream(values);
        
        Token token = stream.next();
        assertEquals("foo", token.termText());
        assertEquals(0, token.startOffset());
        assertEquals(3, token.endOffset());
        
        token = stream.next();
        assertEquals("bar", token.termText());
        assertEquals(3, token.startOffset());
        assertEquals(6, token.endOffset());
        
        token = stream.next();
        assertEquals("hepp", token.termText());
        assertEquals(6, token.startOffset());
        assertEquals(10, token.endOffset());
        
        token = stream.next();
        assertEquals("hopp", token.termText());
        assertEquals(10, token.startOffset());
        assertEquals(14, token.endOffset());
        
        token = stream.next();
        assertNull(token);
    }
    
    public void testZeroValues() throws Exception {
        
        TokenStream stream = new StringArrayTokenStream(new String[]{});
        assertNull(stream.next());
    }
    
}

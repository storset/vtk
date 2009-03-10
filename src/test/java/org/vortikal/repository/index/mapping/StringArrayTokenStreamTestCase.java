package org.vortikal.repository.index.mapping;

import junit.framework.TestCase;

import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.vortikal.repository.index.mapping.StringArrayTokenStream;

public class StringArrayTokenStreamTestCase extends TestCase {

    public void testMultipleValues() throws Exception {
        
        String[] values = new String[]{ "foo", "bar", "hepp", "hopp" };
        TokenStream stream = new StringArrayTokenStream(values);
        
        final Token reusableToken = new Token();
        
        Token token = stream.next(reusableToken);
        assertEquals("foo", token.term());
        assertEquals(0, token.startOffset());
        assertEquals(3, token.endOffset());
        
        token = stream.next(reusableToken);
        assertEquals("bar", token.term());
        assertEquals(3, token.startOffset());
        assertEquals(6, token.endOffset());
        
        token = stream.next(reusableToken);
        assertEquals("hepp", token.term());
        assertEquals(6, token.startOffset());
        assertEquals(10, token.endOffset());
        
        token = stream.next(reusableToken);
        assertEquals("hopp", token.term());
        assertEquals(10, token.startOffset());
        assertEquals(14, token.endOffset());
        
        token = stream.next(reusableToken);
        assertNull(token);
    }
    
    public void testZeroValues() throws Exception {
    	
    	final Token reusableToken = new Token();
        
        TokenStream stream = new StringArrayTokenStream(new String[]{});
        assertNull(stream.next(reusableToken));
    }
    
}

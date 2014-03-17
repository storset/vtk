package org.vortikal.repository.index.mapping;

import static org.junit.Assert.*;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.junit.Test;

public class StringArrayTokenStreamTest {

    @Test
    public void multipleValues() throws Exception {
        
        String[] values = new String[]{ "foo", "bar", "hepp", "hopp" };
        TokenStream stream = new StringArrayTokenStream(values);
        
        assertTrue(stream.incrementToken());
        
        TermAttribute ta = stream.getAttribute(TermAttribute.class);
//        OffsetAttribute oa = stream.getAttribute(OffsetAttribute.class);
        assertEquals("foo", ta.term());
//        assertEquals(0, oa.startOffset());
//        assertEquals(3, oa.endOffset());
        
        assertTrue(stream.incrementToken());

        ta = stream.getAttribute(TermAttribute.class);
//        oa = stream.getAttribute(OffsetAttribute.class);
        assertEquals("bar", ta.term());
//        assertEquals(3, oa.startOffset());
//        assertEquals(6, oa.endOffset());
        
        assertTrue(stream.incrementToken());

        ta = stream.getAttribute(TermAttribute.class);
//        oa = stream.getAttribute(OffsetAttribute.class);
        assertEquals("hepp", ta.term());
//        assertEquals(6, oa.startOffset());
//        assertEquals(10, oa.endOffset());
        
        assertTrue(stream.incrementToken());

        ta = stream.getAttribute(TermAttribute.class);
//        oa = stream.getAttribute(OffsetAttribute.class);
        assertEquals("hopp", ta.term());
//        assertEquals(10, oa.startOffset());
//        assertEquals(14, oa.endOffset());
        
        assertFalse(stream.incrementToken());
    }
    
    @Test
    public void zeroValues() throws Exception {
        TokenStream stream = new StringArrayTokenStream(new String[]{});
        assertFalse(stream.incrementToken());
    }
    
}

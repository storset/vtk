/* Copyright (c) 2006, University of Oslo, Norway
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
package org.vortikal.repository.index.mapping;

import java.io.IOException;
import java.io.StringReader;

import junit.framework.TestCase;

import org.apache.lucene.analysis.Token;

public class EscapedMultiValueFieldTokenizerTest extends TestCase {
    
    private char splitChar = ';';
    
    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    @SuppressWarnings("deprecation")
    public void testTokenizer() throws IOException {
        
        String testString = 
            ";;value1;value2;value3;;;;value4;;two escaped semicolons: \\;\\;;lastValue;literalBackslash\\\\";
        
        EscapedMultiValueFieldTokenizer tokenizer = 
            new EscapedMultiValueFieldTokenizer(new StringReader(testString), this.splitChar);
        
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
        assertEquals("literalBackslash\\", t.termText());
        assertEquals(73, t.startOffset());
        assertEquals(91, t.endOffset());

        t = tokenizer.next();
        assertNull(t);
        
    }
    
    @SuppressWarnings("deprecation")
    public void testEmptyInput() throws IOException {

        EscapedMultiValueFieldTokenizer tokenizer = 
            new EscapedMultiValueFieldTokenizer(new StringReader(""), this.splitChar);        
        
        Token t = tokenizer.next();
        assertNull(t);
    }
    
    @SuppressWarnings("deprecation")
    public void testOnlySplitCharsInput() throws IOException {
        
        EscapedMultiValueFieldTokenizer tokenizer = 
            new EscapedMultiValueFieldTokenizer(new StringReader(";;;;;;;;;;;"), this.splitChar);        
        
        Token t = tokenizer.next();
        assertNull(t);
        
        
    }
    
}

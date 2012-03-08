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

import java.util.Calendar;
import java.util.Date;

import junit.framework.TestCase;

public class FieldDataEncoderTest extends TestCase {

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    public void testBinaryBooleanEncoding() {
                
        byte[] encoded = FieldDataEncoder.encodeBooleanToBinary(false);
        assertFalse(FieldDataEncoder.decodeBooleanFromBinary(encoded, 0, encoded.length));
        
        encoded = FieldDataEncoder.encodeBooleanToBinary(true);
        assertTrue(FieldDataEncoder.decodeBooleanFromBinary(encoded, 0, encoded.length));
        
    }
    
    public void testDateStringEncoding() {
        
        Calendar test = Calendar.getInstance();
        
        String encoded = FieldDataEncoder.encodeDateValueToString(test.getTimeInMillis());
        
        Date decodedDate = new Date(FieldDataEncoder.decodeDateValueFromString(encoded));
        
        Calendar decoded = Calendar.getInstance();
        decoded.setTime(decodedDate);
        
        assertEquals(test.get(Calendar.DAY_OF_MONTH), decoded.get(Calendar.DAY_OF_MONTH));
        assertEquals(test.get(Calendar.MONTH), decoded.get(Calendar.MONTH));
        assertEquals(test.get(Calendar.YEAR), decoded.get(Calendar.YEAR));
        assertEquals(test.get(Calendar.HOUR), decoded.get(Calendar.HOUR));
        assertEquals(test.get(Calendar.MINUTE), decoded.get(Calendar.MINUTE));
        assertEquals(test.get(Calendar.SECOND), decoded.get(Calendar.SECOND));

        // Dates stored in index do not have millisecond resolution, so it should be zero
        assertEquals(decoded.get(Calendar.MILLISECOND), 0);
        
        // Test lexicographic sorting
        String encoded1 = FieldDataEncoder.encodeDateValueToString(test.getTimeInMillis());
        String encoded2 = FieldDataEncoder.encodeDateValueToString(test.getTimeInMillis() + 10000);
        assertTrue(encoded2.compareTo(encoded1) > 0);

        encoded2 = FieldDataEncoder.encodeDateValueToString(test.getTimeInMillis() - 10000);
        assertTrue(encoded2.compareTo(encoded1) < 0);
}

    public void testIntegerEncoding() {
        
        int testNumber = 666;
        
        String encoded = FieldDataEncoder.encodeIntegerToString(testNumber);
        
        assertEquals("8000029a", encoded);
        
        int decoded = FieldDataEncoder.decodeIntegerFromString(encoded);
        
        assertEquals(testNumber, decoded);
        
        byte[] encodedBinary = FieldDataEncoder.encodeIntegerToBinary(testNumber);
        assertEquals(4, encodedBinary.length);
        
        assertEquals(testNumber, FieldDataEncoder.decodeIntegerFromBinary(encodedBinary, 0, encodedBinary.length));
        
        // Test lexicographic sorting
        String encoded1 = FieldDataEncoder.encodeIntegerToString(-4000);
        String encoded2 = FieldDataEncoder.encodeIntegerToString(4000);
        
        assertTrue(encoded2.compareTo(encoded1) > 0);
        
        encoded1 = FieldDataEncoder.encodeIntegerToString(200);
        encoded2 = FieldDataEncoder.encodeIntegerToString(1000);
        
        assertTrue(encoded2.compareTo(encoded1) > 0);
    }
    
    public void testLongEncoding() {
        
        long testNumber = Long.MIN_VALUE + 100;
        
        String encoded = FieldDataEncoder.encodeLongToString(testNumber);
        
        assertEquals("0000000000000064", encoded);
        
        long decoded = FieldDataEncoder.decodeLongFromString(encoded);
        
        assertEquals(testNumber, decoded);
        
        byte[] encodedBinary = FieldDataEncoder.encodeLongToBinary(testNumber);
        assertEquals(8, encodedBinary.length);
        
        assertEquals(testNumber, FieldDataEncoder.decodeLongFromBinary(encodedBinary, 0, encodedBinary.length));

        // Test lexicographic sorting
        String encoded1 = FieldDataEncoder.encodeLongToString(-400000000L);
        String encoded2 = FieldDataEncoder.encodeLongToString(400000000L);
        
        assertTrue(encoded2.compareTo(encoded1) > 0);
        
        encoded1 = FieldDataEncoder.encodeLongToString(20000000);
        encoded2 = FieldDataEncoder.encodeLongToString(100000000);
        
        assertTrue(encoded2.compareTo(encoded1) > 0);
    }
}

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
package org.vortikal.repositoryimpl.index;

import java.util.Calendar;
import java.util.Date;

import org.vortikal.repositoryimpl.index.FieldValueEncoder;

import junit.framework.TestCase;

public class FieldValueEncoderTestCase extends TestCase {

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    public void testBinaryBooleanEncoding() {
                
        byte[] encoded = FieldValueEncoder.encodeBooleanToBinary(false);
        assertFalse(FieldValueEncoder.decodeBooleanFromBinary(encoded));
        
        encoded = FieldValueEncoder.encodeBooleanToBinary(true);
        assertTrue(FieldValueEncoder.decodeBooleanFromBinary(encoded));
        
    }
    
    public void testDateStringEncoding() {
        
        Calendar test = Calendar.getInstance();
        
        String encoded = FieldValueEncoder.encodeDateValueToString(test.getTimeInMillis());
        
        Date decodedDate = new Date(FieldValueEncoder.decodeDateValueFromString(encoded));
        
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
    }

    public void testIntegerEncoding() {
        
        int testNumber = 666;
        
        String encoded = FieldValueEncoder.encodeIntegerToString(testNumber);
        
        assertEquals("8000029a", encoded);
        
        int decoded = FieldValueEncoder.decodeIntegerFromString(encoded);
        
        assertEquals(testNumber, decoded);
        
        byte[] encodedBinary = FieldValueEncoder.encodeIntegerToBinary(testNumber);
        assertEquals(4, encodedBinary.length);
        
        assertEquals(testNumber, FieldValueEncoder.decodeIntegerFromBinary(encodedBinary));
    }
    
    public void testLongEncoding() {
        
        long testNumber = Long.MIN_VALUE + 100;
        
        String encoded = FieldValueEncoder.encodeLongToString(testNumber);
        
        assertEquals("0000000000000064", encoded);
        
        long decoded = FieldValueEncoder.decodeLongFromString(encoded);
        
        assertEquals(testNumber, decoded);
        
        byte[] encodedBinary = FieldValueEncoder.encodeLongToBinary(testNumber);
        assertEquals(8, encodedBinary.length);
        
        assertEquals(testNumber, FieldValueEncoder.decodeLongFromBinary(encodedBinary));
    }
}

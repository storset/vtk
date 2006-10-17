package org.vortikal.repositoryimpl.query;

import java.util.Calendar;
import java.util.Date;

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

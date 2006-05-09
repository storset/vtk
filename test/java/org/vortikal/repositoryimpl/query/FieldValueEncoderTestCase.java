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
    
    public void testDateEncoding() {
        
        Calendar test = Calendar.getInstance();
        
        String encoded = FieldValueEncoder.encodeDateValue(test.getTimeInMillis());
        
        Date decodedDate = new Date(FieldValueEncoder.decodeDateValue(encoded));
        
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
        
        String encoded = FieldValueEncoder.encodeInteger(testNumber);
        
        assertEquals("8000029a", encoded);
        
        int decoded = FieldValueEncoder.decodeInteger(encoded);
        
        assertEquals(testNumber, decoded);
    }
    
    public void testLongEncoding() {
        
        long testNumber = Long.MIN_VALUE + 100;
        
        String encoded = FieldValueEncoder.encodeLong(testNumber);
        
        assertEquals("0000000000000064", encoded);
        
        long decoded = FieldValueEncoder.decodeLong(encoded);
        
        assertEquals(testNumber, decoded);
        
    }
}

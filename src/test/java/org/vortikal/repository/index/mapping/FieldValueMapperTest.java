/* Copyright (c) 2006, 2009 University of Oslo, Norway
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

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.List;

import org.apache.lucene.document.Fieldable;
import org.vortikal.repository.RepositoryException;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.resourcetype.PropertyType.Type;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.repository.resourcetype.ValueFactory;
import org.vortikal.repository.resourcetype.ValueFactoryImpl;
import org.vortikal.security.InvalidPrincipalException;
import org.vortikal.security.Principal;
import org.vortikal.security.PrincipalFactory;
import org.vortikal.security.PrincipalImpl;

import junit.framework.TestCase;

public class FieldValueMapperTest extends TestCase {

    private FieldValueMapper fieldValueMapper;
    private ValueFactory vf;

    public FieldValueMapperTest() {
        this.fieldValueMapper = new FieldValueMapper();
        ValueFactoryImpl vf = new ValueFactoryImpl();
        vf.setPrincipalFactory(new MockPrincipalFactory());
        this.vf = vf;
        this.fieldValueMapper.setValueFactory(new ValueFactoryImpl());
    }
    
    public void testDateValueIndexFieldEncoding() {

        String[] dateFormats = new String[] { "Long-format",
                "yyyy-MM-dd HH:mm:ss Z", "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd HH:mm", "yyyy-MM-dd HH", "yyyy-MM-dd" };

        Date now = new Date();
        String[] dateStrings = new String[] { Long.toString(now.getTime()),
                "2005-10-10 14:22:00 +0100", "2005-10-10 14:22:00",
                "2005-10-10 14:22", "2005-10-10 14", "2005-10-10" };

        for (int i = 0; i < dateStrings.length; i++) {
            try {
                this.fieldValueMapper.encodeIndexFieldValue(dateStrings[i],
                        PropertyType.Type.TIMESTAMP, false);
            } catch (Exception e) {
                fail("Failed to encode index field value for date format '" + dateFormats[i]
                        + "', date string '" + dateStrings[i] + "':"
                        + e.getMessage());
            }
        }
    }
    
    /**
     * Tests FieldValueMapper.getValueFromStoredBinaryField(Field,Type)
     * and   FieldValueMapper.getStoredBinaryFieldFromValue(String,Value)
     */
    public void testBinaryMapping() {
        
        Fieldable stringField = this.fieldValueMapper.getStoredBinaryFieldFromValue("string", this.vf.createValue("bâr", Type.STRING));
        Fieldable intField = this.fieldValueMapper.getStoredBinaryFieldFromValue("int", this.vf.createValue("1024", Type.INT));
        Fieldable longField = this.fieldValueMapper.getStoredBinaryFieldFromValue("long", this.vf.createValue("1024", Type.LONG));
        
        assertEquals("string", stringField.name());
        assertEquals("int", intField.name());
        assertEquals("long", longField.name());
        
        try {
            assertEquals("bâr".getBytes("utf-8").length, stringField.getBinaryLength());
            assertEquals("bâr", new String(stringField.getBinaryValue(), stringField.getBinaryOffset(), stringField.getBinaryLength(), "utf-8"));
        } catch (UnsupportedEncodingException ue) {}
        
        byte[] data = new byte[intField.getBinaryLength()];
        System.arraycopy(intField.getBinaryValue(), intField.getBinaryOffset(), data, 0, intField.getBinaryLength());
        
        assertEquals(4, data.length);
        assertEquals(0x0, data[3]);
        assertEquals(0x0, data[2]);
        assertEquals(0x4, data[1]);
        assertEquals(0x0, data[0]);

    
        data = new byte[longField.getBinaryLength()];
        System.arraycopy(longField.getBinaryValue(), longField.getBinaryOffset(), data, 0, longField.getBinaryLength());
        
        assertEquals(8, data.length);
        assertEquals(0x0, data[7]);
        assertEquals(0x0, data[6]);
        assertEquals(0x0, data[5]);
        assertEquals(0x0, data[4]);
        assertEquals(0x0, data[3]);
        assertEquals(0x0, data[2]);
        assertEquals(0x4, data[1]);
        assertEquals(0x0, data[0]);
        
        Value stringValue = this.fieldValueMapper.getValueFromStoredBinaryField(stringField, Type.STRING);
        assertEquals(stringValue.getNativeStringRepresentation(), "bâr");
        
        Value intValue = this.fieldValueMapper.getValueFromStoredBinaryField(intField, Type.INT);
        assertEquals(1024, intValue.getIntValue());
        
        Value longValue = this.fieldValueMapper.getValueFromStoredBinaryField(longField, Type.LONG);
        assertEquals(1024, longValue.getLongValue());
}
    
    public void testMultithreadedDateValueIndexFieldEncoding() {
        
        Thread[] threads = new Thread[10];
        for (int i=0; i<threads.length; i++) {
            threads[i] = new Thread(new Runnable() {
               public void run() {
                   testDateValueIndexFieldEncoding();
               }
            });
        }
        
        for (int i=0; i<threads.length; i++) {
            threads[i].start();
        }
        
        for (int i = 0; i < threads.length; i++) {
            try {
                threads[i].join(); } catch (InterruptedException ie) { 
                    fail("Interrupted while waiting for test threads to finish.");}
        }
        
    }
    
}

class MockPrincipalFactory extends PrincipalFactory {

    @Override
    public Principal getPrincipal(String id, Principal.Type type)
            throws InvalidPrincipalException {
        return new PrincipalImpl(id, type);
    }

    @Override
    public List<Principal> search(String filter, Principal.Type type)
            throws RepositoryException {
        return null;
    }

}
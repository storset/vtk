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

import java.util.Date;
import java.util.List;

import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Field;

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

import static org.junit.Assert.*;
import org.junit.Test;
import org.vortikal.repository.index.mapping.Field4ValueMapper.FieldSpec;

public class FieldValueMapperTest {

    private final Field4ValueMapper fvm;
    private final ValueFactory vf;

    public FieldValueMapperTest() {
        this.fvm = new Field4ValueMapper();
        ValueFactoryImpl vf = new ValueFactoryImpl();
        vf.setPrincipalFactory(new MockPrincipalFactory());
        this.vf = vf;
        this.fvm.setValueFactory(new ValueFactoryImpl());
    }

    @Test
    public void dateValueIndexFieldEncoding() {

        String[] dateFormats = new String[]{"Long-format",
            "yyyy-MM-dd HH:mm:ss Z", "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd HH:mm", "yyyy-MM-dd HH", "yyyy-MM-dd"};

        Date now = new Date();
        String[] dateStrings = new String[]{Long.toString(now.getTime()),
            "2005-10-10 14:22:00 +0100", "2005-10-10 14:22:00",
            "2005-10-10 14:22", "2005-10-10 14", "2005-10-10"};

        for (int i = 0; i < dateStrings.length; i++) {
            try {
                fvm.queryTerm("someDate", dateStrings[i],
                        PropertyType.Type.TIMESTAMP, false);
            } catch (Exception e) {
                fail("Failed to encode index field value for date format '" + dateFormats[i]
                        + "', date string '" + dateStrings[i] + "':"
                        + e.getMessage());
            }
        }
    }

    /**
     * Tests FieldValueMapper.getValueFromStoredBinaryField(Field,Type) and
     * FieldValueMapper.getStoredBinaryFieldFromValue(String,Value)
     */
    @Test
    public void storedFields() {

        Field stringField = fvm.makeFields("string", FieldSpec.STORED, new Value("bâr", PropertyType.Type.STRING)).get(0);
        Field intField = fvm.makeFields("int", FieldSpec.STORED, new Value(1024)).get(0);
        Field longField = fvm.makeFields("long", FieldSpec.STORED, new Value(1024L)).get(0);

        assertEquals("string", stringField.name());
        assertEquals("int", intField.name());
        assertEquals("long", longField.name());

        assertEquals("bâr", stringField.stringValue());
        assertNull(stringField.binaryValue());
        assertNull(stringField.numericValue());

        assertEquals(1024, intField.numericValue().intValue());
        assertEquals("1024", intField.stringValue());
        assertNull(intField.binaryValue());

        assertEquals(1024L, longField.numericValue().longValue());
        assertEquals("1024", longField.stringValue());
        assertNull(longField.binaryValue());

        Value stringValue = fvm.valueFromField(Type.STRING, stringField);
        assertEquals("bâr", stringValue.getStringValue());

        Value intValue = fvm.valueFromField(Type.INT, intField);
        assertEquals(1024, intValue.getIntValue());

        Value longValue = fvm.valueFromField(Type.LONG, longField);
        assertEquals(1024, longValue.getLongValue());
    }
    
    @Test
    public void dateFields() {
        final long now = new Date().getTime();
        List<Field> fields = fvm.makeFields("date", FieldSpec.INDEXED_STORED, Type.DATE, now);
        
        assertEquals(2, fields.size());
        boolean haveStored = false;
        boolean haveIndexed = false;
        for (Field f : fields) {
            if (f.fieldType().stored()) {
                long fieldValue = f.numericValue().longValue();
                assertEquals(now, fieldValue);
                haveStored = true;
            }
            if (!f.fieldType().stored()) {
                long fieldValue = f.numericValue().longValue();
                assertEquals(DateTools.round(now, DateTools.Resolution.SECOND), fieldValue);
                haveIndexed = true;
            }
        }
        if (! (haveIndexed && haveStored)) {
            fail("Expected one indexed and one stored field");
        }
    }

    @Test
    public void multithreadedDateValueIndexFieldEncoding() {

        Thread[] threads = new Thread[10];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    dateValueIndexFieldEncoding();
                }
            });
        }
        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException ie) {
                fail("Interrupted while waiting for test threads to finish.");
            }
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

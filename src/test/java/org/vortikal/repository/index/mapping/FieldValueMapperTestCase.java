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

import java.util.Date;

import junit.framework.TestCase;

import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.resourcetype.ValueFactoryImpl;

/**
 * TODO: This JUnit test case class is not complete.
 * 
 *
 */
public class FieldValueMapperTestCase extends TestCase {

    private FieldValueMapper fieldValueMapper;

    public FieldValueMapperTestCase() {
        this.fieldValueMapper = new FieldValueMapper();
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
    
//    public void testMultithreadedDateValueIndexFieldEncoding() {
//        
//        Thread[] threads = new Thread[100];
//        for (int i=0; i<threads.length; i++) {
//            threads[i] = new Thread(new Runnable() {
//               public void run() {
//                   testDateValueIndexFieldEncoding();
//               }
//            });
//        }
//        
//        for (int i=0; i<threads.length; i++) {
//            threads[i].start();
//        }
//        
//        for (int i = 0; i < threads.length; i++) {
//            try {
//                threads[i].join(); } catch (InterruptedException ie) { 
//                    fail("Interrupted while waiting for test threads to finish.");}
//        }
//        
//    }
    
}

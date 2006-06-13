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
package org.vortikal.util.text;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.vortikal.util.cache.ReusableObjectCache;

import junit.framework.TestCase;

/**
 * <code>TestCase</code> for <code>SimpleDateFormatCache</code> 
 * (and indirectly for 
 * {@link org.vortikal.util.cache.AbstractReusableObjectArrayStackCache})
 *  
 * @author oyviste
 *
 */
public class SimpleDateFormatCacheTestCase extends TestCase {

    private String pattern = "yyyy-MM-dd HH:mm:ss:S z";
    
    public void testFormatting() {
        
        Date testDate = new Date();
        ReusableObjectCache cache = new SimpleDateFormatCache(pattern);
        
        DateFormat f = (DateFormat)cache.getInstance();
        String formatted = f.format(testDate);

        try {
            Date parsed = f.parse(formatted);
            
            assertEquals(testDate.getTime(), parsed.getTime());
        } catch (ParseException e) {
            fail(e.getMessage());
        }
        
    }
    
    public void testStackCaching() {
        ReusableObjectCache cache = new SimpleDateFormatCache(pattern, 3);
        
        // Get some instances
        DateFormat f1 = (DateFormat)cache.getInstance();
        DateFormat f2 = (DateFormat)cache.getInstance();
        DateFormat f3 = (DateFormat)cache.getInstance();
        
        // Put them back
        cache.putInstance(f1); 
        cache.putInstance(f2);
        cache.putInstance(f3);
        
        // Test that the instances are cached properly
        // (we know that it works like a stack internally)
        assertTrue(f3 == cache.getInstance());
        assertTrue(f2 == cache.getInstance());
        assertTrue(f1 == cache.getInstance());
        
    }
    
    public void testMultithreadedAccess() {
        
        final ReusableObjectCache cache = new SimpleDateFormatCache(pattern, 50);

        int numWorkers = 100;
        int iterationsPerWorker = 50;
        Thread[] threads = new Thread[numWorkers];
        TestWorker[] workers = new TestWorker[numWorkers];
        
        for (int i=0; i<numWorkers; i++) {
            workers[i] = new TestWorker(cache, iterationsPerWorker, false);
            threads[i] = new Thread(workers[i]);
        }
        
        long start = System.currentTimeMillis();
        
        // Start all threads concurrently, as fast as possible
        for (int i=0; i<numWorkers; i++) {
            threads[i].start();
        }
        
        // Wait for all threads to finish the work
        for (int i=0; i<numWorkers; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException ie) {}
        }
        long end = System.currentTimeMillis();
        
        System.out.println("testMultithreadedAccess(): Time used without caching: " 
                + (end-start) + " ms.");
        
        // Check that none of the workers failed
        for (int i=0; i<numWorkers; i++) {
            TestWorker w = workers[i];
            if (w.failed) {
                fail("Worker # " + i + " failed");
            }
        }

        System.gc();
        
        // Test with cache enabled
        for (int i=0; i<numWorkers; i++) {
            workers[i] = new TestWorker(cache, iterationsPerWorker, true);
            threads[i] = new Thread(workers[i]);
        }
        
        start = System.currentTimeMillis();
        // Start all threads concurrently, as fast as possible
        for (int i=0; i<numWorkers; i++) {
            threads[i].start();
        }
        
        // Wait for all threads to finish the work
        for (int i=0; i<numWorkers; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException ie) {}
        }
        end = System.currentTimeMillis();
        
        System.out.println("testMultithreadedAccess(): Time used with caching enabled: " 
                + (end-start) + " ms.");
        System.out.println("testMultithreadedAccess(): Size of cache at the end: " 
                + cache.size());
        
    }
    
    private class TestWorker implements Runnable {
        
        boolean failed = false;
        boolean useCache;
        int iterations;
        ReusableObjectCache dateFormatCache;
        
        public TestWorker(ReusableObjectCache dateFormatCache, 
                            int iterations, boolean useCache) {
            this.dateFormatCache = dateFormatCache;
            this.iterations = iterations;
            this.useCache = useCache;
        }
        
        public void run() {
            for (int i=0; i<iterations; i++) {
                Date d = new Date();

                DateFormat f;
                if (useCache) {
                    f = (DateFormat)dateFormatCache.getInstance();
                } else {
                    f = new SimpleDateFormat(SimpleDateFormatCacheTestCase.this.pattern); 
                }
                
                String formatted = f.format(d);
                Date parsed = null;
                try {
                    parsed = f.parse(formatted);
                } catch (ParseException pe) {
                    this.failed = true;
                    break;
                }
                
                // NOTE: assuming millisecond resolution in date format !
                if (d.getTime() != parsed.getTime()) {
                    this.failed = true;
                    break;
                }
                
                if (useCache) {
                    dateFormatCache.putInstance(f);
                }
            }
        }
    }
}

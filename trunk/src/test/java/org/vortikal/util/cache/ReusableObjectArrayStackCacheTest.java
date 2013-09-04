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
package org.vortikal.util.cache;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import junit.framework.TestCase;

/**
 * <code>TestCase</code> for <code>ReusableObjectArrayStackCache</code> 
 *  
 * @author oyviste
 *
 */
public class ReusableObjectArrayStackCacheTest extends TestCase {

    private final String pattern = "yyyy-MM-dd HH:mm:ss:S z";
    
    public void testFormatting() {
        
        final Date testDate = new Date();
        final ReusableObjectCache<SimpleDateFormat> cache = 
            new ReusableObjectArrayStackCache<SimpleDateFormat>(1);
        
        assertNull(cache.getInstance()); // Nothing is in cache, yet.
        assertEquals(0, cache.size());
        
        assertTrue(cache.putInstance(new SimpleDateFormat(this.pattern)));
        assertEquals(1, cache.size());
        
        SimpleDateFormat f = cache.getInstance();
        assertEquals(0, cache.size());
        
        assertNull(cache.getInstance()); // Cache is empty, again.
        
        assertTrue(cache.putInstance(f));
        assertFalse(cache.putInstance(new SimpleDateFormat())); // Should cause overflow and not be kept
        
        assertTrue(f == cache.getInstance());
        
        String formatted = f.format(testDate);

        try {
            Date parsed = f.parse(formatted);
            
            assertEquals(testDate.getTime(), parsed.getTime());
        } catch (ParseException e) {
            fail(e.getMessage());
        }
        
    }
    
    public void testStackCaching() {
        final ReusableObjectCache<SimpleDateFormat> cache 
            = new ReusableObjectArrayStackCache<SimpleDateFormat>(3);
        
        // Pre-populate cache
        assertTrue(cache.putInstance(new SimpleDateFormat(this.pattern)));
        assertTrue(cache.putInstance(new SimpleDateFormat(this.pattern)));
        assertTrue(cache.putInstance(new SimpleDateFormat(this.pattern)));
        assertEquals(3, cache.size());
        
        // Get some instances
        SimpleDateFormat f1 = cache.getInstance();
        SimpleDateFormat f2 = cache.getInstance();
        SimpleDateFormat f3 = cache.getInstance();
        assertEquals(0, cache.size());
        
        // Put them back
        assertTrue(cache.putInstance(f1)); 
        assertTrue(cache.putInstance(f2));
        assertTrue(cache.putInstance(f3));
        
        // Test that the instances are cached properly
        // (we know that it works like a stack internally)
        assertTrue(f3 == cache.getInstance());
        assertTrue(f2 == cache.getInstance());
        assertTrue(f1 == cache.getInstance());
        assertNull(cache.getInstance());
        assertEquals(0, cache.size());
    }
    
    // Test multithreaded access and performance difference.
    public void testMultithreadedAccess() {

        final ReusableObjectCache<SimpleDateFormat> cache 
            = new ReusableObjectArrayStackCache<SimpleDateFormat>(50);
        
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
        ReusableObjectCache<SimpleDateFormat> dateFormatCache;
        
        public TestWorker(ReusableObjectCache<SimpleDateFormat> dateFormatCache, 
                            int iterations, boolean useCache) {
            this.dateFormatCache = dateFormatCache;
            this.iterations = iterations;
            this.useCache = useCache;
        }
        
        public void run() {
            for (int i=0; i<this.iterations; i++) {
                Date d = new Date();

                SimpleDateFormat f;
                if (this.useCache) {
                    f = this.dateFormatCache.getInstance();
                    if (f == null) {
                        // Nothing available in cache, create new
                        f = new SimpleDateFormat(ReusableObjectArrayStackCacheTest.this.pattern);
                    }
                } else {
                    f = new SimpleDateFormat(ReusableObjectArrayStackCacheTest.this.pattern); 
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
                
                if (this.useCache) {
                    this.dateFormatCache.putInstance(f);
                }
            }
        }
    }
}

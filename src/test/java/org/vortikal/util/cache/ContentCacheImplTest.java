/* Copyright (c) 2013, University of Oslo, Norway
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.concurrent.Synchroniser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test for default ContentCache implementation.
 */
public class ContentCacheImplTest {

    static {
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.Log4JLogger");
        System.setProperty("log4j.configuration", "log4j.test.xml");
    }
    
    private ContentCacheImpl<String,String> cache;
    private final Mockery context = new Mockery();
    private final ContentCacheLoader<String,String> loader;
    
    public ContentCacheImplTest() {
        loader = context.mock(ContentCacheLoader.class);
    }
    
    @Before
    public void setUp() {
        cache = new ContentCacheImpl<String,String>();
        cache.setName("testcache");
        cache.setMaxItems(10);
        cache.setCacheLoader(loader);
    }
    
    @After
    public void tearDown() {
        cache.clear();
        cache.destroy();
    }

    @Test
    public void getFromCacheWithZeroTimeout() throws Exception {
        context.checking(new Expectations(){{
            oneOf(loader).load("foo");
            will(returnValue("value"));
        }});
        
        cache.afterPropertiesSet();
        assertEquals("value", cache.get("foo"));
        
        context.assertIsSatisfied();
    }
    
    @Test
    public void getFromCacheWithRefresh() throws Exception {
        final String firstValue = "firstValue";
        context.checking(new Expectations(){{
            oneOf(loader).load("foo");
            will(returnValue(firstValue));
            oneOf(loader).load("foo");
            will(returnValue("secondValue"));
        }});
        
        cache.setCacheMilliSeconds(250);
        cache.afterPropertiesSet();
        assertEquals("firstValue", cache.get("foo"));
        assertSame(firstValue, cache.get("foo"));  // Expect to be cached and no loader call done this time
        sleep(300);
        String valueAfterRefresh = cache.get("foo");
        assertNotNull(valueAfterRefresh);
        assertEquals("secondValue", valueAfterRefresh);
        
        context.assertIsSatisfied();
    }
    
    @Test(expected = IOException.class)
    public void loaderException() throws Exception {
        context.checking(new Expectations(){{
            oneOf(loader).load("foo");
            will(throwException(new IOException("Could not load object for foo")));
        }});

        cache.afterPropertiesSet();
        cache.get("foo");
    }
    
    @Test(expected = IOException.class)
    public void loaderExceptionOnSynchronousRefresh() throws Exception {
        context.checking(new Expectations(){{
            oneOf(loader).load("foo");
            will(returnValue("value"));
            oneOf(loader).load("foo");
            will(throwException(new IOException("Could not load object for foo")));
        }});
        
        cache.setCacheMilliSeconds(250);
        cache.afterPropertiesSet();
        assertEquals("value", cache.get("foo"));
        sleep(300);
        cache.get("foo"); // Should throw IOException
    }

    @Test
    public void loaderExceptionOnAsyncRefresh() throws Exception {
        Mockery threadSafeMockery = new Mockery();
        threadSafeMockery.setThreadingPolicy(new Synchroniser());
        final ContentCacheLoader<String,String> threadSafeMockedLoader = threadSafeMockery.mock(ContentCacheLoader.class);
        threadSafeMockery.checking(new Expectations() {{
            oneOf(threadSafeMockedLoader).load("foo");
            will(returnValue("value"));
            oneOf(threadSafeMockedLoader).load("foo");
            will(throwException(new IOException("Could not load object for foo")));
        }});
        
        cache.setCacheLoader(threadSafeMockedLoader);
        cache.setCacheMilliSeconds(250);
        cache.setAsynchronousRefresh(true);
        cache.afterPropertiesSet();
        
        assertEquals("value", cache.get("foo"));
        sleep(300);
        // Loader IOException swallowed (and hopefully logged). Old object returned.
        assertEquals("value", cache.get("foo")); 
    }
    
    @Test
    public void concurrentAccess() throws Exception {

        final Map<String, String> data = new HashMap<String, String>() {{
            put("a", "1"); put("b", "2"); put("c", "3"); put("d", "4");
        }};

        final AtomicLong loaderCallCount = new AtomicLong(0);
        final ContentCacheLoader<String, String> mapLoader = new ContentCacheLoader<String, String>() {
            @Override
            public String load(String identifier) throws Exception {
                loaderCallCount.incrementAndGet();
                return data.get(identifier);
            }
        };
        
        cache.setCacheLoader(mapLoader);
        cache.setCacheMilliSeconds(75);
        cache.setAsynchronousRefresh(true);
        cache.setMaxItems(2);
        cache.afterPropertiesSet();

        final AtomicLong cacheCallCount = new AtomicLong(0);
        class GetValidator implements Callable<String> {
            final String key;
            final String value;
            final long runFor;
            GetValidator(String key, String value, long runFor) { 
                this.key = key;
                this.value = value;
                this.runFor = runFor;
            }
            @Override
            public String call() throws Exception {
                final long now = System.currentTimeMillis();
                while (now + runFor > System.currentTimeMillis()) {
                    for (int i = 0, loop = (int)(Math.random()*10+1); i < loop; i++) {
                        cacheCallCount.incrementAndGet();
                        String v = cache.get(key);
                        if (!value.equals(v)) {
                            throw new IllegalStateException("Expected value " + this.value + " but got " + v);
                        }
                        Thread.yield(); // help intermixing of cache calls from other threads with different keys
                    }
                }
                return "OK";
            }
        }

        ExecutorService es = Executors.newFixedThreadPool(data.size());
        List<Future<String>> results = new ArrayList<Future<String>>(data.size());
        for (Map.Entry<String,String> entry: data.entrySet()) {
            GetValidator gv = new GetValidator(entry.getKey(), entry.getValue(), 1000);
            Future<String> f = es.submit(gv);
            results.add(f);
        }
        for (Future<String> f: results) {
            try {
                assertEquals("OK", f.get());
            } catch (Exception e) {
                fail("GetValidator failed with exception: " + e.getMessage());
            }
        }
        es.shutdown();

        // Assert that cache has actually been effective to some degree
        assertTrue(cacheCallCount.longValue() > loaderCallCount.longValue());
    }

    @Test
    public void sizeLimitWhenLimitThenKeepRecentOnly() throws Exception {
        for (int i = 1; i <= 15; i++) {
            final int index = i;
            context.checking(new Expectations(){{
                oneOf(loader).load("foo" + index); will(returnValue("value" + index));
            }});
        }
        
        cache.setMaxItems(10);
        cache.setCacheMilliSeconds(500);
        cache.afterPropertiesSet();
        for (int i = 1; i <= 15; i++) {
            assertEquals("value" + i, cache.get("foo" + i));
            assertEquals(Math.min(10, i), cache.getSize());
        }
    }

    @Test
    public void sizeLimitWhenNoLimitThenKeepAll() throws Exception {
        for (int i = 1; i <= 15; i++) {
            final int index = i;
            context.checking(new Expectations(){{
                oneOf(loader).load("foo" + index); will(returnValue("value" + index));
            }});
        }
        
        cache.setMaxItems(-1);
        cache.setCacheMilliSeconds(500);
        cache.afterPropertiesSet();
        for (int i = 1; i <= 15; i++) {
            assertEquals("value" + i, cache.get("foo" + i));
            assertEquals(i, cache.getSize());
        }
    }

//    @Test
//    public void sharedCaches() throws Exception {
//        context.checking(new Expectations(){{
//            oneOf(loader).load("foo1"); will(returnValue("value1"));
//        }});
//
//        // Use separate blocks so we don't accidently mix up the caches
//        {
//            ContentCache<String,String> cache1 = new ContentCache<String,String>();
//            cache1.setName("testsharedcache");
//            cache1.setMaxItems(10);
//            cache1.setCacheLoader(loader);
//            cache1.setCacheMilliSeconds(250);
//            cache1.setUseSharedCaches(true);
//            cache1.afterPropertiesSet();
//            assertEquals("value set the first time", "value1", cache1.get("foo1"));
//            cache1.destroy();
//            
//        }
//
//        {
//            ContentCache<String,String> cache2 = new ContentCache<String,String>();
//            cache2.setName("testsharedcache");
//            cache2.setMaxItems(10);
//            cache2.setCacheLoader(loader);
//            cache2.setCacheMilliSeconds(250);
//            cache2.setUseSharedCaches(true);
//            cache2.afterPropertiesSet();
//            assertEquals("value should be reused", "value1", cache2.get("foo1"));
//            cache2.destroy();
//            
//        }
//        
//        // Just to satisfy the common @After method
//        cache.afterPropertiesSet();
//    }
    
    private void sleep(long millis) throws InterruptedException {
        Thread.sleep(millis);
    }

}

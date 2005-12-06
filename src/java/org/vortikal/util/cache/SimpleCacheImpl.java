/* Copyright (c) 2004, University of Oslo, Norway
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


import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * This cache stores items for <code>timeoutSeconds</code> seconds,
 * defaulting to zero seconds.
 * 
 * You can set refreshTimestampOnGet to false if you don't want to 
 * refresh an item's timestamp when it's retrieved from the cache. 
 */
public class SimpleCacheImpl implements SimpleCache, BeanNameAware,
                                        InitializingBean, DisposableBean {
    
    private static Log logger = LogFactory.getLog(SimpleCacheImpl.class);

    
    private Map cache = new HashMap();
    private int timeoutSeconds = 0;
    private boolean refreshTimestampOnGet = true;

    private String name;
    
    private CleanupThread cleanupThread;


    public SimpleCacheImpl() {
    }


    public SimpleCacheImpl(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }
    

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }


    public void put(Object key, Object value) {
        synchronized (cache) {
            cache.put(key, new Item(value));
        }
    }


    public void setRefreshTimestampOnGet(boolean refreshTimestampOnGet) {
        this.refreshTimestampOnGet = refreshTimestampOnGet;
    }


    public void setBeanName(String name) {
        this.name = name;
    }


    public void afterPropertiesSet() {
        if (this.timeoutSeconds > 0) {
            this.cleanupThread = new CleanupThread(this.timeoutSeconds);
            this.cleanupThread.start();
        }
    }
    

    public void destroy() {
        if (this.cleanupThread != null) {
            this.cleanupThread.interrupt();
        }
    }
    

    public Object get(Object key) {
        Item item = (Item) cache.get(key);
        if (item == null)
            return null;
        else if (item.getTimestamp().getTime() + timeoutSeconds * 1000 > new Date().getTime()) {
            return item.getValue();
        } 
            
        if (logger.isDebugEnabled())
            logger.debug("Cache " + name + " expiring item with key='" + key + "'");

        synchronized (cache) {
            cache.remove(key);
        }

        return null;
    }


    public Object remove(Object key) {
        if (cache.containsKey(key)) {
            synchronized (cache) {
                return ((Item) cache.remove(key)).getValue();
            }
        }
        return null;
    }


    /**
     * Cleans up expired cached information periodically.
     * 
     */
    public void cleanupExpiredItems() {

        if (this.timeoutSeconds < 1) return;
        
        ArrayList removeableItems = new ArrayList();

        /* FIXME: Is this a good idea?
         * Trying to avoid concurrentmodificationexception 
         * without syncronization */
        Set set = new HashSet(cache.keySet());
        
        for (Iterator i = set.iterator(); i.hasNext();) {
            String key = (String) i.next();

            Item item = (Item) cache.get(key);

            if (item != null &&
                item.getTimestamp().getTime() + timeoutSeconds * 1000
                < System.currentTimeMillis()) {
                removeableItems.add(key);
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Cache " + name + " removing " + removeableItems.size()
                         + " expired items");
        }

        synchronized (cache) {
            for (Iterator iterator = removeableItems.iterator(); iterator.hasNext();) {
                String key = (String) iterator.next();
                cache.remove(key);

            }
        }
    }
    

    private class Item {
        Date date;
        Object value;
        
        public Item(Object value) {
            this.date = new Date();
            this.value = value;
        }

        public Date getTimestamp() {
            return date;
        }
        
        
        public Object getValue() {
            if (refreshTimestampOnGet) this.date = new Date();
            return value;
        }
    }




    private class CleanupThread extends Thread {

        private long sleepSeconds;
        private boolean alive = true;
    

        public CleanupThread(long sleepSeconds) {
            this.sleepSeconds = sleepSeconds;
        }

        public void run() {

            while (alive) {

                try {

                    sleep(1000 * sleepSeconds);
                    cleanupExpiredItems();
                    
                } catch (InterruptedException e) {
                    this.alive = false;

                } catch (Throwable t) {
                    logger.warn("Caught exception in cleanup thread", t);
                }
            }
        }
    }
    
    
}


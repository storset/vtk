/* Copyright (c) 2007, University of Oslo, Norway
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

import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * Utility class for caching content that is potentially expensive to
 * load for certain periods of time before refreshing it. Typical
 * examples of such content include network resources, compiled XML
 * stylesheets, etc. A {@link ContentLoader} is responsible for
 * loading the actual items.
 *
 * <p>Note: This cache is not meant as a general read/write cache, as
 * it never shrinks (it does not contain a <code>remove()</code> method).
 *
 * <p>Configurable JavaBean properties:
 * <ul>
 *   <li><code>cacheLoader</code> - the {@link CacheLoader content loader}
 *   <li><code>cacheSeconds</code> - the cache timeout in seconds
 *   <li><code>cacheMilliseconds</code> - the cache timeout in milliseconds
 *   <li><code>asynchronousRefresh</code> - if an item is expired and
 *   this property is <code>true</code>, the item is returned as is
 *   and a refresh is triggered in a separate thread. Otherwise, the
 *   current thread blocks while the item is refreshed.
 *   <li><code>refreshInterval</code> - if set to a positive number of
 *   seconds, a thread is created at initialization time, triggering a
 *   refresh of expired items at regular intervals (in a separate
 *   thread).
 * </ul>
 *
 */
public final class ContentCache implements InitializingBean, DisposableBean {
    
    private static Log logger = LogFactory.getLog(ContentCache.class);    

    private ContentCacheLoader loader;
    private int cacheTimeout;
    private ConcurrentHashMap cache = new ConcurrentHashMap();
    private boolean asynchronousRefresh = false;
    private int refreshInterval = -1;
    private RefreshThread refreshThread;
    
    public void setCacheLoader(ContentCacheLoader loader) {
        this.loader = loader;
    }
    
    public void setCacheSeconds(int cacheSeconds) {
        this.cacheTimeout = cacheSeconds * 1000;
    }

    public void setCacheMilliSeconds(int cacheTimeout) {
        this.cacheTimeout = cacheTimeout;
    }

    public void setAsynchronousRefresh(boolean asynchronousRefresh) {
        this.asynchronousRefresh = asynchronousRefresh;
    }
    
    public void setRefreshInterval(int refreshInterval) {
        this.refreshInterval = refreshInterval;
    }

    public void afterPropertiesSet() {
        if (this.loader == null) {
            throw new BeanInitializationException("JavaBean property 'loader' not set");
        }
        if (this.refreshInterval > 0) {
            this.refreshThread = new RefreshThread(this.refreshInterval);
            this.refreshThread.start();
        }
    }
    
    public void destroy() {
        if (this.refreshThread != null) {
            this.refreshThread.interrupt();
        }
    }

    public Object get(Object identifier) throws Exception {
        if (identifier == null) {
            throw new IllegalArgumentException("Cache identifiers cannot be NULL");
        }
        if (this.cacheTimeout <= 0) {
            if (logger.isDebugEnabled()) {
                logger.debug("Returning uncached object: '" + identifier + "'");
            }
            return this.loader.load(identifier);
        }
        Item item = (Item) this.cache.get(identifier);
        if (item == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Caching object: '" + identifier + "'");
            }
            cacheItem(identifier);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Returning object '" + identifier + "' from cache");
        }
        item = (Item) this.cache.get(identifier);
        if (item.getTimestamp().getTime() + this.cacheTimeout <= System.currentTimeMillis()) {
            if (this.asynchronousRefresh) {
                triggerSingleRefresh(identifier);
            } else {
                cacheItem(identifier);
                item = (Item) this.cache.get(identifier);
            }
        }
        return item.getObject();
    }
    
    
    private void cacheItem(Object identifier) throws Exception {

        Item item = (Item) this.cache.get(identifier);
        long now = new Date().getTime();

        if (item == null ||
            (item.getTimestamp().getTime() + this.cacheTimeout <= now)) {
            Object object = this.loader.load(identifier);
            this.cache.put(identifier, new Item(object));
            logger.info("Cached object '" + identifier + "'");
        }
    }


    private void triggerSingleRefresh(final Object identifier) {
        Runnable fetcher = new Runnable() {
           public void run() {
              try {
                 loader.load(identifier);
              } catch (Exception e) {
                 logger.info("Error refreshing object '" + identifier + "'", e);
              }
           }
        };
        new Thread(fetcher).start();
    }


    private synchronized void refreshExpired() {
        List refreshList = new ArrayList();

        for (Iterator i = this.cache.keySet().iterator(); i.hasNext();) {
            Object identifier = i.next();
            Item item = (Item) this.cache.get(identifier);

            long now = new Date().getTime();
            if (item.getTimestamp().getTime() + this.cacheTimeout < now) {
                refreshList.add(identifier);
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Checking expired items: " + refreshList.size()
                         + " expired items found");
        }

        for (Iterator i = refreshList.iterator(); i.hasNext();) {
            Object identifier = i.next();
            try {
                cacheItem(identifier);
            } catch (Throwable t) {
                logger.warn("Unable to cache refresh cached object " + identifier, t);
            }
        }
    }

    private class Item {
        private Object object;
        private Date timestamp;

        public Item(Object object) {
            this.object = object;
            this.timestamp = new Date();
        }
        public Object getObject() {
            return this.object;
        }
        public Date getTimestamp() {
            return this.timestamp;
        }
    }

    private class RefreshThread extends Thread {

        private long sleepSeconds;
        private boolean alive = true;
    
        public RefreshThread(long sleepSeconds) {
            this.sleepSeconds = sleepSeconds;
        }
        
        public void run() {
            while (this.alive) {
                try {
                    sleep(1000 * this.sleepSeconds);
                    refreshExpired();
                } catch (InterruptedException e) {
                    this.alive = false;
                } catch (Throwable t) {
                    logger.warn("Caught exception in cleanup thread", t);
                }
            }
            logger.info("Terminating refresh thread");
        }
    }

}

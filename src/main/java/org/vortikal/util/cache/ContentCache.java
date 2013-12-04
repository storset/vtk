/* Copyright (c) 2007,2013, University of Oslo, Norway
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
import java.util.List;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * Utility class for caching content that is potentially expensive to
 * load for certain periods of time before refreshing it. Typical
 * examples of such content include network resources, compiled XML
 * stylesheets, etc. A {@link ContentCacheLoader} is responsible for
 * loading the actual items.
 *
 * <p>Note: This cache is not meant as a generalized read/write cache,
 * as it never shrinks (it does not contain a <code>remove()</code>
 * method, although a size limit can be specified). 
 *
 * <p>Configurable JavaBean properties:
 * <ul>
 *   <li><code>cacheLoader</code> - the {@link ContentCacheLoader}
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
 *   <li><code>maxItems</code> - the maximum number of items to allow
 *   in the cache. A negative number means no limit (the default).
 * </ul>
 *
 * @param <K> key of the cached objects
 * @param <V> value of the cached objects
 * 
 * XXX Should probably not allow unlimited number of asynchronous refresh threads
 * to be created. What if the loader is slow and a common and expired key is requested
 * several times ? That will dispatch a new async refresh thread for each
 * get on that key, which will all hit loader pretty hard or bog down system.
 * Switch to a queue and fixed max number of refresh threads instead ?
 */
public final class ContentCache<K, V> implements InitializingBean, DisposableBean {
    
    private static Log logger = LogFactory.getLog(ContentCache.class);    

    private String name;
    private ContentCacheLoader<K, V> loader;
    private long cacheTimeoutMilliSeconds;
    private Ehcache cache = null;
    private boolean asynchronousRefresh = false;
    private int refreshInterval = -1;
    private RefreshThread refreshThread;
    private int maxItems = -1;
    boolean useSharedCaches = false;
    
    public void setName(String name) {
        this.name = name;
    }

    public void setCacheLoader(ContentCacheLoader<K, V> loader) {
        this.loader = loader;
    }
    
    public void setCacheSeconds(long cacheSeconds) {
        this.cacheTimeoutMilliSeconds = cacheSeconds * 1000;
    }

    public void setCacheMilliSeconds(long cacheTimeout) {
        this.cacheTimeoutMilliSeconds = cacheTimeout;
    }

    public void setAsynchronousRefresh(boolean asynchronousRefresh) {
        this.asynchronousRefresh = asynchronousRefresh;
    }
    
    public void setRefreshInterval(int refreshInterval) {
        this.refreshInterval = refreshInterval;
    }

    public void setMaxItems(int maxItems) {
        this.maxItems = maxItems;
    }

    public void setUseSharedCaches(boolean useSharedCaches) {
        this.useSharedCaches = useSharedCaches;
    }

    @Override
    public void afterPropertiesSet() {
        if (this.name == null) {
            throw new BeanInitializationException("JavaBean property 'name' not set");
        }
        if (this.loader == null) {
            throw new BeanInitializationException("JavaBean property 'loader' not set");
        }
        if (this.maxItems == 0) {
            throw new BeanInitializationException(
                "JavaBean property 'maxItems' has an illegal value: specify "
                + "either a positive or negative integer");
        }
        
        if (useSharedCaches) {
            // Since cache manager is a singleton, this will reuse and share named caches in the JVM
            CacheManager cacheManager = CacheManager.create();
            this.cache = cacheManager.getEhcache(name);
            if (this.cache == null) {
                if (this.maxItems < 0) {
                    // Will read configuration from XML file
                    this.cache = cacheManager.addCacheIfAbsent(name);
                } else {
                    // Explicit configuration
                    CacheConfiguration cacheConfig = new CacheConfiguration(name, maxItems);
                    this.cache = new Cache(cacheConfig);
                    cacheManager.addCache(cache);
                }
            }
        } else {
            int maxElementsInMemory = maxItems > 0 ? maxItems : 0;
            CacheConfiguration cacheConfig = new CacheConfiguration(name, maxElementsInMemory);
            this.cache = new Cache(cacheConfig);
            this.cache.initialise();
        }

        if (this.refreshInterval > 0) {
            this.refreshThread = new RefreshThread(this.refreshInterval);
            this.refreshThread.setName(this.name + ".periodic-refresh");
            this.refreshThread.start();
        }
    }
    
    @Override
    public void destroy() {
        if (this.refreshThread != null) {
            this.refreshThread.interrupt();
        }
    }

    public V get(K identifier) throws Exception {
        if (identifier == null) {
            throw new IllegalArgumentException("Cache identifiers cannot be NULL");
        }
        if (this.cacheTimeoutMilliSeconds <= 0) {
            if (logger.isTraceEnabled()) {
                logger.trace("Returning uncached object: '" + identifier + "'");
            }
            return this.loader.load(identifier);
        }

        @SuppressWarnings("unchecked")
        Item item = (Item)this.cache.get(identifier);
        if (item == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Caching object: '" + identifier + "'");
            } 
            item = cacheItem(identifier);
        }

        if (item.isTimedOut()) {
            if (logger.isTraceEnabled()) {
                logger.trace("Object '" + item.out() + "' is timed out");
            }
            if (this.asynchronousRefresh) {
                triggerAsynchronousRefresh(identifier);
            } else {
                item = cacheItem(identifier);
            }
        }
        if (logger.isTraceEnabled()) {
            logger.trace("Returning object '" + item + "' from cache");
        }
        return (V)item.getObjectValue();
    }
    
    /**
     * 
     * @param identifier cache key to pass to loader
     * @return newly loaded item, or item already in cache if it has not
     *         expired.
     * @throws Exception in case of loader failure
     */
    private Item cacheItem(K identifier) throws Exception {

        @SuppressWarnings("unchecked")
        Item item = (Item)this.cache.getQuiet(identifier);
        if (item == null || item.isTimedOut()) {
            if (logger.isTraceEnabled()) {
                logger.trace("Loading for '" + identifier + "', existing item=" + item);
            }
            V object = this.loader.load(identifier);
            Item newItem = new Item(identifier, object);
            this.cache.put(newItem);
            item = newItem;
        }
        return item;
    }


    private void triggerAsynchronousRefresh(final K identifier) {
        Runnable fetcher = new Runnable() {
           @Override
           public void run() {
              try {
                 cacheItem(identifier);
              } catch (Exception e) {
                 logger.info("Error refreshing object '" + identifier + "'", e);
              }
           }
        };
        new Thread(fetcher, this.name + ".async-refresh").start();
    }

   
    private synchronized void refreshExpired() {
        int size = this.cache.getSize();

        List<K> refreshList = new ArrayList<K>();
        for (K identifier: (List<K>)cache.getKeys()) {
            Item item = (Item)cache.getQuiet(identifier);
            if (item.isTimedOut())
            {
                refreshList.add(identifier);
            }
        }
            
        if (logger.isTraceEnabled()) {
            logger.trace("Checking expired items: " + refreshList.size()
                         + " expired items found (of total " + size + ")");
        }

        for (K identifier: refreshList) {
            try {
                cacheItem(identifier);
                if (logger.isDebugEnabled()) {
                    logger.debug("Refreshed expired cache item: '" + identifier + "'");
                }
            } catch (Throwable t) {
                logger.warn("Unable to refresh cached object " + identifier, t);
            }
        }
    }

    public int getSize() {
        return this.cache.getSize();
    }
    
    public void clear() {
        this.cache.removeAll();
    }
    
    private class Item extends Element {
        private static final long serialVersionUID = -1448257203055826861L;
        private long itemTimeoutMilliSeconds;
        private long createdAtMilliSeconds;
        
        public Item(final Object key, final Object value) {
            super(key, value);
            this.setEternal(true);
            this.createdAtMilliSeconds = System.currentTimeMillis();
            this.itemTimeoutMilliSeconds = cacheTimeoutMilliSeconds;
        }

        /**
         * Standard Eh elements only work with second resolution.
         * We override and use milliseconds instead.
         * This should make the tests work, but EhCache may use other methods internally... :-/
         */
        public boolean isTimedOut() {
            return (createdAtMilliSeconds + itemTimeoutMilliSeconds) < System.currentTimeMillis();
        }
        
        /**
         * Argh! toString() is declared "final" in ancestor so we use this instead.
         * @return
         */
        public String out() {
            return toString() + "[createdAtMs=" + createdAtMilliSeconds + ", itemTimeOutMs=" + itemTimeoutMilliSeconds + "]";
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

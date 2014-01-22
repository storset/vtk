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

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import net.sf.ehcache.Element;
import net.sf.ehcache.constructs.blocking.SelfPopulatingCache;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;

/**
 * Thin {@link ContentCache} layer above a self-populating ("pull-through") Ehcache
 * instance. All aspects of the cache behavior and item loading are controlled
 * by configuration of the injected cache instance underlying this {@link ContentCache}.
 * 
 * TODO async refresh is currently not done. However, at most one thread will
 * load a given key at any time, other threads requesting the same key will block until
 * the first thread has finished loading. This is good, but might not be as hiccup-free as
 * we want. Ehcache also allows to set a blocking timeout, which we can consider
 * using to prevent thread pileups due to slow loading.
 * 
 * @param <K> key type for the cache
 * @param <V> value type for the cache
 * 
 * TODO Add test case for this impl.
 */
public class EhContentCache<K, V>  implements ContentCache<K, V>, InitializingBean, DisposableBean {
    
    private SelfPopulatingCache cache;
    private boolean requireSerializable = true;
    private int refreshIntervalSeconds = -1;
    private ScheduledExecutorService refreshExecutor;
    
    @Override
    public void afterPropertiesSet() throws Exception {
        // TODO one refresh thread for each vhost with shared Ehcache is unnecessary, but harmless.
        // A fix might be to expose refresh as a ContentCache API call, and have an external
        // static singleton common to all vhosts in JVM, which does refresh on shared Ehcache instances.
        if (refreshIntervalSeconds > 0) {
            refreshExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory(){
                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, cache.getName() + ".refresh");
                }
            });
            refreshExecutor.scheduleWithFixedDelay(new Runnable() {
                @Override
                public void run() {
                    cache.refresh();
                }
            }, refreshIntervalSeconds, refreshIntervalSeconds, TimeUnit.SECONDS);
        }
    }
    
    @Override
    public V get(K identifier) throws Exception {
        if (identifier == null) {
            throw new IllegalArgumentException("Cache identifiers cannot be null");
        }
        
        // TODO cache exceptions for a short time (grace time) before letting loaders retry.
        // This may prove useful when loaders are slow and eventually fail (timeout on web resources for instance).
        Element e = cache.get(identifier);
        if (e != null) {
            if (requireSerializable) {
                return (V) e.getValue();
            } else {
                return (V) e.getObjectValue();
            }
        }
        return null; // consider throwing exception here instead. (null Element is impossible with SelfPopulatingCache.)
    }
    
    @Override
    public int getSize() {
        return cache.getSize();
    }
    
    @Override
    public void clear() {
        cache.removeAll();
    }
    
    /**
     * Set the self-populating Ehcache instance used to back this content cache.
     * @param cache 
     */
    @Required
    public void setCache(SelfPopulatingCache cache) {
        this.cache = cache;
    }

    @Override
    public void destroy() throws Exception {
        if (refreshExecutor != null) {
            refreshExecutor.shutdown();
        }
    }

    /**
     * @param refreshIntervalSeconds the refreshIntervalSeconds to set
     */
    public void setRefreshIntervalSeconds(int refreshIntervalSeconds) {
        this.refreshIntervalSeconds = refreshIntervalSeconds;
    }

    /**
     * Whether to use Ehcache API which requires keys and values to be
     * serializable or not.
     * @param requireSerializable 
     */
    public void setRequireSerializable(boolean requireSerializable) {
        this.requireSerializable = requireSerializable;
    }
    
}

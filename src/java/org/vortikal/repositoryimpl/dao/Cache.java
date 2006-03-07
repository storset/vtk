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
package org.vortikal.repositoryimpl.dao;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentReaderHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;

import org.vortikal.repositoryimpl.Resource;
import org.vortikal.util.repository.URIUtil;


/**
 * Very simple resource cache, wrapping around a {@link DataAccessor}.
 * Resources are cached when loaded (and stored).  Resources are
 * evicted in the following cases:
 *
 * <ul>
 *   <ul>the cache grows beyond its maximum size
 *   <ul>the resource itself is deleted
 *   <ul>the resource was locked and the lock has timed out
 *   <ul>a modification is made to one of its ancestors
 *   <ul>the resource's content is modified
 * </ul>
 *
 * <p>When the cache reaches its maximum size, a FIFO scheme is
 * applied; a configurable percentage of the items are removed, the
 * starting with the oldest ones.
 *
 * <p>Configurable JavaBean properties:
 * <ul>
 *   <li><code>wrappedAccessor</code> - the {@link DataAccessor} to
 *   act as a cache for.
 *   <li><code>maxItems</code> - an positive integer denoting the
 *   cache size. The default value is <code>1000</code>.
 *   <li><code>evictionRatio</code> - a number between 0 and 1
 *   specifying the number of items (as a percentage of
 *   <code>maxItems</code>) to remove when the cache is filled up. The
 *   default value is <code>0.1</code> (10%).
 *   <li><code>gatherStatistics</code> - a boolean specifying whether
 *   or not to gather hit/miss statistics during operation. The
 *   default value is <code>false</code>.
 * </ul>
 */
public class Cache implements DataAccessor, InitializingBean {
    private Log logger = LogFactory.getLog(this.getClass());
    private DataAccessor wrappedAccessor;
    private LockManager lockManager = new LockManager();
    private int maxItems = 1000;
    private double evictionRatio = 0.1;
    private int removeItems;
    private Items items = new Items();

    private boolean gatherStatistics = false;
    private long hits = 0;
    private long misses = 0;
    

    public boolean validate() throws IOException {
        return wrappedAccessor.validate();
    }

    public void setMaxItems(int maxItems) {
        if (maxItems <= 0) {
            throw new IllegalArgumentException("Cache size must be a positive number");
        }

        this.maxItems = maxItems;
    }

    public void setEvictionRatio(double evictionRatio) {
        if (evictionRatio <= 0 || evictionRatio >= 1) {
            throw new IllegalArgumentException(
                "JavaBean property 'evictionRatio' must be a "
                + "number between 0 and 1");
        }
        this.evictionRatio = evictionRatio;
    }
    

    public void setWrappedAccessor(DataAccessor wrappedAccessor) {
        this.wrappedAccessor = wrappedAccessor;
    }


    public void setGatherStatistics(boolean gatherStatistics) {
        this.gatherStatistics = gatherStatistics;
    }
    
    public long getMisses() {
        return this.misses;
    }


    public long getHits() {
        return this.hits;
    }
    
    
    public void afterPropertiesSet() {
        if (this.wrappedAccessor == null) {
            throw new BeanInitializationException(
                "JavaBean property 'wrappedAccessor' not set");
        }

        this.removeItems = (int) (maxItems * this.evictionRatio);

        if (this.removeItems == 0) {
            this.removeItems = 1;
        }
    }
    

    public Resource[] loadChildren(Resource parent) throws IOException {

        List found = new ArrayList();
        List notFound = new ArrayList();

        String[] uris = parent.getChildURIs();

        for (int i = 0; i < uris.length; i++) {

            Resource r = items.get(uris[i]);
            boolean lockTimedOut =
                (r != null && r.getLock() != null
                 && r.getLock().getTimeout().getTime() < System.currentTimeMillis());

            if (logger.isInfoEnabled() && lockTimedOut) {
                logger.info("Dropping cached copy of " + r.getURI()  + " (lock timed out)");
            }

            if (r == null || lockTimedOut) {
                notFound.add(uris[i]);
            } else {
                found.add(r);
            }
        }

        if (this.gatherStatistics) {
            updateStatistics(found.size(), notFound.size());
        }

        if (notFound.size() == 0) {
            return (Resource[]) found.toArray(new Resource[] {  });
        }

        found = new ArrayList();
        
        Resource[] resources = null;
        
        try {
            this.lockManager.lock(uris);

            if (logger.isDebugEnabled()) {
                logger.debug("Loading " + uris.length + " resources");
            }

            resources = this.wrappedAccessor.loadChildren(parent);

            for (int i = 0; i < resources.length; i++) {
                enterResource(resources[i]);
            }
        } finally {
            this.lockManager.unlock(uris);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("cache size : " + items.size());
        }

        return resources;
    }
    


    /**
     * Loads resource into cache (if it wasn't already there), returns
     * resource.
     *
     * @param uri a <code>String</code> value
     * @return a <code>Resource</code> value
     * @exception IOException if an error occurs
     */
    public Resource load(String uri) throws IOException {
        long start = System.currentTimeMillis();

        Resource r = items.get(uri);

        boolean lockTimedOut =
            (r != null && r.getLock() != null
             && r.getLock().getTimeout().getTime() < System.currentTimeMillis());

        if (logger.isInfoEnabled() && lockTimedOut) {
            logger.info("Dropping cached copy of " + r.getURI()  + " (lock timed out)");
        }


        if (r != null && ! lockTimedOut) {
            if (this.gatherStatistics) {
                updateStatistics(1, 0);
            }
            return r;
        }

        if (this.gatherStatistics) {
            updateStatistics(0, 1);
        }

        lockManager.lock(uri);

        try {
            r = this.items.get(uri);

            lockTimedOut =
                (r != null && r.getLock() != null
                 && r.getLock().getTimeout().getTime() < System.currentTimeMillis());

            if (r != null && ! lockTimedOut) {
                return r;
            }

            if (logger.isDebugEnabled()) {
                logger.debug("load from wrappedAccessor: " + uri);
            }

            r = this.wrappedAccessor.load(uri);

            if (r == null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("not found in wrappedAccessor: " + uri);
                }

                return null;
            }

            if (logger.isDebugEnabled()) {
                logger.debug("miss: " + uri);
            }

            enterResource(r);

            return r;
        } finally {
            lockManager.unlock(uri);

            if (logger.isDebugEnabled()) {
                logger.debug("load: took " +
                    (System.currentTimeMillis() - start) + " ms");
            }

            if (logger.isDebugEnabled()) {
                logger.debug("cache size : " + items.size());
            }
        }
    }

    public void store(Resource r) throws IOException {
        List uris = new ArrayList();

        uris.add(r.getURI());

        if (r.isCollection() && r.isDirtyACL()) {
            String testURI = r.getURI();

            if (!testURI.equals("/")) {
                testURI += "/";
            }

            for (Iterator iterator = this.items.uriSet().iterator();
                    iterator.hasNext();) {
                String uri = (String) iterator.next();

                if (uri.startsWith(testURI) && !uri.equals("/")) {
                    uris.add(uri);
                }
            }
        }

        this.lockManager.lock(uris);

        try {
            this.wrappedAccessor.store(r);

            for (Iterator i = uris.iterator(); i.hasNext();) {
                String uri = (String) i.next();

                if (!uri.equals(r.getURI()) && this.items.containsURI(uri)) {
                    this.items.remove(uri);
                }
            }
        } finally {
            this.lockManager.unlock(uris);

            if (logger.isDebugEnabled()) {
                logger.debug("cache size : " + items.size());
            }
        }
    }

    public void copy(Resource r, String destURI, boolean copyACLs,
                     boolean setOwner, String owner) throws IOException {
        
        List uris = new ArrayList();
        uris.add(r.getURI());
        uris.add(destURI);

        String destParentURI = URIUtil.getParentURI(destURI);
        if ((destParentURI != null) && this.items.containsURI(destParentURI)) {
            uris.add(destParentURI);
        }

        String srcParentURI = URIUtil.getParentURI(r.getURI());
        if ((srcParentURI != null) && !uris.contains(srcParentURI) &&
            this.items.containsURI(srcParentURI)) {
            uris.add(srcParentURI);
        }

        long timestamp = System.currentTimeMillis();
        this.lockManager.lock(uris);
        long duration = System.currentTimeMillis() - timestamp;
        System.out.println("CACHE__COPY: " + r.getURI() + " -> " + destURI
                           + ": lock took " + duration + " ms");

        try {
            this.wrappedAccessor.copy(r, destURI, copyACLs, setOwner, owner);

            if (this.items.containsURI(destParentURI)) {
                this.items.remove(destParentURI);
            }

        } finally {
            timestamp = System.currentTimeMillis();
            this.lockManager.unlock(uris);

            duration = System.currentTimeMillis() - timestamp;
            System.out.println("CACHE__COPY: " + r.getURI() + " -> " + destURI
                               + ": unlock took " + duration + " ms");
            if (logger.isDebugEnabled()) {
                logger.debug("cache size : " + items.size());
            }
        }

    }
    


    public void delete(Resource r) throws IOException {
        List uris = new ArrayList();

        uris.add(r.getURI());

        if (r.isCollection()) {
            for (Iterator iterator = this.items.uriSet().iterator();
                    iterator.hasNext();) {
                String uri = (String) iterator.next();

                if (uri.startsWith(r.getURI()) && !uri.equals(r.getURI())) {
                    uris.add(uri);
                }
            }
        }

        String parentURI = URIUtil.getParentURI(r.getURI());

        if ((parentURI != null) && this.items.containsURI(parentURI)) {
            uris.add(parentURI);
        }

        this.lockManager.lock(uris);

        try {
            this.wrappedAccessor.delete(r);

            for (Iterator i = uris.iterator(); i.hasNext();) {
                String uri = (String) i.next();

                if (this.items.containsURI(uri)) {
                    this.items.remove(uri);
                }
            }
        } finally {
            this.lockManager.unlock(uris);

            if (logger.isDebugEnabled()) {
                logger.debug("cache size : " + items.size());
            }
        }
    }

    public InputStream getInputStream(Resource resource)
        throws IOException {
        return this.wrappedAccessor.getInputStream(resource);
    }

    public OutputStream getOutputStream(Resource resource)
        throws IOException {
        try {
            this.lockManager.lock(resource.getURI());
            this.items.remove(resource.getURI());

            return this.wrappedAccessor.getOutputStream(resource);
        } finally {
            this.lockManager.unlock(resource.getURI());
        }
    }

    public long getContentLength(Resource resource) throws IOException {
        return this.wrappedAccessor.getContentLength(resource);
    }

    public String[] listSubTree(Resource parent) throws IOException {
        return this.wrappedAccessor.listSubTree(parent);
    }

    public void deleteExpiredLocks() throws IOException {
        this.wrappedAccessor.deleteExpiredLocks();
    }
    

    public String[] discoverLocks(Resource r) throws IOException {
        return this.wrappedAccessor.discoverLocks(r);
    }

    public void addChangeLogEntry(String loggerID, String loggerType,
                                  String uri, String operation, int resourceId,
                                  boolean collection, boolean recurse) throws IOException {
        this.wrappedAccessor.addChangeLogEntry(loggerID, loggerType, uri, operation,
                                               resourceId, collection, recurse);
    }

    /**
     * Clears all cache entries.
     *
     */
    public void clear() {
        synchronized (this.items) {
            this.items.clear();
        }
    }

    /**
     * Gets the number of resources currently in the cache.
     *
     * @return the number of resources in the cache.
     */
    public int size() {
        return this.items.size();
    }

    /**
     * Note: this method is not thread safe, lock uri first
     *
     * @param item a <code>Resource</code> value
     */
    private void enterResource(Resource item) {
        synchronized (this.items) {
            if (this.items.size() > (this.maxItems - 1)) {

                long startTime = System.currentTimeMillis();

                for (int i = 0; i < this.removeItems; i++) {
                    this.items.removeOldest();
                }

                long processingTime = System.currentTimeMillis() - startTime;
                if (logger.isInfoEnabled()) {
                    logger.info("Maximum cache size (" + this.maxItems
                                + ") reached, removed " + this.removeItems
                                + " oldest items in " + processingTime + " ms");
                }
                
            }

            this.items.put(item.getURI(), item);
        }
    }


    private synchronized void updateStatistics(long hits, long misses) {
        if ((this.hits > (Long.MAX_VALUE - this.hits))
            || (this.misses > (Long.MAX_VALUE) - misses)) {

            if (logger.isInfoEnabled()) {
                logger.info("Number of hits/misses too big, resetting counters");
            }

            this.hits = 0;
            this.misses = 0;
        }

        this.hits += hits;
        this.misses += misses;
    }
    

    private class Items {
        private Map map = new ConcurrentReaderHashMap();
        private Item in = null;
        private Item out = null;

        public void clear() {
            this.map.clear();
            this.in = this.out = null;
        }

        public Resource get(String uri) {
            Item i = (Item) this.map.get(uri);

            if (i != null) {
                return i.getResource();
            }

            return null;
        }

        public synchronized void put(String uri, Resource resource) {
            Item i = new Item(resource);

            this.map.put(uri, i);

            //             synchronized(this) {
            if (this.in != null) {
                this.in.newer = i;
            }

            i.older = this.in;
            this.in = i;

            if (this.out == null) {
                this.out = in;
            }

            //              }
        }

        public synchronized void remove(String uri) {
            Item i = (Item) this.map.get(uri);

            //             synchronized(this) {
            if (i != null) {
                if (i.older != null) {
                    i.older.newer = i.newer;
                } else {
                    this.out = i.newer;
                }

                if (i.newer != null) {
                    i.newer.older = i.older;
                } else {
                    this.in = i.older;
                }
            }

            //             }
            this.map.remove(uri);
        }

        public int size() {
            return this.map.size();
        }

        public Set uriSet() {
            return this.map.keySet();
        }

        public boolean containsURI(String uri) {
            return this.map.containsKey(uri);
        }

        public synchronized void hit(String uri) {
            Item i = (Item) this.map.get(uri);

            //             synchronized(this) {
            //             if (logger.isDebugEnabled())
            //                logger.debug("Before hit: " + dump());
            if ((i != null) && (i != in)) {
                /* Remove i from list: */
                if (i != this.out) {
                    i.older.newer = i.newer;
                } else {
                    this.out = i.newer;
                }

                i.newer.older = i.older;

                /* Insert i first in list: */
                this.in.newer = i;
                i.older = this.in;
                i.newer = null;
                this.in = i;
            }

            //             if (logger.isDebugEnabled())
            //                 logger.debug("After hit: " + dump());
            //             }
        }

        public synchronized void removeOldest() {
            //             synchronized(this) {
            if (this.out != null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Removing oldest item " +
                        out.getResource().getURI());
                }

                this.map.remove(this.out.getResource().getURI());

                if (this.in != this.out) {
                    this.out.newer.older = null;
                    this.out = this.out.newer;
                } else {
                    this.in = this.out = null;
                }
            }
        }

        private String dump() {
            StringBuffer s = new StringBuffer();
            Item i = this.in;

            while (i != null) {
                s.append("[");
                s.append(i.getResource().getURI());
                s.append("]");
                i = i.older;
            }

            return s.toString();
        }
    }

    private class Item {
        Item older = null;
        Item newer = null;
        Resource resource;

        Item(Resource resource) {
            this.resource = resource;
        }

        Resource getResource() {
            return this.resource;
        }
    }

    // --------------  New Stuff

    public String[] discoverACLs(Resource resource) throws IOException {
        return this.wrappedAccessor.discoverACLs(resource);
    }
    

}

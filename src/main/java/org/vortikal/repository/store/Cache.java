/* Copyright (c) 2004, 2005, 2006, 2007, University of Oslo, Norway
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
package org.vortikal.repository.store;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.ChangeLogEntry;
import org.vortikal.repository.Path;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.ResourceImpl;
import org.vortikal.security.Principal;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentReaderHashMap;


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
 * applied; a configurable percentage of the items are removed,
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
    

    public boolean validate() throws DataAccessException {
        return this.wrappedAccessor.validate();
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
    

    @Required
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

        this.removeItems = (int) (this.maxItems * this.evictionRatio);

        if (this.removeItems == 0) {
            this.removeItems = 1;
        }
    }
    

    public ResourceImpl[] loadChildren(ResourceImpl parent) throws DataAccessException {

        List<ResourceImpl> found = new ArrayList<ResourceImpl>();
        List<Path> notFound = new ArrayList<Path>();

        for (Path uri: parent.getChildURIs()) {

            ResourceImpl r = this.items.get(uri);
            boolean lockTimedOut =
                (r != null && r.getLock() != null
                 && r.getLock().getTimeout().getTime() < System.currentTimeMillis());

            if (this.logger.isInfoEnabled() && lockTimedOut) {
                this.logger.info("Dropping cached copy of " + r.getURI()  + " (lock timed out)");
            }

            if (r == null || lockTimedOut) {
                notFound.add(uri);
            } else {
                found.add(r);
            }
        }

        if (this.gatherStatistics) {
            updateStatistics(found.size(), notFound.size());
        }

        if (notFound.size() == 0) {
            return found.toArray(new ResourceImpl[found.size()]);
        }

        ResourceImpl[] resources = null;
        
        
        List<Path> obtainedLocks = this.lockManager.lock(parent.getChildURIs());
        try {

            if (this.logger.isDebugEnabled()) {
                this.logger.debug("Loading " + parent.getChildURIs().length + " resources");
            }

            resources = this.wrappedAccessor.loadChildren(parent);

            for (ResourceImpl resourceImpl: resources) {
                enterResource(resourceImpl);
            }
        } finally {
            this.lockManager.unlock(obtainedLocks); // Release URI sync locks
        }

        if (this.logger.isDebugEnabled()) {
            this.logger.debug("cache size : " + this.items.size());
        }

        return resources;
    }
    


    /**
     * Loads resource into cache (if it wasn't already there), returns
     * resource.
     *
     * @param uri a <code>String</code> value
     * @return a <code>Resource</code> value
     * @exception DataAccessException if an error occurs
     */
    public ResourceImpl load(Path uri) throws DataAccessException {
        long start = System.currentTimeMillis();

        ResourceImpl r = this.items.get(uri);

        boolean lockTimedOut =
            (r != null && r.getLock() != null
             && r.getLock().getTimeout().getTime() < System.currentTimeMillis());

        if (this.logger.isInfoEnabled() && lockTimedOut) {
            this.logger.info("Dropping cached copy of " + r.getURI()  + " (lock timed out)");
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

        List<Path> lock = this.lockManager.lock(new Path[]{uri});

        try {
            r = this.items.get(uri);

            lockTimedOut =
                (r != null && r.getLock() != null
                 && r.getLock().getTimeout().getTime() < System.currentTimeMillis());

            if (r != null && ! lockTimedOut) {
                return r;
            }

            if (this.logger.isDebugEnabled()) {
                this.logger.debug("load from wrappedAccessor: " + uri);
            }

            r = this.wrappedAccessor.load(uri);

            if (r == null) {
                if (this.logger.isDebugEnabled()) {
                    this.logger.debug("not found in wrappedAccessor: " + uri);
                }

                return null;
            }

            if (this.logger.isDebugEnabled()) {
                this.logger.debug("miss: " + uri);
            }

            enterResource(r);

            return r;
        } finally {
            this.lockManager.unlock(lock); // Release URI sync lock

            if (this.logger.isDebugEnabled()) {
                this.logger.debug("load: took " +
                    (System.currentTimeMillis() - start) + " ms");
            }

            if (this.logger.isDebugEnabled()) {
                this.logger.debug("cache size : " + this.items.size());
            }
        }
    }

    public void storeACL(ResourceImpl r) throws DataAccessException {
        List<Path> uris = new ArrayList<Path>();
        uris.add(r.getURI());
        
        if (r.isCollection()) {
            Path testURI = r.getURI();

            for (Path uri: this.items.uriSet()) {
                if (testURI.isAncestorOf(uri) && !uri.isRoot()) {
                    uris.add(uri);
                }
            }
        }
        
        List<Path> lockedUris = this.lockManager.lock(uris);
        try {
            this.wrappedAccessor.storeACL(r); // Persist
            this.items.remove(uris);          // Purge all affected items from cache
//            for (Path uri: uris) {
//                if (this.items.containsURI(uri)) {
//                    this.items.remove(uri);
//                }
//            }
        } finally {
            this.lockManager.unlock(lockedUris); // Release URI sync lock

            if (this.logger.isDebugEnabled()) {
                this.logger.debug("cache size : " + this.items.size());
            }
        }
    }
    

    public void store(ResourceImpl r) throws DataAccessException {
        List<Path> uris = new ArrayList<Path>();

        uris.add(r.getURI());

        List<Path> lockedUris = this.lockManager.lock(uris);
        try {
            this.wrappedAccessor.store(r); // Persist
            this.items.remove(uris);       // Purge item from cache

//            for (Path uri: uris) {
//                if (this.items.containsURI(uri)) {
//                    this.items.remove(uri);
//                }
//            }
        } finally {
            this.lockManager.unlock(lockedUris); // Release URI sync lock

            if (this.logger.isDebugEnabled()) {
                this.logger.debug("cache size : " + this.items.size());
            }
        }
    }

    public void copy(ResourceImpl r, ResourceImpl dest, PropertySet newResource, boolean copyACLs,
                     PropertySet fixedProperties) throws DataAccessException {
        
        Path destURI = newResource.getURI();

        List<Path> uris = new ArrayList<Path>();
        uris.add(r.getURI());
        uris.add(destURI);

        Path destParentURI = dest.getURI();
        if (this.items.containsURI(destParentURI)) {
            uris.add(destParentURI);
        }

        Path srcParentURI = r.getURI().getParent();
        if ((srcParentURI != null) && !uris.contains(srcParentURI) &&
            this.items.containsURI(srcParentURI)) {
            uris.add(srcParentURI);
        }

        List<Path> lockedUris = this.lockManager.lock(uris);

        try {
            // Persist copy operation
            this.wrappedAccessor.copy(r, dest, newResource, copyACLs, fixedProperties);
            // Purge affected destination parent from cache
            this.items.remove(destParentURI);        

//            if (this.items.containsURI(destParentURI)) {
//                this.items.remove(destParentURI);
//            }
        } finally {
            this.lockManager.unlock(lockedUris); // Release URI sync locks

            if (this.logger.isDebugEnabled()) {
                this.logger.debug("cache size : " + this.items.size());
            }
        }
    }
    

    public void move(ResourceImpl r, ResourceImpl newResource) throws DataAccessException {
        Path destURI = newResource.getURI();
        List<Path> uris = new ArrayList<Path>();
        uris.add(r.getURI());
        uris.add(destURI);
        if (r.isCollection()) {
            for (Path uri: this.items.uriSet()) {
                if (r.getURI().isAncestorOf(uri) && !uri.equals(r.getURI())) {
                    uris.add(uri);
                }
            }
        }
        
        Path destParentURI = newResource.getURI().getParent();
        if (this.items.containsURI(destParentURI)) {
            uris.add(destParentURI);
        }

        Path srcParentURI = r.getURI().getParent();
        if ((srcParentURI != null) && !uris.contains(srcParentURI) &&
            this.items.containsURI(srcParentURI)) {
            uris.add(srcParentURI);
        }
        
        List<Path> locks = this.lockManager.lock(uris);

        try {
            this.wrappedAccessor.move(r, newResource); // Persist move operation
            this.items.remove(uris);                   // Purge all affected items from cache

//            for (Path uri: uris) {
//                if (this.items.containsURI(uri)) {
//                    this.items.remove(uri);
//                }
//            }
        } finally {
            this.lockManager.unlock(locks); // Release URI sync locks

            if (this.logger.isDebugEnabled()) {
                this.logger.debug("cache size : " + this.items.size());
            }
        }
    }
    

    public void delete(ResourceImpl r) throws DataAccessException {
        List<Path> uris = new ArrayList<Path>();

        uris.add(r.getURI());

        if (r.isCollection()) {
            for (Path uri: this.items.uriSet()) {
                if (r.getURI().isAncestorOf(uri) && !uri.equals(r.getURI())) {
                    uris.add(uri);
                }
            }
        }

        Path parentURI = r.getURI().getParent();

        if ((parentURI != null) && this.items.containsURI(parentURI)) {
            uris.add(parentURI);
        }

        List<Path> locks = this.lockManager.lock(uris);

        try {
            this.wrappedAccessor.delete(r); // Dispatch to wrapped DAO for persistence
            this.items.remove(uris);        // Purge all affected items from cache 

//            for (Path uri: uris) {
//                if (this.items.containsURI(uri)) {
//                    this.items.remove(uri);
//                }
//            }
        } finally {
            this.lockManager.unlock(locks); // Release URI sync locks

            if (this.logger.isDebugEnabled()) {
                this.logger.debug("cache size : " + this.items.size());
            }
        }
    }

    public void deleteExpiredLocks(Date expireDate) throws DataAccessException {
        this.wrappedAccessor.deleteExpiredLocks(expireDate);
    }
    

    public Path[] discoverLocks(Path uri) throws DataAccessException {
        return this.wrappedAccessor.discoverLocks(uri);
    }

    /**
     * Clears all cache entries.
     *
     */
    public void clear() {
        synchronized (this.items) {
            this.items.clear();
            this.lockManager = new LockManager();
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
    private void enterResource(ResourceImpl item) {
        synchronized (this.items) {
            if (this.items.size() > (this.maxItems - 1)) {

                long startTime = System.currentTimeMillis();

                for (int i = 0; i < this.removeItems; i++) {
                    this.items.removeOldest();
                }

                long processingTime = System.currentTimeMillis() - startTime;
                if (this.logger.isInfoEnabled()) {
                    this.logger.info("Maximum cache size (" + this.maxItems
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

            if (this.logger.isInfoEnabled()) {
                this.logger.info("Number of hits/misses too big, resetting counters");
            }

            this.hits = 0;
            this.misses = 0;
        }

        this.hits += hits;
        this.misses += misses;
    }

    
    public void dump(java.io.PrintStream out) {
        this.items.dump(out);
    }

    private class Item {
        Item older = null;
        Item newer = null;
        ResourceImpl resource;

        Item(ResourceImpl resource) {
            this.resource = resource;
        }

        ResourceImpl getResource() {
            return this.resource;
        }
    }

    private class Items {
        @SuppressWarnings("unchecked")
        private Map<Path, Item> map = new ConcurrentReaderHashMap();
        private Item in = null;
        private Item out = null;

        public void clear() { // Synchronized externally
            this.map.clear();
            this.in = this.out = null;
        }

        public ResourceImpl get(Path uri) {
            Item i = this.map.get(uri);

            if (i != null) {
                return i.getResource();
            }

            return null;
        }

        public synchronized void put(Path uri, ResourceImpl resource) {
            Item i = new Item(resource);

            this.map.put(uri, i);

            if (this.in != null) {
                this.in.newer = i;
            }

            i.older = this.in;
            this.in = i;

            if (this.out == null) {
                this.out = this.in;
            }
        }

        public synchronized void remove(Path uri) {
            Item i = this.map.get(uri);

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

            this.map.remove(uri);
        }
        
        public synchronized void remove(List<Path> uris) {
            for (Path uri: uris) {
                remove(uri);
            }
        }

        public int size() {
            return this.map.size();
        }

        public Set<Path> uriSet() {
            return this.map.keySet();
        }

        public boolean containsURI(Path uri) {
            return this.map.containsKey(uri);
        }

        public synchronized void hit(Path uri) {
            Item i = this.map.get(uri);

            if ((i != null) && (i != this.in)) {
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
        }

        public synchronized void removeOldest() {
            if (this.out != null) {
                if (Cache.this.logger.isDebugEnabled()) {
                    Cache.this.logger.debug("Removing oldest item " +
                        this.out.getResource().getURI());
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

        public void dump(java.io.PrintStream out) {
            Item i = this.in;

            while (i != null) {
                out.println(i.getResource().getURI());
                i = i.older;
            }
        }
    }

    public Path[] discoverACLs(Path uri) throws DataAccessException {
        return this.wrappedAccessor.discoverACLs(uri);
    }
    
    public Set<Principal> discoverGroups() throws DataAccessException {
        return this.wrappedAccessor.discoverGroups();
    }


}

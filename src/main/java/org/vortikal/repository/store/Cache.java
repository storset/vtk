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
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.PathLockManager;
import org.vortikal.repository.Path;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.RecoverableResource;
import org.vortikal.repository.ResourceImpl;
import org.vortikal.security.Principal;

/**
 * Very simple resource cache, wrapping around a {@link DataAccessor}. Resources
 * are cached when loaded (and stored). Resources are evicted in the following
 * cases:
 * 
 * <ul>
 * <ul>
 * the cache grows beyond its maximum size
 * <ul>
 * the resource itself is deleted
 * <ul>
 * the resource was locked and the lock has timed out
 * <ul>
 * a modification is made to one of its ancestors
 * <ul>
 * the resource's content is modified
 * </ul>
 * 
 * <p>
 * When the cache reaches its maximum size, a FIFO scheme is applied; a
 * configurable percentage of the items are removed, starting with the oldest
 * ones.
 * 
 * <p>
 * Configurable JavaBean properties:
 * <ul>
 * <li><code>wrappedAccessor</code> - the {@link DataAccessor} to act as a cache
 * for.
 * 
 * <li><code>maxItems</code> - an positive integer denoting the cache size. The
 * default value is <code>1000</code>.
 * 
 * <li><code>evictionRatio</code> - a number between 0 and 1 specifying the
 * number of items (as a percentage of <code>maxItems</code>) to remove when the
 * cache is filled up. The default value is <code>0.1</code> (10%).
 * 
 * <li><code>gatherStatistics</code> - a boolean specifying whether or not to
 * gather hit/miss statistics during operation. The default value is
 * <code>false</code>.
 * 
 * <li><code>loadChildrenSelectivelyThreshold</code> - Specifies threshold for
 * when children are loaded selectively from database, in
 * #loadChildren(ResourceImpl). If less than this percentage of children is
 * *missing* from cache, then *only* the missing children are loaded
 * invidiually, instead of loading absolutely all children from database
 * (regardless of their presence in the cache).
 * 
 * Example: If a resource has 1000 children, and 951 of those are currently
 * present in the cache, then 49 are missing. 49 is below the default threshold
 * of 5 percent, which will cause only the missing children to be loaded with
 * individual calls to the wrapped {@link DataAccessor#load(Path)}-method. If 51
 * were missing, then this would be above the threshold and all 1000 children
 * would be loaded by calling wrapped
 * {@link DataAccessor#loadChildren(ResourceImpl)}.
 * 
 * The number should be a float in the typical range of [0.01..0.1]. Don't set
 * it too high, as normally it's efficient to to load all children of a single
 * parent URI in one go. Only when the number of children gets high (in the
 * thousands) should selective loading of missing ones kick in. Default is 5
 * percent or 0.05.
 * </ul>
 */
@Deprecated
public class Cache implements DataAccessor, InitializingBean {

    private Log logger = LogFactory.getLog(this.getClass());
    private DataAccessor wrappedAccessor;
    private PathLockManager lockManager = new PathLockManager();
    private int maxItems = 1000;
    private double evictionRatio = 0.1;
    private int removeItems;
    private Items items = new Items(this.maxItems);

    private boolean gatherStatistics = false;
    private long hits = 0;
    private long misses = 0;

    private float loadChildrenSelectivelyThreshold = 0.05f;

    @Override
    public boolean validate() throws DataAccessException {
        return this.wrappedAccessor.validate();
    }

    public void setMaxItems(int maxItems) {
        if (maxItems <= 0) {
            throw new IllegalArgumentException("Cache size must be a positive number");
        }
        this.maxItems = maxItems;
        this.items = new Items(this.maxItems);
    }

    public void setEvictionRatio(double evictionRatio) {
        if (evictionRatio <= 0 || evictionRatio >= 1) {
            throw new IllegalArgumentException("JavaBean property 'evictionRatio' must be a "
                    + "number between 0 and 1");
        }
        this.evictionRatio = evictionRatio;
    }

    public void setLoadChildrenSelectivelyThreshold(float loadChildrenSelectivelyThreshold) {
        this.loadChildrenSelectivelyThreshold = loadChildrenSelectivelyThreshold;
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

    @Override
    public void afterPropertiesSet() {
        this.removeItems = (int) (this.maxItems * this.evictionRatio);

        if (this.removeItems == 0) {
            this.removeItems = 1;
        }
    }

    @Override
    public ResourceImpl[] loadChildren(ResourceImpl parent) throws DataAccessException {

        List<ResourceImpl> found = new ArrayList<ResourceImpl>();
        List<Path> notFound = new ArrayList<Path>();

        List<Path> childUris = parent.getChildURIs();

        for (Path uri : childUris) {

            ResourceImpl r = this.items.get(uri);
            boolean lockTimedOut = (r != null && r.getLock() != null && r.getLock().getTimeout().getTime() < System
                    .currentTimeMillis());

            if (this.logger.isInfoEnabled() && lockTimedOut) {
                this.logger.info("Dropping cached copy of " + r.getURI() + " (lock timed out)");
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
            // Every child was found in cache, and none of them had expired
            // locks.
            return found.toArray(new ResourceImpl[found.size()]);
        }

        ResourceImpl[] resources = null;

        List<Path> obtainedLocks = this.lockManager.lock(childUris, true);

        float notFoundRatio = (float) notFound.size() / childUris.size();

        if (notFoundRatio <= this.loadChildrenSelectivelyThreshold) {
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("loadChildren(): Less than " + (this.loadChildrenSelectivelyThreshold * 100)
                        + " percent of children missing from cache for URI '" + parent.getURI() + "', " + "loading "
                        + notFound.size() + " missing or expired children individually.");
            }

            try {
                // Below threshold for number of missing children in cache, we
                // load the missing ones selectively from database for better
                // efficiency.
                for (Path missingChild : notFound) {
                    ResourceImpl resourceImpl = this.wrappedAccessor.load(missingChild);
                    if (resourceImpl != null) {
                        found.add(resourceImpl);
                        enterResource(resourceImpl);
                    }
                }

            } finally {
                this.lockManager.unlock(obtainedLocks, true); // Release URI sync
                // locks
            }

            return found.toArray(new ResourceImpl[found.size()]);

        } else {
            // Above threshold of children missing from cache or child too list
            // too large
            // for current cache size. Load everything in one go.
            try {

                if (this.logger.isDebugEnabled()) {
                    this.logger.debug("loadChildren(): Loading all children of URI '" + parent.getURI()
                            + "' from database, " + parent.getChildURIs().size() + " resources.");
                }

                resources = this.wrappedAccessor.loadChildren(parent);

                for (ResourceImpl resourceImpl : resources) {
                    enterResource(resourceImpl); // Put in cache, replace any
                    // existing (full refresh).
                }
            } finally {
                this.lockManager.unlock(obtainedLocks, true); // Release URI sync
                // locks
            }

        }

        if (this.logger.isDebugEnabled()) {
            this.logger.debug("cache size : " + this.items.size());
        }

        return resources;
    }

    /**
     * Loads resource into cache (if it wasn't already there), returns resource.
     * 
     * @param uri
     *            a <code>String</code> value
     * @return a <code>Resource</code> value
     * @exception DataAccessException
     *                if an error occurs
     */
    @Override
    public ResourceImpl load(Path uri) throws DataAccessException {
        long start = System.currentTimeMillis();

        ResourceImpl r = this.items.get(uri);

        boolean davLockTimedOut = (r != null && r.getLock() != null && r.getLock().getTimeout().getTime() < System
                .currentTimeMillis());

        if (this.logger.isInfoEnabled() && davLockTimedOut) {
            this.logger.info("Dropping cached copy of " + r.getURI() + " (DAV lock timed out)");
        }

        if (r != null && !davLockTimedOut) {
            if (this.gatherStatistics) {
                updateStatistics(1, 0);
            }
            return r;
        }

        if (this.gatherStatistics) {
            updateStatistics(0, 1);
        }

        List<Path> lock = this.lockManager.lock(uri, true);

        try {
            r = this.items.get(uri);

            davLockTimedOut = (r != null && r.getLock() != null && r.getLock().getTimeout().getTime() < System
                    .currentTimeMillis());

            if (r != null && !davLockTimedOut) {
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
            this.lockManager.unlock(lock, true); // Release URI sync lock

            if (this.logger.isDebugEnabled()) {
                this.logger.debug("load: took " + (System.currentTimeMillis() - start) + " ms");
            }

            if (this.logger.isDebugEnabled()) {
                this.logger.debug("cache size : " + this.items.size());
            }
        }
    }

    @Override
    public ResourceImpl storeACL(ResourceImpl r) throws DataAccessException {
        List<Path> uris = new ArrayList<Path>();
        uris.add(r.getURI());

        if (r.isCollection()) {
            Path testURI = r.getURI();

            for (Path uri : this.items.uriSet()) {
                if (testURI.isAncestorOf(uri) && !uri.isRoot()) {
                    uris.add(uri);
                }
            }
        }

        List<Path> lockedUris = this.lockManager.lock(uris, true);

        ResourceImpl writtenResource;
        try {
            writtenResource = this.wrappedAccessor.storeACL(r); // Persist
            this.items.remove(uris); // Purge all affected items from cache
        } finally {
            this.lockManager.unlock(lockedUris, true); // Release URI sync lock

            if (this.logger.isDebugEnabled()) {
                this.logger.debug("cache size : " + this.items.size());
            }
        }
        
        return writtenResource;
    }

    @Override
    public ResourceImpl store(final ResourceImpl resource) throws DataAccessException {
        List<Path> locked = this.lockManager.lock(resource.getURI(), true);
        
        ResourceImpl writtenResource;
        try {
            writtenResource = this.wrappedAccessor.store(resource); // Persist
            this.items.remove(resource.getURI()); // Purge item from cache
        } finally {
            this.lockManager.unlock(locked, true); // Release URI sync lock

            if (this.logger.isDebugEnabled()) {
                this.logger.debug("cache size : " + this.items.size());
            }
        }
        
        return writtenResource;
    }

    /**
     * Explicity purge a resource from cache by URI. Used by external cache
     * control code.
     * 
     * @param uri
     */
    public void purgeFromCache(Path uri) {
        List<Path> locked = this.lockManager.lock(uri, true);
        try {
            this.items.remove(locked);
        } finally {
            this.lockManager.unlock(locked, true);
        }
    }

    /**
     * Eplicitly purge a list of resources from cache by URI. Used by external
     * cache control code.
     * 
     * @param uris
     */
    public void purgeFromCache(List<Path> uris) {
        List<Path> lockedUris = this.lockManager.lock(uris, true);
        try {
            this.items.remove(lockedUris);
        } finally {
            this.lockManager.unlock(lockedUris, true);
        }
    }

    @Override
    public ResourceImpl copy(ResourceImpl r, ResourceImpl destParent, PropertySet newResource, boolean copyACLs,
            PropertySet fixedProperties) throws DataAccessException {

        Path destURI = newResource.getURI();

        List<Path> uris = new ArrayList<Path>(4);
        uris.add(r.getURI());
        uris.add(destURI);

        Path destParentURI = destParent.getURI();
        if (this.items.containsURI(destParentURI)) {
            uris.add(destParentURI);
        }

        Path srcParentURI = r.getURI().getParent();
        if ((srcParentURI != null) && !uris.contains(srcParentURI) && this.items.containsURI(srcParentURI)) {
            uris.add(srcParentURI);
        }

        List<Path> lockedUris = this.lockManager.lock(uris, true);
        ResourceImpl writtenDestResource;
        try {
            // Persist copy operation
            writtenDestResource = this.wrappedAccessor.copy(r, destParent, newResource, copyACLs, fixedProperties);
            // Purge affected destination parent from cache
            this.items.remove(destParentURI);
        } finally {
            this.lockManager.unlock(lockedUris, true); // Release URI sync locks

            if (this.logger.isDebugEnabled()) {
                this.logger.debug("cache size : " + this.items.size());
            }
        }
        
        return writtenDestResource;
    }

    @Override
    public ResourceImpl move(ResourceImpl r, ResourceImpl newResource) throws DataAccessException {
        Path destURI = newResource.getURI();
        List<Path> uris = new ArrayList<Path>();
        uris.add(r.getURI());
        uris.add(destURI);
        if (r.isCollection()) {
            for (Path uri : this.items.uriSet()) {
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
        if ((srcParentURI != null) && !uris.contains(srcParentURI) && this.items.containsURI(srcParentURI)) {
            uris.add(srcParentURI);
        }

        List<Path> locks = this.lockManager.lock(uris, true);
        ResourceImpl writtenDestResource;
        try {
            writtenDestResource = this.wrappedAccessor.move(r, newResource); // Persist move operation
            this.items.remove(uris); // Purge all affected items from cache

        } finally {

            this.lockManager.unlock(locks, true); // Release URI sync locks

            if (this.logger.isDebugEnabled()) {
                this.logger.debug("cache size : " + this.items.size());
            }
        }
        
        return writtenDestResource;
    }

    @Override
    public void delete(ResourceImpl r) throws DataAccessException {
        this.performDelete(false, r, null, null, null);
    }

    @Override
    public void markDeleted(ResourceImpl resource, ResourceImpl parent, Principal principal, final String trashID)
            throws DataAccessException {
        this.performDelete(true, resource, parent, principal, trashID);
    }

    private void performDelete(boolean restorable, ResourceImpl resource, ResourceImpl parent, Principal principal,
            final String trashID) {
        List<Path> uris = new ArrayList<Path>();

        uris.add(resource.getURI());

        if (resource.isCollection()) {
            for (Path uri : this.items.uriSet()) {
                if (resource.getURI().isAncestorOf(uri) && !uri.equals(resource.getURI())) {
                    uris.add(uri);
                }
            }
        }

        Path parentURI = resource.getURI().getParent();

        if ((parentURI != null) && this.items.containsURI(parentURI)) {
            uris.add(parentURI);
        }

        List<Path> locks = this.lockManager.lock(uris, true);

        try {
            // Dispatch to wrapped DAO for persistence
            if (restorable) {
                this.wrappedAccessor.markDeleted(resource, parent, principal, trashID);
            } else {
                this.wrappedAccessor.delete(resource);
            }

            this.items.remove(uris); // Purge all affected items from cache
        } finally {
            this.lockManager.unlock(locks, true); // Release URI sync locks

            if (this.logger.isDebugEnabled()) {
                this.logger.debug("cache size : " + this.items.size());
            }
        }
    }

    @Override
    public List<RecoverableResource> getRecoverableResources(final int parentResourceId) throws DataAccessException {
        return this.wrappedAccessor.getRecoverableResources(parentResourceId);
    }

    @Override
    public ResourceImpl recover(Path parent, RecoverableResource recoverableResource) {
        List<Path> locked = this.lockManager.lock(parent, true);
        try {
            ResourceImpl recoveredResource = this.wrappedAccessor.recover(parent, recoverableResource);
            this.items.remove(parent);
            return recoveredResource;
        } finally {
            this.lockManager.unlock(locked, true);
        }
    }

    @Override
    public void deleteRecoverable(RecoverableResource recoverableResource) throws DataAccessException {
        this.wrappedAccessor.deleteRecoverable(recoverableResource);
    }

    @Override
    public List<RecoverableResource> getTrashCanOverdue(int overDueLimit) throws DataAccessException {
        return this.wrappedAccessor.getTrashCanOverdue(overDueLimit);
    }

    @Override
    public java.util.List<RecoverableResource> getTrashCanOrphans() throws DataAccessException {
        return this.wrappedAccessor.getTrashCanOrphans();
    }

    @Override
    public void deleteExpiredLocks(Date expireDate) throws DataAccessException {
        this.wrappedAccessor.deleteExpiredLocks(expireDate);
    }

    @Override
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
            this.lockManager = new PathLockManager();
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
     * @param item
     *            a <code>Resource</code> value
     * 
     */
    private void enterResource(ResourceImpl resource) {
        synchronized (this.items) {
            if (this.items.size() > (this.maxItems - 1)) {

                long startTime = System.currentTimeMillis();

                for (int i = 0; i < this.removeItems; i++) {
                    this.items.removeOldest();
                }

                long processingTime = System.currentTimeMillis() - startTime;
                if (this.logger.isInfoEnabled()) {
                    this.logger.info("Maximum cache size (" + this.maxItems + ") reached, removed " + this.removeItems
                            + " oldest items in " + processingTime + " ms");
                }
            }

            this.items.put(resource.getURI(), resource);
        }
    }

    private synchronized void updateStatistics(long hits, long misses) {
        if ((this.hits > (Long.MAX_VALUE - this.hits)) || (this.misses > (Long.MAX_VALUE) - misses)) {

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
        private Map<Path, Item> map;
        private Item in = null; // Item eviction list head (newest item)
        private Item out = null;// Item eviction list tail (oldest item)

        public Items(int initialCapacity) {
            this.map = new ConcurrentHashMap<Path, Item>(initialCapacity);
        }

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
            Item item = new Item(resource);

            Item replaced = this.map.put(uri, item);
            if (replaced != null) {
                // Remove replaced item from eviction list
                // This avoid growing the eviction list if the same set of URIs
                // are frequently replaced in the cache (which can cause huge
                // memory usage).
                if (replaced.older != null)
                    replaced.older.newer = replaced.newer;
                else
                    this.out = replaced.newer;

                if (replaced.newer != null)
                    replaced.newer.older = replaced.older;
                else
                    this.in = replaced.older;
            }

            // Put new item in eviction list head
            if (this.in != null) {
                this.in.newer = item;
            }

            item.older = this.in;
            this.in = item;

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
            for (Path uri : uris) {
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

        // XXX: Not in use, for LRU-style eviction list shuffling ?
        @SuppressWarnings("unused")
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
                    Cache.this.logger.debug("Removing oldest item " + this.out.getResource().getURI());
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

    @Override
    public Path[] discoverACLs(Path uri) throws DataAccessException {
        return this.wrappedAccessor.discoverACLs(uri);
    }

    @Override
    public Set<Principal> discoverGroups() throws DataAccessException {
        return this.wrappedAccessor.discoverGroups();
    }

}

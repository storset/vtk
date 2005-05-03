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

import EDU.oswego.cs.dl.util.concurrent.ConcurrentReaderHashMap;

import org.vortikal.repositoryimpl.Collection;
import org.vortikal.repositoryimpl.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Very simple write-trough cache implementation
 */
public class Cache implements DataAccessor {
    private Log logger = LogFactory.getLog(this.getClass());
    private DataAccessor wrappedAccessor;
    private LockManager lockManager = new LockManager();
    private int maxItems;
    private int removeItems;
    private Items items = new Items();

    public boolean validate() throws IOException {
        return wrappedAccessor.validate();
    }

    public void destroy() throws IOException {
        wrappedAccessor.destroy();
    }

    public Resource[] load(String[] uris) throws IOException {
        List found = new ArrayList();
        List notFound = new ArrayList();

        for (int i = 0; i < uris.length; i++) {
            Resource r = items.get(uris[i]);

            boolean lockTimedOut =
                (r != null && r.getLock() != null
                 && r.getLock().getTimeout().getTime() < System.currentTimeMillis());

            if (logger.isDebugEnabled() && lockTimedOut) {
                logger.debug("Dropping cached copy of " + r.getURI() + " (lock timed out)");
            }

            if (r == null || lockTimedOut) {
                notFound.add(uris[i]);
            } else {
                found.add(r);
            }
        }

        if (notFound.size() == 0) {
            return (Resource[]) found.toArray(new Resource[] {  });
        }

        String[] loadSet = (String[]) notFound.toArray(new String[] {  });

        try {
            lockManager.lock(loadSet);

            if (logger.isDebugEnabled()) {
                logger.debug("Loading " + loadSet.length + " resources");
            }

            Resource[] resources = wrappedAccessor.load(loadSet);

            for (int i = 0; i < resources.length; i++) {
                resources[i].setDataAccessor(this);
                enterResource(resources[i]);
                found.add(resources[i]);
            }
        } finally {
            lockManager.unlock(loadSet);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("cache size : " + items.size());
        }

        return (Resource[]) found.toArray(new Resource[] {  });
    }


    public Resource[] loadChildren(Collection parent) throws IOException {

        List found = new ArrayList();
        List notFound = new ArrayList();

        String[] uris = parent.getChildURIs();

        for (int i = 0; i < uris.length; i++) {

            Resource r = items.get(uris[i]);
            boolean lockTimedOut =
                (r != null && r.getLock() != null
                 && r.getLock().getTimeout().getTime() < System.currentTimeMillis());

            if (logger.isDebugEnabled() && lockTimedOut) {
                logger.debug("Dropping cached copy of " + r.getURI() + " (lock timed out)");
            }

            if (r == null || lockTimedOut) {
                notFound.add(uris[i]);
            } else {
                found.add(r);
            }
        }

        if (notFound.size() == 0) {
            return (Resource[]) found.toArray(new Resource[] {  });
        }

        found = new ArrayList();
        
        Resource[] resources = null;
        
        try {
            lockManager.lock(uris);

            if (logger.isDebugEnabled()) {
                logger.debug("Loading " + uris.length + " resources");
            }

            resources = wrappedAccessor.loadChildren(parent);

            for (int i = 0; i < resources.length; i++) {
                resources[i].setDataAccessor(this);
                enterResource(resources[i]);
            }
        } finally {
            lockManager.unlock(uris);
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

        if (logger.isDebugEnabled() && lockTimedOut) {
            logger.debug("Dropping cached copy of " + r.getURI()  + " (lock timed out)");
        }


        if (r != null && ! lockTimedOut) {
            return r;
        }

        lockManager.lock(uri);

        try {
            r = items.get(uri);

            lockTimedOut =
                (r != null && r.getLock() != null
                 && r.getLock().getTimeout().getTime() < System.currentTimeMillis());

            if (r != null && ! lockTimedOut) {
                return r;
            }

            if (logger.isDebugEnabled()) {
                logger.debug("load from wrappedAccessor: " + uri);
            }

            r = wrappedAccessor.load(uri);

            if (r == null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("not found in wrappedAccessor: " + uri);
                }

                return null;
            }

            if (logger.isDebugEnabled()) {
                logger.debug("miss: " + uri);
            }

            r.setDataAccessor(this);
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

        if (r.isCollection() && r.dirtyACL()) {
            String testURI = r.getURI();

            if (!testURI.equals("/")) {
                testURI += "/";
            }

            for (Iterator iterator = items.uriSet().iterator();
                    iterator.hasNext();) {
                String uri = (String) iterator.next();

                if (uri.startsWith(testURI) && !uri.equals("/")) {
                    uris.add(uri);
                }
            }
        }

        lockManager.lock(uris);

        try {
            wrappedAccessor.store(r);

            for (Iterator i = uris.iterator(); i.hasNext();) {
                String uri = (String) i.next();

                if (!uri.equals(r.getURI()) && items.containsURI(uri)) {
                    items.remove(uri);
                }
            }
        } finally {
            lockManager.unlock(uris);

            if (logger.isDebugEnabled()) {
                logger.debug("cache size : " + items.size());
            }
        }
    }

    public void delete(Resource r) throws IOException {
        List uris = new ArrayList();

        uris.add(r.getURI());

        if (r.isCollection()) {
            for (Iterator iterator = items.uriSet().iterator();
                    iterator.hasNext();) {
                String uri = (String) iterator.next();

                if (uri.startsWith(r.getURI()) && !uri.equals(r.getURI())) {
                    uris.add(uri);
                }
            }
        }

        String parentURI = Resource.getParent(r.getURI());

        if ((parentURI != null) && items.containsURI(parentURI)) {
            uris.add(parentURI);
        }

        lockManager.lock(uris);

        try {
            wrappedAccessor.delete(r);

            for (Iterator i = uris.iterator(); i.hasNext();) {
                String uri = (String) i.next();

                if (items.containsURI(uri)) {
                    items.remove(uri);
                }
            }
        } finally {
            lockManager.unlock(uris);

            if (logger.isDebugEnabled()) {
                logger.debug("cache size : " + items.size());
            }
        }
    }

    public InputStream getInputStream(Resource resource)
        throws IOException {
        return wrappedAccessor.getInputStream(resource);
    }

    public OutputStream getOutputStream(Resource resource)
        throws IOException {
        try {
            lockManager.lock(resource.getURI());
            items.remove(resource.getURI());

            return wrappedAccessor.getOutputStream(resource);
        } finally {
            lockManager.unlock(resource.getURI());
        }
    }

    public long getContentLength(Resource resource) throws IOException {
        return wrappedAccessor.getContentLength(resource);
    }

    public String[] listSubTree(Collection parent) throws IOException {
        return wrappedAccessor.listSubTree(parent);
    }

    public String[] listLockExpired() throws IOException {
        return wrappedAccessor.listLockExpired();
    }

    public void deleteLocks(Resource[] resources) throws IOException {
        List uris = new ArrayList();

        for (int i = 0; i < resources.length; i++) {
            uris.add(resources[i].getURI());
        }

        try {
            lockManager.lock(uris);
            wrappedAccessor.deleteLocks(resources);

            for (int i = 0; i < resources.length; i++) {
                items.remove(resources[i].getURI());
            }
        } finally {
            lockManager.unlock(uris);
        }
    }

    public String[] discoverLocks(Resource r) throws IOException {
        return wrappedAccessor.discoverLocks(r);
    }

    public void addChangeLogEntry(String loggerID, String loggerType,
        String uri, String operation, int resourceId, boolean collection) throws IOException {
        wrappedAccessor.addChangeLogEntry(loggerID, loggerType, uri, operation,
                                          resourceId, collection);
    }

    /**
     * Clears all cache entries.
     *
     */
    public void clear() {
        synchronized (items) {
            items.clear();
        }
    }

    /**
     * Gets the number of resources currently in the cache.
     *
     * @return the number of resources in the cache.
     */
    public int size() {
        return items.size();
    }

    /**
     * Note: this method is not thread safe, lock uri first
     *
     * @param item a <code>Resource</code> value
     */
    protected void enterResource(Resource item) {
        synchronized (items) {
            if (items.size() > (maxItems - 1)) {
                for (int i = 0; i < removeItems; i++) {
                    items.removeOldest();
                }
            }

            items.put(item.getURI(), item);
        }
    }

    /**
     * @param maxItems The maxItems to set.
     */
    public void setMaxItems(int maxItems) {
        if (maxItems <= 0) {
            throw new RuntimeException("Cache size must be a positive number");
        }

        this.maxItems = maxItems;
        removeItems = (int) (maxItems * 0.1);

        if (removeItems == 0) {
            removeItems = 1;
        }
    }

    /**
     * @param wrappedAccessor The wrappedAccessor to set.
     */
    public void setWrappedAccessor(DataAccessor wrappedAccessor) {
        this.wrappedAccessor = wrappedAccessor;
    }

    private class Items {
        private Map map = new ConcurrentReaderHashMap();
        private Item in = null;
        private Item out = null;

        public void clear() {
            map.clear();
            in = out = null;
        }

        public Resource get(String uri) {
            Item i = (Item) map.get(uri);

            if (i != null) {
                return i.getResource();
            }

            return null;
        }

        public synchronized void put(String uri, Resource resource) {
            Item i = new Item(resource);

            map.put(uri, i);

            //             synchronized(this) {
            if (in != null) {
                in.newer = i;
            }

            i.older = in;
            in = i;

            if (out == null) {
                out = in;
            }

            //              }
        }

        public synchronized void remove(String uri) {
            Item i = (Item) map.get(uri);

            //             synchronized(this) {
            if (i != null) {
                if (i.older != null) {
                    i.older.newer = i.newer;
                } else {
                    out = i.newer;
                }

                if (i.newer != null) {
                    i.newer.older = i.older;
                } else {
                    in = i.older;
                }
            }

            //             }
            map.remove(uri);
        }

        public int size() {
            return map.size();
        }

        public Set uriSet() {
            return map.keySet();
        }

        public boolean containsURI(String uri) {
            return map.containsKey(uri);
        }

        public synchronized void hit(String uri) {
            Item i = (Item) map.get(uri);

            //             synchronized(this) {
            //             if (logger.isDebugEnabled())
            //                logger.debug("Before hit: " + dump());
            if ((i != null) && (i != in)) {
                /* Remove i from list: */
                if (i != out) {
                    i.older.newer = i.newer;
                } else {
                    out = i.newer;
                }

                i.newer.older = i.older;

                /* Insert i first in list: */
                in.newer = i;
                i.older = in;
                i.newer = null;
                in = i;
            }

            //             if (logger.isDebugEnabled())
            //                 logger.debug("After hit: " + dump());
            //             }
        }

        public synchronized void removeOldest() {
            //             synchronized(this) {
            if (out != null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Removing oldest item " +
                        out.getResource().getURI());
                }

                map.remove(out.getResource().getURI());

                if (in != out) {
                    out.newer.older = null;
                    out = out.newer;
                } else {
                    in = out = null;
                }
            }
        }

        private String dump() {
            StringBuffer s = new StringBuffer();
            Item i = in;

            while (i != null) {
                s.append("[" + i.getResource().getURI() + "]");
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
}

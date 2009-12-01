/* Copyright (c) 2006, 2007, University of Oslo, Norway
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
package org.vortikal.repository.index.observation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.ChangeLogEntry;
import org.vortikal.repository.Path;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.ChangeLogEntry.Operation;
import org.vortikal.repository.index.PropertySetIndex;
import org.vortikal.repository.store.IndexDao;
import org.vortikal.repository.store.PropertySetHandler;
import org.vortikal.security.Principal;

/**
 * Incremental index updates from resource changes.
 * Hooking up to the old resource change event system, for now.
 *  
 * TODO: Should consider batch processing of a set of changes and indexing
 *       to a volatile (memory) index, then merge back each finished batch.
 * 
 * @author oyviste
 *
 */
public class PropertySetIndexUpdater implements BeanNameAware, 
                                        ResourceChangeObserver, InitializingBean {

    private final Log logger = LogFactory.getLog(PropertySetIndexUpdater.class);
    
    private PropertySetIndex index;
    private String beanName;
    private ResourceChangeNotifier notifier;
    private IndexDao indexDao;
    private boolean enabled;
    
    public void afterPropertiesSet() throws BeanInitializationException {
        // If a notifier is configured, we register ourselves.
        enable();
    }
    
    /**
     * @see org.vortikal.repository.index.observation.ResourceChangeObserver#disable()
     */
    public synchronized void disable() {
        if (this.notifier != null) {
            if (this.notifier.unregisterObserver(this)) {
                logger.info("Un-registered from resource change notifier.");
            }
        }
        this.enabled = false;
        logger.info("Disabled.");
    }
    
    /**
     * @see org.vortikal.repository.index.observation.ResourceChangeObserver#enable()
     */
    public synchronized void enable() {
        if (this.notifier != null) {
            if (this.notifier.registerObserver(this)) {
                logger.info("Registered with resource change notifier.");
            }
        }
        this.enabled = true;
        logger.info("Enabled.");
    }
    
    /**
     * @see org.vortikal.repository.index.observation.ResourceChangeObserver#isEnabled()
     */
    public boolean isEnabled() {
        return this.enabled;
    }
    
    /**
     * 
     */
    public void setBeanName(String beanName) {
        this.beanName = beanName;
        
    }
    
    public void notifyResourceChanges(final List<ChangeLogEntry> changes) {

        synchronized (this) {
            if (! this.enabled) {
                logger.info("Ignoring resource changes, disabled.");
                return;
            }
        }
        
        try {
            // Take lock immediately, we'll be doing some writing.
            if (! this.index.lock()) {
                logger.error("Unable to acquire lock on index, will not attempt to " +
                             "apply modifications, changes are lost !");
                return;
            }
            
            logger.debug("--- indexUpdate(): Going to process change log window ---");
            
            // Map maintaining last change *per URI*
            Map<Path, ChangeLogEntry> lastChanges = new HashMap<Path, ChangeLogEntry>();

            for (ChangeLogEntry change: changes) {
                // If delete, we do it immediately
                if (change.getOperation() == Operation.DELETED) {
                    if (change.isCollection()) {
                        this.index.deletePropertySetTree(change.getUri());
                    } else {
                        this.index.deletePropertySet(change.getUri());
                    }
                }

                // Update map of last changes per URI
                lastChanges.put(change.getUri(), change);
            }
            
            // Updates/additions
            if (lastChanges.size() > 0) {
                final List<Path> updateUris = new ArrayList<Path>(lastChanges.size());
                
                // Remove updated property sets from index in one batch, first, 
                // before re-adding them. This is very necessary to keep things
                // efficient.
                logger.debug("--- indexUpdate(): Update list:");
                for (Map.Entry<Path, ChangeLogEntry> entry: lastChanges.entrySet()) {
                    // If not last operation on resource was delete, we add to updates
                    if (! (entry.getValue().getOperation() == Operation.DELETED)) {
                        Path uri = entry.getKey();
                        logger.debug(uri);

                        this.index.deletePropertySet(uri);
                        updateUris.add(uri);
                    }
                }

                logger.debug("--- indexUpdate(): End of update list, going to fetch from DAO and add to index:");
                
                // Immediately make lastChanges available for GC, since the next operation
                // can take a while, and lastChanges can be huge (tens of thousands of entries).
                lastChanges = null;

                // Now query index dao for a list of all property sets that 
                // need updating.
                class CountingPropertySetHandler implements PropertySetHandler {
                    int count = 0;
                    public void handlePropertySet(PropertySet propertySet,
                            Set<Principal> aclReadPrincipals) {
  
                        if (logger.isDebugEnabled()) {
                            PropertySetIndexUpdater.this.logger.debug("ADD " + propertySet.getURI());
                        }

                        // Add updated resource to index
                        PropertySetIndexUpdater.this.index.addPropertySet(propertySet, 
                                                            aclReadPrincipals);
                        if (++count % 10000 == 0) {
                            // Logg some progress to update
                            PropertySetIndexUpdater.this.logger.info(
                                    "Incremental index update progress: "  + count + " resources indexed of "
                                    + updateUris.size() + " total in current update batch.");
                        }
                    }
                }
                
                CountingPropertySetHandler handler = new CountingPropertySetHandler();
                
                this.indexDao.orderedPropertySetIterationForUris(updateUris, handler);

                if (this.logger.isInfoEnabled()) {
                    if (updateUris.size() >= 10000) {
                        this.logger.info("Incremental index update for current batch finished"
                               + ", final resource update count was " + handler.count);
                    }
                }
                
                // Note that it is OK to get less resources than requested from DAO, because
                // they can be deleted in the mean time. 
                if (logger.isDebugEnabled() && updateUris.size() > 0) {
                    logger.debug("--- indexUpdate(): Requested " + updateUris.size() 
                            + " resources for updating, got " + handler.count + " from DAO.");
                }
            }

            logger.debug("--- indexUpdate(): Committing changes to index.");
            this.index.commit();
            
        } catch (Exception e) {
            logger.error("Something went wrong while updating new index with changes", e);
        } finally {
            this.index.unlock();
        }
    }

    public String getObserverId() {
        return this.beanName;
    }

    public void setNotifier(ResourceChangeNotifier notifier) {
        this.notifier = notifier;
    }

    @Required
    public void setIndex(PropertySetIndex index) {
        this.index = index;
    }

    @Required
    public void setIndexDao(IndexDao indexDao) {
        this.indexDao = indexDao;
    }
}

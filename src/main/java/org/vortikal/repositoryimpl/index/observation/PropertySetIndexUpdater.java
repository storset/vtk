/* Copyright (c) 2006, University of Oslo, Norway
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
package org.vortikal.repositoryimpl.index.observation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repository.PropertySet;
import org.vortikal.repositoryimpl.dao.IndexDataAccessor;
import org.vortikal.repositoryimpl.index.PropertySetIndex;

/**
 * Incremental index updates from resource changes.
 * Hooking up to the old resource change event system, for now.
 *  
 * <p>
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
    private IndexDataAccessor indexDataAccessor;
    private boolean enabled;
    
    public void afterPropertiesSet() throws BeanInitializationException {
        if (this.index == null) {
            throw new BeanInitializationException("Property 'index' not set.");
        } else if (this.indexDataAccessor == null) {
            throw new BeanInitializationException("Property 'indexDataAccessor' not set.");
        }
        
        // If a notifier is configured, we register ourselves.
        enable();
    }
    
    /**
     * @see org.vortikal.repositoryimpl.index.observation.ResourceChangeObserver#disable()
     */
    public synchronized void disable() {
        if (this.notifier != null) {
            if (this.notifier.unregisterObserver(this)) {
                this.logger.info("Un-registered from resource change notifier.");
            }
        }
        this.enabled = false;
        this.logger.info("Disabled.");
    }
    
    /**
     * @see org.vortikal.repositoryimpl.index.observation.ResourceChangeObserver#enable()
     */
    public synchronized void enable() {
        if (this.notifier != null) {
            if (this.notifier.registerObserver(this)) {
                this.logger.info("Registered with resource change notifier.");
            }
        }
        this.enabled = true;
        this.logger.info("Enabled.");
    }
    
    /**
     * @see org.vortikal.repositoryimpl.index.observation.ResourceChangeObserver#isEnabled()
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

    public void notifyResourceChanges(List resourceChanges) {
        
        synchronized (this) {
            if (! this.enabled) {
                this.logger.info("Ignoring resource changes, disabled.");
                return;
            }
        }
        
        Iterator propSetIterator = null;
        try {
            // Take lock immediately, we'll be doing some writing.
            if (! this.index.lock()) {
                this.logger.error("Unable to acquire lock on index, will not attempt to " +
                             "apply modifications, changes are lost !");
                return;
            }
            
            // Batch/separate deletes and updates for better performance.
            // List of additions and updates
            List updates = new ArrayList();
            
            // List of resources that will be deleted from index.
            List deletes = new ArrayList();
            
            for (Iterator i = resourceChanges.iterator(); i.hasNext();) {
                ResourceChange change = (ResourceChange)i.next();

                if (change instanceof ResourceDeletion) {
                    deletes.add(change);
                } else {
                    updates.add(change);
                }
            }
            
            // Apply changes to index
            // Regular deletes (might include collections)
            for (Iterator i = deletes.iterator(); i.hasNext();) {
                ResourceDeletion deletion = (ResourceDeletion)i.next();
                
                // Delete by resource ID, this info should be provided in the event
                if (deletion.isCollection()) {
                    this.index.deletePropertySetTreeByUUID(deletion.getResourceId());
                } else {
                    this.index.deletePropertySetByUUID(deletion.getResourceId());
                }
            }
            
            // Updates/additions
            if (updates.size() > 0) {
                List updateUris = new ArrayList(updates.size());
                
                // Remove updated property sets from index in one batch, first, 
                // before re-adding them. This is very necessary to keep things
                // efficient.
                for (Iterator i = updates.iterator(); i.hasNext();) {
                    ResourceChange change = (ResourceChange)i.next();
                    this.index.deletePropertySet(change.getUri());
                    updateUris.add(change.getUri());
                }
                
                // Get iterator over property sets that need updating
                propSetIterator = this.indexDataAccessor.getPropertySetIteratorForUris(updateUris);
                
                while (propSetIterator.hasNext()) {
                    PropertySet propSet = (PropertySet)propSetIterator.next();
                    this.index.addPropertySet(propSet);
                }
            }
            
            this.index.commit();
        // XXX: only temporary. We don't want to halt other (old) indexes because of bugs in new
        //      system index. Log must be watched for errors.
        } catch (Exception e) {
            this.logger.error("Something went wrong while updating new index with changes", e);
        } finally {
            this.index.unlock();
            
            if (propSetIterator != null) {
                try {
                    this.indexDataAccessor.close(propSetIterator);
                } catch (IOException io) {
                    this.logger.warn("Exception while closing result set iterator");
                }
            }
        }
    }

    public String getObserverId() {
        return this.beanName;
    }

    public void setIndexDataAccessor(IndexDataAccessor indexDataAccessor) {
        this.indexDataAccessor = indexDataAccessor;
    }

    public void setIndex(PropertySetIndex index) {
        this.index = index;
    }

    public void setNotifier(ResourceChangeNotifier notifier) {
        this.notifier = notifier;
    }
}

package org.vortikal.repositoryimpl.query.observation;

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
import org.vortikal.repository.query.PropertySetIndex;
import org.vortikal.repositoryimpl.dao.IndexDataAccessor;
import org.vortikal.repositoryimpl.dao.ResultSetIterator;
import org.vortikal.repositoryimpl.index.observation.ResourceChange;
import org.vortikal.repositoryimpl.index.observation.ResourceChangeNotifier;
import org.vortikal.repositoryimpl.index.observation.ResourceChangeObserver;
import org.vortikal.repositoryimpl.index.observation.ResourceDeletion;

/**
 * Incremental index updates from resource changes.
 * Hooking up to the old resource change event system, for now.
 *  
 * @author oyviste
 *
 */
public class PropertySetIndexUpdater implements BeanNameAware, 
                                        ResourceChangeObserver, InitializingBean {

    Log logger = LogFactory.getLog(PropertySetIndexUpdater.class);
    
    private PropertySetIndex index;
    private String beanName;
    private ResourceChangeNotifier notifier;
    private IndexDataAccessor indexDataAccessor;
    private boolean enabled;
    
    public void afterPropertiesSet() throws BeanInitializationException {
        if (index == null) {
            throw new BeanInitializationException("Property 'index' not set.");
        } else if (indexDataAccessor == null) {
            throw new BeanInitializationException("Property 'indexDataAccessor' not set.");
        }
        
        // If a notifier is configured, we register ourselves.
        enable();
    }
    
    /**
     * @see org.vortikal.repositoryimpl.index.observation.ResourceChangeObserver#disable()
     */
    public synchronized void disable() {
        if (notifier != null) {
            if (notifier.unregisterObserver(this)) {
                logger.info("Un-registered from resource change notifier.");
            }
        }
        this.enabled = false;
        logger.info("Disabled.");
    }
    
    /**
     * @see org.vortikal.repositoryimpl.index.observation.ResourceChangeObserver#enable()
     */
    public synchronized void enable() {
        if (notifier != null) {
            if (notifier.registerObserver(this)) {
                logger.info("Registered with resource change notifier.");
            }
        }
        this.enabled = true;
        logger.info("Enabled.");
    }
    
    /**
     * @see org.vortikal.repositoryimpl.index.observation.ResourceChangeObserver#isEnabled()
     */
    public boolean isEnabled() {
        return this.enabled;
    }
    
    public void setBeanName(String beanName) {
        this.beanName = beanName;
        
    }

    public void notifyResourceChanges(List resourceChanges) {
        
        synchronized (this) {
            if (! this.enabled) {
                logger.info("Ignoring resource changes, disabled.");
                return;
            }
        }
        
        ResultSetIterator rsi = null;
        try {
            // Take lock immediately, we'll be doing some writing.
            if (! index.lock()) {
                logger.error("Unable to acquire lock on index, will not attempt to " +
                             "apply modifications, changes are lost !");
                return;
            }
            
            // Batch/separate deletes and updates for better performance.
            List updates = new ArrayList(); // Updates
            
            // List of resources that will be deleted from index.
            List deletes = new ArrayList(); // Deletes
            
            for (Iterator i = resourceChanges.iterator(); i.hasNext();) {
                ResourceChange c = (ResourceChange)i.next();
                deletes.add(c.getUri());
                if (! (c instanceof ResourceDeletion)) {
                    updates.add(c.getUri());
                }
            }
            
            // Apply changes to index
            for (Iterator i = deletes.iterator(); i.hasNext();) {
                String uri = (String)i.next();
                if (logger.isDebugEnabled()) {
                    logger.debug("Deleting property set at URI '" + uri + "'");                    
                }
                index.deletePropertySet(uri);
            }
            
            if (updates.size() > 0) {
                // Get iterator over property sets that need updating
                rsi = indexDataAccessor.getPropertySetIteratorForURIs(updates);
                
                while (rsi.hasNext()) {
                    PropertySet propSet = (PropertySet)rsi.next();
                    if (logger.isDebugEnabled()) {
                        logger.debug("Adding property set at URI '" + propSet.getURI() + "' to index");
                    }
                    
                    index.addPropertySet(propSet);
                }
            }
            
            index.commit();
        // XXX: only temporary. We don't want to halt other (old) indexes because of bugs in new
        //      system index. Log must be watched for errors.
        } catch (Exception e) {
            logger.error("Something went wrong while updating new index with changes", e);
        } finally {
            index.unlock();
            
            if (rsi != null) {
                try {
                    rsi.close();
                } catch (IOException io) {
                    logger.warn("Exception while closing ResultSetIterator");
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

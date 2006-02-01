/* Copyright (c) 2005, University of Oslo, Norway
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;


/**
 * Run resource changes through filter(s) and distribute them on
 * to the wrapped observers. Used for doing selective indexing of resource
 * changes. 
 * 
 * @author oyviste
 */
public class FilteringResourceChangeObserverWrapper 
    implements ResourceChangeObserver, InitializingBean, BeanNameAware {
    
    Log logger = LogFactory.getLog(FilteringResourceChangeObserverWrapper.class);
    
    private ResourceChangeNotifier notifier;
    private List wrappedObservers;
    private FilterCriterion filterCriterion;
    private String beanName;
    private boolean enabled;
    
    /** Creates a new instance of FilteringResourceChangeObserverWrapper */
    public FilteringResourceChangeObserverWrapper() {
    }

    public void afterPropertiesSet() {
        if (wrappedObservers != null) {
            for (Iterator iter=wrappedObservers.iterator(); iter.hasNext();) {
                logger.info("Wrapped observer: '" + 
                            ((ResourceChangeObserver)iter.next()).getObserverId()+ "'");
            }
        } else {
            logger.warn("No wrapped observers set.");
        }

        if (filterCriterion != null) {
            logger.info("Filter criterion: " + this.filterCriterion);
        } else {
            logger.warn("No filter criterion set.");
        }
        
        enable(); // Enable, and register with notifier, if any.
    }
    
    /**
     * @see org.vortikal.repositoryimpl.index.observation.ResourceChangeObserver#disable()
     */
    public synchronized void disable() {
        if (this.notifier != null) {
            if (notifier.unregisterObserver(this)) {
                logger.info("Un-registered from resource change notifier.");
            }
        }
        enabled = false;
        logger.info("Disabled.");
    }
    
    /**
     * @see org.vortikal.repositoryimpl.index.observation.ResourceChangeObserver#enable()
     */
    public synchronized void enable() {
        if (this.notifier != null) {
            if (notifier.registerObserver(this)) {
                logger.info("Registered with resource change notifier.");
            }
        }
        enabled = true;
        logger.info("Enabled.");
    }
    
    /**
     * @see org.vortikal.repositoryimpl.index.observation.ResourceChangeObserver#isEnabled()
     */
    public synchronized boolean isEnabled() {
        return this.enabled; 
    }
    
    /**
     * <code>ResourceChangeObserver</code> callback method.
     */
    public synchronized void notifyResourceChanges(List changes) {
        if (! isEnabled()) {
            logger.warn("Received changes discarded. Reason: disabled.");
            return;
        }
        
        if (logger.isDebugEnabled()) {
            logger.debug("Received list of " + changes.size() + " resource change(s).");
        }
        
        // TODO: filter on change type (implement only if needed)
        
        List filtered = new ArrayList(changes); // Make copy of received list
        
        // Process list of changes using the standard filter criterion
        if (filterCriterion != null) {
            for (Iterator iter = filtered.iterator(); iter.hasNext();) {
                ResourceChange change = (ResourceChange) iter.next();
                String uri = change.getUri();
                
                // Subtree-deletions go to all indexes, no matter what.
                // A filter matching on URI pattern or prefix won't necessarily match
                // a relevant parent folder URI deletion for its resources.
                if (change instanceof ResourceDeletion && change.isCollection()) {
                    continue;
                }
                
                if (filterCriterion.isFiltered(uri)) {
                    iter.remove();
                }
            }
        }
        
        if (logger.isDebugEnabled()) {
            logger.debug("List contains " + filtered.size() + " change(s) after filtering.");
        }
        
        // Send the filtered list on to the wrapped observers.
        if (wrappedObservers != null && filtered.size() > 0) {
            for (Iterator iter = wrappedObservers.iterator(); iter.hasNext();) {
                ((ResourceChangeObserver)iter.next()).notifyResourceChanges(filtered);
            }
        }
        
    }
    
    public void setNotifier(ResourceChangeNotifier notifier) {
        this.notifier = notifier;
    }
    
    public void setFilterCriterion(FilterCriterion filterCriterion) {
        this.filterCriterion = filterCriterion;
    }
    
    public void setWrappedObservers(List wrappedObservers) {
        this.wrappedObservers = wrappedObservers;
    }
    
    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    public String getObserverId() {
        return this.beanName;
    }
    
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("[ Observer wrapper id='");
        buf.append(getObserverId());
        buf.append("', filter=(");
        buf.append(this.filterCriterion);
        buf.append("), wrapped observers=(");
        if (this.wrappedObservers != null) {
            for (Iterator iter = wrappedObservers.iterator(); iter.hasNext();) {
                buf.append("'");
                buf.append(((ResourceChangeObserver)iter.next()).getObserverId());
                buf.append("'");
            }
        }
        buf.append(") ]");
        return buf.toString();
    }
}

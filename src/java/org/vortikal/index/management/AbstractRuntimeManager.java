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
package org.vortikal.index.management;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.vortikal.index.Index;
import org.vortikal.index.observation.ResourceChangeObserver;

/**
 * Based on looking up instances for indexes, observers and reindexers in
 * application context, which makes it unnecessary to bind indexes to the 
 * index manager in the configuration. This behaviour might change.
 *  
 * @author oyviste
 *
 */
public abstract class AbstractRuntimeManager implements RuntimeManager, 
    ApplicationContextAware, InitializingBean {
    
    Log logger = LogFactory.getLog(AbstractRuntimeManager.class);
    
    private ApplicationContext context;
    private List indexes;
    private List observers;
    private List reindexers;
    
    public void afterPropertiesSet() {
        // Get all known indexes, observers and reindexers from application context.
        // Alternatively, this can be made configurable.
        this.indexes = new ArrayList(context.getBeansOfType(Index.class,
                                                    false, false).values());
        
        this.observers = new ArrayList(context.getBeansOfType(
                             ResourceChangeObserver.class, false, false).values());
        
        this.reindexers = new ArrayList(context.getBeansOfType(
                                    Reindexer.class, false, false).values());
        
        if (logger.isInfoEnabled()) {
            logger.info("Found " + indexes.size() + " index(es) in application context.");
            logger.info("Found " + observers.size() + " observer(s) in application context.");
            logger.info("Found " + reindexers.size() + " reindexer(s) in application context.");
        }
    }
    
    public List getIndexes() {
        return this.indexes;
    }
    
    public List getObservers() {
        return this.observers;
    }
    
    public ResourceChangeObserver getObserver(String observerId) 
        throws ManagementException {
        if (observerId == null) {
            throw new IllegalArgumentException("Observer id cannot be null.");
        }
        
        Object bean = null;
        try {
            bean = context.getBean(observerId, ResourceChangeObserver.class);
        } catch (BeansException be) {
            throw new ManagementException("No observer with id '" + observerId + "' found.", be);
        }
        
        return (ResourceChangeObserver)bean;
    }
    
    public void disableAllObservers() {
        for (Iterator iter = observers.iterator(); iter.hasNext();) {
            ResourceChangeObserver o = (ResourceChangeObserver)iter.next();
            o.disable();
            if (logger.isDebugEnabled()) {
                logger.debug("Disabled observer '" + o.getObserverId() + "'.");
            }
        }
        logger.info("Disabled all resource change observers.");
    }
    
    public void enableAllObservers()  {
        for (Iterator iter = observers.iterator(); iter.hasNext();) {
            ResourceChangeObserver o = (ResourceChangeObserver)iter.next();
            o.enable();
            if (logger.isDebugEnabled()) {
                logger.debug("Enabled observer '" + o.getObserverId() + "'.");
            }
        }
        
        logger.info("Enabled all resource change observers.");
    }
   
    /**
     * Get an <code>Index</code> instance with the given id.
     */
    public Index getIndex(String indexId) {
        if (indexId == null) {
            throw new IllegalArgumentException("Index id cannot be null.");
        }
        
        Object bean = null;
        try {
            bean = context.getBean(indexId, Index.class);
        } catch (BeansException be) {
            throw new ManagementException("No index with id '" + indexId + "' found.", be);
        }
        
        return (Index)bean;
    }

    /**
     * Returns the <code>Reindexer</code> instance for a given 
     * <code>Index</code> instance, <code>null</code> if none found/configured.
     */
    public Reindexer getReindexerForIndex(Index index) {
        for (Iterator iter = this.reindexers.iterator(); iter.hasNext();) {
            Reindexer r = (Reindexer)iter.next();
            if (r.getIndexId().equals(index.getIndexId())) {
                return r;
            }
        }

        return null;
    }
    

    public void setApplicationContext(ApplicationContext context) {
        this.context = context;
    }
   
    public abstract IndexStatus getStatusForIndex(Index index);

}

/* Copyright (c) 2004, 2007, University of Oslo, Norway
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.transaction.annotation.Transactional;
import org.vortikal.repository.ChangeLogEntry;
import org.vortikal.repository.ChangeLogEntry.Operation;
import org.vortikal.repository.store.ChangeLogDAO;

/**
 * Class for distributing resouce changes from database to a set of observers.
 * XXX: Simplify and get rid of publish/subscriber pattern for index updates ?
 *      The only benefit is the ability to take one set of changes from database
 *      and update multiple indexes (if the need should arise).
 */
public class ResourceChangeNotifierImpl implements ResourceChangeNotifier {
    
    private Log logger = LogFactory.getLog(ResourceChangeNotifierImpl.class);
    private Set<ResourceChangeObserver> observers = new HashSet<ResourceChangeObserver>();
    private ChangeLogDAO changeLogDAO;
    private int loggerType;
    private int loggerId;
    private int maxChangelogEntriesPerPoll = 40000;
    
    /**
     * This method should be periodically called to poll for resource changes.
     */
    @Override
    @Transactional
    public synchronized void pollChanges() {
        
        try {
            List<ChangeLogEntry> changes = 
            	this.changeLogDAO.getChangeLogEntries(this.loggerType, 
                                                      this.loggerId,
                                                      this.maxChangelogEntriesPerPoll);
            
            if (logger.isDebugEnabled() && changes.size() > 0) {
                logger.debug("");
                logger.debug("--- pollChanges(): Start of window");
                logger.debug("--- pollChanges(): Got the following changelog events from DAO");
                for (ChangeLogEntry change: changes) {
                    StringBuilder log = new StringBuilder();
                    if (change.getOperation() == Operation.DELETED) {
                        log.append("DEL    ");                        
                    } else {
                        log.append("UPDATE ");
                    }

                    if (change.isCollection()) {
                        log.append("COL ");
                    }
                    log.append(change.getUri());
                    
                    log.append(", RESOURCE ID=").append(change.getResourceId());
                    log.append(", EVENT ID=").append(change.getChangeLogEntryId());
                    logger.debug(log.toString());
                }
                logger.debug("--- pollChanges(): End of list, going to dispatch to observers");
                logger.debug("");
            }
            
            if (changes != null && changes.size() > 0) {
                // Index changes
                this.logger.debug("--- pollChanges(): Notifying observers/indexes of resource changes");
                for (ResourceChangeObserver observer: this.observers) {
                    
                    if (this.logger.isDebugEnabled()) {
                        this.logger.debug("--- pollChanges(): Notifying observer '" + observer.getObserverId() + "'");
                    }
                    
                    observer.notifyResourceChanges(changes);
                }
                this.logger.debug("--- pollChanges(): Finished notifying observers of changes.");
                
                // Remove changelog entries from DAO
                this.changeLogDAO.removeChangeLogEntries(changes);
                
                if (this.logger.isDebugEnabled() && this.observers.isEmpty()) {
                    this.logger.debug("Changelog contents discarded, no observers are registered.");
                }
                
                logger.debug("--- pollChanges(): End of window"); 
                this.logger.debug("");
                this.logger.debug("");
            }
        } catch (Throwable t) {
            this.logger.error("Unexpected error while updating indexes !", t);
            this.logger.error("CHANGELOG HAS NOT BEEN FLUSHED !");
            // FIXME: We need a proper policy on what happens when errors occur during
            //        update. For instance, we can require of observers to maintain their
            //        own redo-log, instead of relying on the notifier, in case of
            //        system crashes, or similar conditions. Or we can deregister misbehaving
            //        indexes. Or fine tune exception handling, somewhat=).
        }
    }
    
    @Override
    public synchronized boolean registerObserver(ResourceChangeObserver observer) {
        if (this.observers.add(observer)) {
            this.logger.info("An observer with id '" + observer.getObserverId() + "' was registered.");
            return true;
        }
        return false;
    }
    
    @Override
    public synchronized boolean unregisterObserver(ResourceChangeObserver observer) {
        if (this.observers.remove(observer)) {
            this.logger.info("An observer with id '" + observer.getObserverId() + "' was removed.");
            return true;
        } 
        return false;
    }
    
    public void setObservers(Set<ResourceChangeObserver> observers) {
        synchronized(this) {            // Perhaps overkill, only Spring bean-factory
            this.observers = observers; // code should call this setter during init.
        }
    }

    @Required
    public void setChangeLogDAO(ChangeLogDAO changeLogDAO) {
        this.changeLogDAO = changeLogDAO;
    }

    @Required
    public void setLoggerType(int loggerType) {
        this.loggerType = loggerType;
    }

    @Required
    public void setLoggerId(int loggerId) {
        this.loggerId = loggerId;
    }

	public void setMaxChangelogEntriesPerPoll(int maxChangelogEntriesPerPoll) {
		if (maxChangelogEntriesPerPoll <= 0) {
			throw new IllegalArgumentException("Number must be greater than zero");
		}
		this.maxChangelogEntriesPerPoll = maxChangelogEntriesPerPoll;
	}

}

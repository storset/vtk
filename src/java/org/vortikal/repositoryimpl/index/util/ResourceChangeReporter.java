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
package org.vortikal.repositoryimpl.index.util;

import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repositoryimpl.index.observation.ResourceChange;
import org.vortikal.repositoryimpl.index.observation.ResourceChangeFetcher;


/**
 * This class periodically polls an org.vortikal.index.ResourceChangeFetcher to get
 * the changes, and then logs them. Primarily meant for testing/debugging.
 * TODO: javadoc
 * 
 * @author oyviste
 */
public class ResourceChangeReporter implements InitializingBean {
    
    public final static String METHOD_LOG = "logging";
    public final static String METHOD_FILE = "file";
    
    Log logger = LogFactory.getLog(this.getClass());
    
    private PollingThread poller;
    private Thread pollerThread;
    private ResourceChangeFetcher changeFetcher;
    private int pollingInterval = 5; // poll for changes every five seconds per default
    private String loglevel = "debug";
    private boolean removeChanges = true;

    public ResourceChangeReporter() {
    }
      
    public void afterPropertiesSet() 
    throws BeanInitializationException {
        if (changeFetcher == null) {
            throw new BeanInitializationException("Property changeFetcher not set.");
        }
        
        logger.debug("Starting polling thread. Poll interval is " + pollingInterval + " seconds.");
        poller = new PollingThread();
        pollerThread = new Thread(poller);
        pollerThread.start();
    }

    private class PollingThread implements Runnable {
        
        boolean isRunning = true;
        
        public void run() {
            while(isRunning) {
                try {
                    Thread.sleep(1000*pollingInterval);
                } catch (InterruptedException i) {}
                
                // Fetch changes
                //List changes = changeFetcher.fetchChanges();
                List changes = changeFetcher.fetchLastChanges();
                
                // Report them
                reportChanges(changes);
                
                // Tell changeFetcher to remove them
                if (removeChanges) {
                   changeFetcher.removeChanges(changes); // Tell the change fetcher that we have "committed" the changes to index.
                }
           }
        }
        
        void kill() {
            this.isRunning = false;
        }
    }
    
    private void reportChanges(List changes) {
        Iterator i = changes.iterator();
        while (i.hasNext()) {
            ResourceChange c = (ResourceChange) i.next();
            logger.debug(c);
        }
    }
    
    
    /**
     * Getter for property pollingInterval.
     * @return Value of property pollingInterval.
     */
    public int getPollingInterval() {
        return this.pollingInterval;
    }

    /**
     * Setter for property pollingInterval.
     * @param pollingInterval New value of property pollingInterval.
     */
    public void setPollingInterval(int pollingInterval) {

        this.pollingInterval = pollingInterval;
    }

    public void setChangeFetcher(ResourceChangeFetcher changeFetcher) {
        this.changeFetcher = changeFetcher;
    }

    /**
     * Getter for property loglevel.
     * @return Value of property loglevel.
     */
    public String getLoglevel() {
        return this.loglevel;
    }

    /**
     * Setter for property loglevel.
     * @param loglevel New value of property loglevel.
     */
    public void setLoglevel(String loglevel) {
        this.loglevel = loglevel;
    }

    /**
     * Getter for property removeChanges.
     * @return Value of property removeChanges.
     */
    public boolean isRemoveChanges() {
        return this.removeChanges;
    }

    /**
     * Setter for property removeChanges.
     * @param removeChanges New value of property removeChanges.
     */
    public void setRemoveChanges(boolean removeChanges) {
        this.removeChanges = removeChanges;
    }

}

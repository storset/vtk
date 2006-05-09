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
package org.vortikal.repositoryimpl.query;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;

import EDU.oswego.cs.dl.util.concurrent.FIFOSemaphore;

/**
 * New class for low-level index access.
 * 
 * XXX: not finished.
 * XXX: cache index searchers/readers.
 * @author oyviste
 *
 */
public class LuceneIndex extends FSBackedLuceneIndex implements
    InitializingBean {
    
    private Log logger = LogFactory.getLog(this.getClass());
    
    private int optimizeInterval = 100;
    private int commitCounter = 0;
    private boolean dirty = false;
    
    /** This is our FIFO write lock on this index. Operations requiring write-access
     *  will need to acquire this before doing the operation. This includes all 
     *  operations using an IndexWriter and all operations using an IndexReader 
     *  for document deletion.
     */
    private FIFOSemaphore lock = new FIFOSemaphore(1);
    
    public void afterPropertiesSet() throws BeanInitializationException {
        super.afterPropertiesSet();
    }
    
    // Locking
    protected boolean lockAcquire() {
        try {
            this.lock.acquire();
        } catch (InterruptedException ie) {
            return false;
        }
        
        if (logger.isDebugEnabled()) {
            logger.debug("Thread '" + Thread.currentThread().getName() + 
                         "' got index lock.");
        }
        
        return true;
    }
    
    protected boolean lockAttempt(long timeout) {
        try {
            return this.lock.attempt(timeout);
        } catch (InterruptedException ie) {
            return false;
        }
    }
    
    protected void lockRelease() {
        if (logger.isDebugEnabled()) {
            logger.debug("Thread '" + Thread.currentThread().getName() + 
                         "' released index lock.");
        }
        
        this.lock.release();
    }
    
    // Override parent's commit() to allow for optimization at a certain interval.
    protected synchronized void commit() throws IOException {
        // Optimize index, if we've reached 'optimizeInterval' number of commits.
        if (++commitCounter % optimizeInterval == 0) {
            super.optimize();
        }
        
        super.commit();
    }
    
    public void setOptimizeInterval(int optimizeInterval) {
        this.optimizeInterval = optimizeInterval;
    }

}

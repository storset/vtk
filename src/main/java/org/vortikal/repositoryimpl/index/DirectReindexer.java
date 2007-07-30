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
package org.vortikal.repositoryimpl.index;

import java.io.IOException;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.vortikal.repository.PropertySet;
import org.vortikal.repositoryimpl.store.IndexDataAccessor;

/**
 * A simple re-indexer that works directly on the provided <code>PropertySetIndex</code> instance.
 * Locking is handled internally.
 * 
 * @author oyviste
 *
 */
public class DirectReindexer implements PropertySetIndexReindexer {

    private PropertySetIndex targetIndex;
    private IndexDataAccessor indexDataAccessor;
    private final Log LOG = LogFactory.getLog(DirectReindexer.class);
    
    public DirectReindexer(PropertySetIndex targetIndex, IndexDataAccessor indexDataAccessor) {
        this.targetIndex = targetIndex;
        this.indexDataAccessor = indexDataAccessor;
    }
    
    public int run() throws IndexException {
        if (! this.targetIndex.lock()) {
            throw new IndexException("Unable to acquire exclusive write lock on target index '"
                    + this.targetIndex.getId() + "'");
        }
        
        if (LOG.isInfoEnabled()) {
            LOG.info("Exclusive write lock acquired on target index '" 
                    + this.targetIndex.getId() + "', initiating direct re-indexing");
        }
        
        try {
            return runWithExternalLocking();
        } finally {
            this.targetIndex.unlock();
        }
    }
    
    /**
     * Run the re-indexing assuming that index is properly locked.
     * 
     * @return
     * @throws IndexException
     */
    protected int runWithExternalLocking() throws IndexException {
        
        int counter = 0;
        Iterator iterator = null;
        try {

            LOG.info("Clearing index contents ..");
            targetIndex.clearContents();
            iterator = indexDataAccessor.getOrderedPropertySetIterator();
            

            LOG.info("Starting re-indexing ..");
            while (iterator.hasNext()) {
                PropertySet propertySet = (PropertySet)iterator.next();
                
                if (propertySet == null) {
                    LOG.warn("Property set iterator returned null");
                    throw new IndexException("Property set iterator returned null");
                }

                targetIndex.addPropertySet(propertySet);
                ++counter; 
            }
            
            targetIndex.commit();
            if (LOG.isInfoEnabled()) {
                LOG.info("Index '" + this.targetIndex.getId() + "' committed, " 
                        + counter + " property sets indexed successfully");
            }
            
            return counter;
        } catch (IOException io) {
            LOG.warn("IOException while re-indexing: " + io.getMessage());
            throw new IndexException(io);
        } finally {
            try {
                if (iterator != null) {
                    indexDataAccessor.close(iterator);
                } 
            } catch (IOException io) {
                this.LOG.warn("IOException while closing property set iterator.");
            }
        }
    }

}

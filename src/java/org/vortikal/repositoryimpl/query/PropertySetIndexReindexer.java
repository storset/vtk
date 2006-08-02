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
package org.vortikal.repositoryimpl.query;

import java.io.IOException;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.query.PropertySetIndex;
import org.vortikal.repositoryimpl.dao.IndexDataAccessor;

/**
 * Reindexer with support for primary and secondary instances.
 * 
 * @author oyviste
 */
public class PropertySetIndexReindexer implements InitializingBean {

   private IndexDataAccessor indexDataAccessor;
   private PropertySetIndex primaryIndex;
   private PropertySetIndex secondaryIndex;
   
   Log logger = LogFactory.getLog(PropertySetIndexReindexer.class);

   public void afterPropertiesSet() {
       
       if (this.indexDataAccessor == null) {
           throw new BeanInitializationException("Property 'indexDataAccessor' not set.");
       } else if (this.primaryIndex == null) {
           throw new BeanInitializationException("Property 'primaryIndex' not set.");
       } else if (this.secondaryIndex == null) {
           logger.warn("Property 'secondaryIndex' not set, "
                + "only direct reindexing of primary index will be available");
       }
       
   }
   
   /**
    * Re-index to secondary index, replace contents of primary index with
    * newly re-indexed contents of secondary index when finished.
    * 
    * @return The number of <code>PropertySet</code>s indexed.
    * @throws IndexException
    */
   public synchronized int reindexSecondaryReplacePrimary() throws IndexException {
       
       if (this.secondaryIndex == null) {
           throw new IllegalStateException("No secondary index is configured");
       }
       
       logger.info("Reindexing to secondary index with swap-in to primary index"
               + " after completion");
       
       logger.debug("Locking primary and secondary index");
       
       try {
           if (!this.primaryIndex.lock()) {
               throw new IndexException("Unable to acquire lock on primary index");
           }
          
           if (!this.secondaryIndex.lock()) {
               throw new IndexException("Unable to acquire lock on secondary index");
           }
           
           // Start by re-indexing to secondary index, while keeping the now
           // static contents of the primary index available for searching
           // (it has been locked)
           logger.info("Starting reindexing on secondary index");
           int count = reindex(this.secondaryIndex, this.indexDataAccessor);
           
           // Delete the contents of the primary index. Searches occuring 
           // at this moment might fail. It is not really feasible to avoid this.
           logger.info("Clearing contents of primary index now");
           this.primaryIndex.clearContents();
           
           // Put contents of secondary index into primary index
           logger.info("Adding contents of secondary index to primary index");
           this.primaryIndex.addIndexContents(this.secondaryIndex);
           this.primaryIndex.commit();
           
           // Close secondary index
           logger.info("Closing secondary index");
           this.secondaryIndex.close();
           
           logger.info("Finished");
           
           return count;
       } finally {
           // Make sure both indexes are unlocked before returning
           this.primaryIndex.unlock();
           this.secondaryIndex.unlock();
       }
       
   }
   
   /**
    * Directly re-index the primary index.
    * 
    * @return The numbers of <code>PropertySet</code>s indexed.
    * @throws IndexException
    */
   public synchronized int reindexPrimary() throws IndexException {
    
       if (!this.primaryIndex.lock()) {
           throw new IndexException("Unable to acquire lock on primary index");
       }
       
       try {
           return reindex(this.primaryIndex, this.indexDataAccessor);
       } finally {
           this.primaryIndex.unlock();
       }
   }
   
   /**
    * Directly re-index the secondary index.
    * 
    * @return The numbers of <code>PropertySet</code>s indexed.
    * @throws IndexException
    */
   public synchronized int reindexSecondary() throws IndexException {
    
       if (this.secondaryIndex == null) {
           throw new IllegalStateException("No secondary index is configured");
       }
       
       if (!this.secondaryIndex.lock()) {
           throw new IndexException("Unable to acquire lock on secondary index");
       }
       
       try {
           return reindex(this.secondaryIndex, this.indexDataAccessor);
       } finally {
           this.secondaryIndex.unlock();
       }
   }

   private int reindex(PropertySetIndex index, IndexDataAccessor indexDataAccessor) 
       throws IndexException {
       
       int counter = 0;
       
       Iterator iterator = null;
       try {
           index.clearContents();
           iterator = indexDataAccessor.getOrderedPropertySetIterator();
           
           while (iterator.hasNext()) {
               PropertySet propertySet = (PropertySet)iterator.next();
               
               if (propertySet == null) {
                   throw new IndexException("Property set iterator returned null");
               }

               index.addPropertySet(propertySet);
               ++counter; 
           }
           
           index.commit();
           
           return counter;
       } catch (IOException io) {
           throw new IndexException(io);
       } finally {
           try {
               if (iterator != null) {
                   indexDataAccessor.close(iterator);
               } 
           } catch (IOException io) {
               this.logger.warn("IOException while closing property set iterator.");
           }
       }
       
   }
   
   public void setIndexDataAccessor(IndexDataAccessor indexDataAccessor) {
       this.indexDataAccessor = indexDataAccessor;
   }
   
   public void setSecondaryIndex(PropertySetIndex secondaryIndex) {
       this.secondaryIndex = secondaryIndex;
   }
   
   public PropertySetIndex getSecondaryIndex() {
       return this.secondaryIndex;
   }
   
   public void setPrimaryIndex(PropertySetIndex primaryIndex) {
       this.primaryIndex = primaryIndex;
   }
   
   public PropertySetIndex getPrimaryIndex() {
       return this.primaryIndex;
   }
   
}

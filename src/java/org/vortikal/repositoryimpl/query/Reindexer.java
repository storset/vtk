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
 * Fast and furious re-indexer whipped together even faster ..
 * 
 * Only for testing, for now ..
 * 
 * @author oyviste
 *
 */
public class Reindexer implements InitializingBean {

   private IndexDataAccessor indexDataAccessor;
   private PropertySetIndex index;
   
   Log logger = LogFactory.getLog(Reindexer.class);

   public void afterPropertiesSet() {
       
       if (this.indexDataAccessor == null) {
           throw new BeanInitializationException("Property 'indexDataAccessor' not set.");
       } else if (this.index == null) {
           throw new BeanInitializationException("Property 'index' not set.");
       }

   }
   
   public synchronized void run() throws IndexException {
    
       if (!this.index.lock()) {
           throw new IndexException("Unable to acquire lock.");
       }
       
       Iterator iterator = null;
       try {
           this.index.clear();
           iterator = this.indexDataAccessor.getOrderedPropertySetIterator();
           
           while (iterator.hasNext()) {
               PropertySet propertySet = (PropertySet)iterator.next();
               
               if (propertySet == null) {
                   throw new IndexException("Property set iterator returned null");
               }
               
               if (this.logger.isDebugEnabled()) {
                   this.logger.debug("Adding property set at URI '" 
                           + propertySet.getURI() + "' to index.");
               }
               
               this.index.addPropertySet(propertySet);
           }
           
           this.index.commit();
       } catch (IOException io) {
           throw new IndexException(io);
       } finally {
           try {
               if (iterator != null) {
                   this.indexDataAccessor.close(iterator);
               } 
           } catch (IOException io) {
               this.logger.warn("IOException while closing result set iterator.");
           }
           
           this.index.unlock();
       }
       
   }
   
   public void setIndexDataAccessor(IndexDataAccessor indexDataAccessor) {
       this.indexDataAccessor = indexDataAccessor;
   }
   
   public void setIndex(PropertySetIndex index) {
       this.index = index;
   }
   
}

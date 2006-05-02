package org.vortikal.repositoryimpl.query;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.query.PropertySetIndex;
import org.vortikal.repositoryimpl.dao.IndexDataAccessor;
import org.vortikal.repositoryimpl.dao.ResultSetIterator;

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
       
       if (indexDataAccessor == null) {
           throw new BeanInitializationException("Property 'indexDataAccessor' not set.");
       } else if (index == null) {
           throw new BeanInitializationException("Property 'index' not set.");
       }
       
       new Thread(new Runnable() {
           public void run() {
               try {
                   Thread.sleep(6000);
               } catch (InterruptedException ie) {}
               Reindexer.this.run();
           }
       }).start();
   }
   
   public synchronized void run() throws IndexException {
    
       if (!index.lock()) {
           throw new IndexException("Unable to acquire lock.");
       }
       
       ResultSetIterator i = null;
       try {
           index.clearIndex();
           i = indexDataAccessor.getOrderedPropertySetIterator();
           
           while (i.hasNext()) {
               PropertySet propertySet = (PropertySet)i.next();
               if (logger.isDebugEnabled()) {
                   logger.debug("Adding property set at URI '" 
                           + propertySet.getURI() + "' to index.");
               }
               index.addPropertySet(propertySet);
           }
           
           index.commit();
       } catch (IOException io) {
           throw new IndexException(io);
       } finally {
           try {
               if (i != null) {
                   i.close();
               } 
           } catch (IOException io) {
               logger.warn("IOException while closing result set iterator.");
           }
           index.unlock();
       }
   }
   
   public void setIndexDataAccessor(IndexDataAccessor indexDataAccessor) {
       this.indexDataAccessor = indexDataAccessor;
   }
   
   public void setIndex(PropertySetIndex index) {
       this.index = index;
   }
   
}

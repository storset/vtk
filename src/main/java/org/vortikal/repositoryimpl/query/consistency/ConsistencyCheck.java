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
package org.vortikal.repositoryimpl.query.consistency;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.vortikal.repository.query.PropertySetIndex;
import org.vortikal.repositoryimpl.PropertySetImpl;
import org.vortikal.repositoryimpl.dao.IndexDataAccessor;
import org.vortikal.repositoryimpl.query.DocumentMappingException;
import org.vortikal.repositoryimpl.query.IndexException;
import org.vortikal.repositoryimpl.query.PropertySetIndexRandomAccessor;
import org.vortikal.repositoryimpl.query.StorageCorruptionException;

/**
 * Check consistency and optionally repair errors afterwords.
 * 
 * Note that usage of this class requires external locking of the index in question, if 
 * writing operations are known to occur during testing.
 * 
 * TODO: handle locking internally instead (makes this tool a little easier to use from management
 * code)
 * 
 * @author oyviste
 */
public class ConsistencyCheck {

    private static final Log LOG = LogFactory.getLog(ConsistencyCheck.class);

    private IndexDataAccessor indexDataAccessor;
    private PropertySetIndex index;

    private List errors = new ArrayList(); // List of detected inconsistencies
    private boolean completed = false; 

    /**
     * 
     * @param index
     * @param indexDataAccessor
     */
    private ConsistencyCheck(PropertySetIndex index,
                                        IndexDataAccessor indexDataAccessor) {
        this.indexDataAccessor = indexDataAccessor;
        this.index = index;
    }

    /**
     * Prepare a ConsistencyCheck instance by running the check.
     * 
     * 
     * @param index
     * @param indexDataAccessor
     * @return
     */
    public static ConsistencyCheck run(PropertySetIndex index,
            IndexDataAccessor indexDataAccessor) throws IndexException,
            ConsistencyCheckException, StorageCorruptionException {

        ConsistencyCheck check = new ConsistencyCheck(index, indexDataAccessor);
        check.runInternal();
        return check;
    }

    private void runInternal() throws IndexException,
            ConsistencyCheckException, StorageCorruptionException {

        String indexId = this.index.getId();

        LOG.info("Running consistency check on index '" + indexId + "'");

        LOG.info("Running storage corruption test ..");
        // This has the positive side effect of warming up the Lucene reader cache
        this.index.validateStorageFacility();
        LOG.info("Storage corruption test passed.");

        Iterator indexUriIterator = null;
        Iterator daoIterator = null;
        PropertySetIndexRandomAccessor randomIndexAccessor = null;

        try {
            
            daoIterator = this.indexDataAccessor.getOrderedPropertySetIterator();
            indexUriIterator = this.index.orderedUriIterator();
            randomIndexAccessor = this.index.randomAccessor();
            
            LOG.info("Running consistency check ..");
            runConsistencyCheck(daoIterator, randomIndexAccessor, indexUriIterator);
            
            if (this.errors.size() > 0) {
                LOG.warn("Consistency check completed, " + this.errors.size() + " inconsistencies detected");
            } else {
                LOG.info("Consistency check completed successfully without any errors detected");
            }

        } catch (IOException io) {
            throw new ConsistencyCheckException("IOException while running consistency check", io);
        } catch (StorageCorruptionException sce) {
            LOG.warn("Storage corruption test failed: " + sce.getMessage());
            throw sce; // Re-throw, since we can't work on or fix a corrupted index
        } finally {
            // Clean up resources
            try {
                if (daoIterator != null) this.indexDataAccessor.close(daoIterator);
                if (indexUriIterator != null) this.index.close(indexUriIterator);
                if (randomIndexAccessor != null) randomIndexAccessor.close();
            } catch (IOException io) {
                LOG.warn("IOException while freeing index or dao resources: " + io.getMessage());
            }
        }

        this.completed = true;
    }
    
    private void runConsistencyCheck(Iterator daoIterator, 
                                     PropertySetIndexRandomAccessor randomIndexAccessor,
                                     Iterator indexUriIterator) 
        throws IOException, IndexException {
        Set valid = new HashSet(10000);
        
        while (daoIterator.hasNext()) {
            PropertySetImpl daoPropSet = (PropertySetImpl)daoIterator.next();
            String currentUri = daoPropSet.getURI();
            
            int indexInstances = randomIndexAccessor.countInstances(currentUri);
            
            if (indexInstances == 0) {
                // Missing in index
                this.errors.add(new MissingInconsistency(currentUri, daoPropSet));
                continue;
            } else  if (indexInstances == 1) {
                // OK, only a single instance exists, verify the instance data
                checkPropertySet(currentUri, randomIndexAccessor, daoPropSet);
            } else {
                // Multiples inconsistency
                this.errors.add(new MultiplesInconsistency(currentUri, indexInstances, daoPropSet));
            }
            
            // Add to set of valid index property set URIs
            valid.add(currentUri);
        }
        
        // Need to make a complete pass over index URIs to detect dangling inconsistencies
        while (indexUriIterator.hasNext()) {
            String currentUri = (String)indexUriIterator.next();
            if (! valid.contains(currentUri)) {
                this.errors.add(new DanglingInconsistency(currentUri));
            }
        }
        
        
    }
    
    /**
     * Verify consistency of a single index property set. Checks resource ID (= index UUID) and
     * the ACL_INHERITED_FROM ID.
     * 
     * @param indexUri
     * @param randomIndexAccessor
     * @param daoPropSet
     * @throws IOException
     * @throws IndexException
     */
    private void checkPropertySet(String indexUri, 
                                  PropertySetIndexRandomAccessor randomIndexAccessor, 
                                  PropertySetImpl daoPropSet)
        throws IOException, IndexException {
    
        try {
            PropertySetImpl indexPropSet = 
                                (PropertySetImpl)randomIndexAccessor.getPropertySetByURI(indexUri);
            
            // If we get here, the index document has been successfully mapped to a property set instance
            int indexUUID = indexPropSet.getID();
            int daoUUID = daoPropSet.getID();
            int indexACL = indexPropSet.getAclInheritedFrom();
            int daoACL = daoPropSet.getAclInheritedFrom();
            
            if (indexUUID != daoUUID) {
                // Invalid UUID (this can also be considered a dangling inconsistency)
                this.errors.add(new InvalidUUIDInconsistency(indexUri, daoPropSet, indexUUID, daoUUID));
            } else if (indexACL != daoACL) {
                // Invalid ACL inherited from
                this.errors.add(new InvalidACLInheritedFromInconsistency(indexUri, daoPropSet, indexACL, daoACL));
            }
            
        } catch (DocumentMappingException dme) {
            // Unmappable inconsistency
            this.errors.add(new UnmappableConsistencyError(indexUri, dme, daoPropSet));
        }
    
}


    /**
     * Faster and more resource efficient check (currently not in use because of uncertainties
     * about database lexicographic URI ordering).
     * 
     * It requires that the database sorts characters according to standard C locale.
     * (otherwise there are small differences between sorting of special characters, which is
     * significant for the algorithm in this method). There cannot be any difference between
     * Java's string sorting and the database's string sorting.
     * 
     * Oracle seems to do it right in our case, but Postgresql sorts special characters 
     * differently if the cluster has been created with a non-C locale environment (which is typical
     * for local installations, etc).
     * 
     * @param daoIterator
     * @param indexUriIterator
     * @throws IOException
     * @throws IndexException
     */    
    private void runConsistencyCheckExperimental(Iterator daoIterator, Iterator indexUriIterator, 
                                                PropertySetIndexRandomAccessor randomIndexAccessor) 
        throws IOException, IndexException {

        String indexUri = null;
        String nextIndexUri = null;
        PropertySetImpl nextDaoPropSet = null;

        while (indexUriIterator.hasNext() || nextIndexUri != null) {

            if (nextIndexUri != null) {
                indexUri = nextIndexUri;
                nextIndexUri = null;
            } else {
                indexUri = (String) indexUriIterator.next();
            }
            
            if (daoIterator.hasNext() || nextDaoPropSet != null) {
                PropertySetImpl daoPropSet = null;
                if (nextDaoPropSet != null) {
                    daoPropSet = nextDaoPropSet;
                    nextDaoPropSet = null;
                } else {
                    daoPropSet = (PropertySetImpl) daoIterator.next();
                }
                
                String daoUri = daoPropSet.getURI();
                
                int compare = indexUri.compareTo(daoUri);
                if (compare == 0) {  // indexUri == daoUri
                    
                    // See if it is duplicated in index
                    int instances = 1;
                    while (indexUriIterator.hasNext()) {
                        nextIndexUri = (String) indexUriIterator.next();
                        if (indexUri.compareTo(nextIndexUri) == 0) {
                            ++instances;
                        } else {
                            break;
                        }
                    }
                    
                    if (instances > 1) {
                        // Multiples in index
                        this.errors.add(new MultiplesInconsistency(indexUri, instances, daoPropSet));
                    } else {
                        // No multiples
                        LOG.debug("URI OK: " + indexUri);
                        checkPropertySet(indexUri, randomIndexAccessor, daoPropSet);
                    }
                    
                } else if (compare > 0) {   // indexUri > daoUri
                    // Index is passed dao, this implies missing in index
                    LOG.debug("Missing start at daoUri = " + daoUri + ", indexUri = " + indexUri);
                    this.errors.add(new MissingInconsistency(daoUri, daoPropSet));
                    
                    // Missing until synced 
                    while (daoIterator.hasNext()) {
                        daoPropSet = (PropertySetImpl)daoIterator.next();
                        daoUri = daoPropSet.getURI();
                        
                        compare = indexUri.compareTo(daoUri);
                        if (compare > 0) {
                            // Another one missing
                            this.errors.add(new MissingInconsistency(daoUri, daoPropSet));
                        } else {
                            // In sync, or dangling
                            LOG.debug("Missing sync-point reached: indexUri = " + indexUri + ", daoURI = " + daoUri);
                            nextDaoPropSet = daoPropSet;
                            nextIndexUri = indexUri;
                            break;
                        } 
                    }
                    
                } else { // indexUri < daoUri
                    LOG.debug("Dangling start at indexUri = " + indexUri + ", daoURI = " + daoUri);
                    // Index is behind dao, this implies dangling instances in index
                    this.errors.add(new DanglingInconsistency(indexUri));
                    
                    // Dangling until synced
                    while (indexUriIterator.hasNext()) {
                        indexUri = (String)indexUriIterator.next();
                        
                        compare = indexUri.compareTo(daoUri);
                        if (compare < 0) {
                            // Index still behind
                            this.errors.add(new DanglingInconsistency(indexUri));
                        } else {
                            LOG.debug("Dangling sync-point reached: indexUri=" + indexUri + ", daoURI = " + daoUri);
                            // In sync, or missing from index
                            nextIndexUri = indexUri;
                            nextDaoPropSet = daoPropSet;
                            break;
                        }
                    }
                }
            } else {
                // Dangling in index
               this.errors.add(new DanglingInconsistency(indexUri));
            }
        }
        
        while (daoIterator.hasNext()) {
            // Missing from index
            PropertySetImpl daoPropSet = (PropertySetImpl) daoIterator.next();
            this.errors.add(new MissingInconsistency(daoPropSet.getURI(), daoPropSet));
        }
    }
    
    
    /**
     * Repair all encountered errors.
     * 
     * @param abortOnFailure
     *            If <code>true</code>, repairing will abort if an error
     *            occurs. If <code>false</code>, the repairing will continue,
     *            even if exceptions occur.
     * 
     */
    public void repairErrors(boolean abortOnFailure) throws IndexException {

        if (!this.completed) {
            throw new IllegalStateException(
                    "Cannot repair errors, the consistency check did not complete successfully.");
        }

        Iterator errorIterator = this.errors.iterator();
        while (errorIterator.hasNext()) {
            AbstractConsistencyError error = (AbstractConsistencyError)errorIterator.next();
            try {
              if (error.canRepair()) {
                  error.repair(this.index);
              } else {
                  LOG.warn("Error cannot be repaired: '" + error.getDescription() + "'");
              }
            } catch (IndexException ie) {
                if (abortOnFailure) {
                    LOG.warn("Aborting error repairing, exception '" + ie.getMessage() 
                            + "' while repairing error with description '" 
                            + error.getDescription() + "'");
                    throw ie;
                }
            }
            
        }
    }

    public List getErrors() {
        return new ArrayList(this.errors);
    }

}

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
package org.vortikal.repository.index.consistency;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.vortikal.repository.Path;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.PropertySetImpl;
import org.vortikal.repository.index.IndexException;
import org.vortikal.repository.index.PropertySetIndex;
import org.vortikal.repository.index.PropertySetIndexRandomAccessor;
import org.vortikal.repository.index.StorageCorruptionException;
import org.vortikal.repository.index.mapping.DocumentMappingException;
import org.vortikal.repository.store.IndexDao;
import org.vortikal.repository.store.PropertySetHandler;
import org.vortikal.security.Principal;
import org.vortikal.security.PrincipalFactory;

/**
 * Check consistency and optionally repair errors afterwords.
 * 
 * NOTE ! Usage of this class requires external locking of the index in question, if 
 * writing operations are known to occur during testing. This is necessary
 * so that the index isn't modified during testing, or between testing
 * and the call to {@link #repairErrors(boolean)} (in case of errors present).
 * 
 * @author oyviste
 */
public class ConsistencyCheck {

    private static final Log LOG = LogFactory.getLog(ConsistencyCheck.class);

    /**
     * Error limit. If more than this number of errors are encountered during check, 
     * an exception will be thrown. This is so as to avoid filling memory with
     * error instances, which do carry some amount of data.
     */
    public static final int ERROR_LIMIT = 10000;

    private IndexDao indexDao;
    private PropertySetIndex index;

    private List<AbstractConsistencyError> errors = 
        new ArrayList<AbstractConsistencyError>(); // List of detected inconsistencies
    private boolean completed = false; 
    

    /**
     * 
     * @param index
     * @param indexDataAccessor
     */
    private ConsistencyCheck(PropertySetIndex index,
                             IndexDao indexDao) {
        this.indexDao = indexDao;
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
            IndexDao indexDao) throws IndexException,
            ConsistencyCheckException, StorageCorruptionException {

        ConsistencyCheck check = new ConsistencyCheck(index, indexDao);
        check.runInternal();
        return check;
    }

    @SuppressWarnings("unchecked")
    private void runInternal() throws IndexException,
            ConsistencyCheckException, StorageCorruptionException {

        String indexId = this.index.getId();

        LOG.info("Running consistency check on index '" + indexId + "'");

        Iterator indexUriIterator = null;
        PropertySetIndexRandomAccessor randomIndexAccessor = null;

        try {
            
            LOG.info("Running storage corruption test ..");
            // This has the positive side effect of warming up the Lucene reader cache
            this.index.validateStorageFacility();
            LOG.info("Storage corruption test passed.");

            indexUriIterator = this.index.orderedUriIterator();
            randomIndexAccessor = this.index.randomAccessor();
            
            LOG.info("Running consistency check ..");
            runConsistencyCheck(randomIndexAccessor, indexUriIterator);
            
            if (this.errors.size() > 0) {
                LOG.warn("Consistency check completed, " + this.errors.size() + " inconsistencies detected");
            } else {
                LOG.info("Consistency check completed successfully without any errors detected");
            }

        } catch (StorageCorruptionException sce) {
            LOG.warn("Storage corruption test failed: " + sce.getMessage());
            throw sce; // Re-throw, since we can't work on or fix a corrupted index
        } finally {
            // Clean up resources
            if (indexUriIterator != null) this.index.close(indexUriIterator);
            if (randomIndexAccessor != null) randomIndexAccessor.close();
        }

        this.completed = true;
    }
    
    @SuppressWarnings("unchecked")
    private void runConsistencyCheck(final PropertySetIndexRandomAccessor randomIndexAccessor,
                                     final Iterator indexUriIterator) 
        throws IndexException {
        
        final Set<Path> validURIs = new HashSet<Path>(30000);
        
        PropertySetHandler handler = new PropertySetHandler() {

            @Override
            public void handlePropertySet(PropertySet propertySet, 
                                          Set<Principal> aclReadPrincipals) {

                PropertySetImpl daoPropSet = (PropertySetImpl)propertySet;
                Path currentUri = daoPropSet.getURI();
                int indexInstances = 
                    randomIndexAccessor.countInstances(currentUri);
                
                if (indexInstances == 0) {
                    // Missing in index
                    ConsistencyCheck.this.addError(
                            new MissingInconsistency(currentUri, daoPropSet, aclReadPrincipals));
                    return;
                } else  if (indexInstances == 1) {
                    // OK, only a single instance exists, verify the instance data
                    try {
                        ConsistencyCheck.this.checkPropertySet(currentUri,
                                                  randomIndexAccessor, 
                                                  daoPropSet,
                                                  aclReadPrincipals);
                    } catch (IOException io) {
                        throw new ConsistencyCheckException("IOException while running consistency check", io);
                    }
                } else {
                    // Multiples inconsistency
                    ConsistencyCheck.this.addError(
                            new MultiplesInconsistency(currentUri, indexInstances, daoPropSet, aclReadPrincipals));
                }
                // Add to set of valid index property set URIs
                validURIs.add(currentUri);
            }
        };
        
        this.indexDao.orderedPropertySetIteration(handler);
        
        // Need to make a complete pass over index URIs to detect dangling inconsistencies
        while (indexUriIterator.hasNext()) {
            Path currentUri = (Path) indexUriIterator.next();
            if (! validURIs.contains(currentUri)) {
                this.addError(new DanglingInconsistency(currentUri));
            }
        }
    }
    
    /**
     * Verify consistency of a single index property set. Checks resource ID (= index UUID) and
     * the ACL_INHERITED_FROM ID.
     * 
     * @param indexUri
     * @param randomIndexAccessor
     * @param repoPropSet
     * @throws IOException
     * @throws IndexException
     */
    private void checkPropertySet(Path indexUri, 
                                  PropertySetIndexRandomAccessor randomIndexAccessor, 
                                  PropertySetImpl repoPropSet,
                                  Set<Principal> repoAclReadPrincipals)
        throws IOException, IndexException {
    
        try {
            PropertySetImpl indexPropSet = 
                    (PropertySetImpl)randomIndexAccessor.getPropertySetByURI(indexUri);
            
            // If we get here, the index document has been successfully
            // mapped to a property set instance.
            int indexUUID = indexPropSet.getID();
            int daoUUID = repoPropSet.getID();
            
            if (indexUUID != daoUUID) {
                // Invalid UUID (this can also be considered a dangling inconsistency)
                addError(new InvalidUUIDInconsistency(indexUri, repoPropSet, repoAclReadPrincipals, indexUUID, daoUUID));
                return;
            } 
            
            // Check ACL read principals data
            Set<String> indexAclReadPrincipalNames = randomIndexAccessor.getAclReadPrincipalNamesByURI(indexUri);
            if (repoAclReadPrincipals.contains(PrincipalFactory.ALL)) {
                if (! (indexAclReadPrincipalNames.contains(PrincipalFactory.NAME_ALL) && indexAclReadPrincipalNames.size() == 1)) {
                    addError(new InvalidACLReadPrincipalsInconsistency(indexUri, repoPropSet, repoAclReadPrincipals, indexAclReadPrincipalNames));
                    return;
                }
            } else {
                // not read-for-all, then size and all elements must match
                if (repoAclReadPrincipals.size() != indexAclReadPrincipalNames.size()) {
                    addError(new InvalidACLReadPrincipalsInconsistency(indexUri, repoPropSet, repoAclReadPrincipals, indexAclReadPrincipalNames));
                    return;
                }
                
                for (Principal p: repoAclReadPrincipals) {
                    String qualifiedName = p.getQualifiedName();
                    if (!indexAclReadPrincipalNames.contains(qualifiedName)) {
                        addError(new InvalidACLReadPrincipalsInconsistency(indexUri, repoPropSet, repoAclReadPrincipals, indexAclReadPrincipalNames));
                        return;
                    }
                }
            }
            
            int indexACL = indexPropSet.getAclInheritedFrom();
            int daoACL = repoPropSet.getAclInheritedFrom();
            if (indexACL != daoACL) {
                // Invalid ACL inherited from
                addError(new InvalidACLInheritedFromInconsistency(indexUri, repoPropSet, repoAclReadPrincipals, indexACL, daoACL));
                return;
            }
            
        } catch (DocumentMappingException dme) {
            // Unmappable inconsistency
            addError(new UnmappableConsistencyError(indexUri, dme, repoPropSet, repoAclReadPrincipals));
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
     * TODO Convert to new index dao API - not done yet, because of the experimental 
     *      status.
     * 
     * 
     * XXX: does not work with current API. 
     * 
     * @param daoIterator
     * @param indexUriIterator
     * @throws IOException
     * @throws IndexException
     */    
//    @SuppressWarnings({ "unchecked", "unused" })
//    private void runConsistencyCheckExperimental(Iterator daoIterator, Iterator indexUriIterator, 
//                                                PropertySetIndexRandomAccessor randomIndexAccessor) 
//        throws IOException, IndexException {
//
//        Path indexUri = null;
//        Path nextIndexUri = null;
//        PropertySetImpl nextDaoPropSet = null;
//
//        while (indexUriIterator.hasNext() || nextIndexUri != null) {
//
//            if (nextIndexUri != null) {
//                indexUri = nextIndexUri;
//                nextIndexUri = null;
//            } else {
//                indexUri = (Path) indexUriIterator.next();
//            }
//            
//            if (daoIterator.hasNext() || nextDaoPropSet != null) {
//                PropertySetImpl daoPropSet = null;
//                if (nextDaoPropSet != null) {
//                    daoPropSet = nextDaoPropSet;
//                    nextDaoPropSet = null;
//                } else {
//                    daoPropSet = (PropertySetImpl) daoIterator.next();
//                }
//                
//                Path daoUri = daoPropSet.getURI();
//                
//                int compare = indexUri.compareTo(daoUri);
//                if (compare == 0) {  // indexUri == daoUri
//                    
//                    // See if it is duplicated in index
//                    int instances = 1;
//                    while (indexUriIterator.hasNext()) {
//                        nextIndexUri = (Path) indexUriIterator.next();
//                        if (indexUri.compareTo(nextIndexUri) == 0) {
//                            ++instances;
//                        } else {
//                            break;
//                        }
//                    }
//                    
//                    if (instances > 1) {
//                        // Multiples in index
//                        addError(new MultiplesInconsistency(indexUri, instances, daoPropSet));
//                    } else {
//                        // No multiples
//                        LOG.debug("URI OK: " + indexUri);
//                        checkPropertySet(indexUri, randomIndexAccessor, daoPropSet);
//                    }
//                    
//                } else if (compare > 0) {   // indexUri > daoUri
//                    // Index is passed dao, this implies missing in index
//                    LOG.debug("Missing start at daoUri = " + daoUri + ", indexUri = " + indexUri);
//                    addError(new MissingInconsistency(daoUri, daoPropSet));
//                    
//                    // Missing until synced 
//                    while (daoIterator.hasNext()) {
//                        daoPropSet = (PropertySetImpl)daoIterator.next();
//                        daoUri = daoPropSet.getURI();
//                        
//                        compare = indexUri.compareTo(daoUri);
//                        if (compare > 0) {
//                            // Another one missing
//                            addError(new MissingInconsistency(daoUri, daoPropSet));
//                        } else {
//                            // In sync, or dangling
//                            LOG.debug("Missing sync-point reached: indexUri = " + indexUri + ", daoURI = " + daoUri);
//                            nextDaoPropSet = daoPropSet;
//                            nextIndexUri = indexUri;
//                            break;
//                        } 
//                    }
//                    
//                } else { // indexUri < daoUri
//                    LOG.debug("Dangling start at indexUri = " + indexUri + ", daoURI = " + daoUri);
//                    // Index is behind dao, this implies dangling instances in index
//                    addError(new DanglingInconsistency(indexUri));
//                    
//                    // Dangling until synced
//                    while (indexUriIterator.hasNext()) {
//                        indexUri = (Path) indexUriIterator.next();
//                        
//                        compare = indexUri.compareTo(daoUri);
//                        if (compare < 0) {
//                            // Index still behind
//                            addError(new DanglingInconsistency(indexUri));
//                        } else {
//                            LOG.debug("Dangling sync-point reached: indexUri=" + indexUri + ", daoURI = " + daoUri);
//                            // In sync, or missing from index
//                            nextIndexUri = indexUri;
//                            nextDaoPropSet = daoPropSet;
//                            break;
//                        }
//                    }
//                }
//            } else {
//                // Dangling in index
//               addError(new DanglingInconsistency(indexUri));
//            }
//        }
//        
//        while (daoIterator.hasNext()) {
//            // Missing from index
//            PropertySetImpl daoPropSet = (PropertySetImpl) daoIterator.next();
//            addError(new MissingInconsistency(daoPropSet.getURI(), daoPropSet));
//        }
//    }
    
    
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

        for(AbstractConsistencyError error: this.errors) {
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
        this.index.commit();
    }
    
    private void addError(AbstractConsistencyError error) throws ConsistencyCheckException {

        if (this.errors.size() > ERROR_LIMIT) {
            this.completed = true;
            throw new ConsistencyCheckException("Too many errors, please consider re-indexing.");
        }

        this.errors.add(error);
    }

    public List<AbstractConsistencyError> getErrors() {
        return Collections.unmodifiableList(this.errors);
    }

}

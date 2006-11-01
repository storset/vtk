package org.vortikal.repositoryimpl.query.consistency;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.vortikal.repository.query.PropertySetIndex;
import org.vortikal.repositoryimpl.PropertySetImpl;
import org.vortikal.repositoryimpl.query.IndexException;

/**
 * Represents error where multiple index documents (property sets) exist for a single URI.
 * 
 * @author oyviste
 *
 */
public class MultiplesInconsistency extends AbstractConsistencyError {

    private static final Log LOG = LogFactory.getLog(MultiplesInconsistency.class);
    
    private int multiples;
    private PropertySetImpl repositoryPropSet;
    
    public MultiplesInconsistency(String uri, int multiples, PropertySetImpl repositoryPropSet) {
        super(uri);
        this.multiples = multiples;
        this.repositoryPropSet = repositoryPropSet;
    }
    
    public boolean canRepair() {
        return true;
    }
    
    public String getDescription() {
        return "Multiples inconsistency, there are " 
            + multiples + " property sets in index at URI '" + getUri() + "'";
    }

    /**
     * Repair by removing all property sets for the URI, then re-adding a pristine copy from the
     * repository.
     */
    protected void repair(PropertySetIndex index) throws IndexException {
        
        LOG.info("Repairing multiples inconsistency for URI '" + getUri() 
                                                    + "' (" + multiples + " multiples)");

        index.deletePropertySet(getUri());
        
        index.addPropertySet(this.repositoryPropSet);

    }

    public String toString() {
        return "MultipleConsistencyError[URI = '" + getUri() + "', number of multiples in index: " 
                                                                            + this.multiples + "]";
    }
    
}

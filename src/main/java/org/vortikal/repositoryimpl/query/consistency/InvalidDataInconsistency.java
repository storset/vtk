package org.vortikal.repositoryimpl.query.consistency;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.vortikal.repository.query.PropertySetIndex;
import org.vortikal.repositoryimpl.PropertySetImpl;
import org.vortikal.repositoryimpl.query.IndexException;

/**
 * General data inconsistency error when there is a mismatch between the property set data in the index
 * and the property set data in the repository.
 *  
 * @author oyviste
 */
public class InvalidDataInconsistency extends AbstractConsistencyError {

    private static final Log LOG = LogFactory.getLog(InvalidDataInconsistency.class);
    private PropertySetImpl repositoryPropSet;
    
    public InvalidDataInconsistency(String uri, PropertySetImpl repositoryPropSet) {
        super(uri);
        this.repositoryPropSet = repositoryPropSet;
    }

    public boolean canRepair() {
        return true;
    }
    
    public String getDescription() {
        return "Invalid data inconsistency, data in index property set at URI '" 
                              + getUri() + "' does not match data in property set in repository.";
    }

    /**
     * Fix by deleting property set in index, and re-adding pristine repository copy
     */
    protected void repair(PropertySetIndex index) throws IndexException {
        
        LOG.info("Repairing invalid data for property set at URI '"
                + getUri() + "'");
        
        index.deletePropertySet(getUri());
        
        index.addPropertySet(this.repositoryPropSet);
    }
    
    public String toString() {
        return "InvalidDataInconsistency[URI='" + getUri() + "']"; 
    }

}

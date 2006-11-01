package org.vortikal.repositoryimpl.query.consistency;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.vortikal.repository.query.PropertySetIndex;
import org.vortikal.repositoryimpl.query.IndexException;

/**
 * Represents inconsistency where property sets deleted from the repository still exists in the
 * index. There might be multiple property sets for the given URI in the index, but there is
 * no property set for the URI in the repository.
 * 
 * @author oyviste
 *
 */
public class DanglingInconsistency extends AbstractConsistencyError {

    private static final Log LOG = LogFactory.getLog(DanglingInconsistency.class);
    
    public DanglingInconsistency(String uri) {
        super(uri);
    }
    
    public boolean canRepair() {
        return true;
    }
    
    public String getDescription() {
        return "Dangling inconsistency, an instance with URI '" 
                + getUri() + "' exists in index, but not in the repository.";
    }

    /**
     * Repair by deleting all property sets for the URI.
     */
    protected void repair(PropertySetIndex index) throws IndexException {
        LOG.info("Repairing dangling inconsistency by deleting all index property sets with URI '"
                + getUri() + "'");
        
        int n = index.deletePropertySet(getUri());
        
        LOG.info("Deleted " + n + " index property sets");
    }
    
    public String toString() {
        return "DanglingInconsistency[URI='" + getUri() + "']";
    }

}

package org.vortikal.repositoryimpl.query.consistency;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.vortikal.repository.query.PropertySetIndex;
import org.vortikal.repositoryimpl.PropertySetImpl;
import org.vortikal.repositoryimpl.query.IndexException;

/**
 * Consistency error where a property exists in the repository, but not in the index.
 * 
 * @author oyviste
 *
 */
public class MissingInconsistency extends AbstractConsistencyError {
    
    private static final Log LOG = LogFactory.getLog(MissingInconsistency.class);
    private PropertySetImpl repositoryPropSet;
    
    public MissingInconsistency(String uri, PropertySetImpl repositoryPropSet) {
        super(uri);
        this.repositoryPropSet = repositoryPropSet;
    }

    public boolean canRepair() {
        return true;
    }
    
    public String getDescription() {
        return "Property set in repository at URI '" + getUri() + "' does not exist in index.";
    }
    
    public String toString() {
        return "MissingInconsistency[URI='" + getUri() + "']";
    }

    /**
     * Fix by adding missing property set.
     */
    protected void repair(PropertySetIndex index) throws IndexException {
        LOG.info("Repairing missing inconsistency by adding property set at URI '" 
                + getUri() + "'");
        
        index.addPropertySet(repositoryPropSet);

    }

}

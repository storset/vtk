package org.vortikal.repositoryimpl.query.consistency;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.vortikal.repository.query.PropertySetIndex;
import org.vortikal.repositoryimpl.PropertySetImpl;
import org.vortikal.repositoryimpl.query.IndexException;

/**
 * Consistency error representing an index document which could not be mapped to a
 * <code>PropertySet</code> instance. This is usually caused by property type configuration 
 * problems. 
 * 
 * @author oyviste
 *
 */
public class UnmappableConsistencyError extends AbstractConsistencyError {

    private static final Log LOG = LogFactory.getLog(UnmappableConsistencyError.class);
    
    private PropertySetImpl repositoryPropSet;
    private Exception mappingException;
    
    public UnmappableConsistencyError(String uri, Exception mappingException, 
                                                                PropertySetImpl repositoryPropSet) {
        super(uri);
        this.repositoryPropSet = repositoryPropSet;
        this.mappingException = mappingException;
    }

    public boolean canRepair() {
        return true;
    }
    
    public String getDescription() {
        return "Index document representing property set at URI '" 
            + getUri() + "' could not be mapped to a PropertySet instance,"
            + " exception message: " + mappingException.getMessage();
    }

    /**
     * Repair by deleting index property set and re-adding pristine copy from repository.
     */
    protected void repair(PropertySetIndex index) throws IndexException {
        LOG.info("Repairing unmappable consistency error at URI '" + getUri() + "'");

        index.deletePropertySet(getUri());
        
        index.addPropertySet(repositoryPropSet);
    }
    
    public String toString() {
        return "UnmappableConsistencyError[URI = '" + getUri() + "', exception message = '" 
        + mappingException.getMessage() + "']";
    }

}

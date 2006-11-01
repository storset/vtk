package org.vortikal.repositoryimpl.query.consistency;

import org.vortikal.repository.query.PropertySetIndex;
import org.vortikal.repositoryimpl.query.IndexException;

/**
 * Base class for consistency errors (aka inconsistencies).
 *  
 * @author oyviste
 */
public abstract class AbstractConsistencyError implements ConsistencyError {
    
    private String uri;
    
    public AbstractConsistencyError(String uri) {
        this.uri = uri;
    }
    
    public String getUri() {
        return this.uri;
    }
    
    public abstract String getDescription();

    public abstract boolean canRepair();
    
    protected abstract void repair(PropertySetIndex index) throws IndexException;

}

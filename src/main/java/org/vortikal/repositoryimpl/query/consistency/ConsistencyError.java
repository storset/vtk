package org.vortikal.repositoryimpl.query.consistency;

/**
 * External interface to consistency errors.
 * 
 * @author oyviste
 *
 */
public interface ConsistencyError {

    public String getUri();
    
    public String getDescription();
    
    public boolean canRepair();
    
}

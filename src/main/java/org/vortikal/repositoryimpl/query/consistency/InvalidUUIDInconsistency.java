package org.vortikal.repositoryimpl.query.consistency;

import org.vortikal.repositoryimpl.PropertySetImpl;

/**
 * Invalid (UU)ID inconsistency (in reality a dangling consistency error)
 * 
 * @author oyviste
 *
 */
public class InvalidUUIDInconsistency extends InvalidDataInconsistency {

    private int indexUUID = -1;
    private int daoUUID = -1;
    
    public InvalidUUIDInconsistency(String uri, PropertySetImpl daoPropSet, int indexUUID, int daoUUID) {
        super(uri, daoPropSet);
        this.indexUUID = indexUUID;
        this.daoUUID = daoUUID;
    }
    
    public boolean canRepair() {
        return true;
    }
    
    public String getDescription() {
        return "Invalid UUID inconsistency for index property set at URI '"
          + getUri() + "', index UUID = + " + this.indexUUID + ", daoUUID = " + this.daoUUID;
    }

    public String toString() {
        return "InvalidUUIDInconsistency[URI='" + getUri() + "', indexUUID = " 
        + this.indexUUID + ", daoUUID = " + this.daoUUID + "]"; 
    }

}

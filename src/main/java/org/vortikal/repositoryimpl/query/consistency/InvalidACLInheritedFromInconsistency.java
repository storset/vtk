package org.vortikal.repositoryimpl.query.consistency;

import org.vortikal.repositoryimpl.PropertySetImpl;

/**
 * Invalid ACL inconsistency.
 * 
 * @author oyviste
 *
 */
public class InvalidACLInheritedFromInconsistency extends
        InvalidDataInconsistency {

    private int indexACL = -1;
    private int daoACL = -1;
    
    public InvalidACLInheritedFromInconsistency(String uri, PropertySetImpl daoPropSet, 
                                                int indexACL, int daoACL) {
        super(uri, daoPropSet);
        this.indexACL = indexACL;
        this.daoACL = daoACL;
    }
    
    public boolean canRepair() {
        return true;
    }
    
    public String getDescription() {
        return "Invalid ACL inherited from inconsistency for index property set at URI '"
          + getUri() + "', indexACL = + " + this.indexACL + ", daoACL = " + this.daoACL;
    }

    public String toString() {
        return "InvalidACLInheritedFromInconsistency[URI='" + getUri() + "', indexACL = " 
        + this.indexACL + ", daoACL = " + this.daoACL + "]"; 
    }

}

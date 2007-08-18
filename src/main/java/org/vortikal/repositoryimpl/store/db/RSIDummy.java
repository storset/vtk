package org.vortikal.repositoryimpl.store.db;

import org.vortikal.repositoryimpl.search.query.security.ResultSecurityInfo;

public class RSIDummy implements ResultSecurityInfo {
    
    private Integer aclNodeId;
    private boolean authorized;
    private String owner;
    
    public RSIDummy(int aclNodeId,
                String owner) {
        this.aclNodeId = new Integer(aclNodeId);
        this.owner = owner;
    }
    
    public Integer getAclNodeId() {
        return this.aclNodeId;
    }

    public String getOwnerAsUserOrGroupName() {
        return this.owner;
    }

    public boolean isAuthorized() {
        return this.authorized;
        
    }

    public void setAuthorized(boolean authorized) {
        this.authorized = authorized;
    }

}

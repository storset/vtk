package org.vortikal.repository;

import java.util.List;
import java.util.Map;

import org.vortikal.security.Principal;

public interface Acl extends Cloneable {

    public void addEntry(String action, String name, boolean isGroup);

    public void removeEntry(String username, String privilegeName);

    /**
     * Gets the set of privileges on this resource for a given principal.
     */
    public String[] getPrivilegeSet(Principal principal);
        
    /**
     * @param privilegeName
     * @return a list of <code>Principal</code> objects
     */
    public Principal[] listPrivilegedUsers(String privilegeName);

    /**
     * @param privilegeName
     * @return a list of <code>String</code> group names.
     */
    public String[] listPrivilegedGroups(String privilegeName);
    
    public boolean isInherited();

    public void setInherited(boolean inherited);
    
    public boolean hasPrivilege(String principalName, String privilegeName);

    public Object clone() throws CloneNotSupportedException;
    
    
    // XXX: From impl...

    public List getPrincipalList(String action);

    public Map getPrivilegeMap();

}

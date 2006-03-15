package org.vortikal.repository;

import java.util.List;

import org.vortikal.security.Principal;

public interface Acl extends Cloneable {

    /**
     * Gets the set of privileges on this resource for a given principal.
     */
    public Privilege[] getPrivilegeSet(Principal principal);
        
    public void addPrivilegeToACL(String username, String privilegeName, boolean isUser);

    public void withdrawPrivilegeFromACL(String username, String privilegeName);

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
    
    /**
     * Lists principals (users and groups) having a given privilege.
     *
     * @param acl an <code>Ace[]</code> value
     * @param privilegeName a <code>String</code> value
     * @return a <code>List</code> of group names
     */
    public List listPrivilegedPrincipals(String privilegeName);

    public boolean isInherited();

    public void setInherited(boolean inherited);
    
    public boolean hasPrivilege(String principalName, String privilegeName);

    public Object clone() throws CloneNotSupportedException;
    
}

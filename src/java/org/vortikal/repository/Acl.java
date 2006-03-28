package org.vortikal.repository;

import java.util.Set;

import org.vortikal.security.Principal;

public interface Acl extends Cloneable {

    public void addEntry(String action, Principal principal);

    public void removeEntry(String privilegeName, Principal principal);

    public Principal getOwner();
    
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
    public Principal[] listPrivilegedGroups(String privilegeName);
    
    public Principal[] listPrivilegedPseudoPrincipals(String action);

    public boolean isInherited();

    public void setInherited(boolean inherited);
    
    public boolean hasPrivilege(Principal principal, String privilegeName);

    public Object clone() throws CloneNotSupportedException;
    
    
    // XXX: From impl...

    public Set getPrincipalSet(String action);

    public Set getActions();

}

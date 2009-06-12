/* Copyright (c) 2006, University of Oslo, Norway
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 
 *  * Neither the name of the University of Oslo nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *      
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.vortikal.repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.vortikal.security.Principal;
import org.vortikal.security.PrincipalFactory;


public class AclImpl implements Acl {

    /**
     * map: [action --> Set(Principal)]
     */
    private Map<RepositoryAction, Set<Principal>> actionSets = 
        new HashMap<RepositoryAction, Set<Principal>>();

    public boolean hasPrivilege(RepositoryAction privilege, Principal principal) {
        Set<Principal> actionSet = this.actionSets.get(privilege);
        
        if (actionSet != null && actionSet.contains(principal)) 
            return true;
        return false;
    }

 
    public Set<RepositoryAction> getActions() {
        return this.actionSets.keySet();
    }

    public Set<Principal> getPrincipalSet(RepositoryAction action) {
        Set<Principal> set = this.actionSets.get(action);
        if (set == null) {
            return new HashSet<Principal>();
        }
        return set;
    }

    public void clear() {
        this.actionSets = new HashMap<RepositoryAction, Set<Principal>>();
        addEntry(Privilege.ALL, PrincipalFactory.OWNER);
    }
    
    public boolean isValidEntry(RepositoryAction action, Principal principal) {
        if (!Privilege.PRIVILEGES.contains(action))
            throw new IllegalArgumentException("Unknown acl privilege");
            
        if (principal == null)
            throw new IllegalArgumentException("Null principal");
            
        if (PrincipalFactory.ALL.equals(principal)) {
            if (Privilege.ALL.equals(action) || Privilege.WRITE.equals(action)
                || Privilege.BIND.equals(action) || Privilege.ADD_COMMENT.equals(action)) {
                return false;
            }
        }
        return true;
    }
    
    
    public void addEntry(RepositoryAction action, Principal p) {
        if (!Privilege.PRIVILEGES.contains(action))
            throw new IllegalArgumentException("Unknown acl privilege");
            
        if (p == null)
            throw new IllegalArgumentException("Null principal");
            
        if (!isValidEntry(action, p)) {
            throw new IllegalArgumentException(
                    "Not allowed to add principal '" + p + "' to privilege '"
                    + action + "'" );
        }
        
        Set<Principal> actionEntry = this.actionSets.get(action);
        if (actionEntry == null) {
            actionEntry = new HashSet<Principal>();
            this.actionSets.put(action, actionEntry);
        }
        
        actionEntry.add(p);
    }
    
    public void addEntryNoValidation(RepositoryAction action, Principal p) {
        if (!Privilege.PRIVILEGES.contains(action))
            throw new IllegalArgumentException("Unknown acl privilege");
            
        if (p == null)
            throw new IllegalArgumentException("Null principal");
        
        Set<Principal> actionEntry = this.actionSets.get(action);
        if (actionEntry == null) {
            actionEntry = new HashSet<Principal>();
            this.actionSets.put(action, actionEntry);
        }
        
        actionEntry.add(p);
    }
    
    public void removeEntry(RepositoryAction action, Principal principal)
        throws IllegalArgumentException {

        if (!Privilege.PRIVILEGES.contains(action))
            throw new IllegalArgumentException("Unknown acl privilege");
            
        if (principal == null)
            throw new IllegalArgumentException("Null principal");
            
        if (PrincipalFactory.OWNER.equals(principal) &&
                Privilege.ALL.equals(action))
                throw new IllegalArgumentException("Not allowed to remove acl entry");
        
        Set<Principal> actionEntry = this.actionSets.get(action);
        
        if (actionEntry == null) return;
        actionEntry.remove(principal);
    }


    public boolean containsEntry(RepositoryAction action, Principal principal) throws IllegalArgumentException {

        if (!Privilege.PRIVILEGES.contains(action))
            throw new IllegalArgumentException("Unknown acl privilege");
            
        if (principal == null)
            throw new IllegalArgumentException("Null principal");
            
        Set<Principal> actionEntry = this.actionSets.get(action);
        
        if (actionEntry == null) return false;
        return actionEntry.contains(principal);
    }


    public Principal[] listPrivilegedUsers(RepositoryAction action) {
        Set<Principal> principals = this.actionSets.get(action);

        if (principals == null) return new Principal[0];
        
        List<Principal> userList = new ArrayList<Principal>();
        for (Principal p: principals) {
            if (p.getType() == Principal.Type.USER) {
                userList.add(p);
            }
            
        }
        return userList.toArray(new Principal[userList.size()]);
    }

    public Principal[] listPrivilegedGroups(RepositoryAction action) {
        Set<Principal> principals = this.actionSets.get(action);
        
        if (principals == null) return new Principal[0];
        
        List<Principal> groupList = new ArrayList<Principal>();
        for (Principal p: principals) {
            if (p.getType() == Principal.Type.GROUP) {
                groupList.add(p);
            }
        }
        return groupList.toArray(new Principal[groupList.size()]);
    }
    
    public Principal[] listPrivilegedPseudoPrincipals(RepositoryAction action) {
        Set<Principal> principals = this.actionSets.get(action);
        
        if (principals == null) return new Principal[0];
        
        List<Principal> principalList = new ArrayList<Principal>();
        for (Principal p: principals) {
            if (p.getType() == Principal.Type.PSEUDO) {
                principalList.add(p);
            }
            
        }
        return principalList.toArray(new Principal[principalList.size()]);
    }


    public RepositoryAction[] getPrivilegeSet(Principal principal) {
        Set<RepositoryAction> actions = new HashSet<RepositoryAction>();
        
        for (Map.Entry<RepositoryAction, Set<Principal>> entry: this.actionSets.entrySet()) {
            RepositoryAction action = entry.getKey();
            Set<Principal> actionEntries = entry.getValue();
            if (actionEntries != null && actionEntries.contains(principal))
                actions.add(action);
        }
        
        return actions.toArray(new RepositoryAction[actions.size()]);
    }


    
    public boolean equals(Object o) {
        if (!(o instanceof AclImpl)) {
            return false;
        }

        AclImpl acl = (AclImpl) o;

        if (acl == this) {
            return true;
        }

        Set<RepositoryAction> actions = this.actionSets.keySet();

        if (actions.size() != acl.actionSets.keySet().size()) {
            return false;
        }

        for (RepositoryAction action: actions) {
            if (!acl.actionSets.containsKey(action)) {
                return false;
            }

            Set<Principal> myPrincipals = this.actionSets.get(action);
            Set<Principal> otherPrincipals = acl.actionSets.get(action);

            if (myPrincipals.size() != otherPrincipals.size()) {
                return false;
            }

            for (Principal p: myPrincipals) {
                if (!otherPrincipals.contains(p)) {
                    return false;
                }
            }
        }

        return true;
    }

    public int hashCode() {
        int hashCode = super.hashCode();

        Set<RepositoryAction> actions = this.actionSets.keySet();

        for (RepositoryAction action: actions) {
            for (Principal p: this.actionSets.get(action)) {
                hashCode += p.hashCode() + action.hashCode();
            }
        }

        return hashCode;
    }

    public Object clone() {
        AclImpl clone = new AclImpl();

        for (Map.Entry<RepositoryAction, Set<Principal>> entry: this.actionSets.entrySet()) {
            
            RepositoryAction action = entry.getKey();

            for (Principal p: entry.getValue()) {
                clone.addEntryNoValidation(action, p);
            }
        }
        return clone;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("[ACL: ");
        sb.append("access: ");
        for (RepositoryAction action: this.actionSets.keySet()) {
            Set<Principal> principalSet = this.actionSets.get(action);

            sb.append(" [");
            sb.append(action);
            sb.append(":");
            for (Iterator<Principal> j = principalSet.iterator(); j.hasNext();) {
                Principal p = j.next();

                sb.append(" ");
                sb.append(p);

                if (j.hasNext()) {
                    sb.append(",");
                }
            }
            sb.append("]");
        }
        sb.append("]");
        return sb.toString();
    }
    
}

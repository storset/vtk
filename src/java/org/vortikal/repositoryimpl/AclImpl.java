/* Copyright (c) 2004, University of Oslo, Norway
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
package org.vortikal.repositoryimpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.vortikal.repository.Acl;
import org.vortikal.repository.Privilege;
import org.vortikal.repository.RepositoryAction;
import org.vortikal.security.Principal;
import org.vortikal.security.PseudoPrincipal;


public class AclImpl implements Acl {

    private boolean inherited = true;
    private boolean dirty = false; 
    
    /**
     * map: [action --> Set(Principal)]
     */
    private Map actionSets = new HashMap();

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public boolean isDirty() {
        return this.dirty;
    }

    public boolean hasPrivilege(RepositoryAction privilege, Principal principal) {
        Set actionSet = (Set) this.actionSets.get(privilege);
        
        if (actionSet != null && actionSet.contains(principal)) 
            return true;
        return false;
    }

 
    public Set getActions() {
        return this.actionSets.keySet();
    }

    public Set getPrincipalSet(RepositoryAction action) {
        Set set = (Set) this.actionSets.get(action);
        if (set == null) {
            return new HashSet();
        }
        return set;
    }

    public void clear() {
        this.actionSets = new HashMap();
        this.inherited = true;
        addEntry(Privilege.ALL, PseudoPrincipal.OWNER);
        this.dirty = true;
    }
    

    public void addEntry(RepositoryAction action, Principal p) throws IllegalArgumentException {
        if (!Privilege.PRIVILEGES.contains(action))
            throw new IllegalArgumentException("Unknown acl privilege");
            
        if (p == null)
            throw new IllegalArgumentException("Null principal");
            
        Principal all = PseudoPrincipal.ALL;
        
        if ((Privilege.ALL.equals(action) || Privilege.WRITE.equals(action)) 
                && all.equals(p))
                throw new IllegalArgumentException("Not allowed to add acl entry");
        
        this.dirty = true;
        
        Set actionEntry = (Set) this.actionSets.get(action);
        if (actionEntry == null) {
            actionEntry = new HashSet();
            this.actionSets.put(action, actionEntry);
        }
        
        actionEntry.add(p);

    }
    
    public void removeEntry(RepositoryAction action, Principal principal) throws IllegalArgumentException {

        if (!Privilege.PRIVILEGES.contains(action))
            throw new IllegalArgumentException("Unknown acl privilege");
            
        if (principal == null)
            throw new IllegalArgumentException("Null principal");
            
        if (PseudoPrincipal.OWNER.equals(principal) &&
                Privilege.ALL.equals(action))
                throw new IllegalArgumentException("Not allowed to remove acl entry");
        
        this.dirty = true;

        Set actionEntry = (Set) this.actionSets.get(action);
        
        if (actionEntry == null) return;
        actionEntry.remove(principal);
    }


    public boolean containsEntry(RepositoryAction action, Principal principal) throws IllegalArgumentException {

        if (!Privilege.PRIVILEGES.contains(action))
            throw new IllegalArgumentException("Unknown acl privilege");
            
        if (principal == null)
            throw new IllegalArgumentException("Null principal");
            
        Set actionEntry = (Set) this.actionSets.get(action);
        
        if (actionEntry == null) return false;
        return actionEntry.contains(principal);
    }


    public Principal[] listPrivilegedUsers(RepositoryAction action) {
        Set principals = (Set) this.actionSets.get(action);

        if (principals == null) return new Principal[0];
        
        List userList = new ArrayList();
        for (Iterator iter = principals.iterator(); iter.hasNext();) {
            Principal p = (Principal) iter.next();

            if (p.getType() == Principal.TYPE_USER) {
                userList.add(p);
            }
            
        }
        return (Principal[]) userList.toArray(new Principal[userList.size()]);
    }

    public Principal[] listPrivilegedGroups(RepositoryAction action) {
        Set principals = (Set) this.actionSets.get(action);
        
        if (principals == null) return new Principal[0];
        
        List groupList = new ArrayList();
        for (Iterator iter = principals.iterator(); iter.hasNext();) {
            Principal p = (Principal) iter.next();

            if (p.getType() == Principal.TYPE_GROUP) {
                groupList.add(p);
            }
            
        }
        return (Principal[]) groupList.toArray(new Principal[groupList.size()]);
    }
    
    public Principal[] listPrivilegedPseudoPrincipals(RepositoryAction action) {
        Set principals = (Set) this.actionSets.get(action);
        
        if (principals == null) return new Principal[0];
        
        List principalList = new ArrayList();
        for (Iterator iter = principals.iterator(); iter.hasNext();) {
            Principal p = (Principal) iter.next();

            if (p.getType() == Principal.TYPE_PSEUDO) {
                principalList.add(p);
            }
            
        }
        return (Principal[]) principalList.toArray(new Principal[principalList.size()]);
    }

    public boolean isInherited() {
        return this.inherited;
    }

    public void setInherited(boolean inherited) {
        this.dirty = true;
        this.inherited = inherited;
    }

    public RepositoryAction[] getPrivilegeSet(Principal principal) {
        Set actions = new HashSet();
        
        for (Iterator iter = this.actionSets.entrySet().iterator(); iter.hasNext();) {
            Map.Entry entry = (Map.Entry) iter.next();
            RepositoryAction action = (RepositoryAction) entry.getKey();
            Set actionEntries = (Set) entry.getValue();
            if (actionEntries != null && actionEntries.contains(principal))
                actions.add(action);
        }
        
        return (RepositoryAction[]) actions.toArray(new String[actions.size()]);
    }


    
    public boolean equals(Object o) {
        if (!(o instanceof AclImpl)) {
            return false;
        }

        AclImpl acl = (AclImpl) o;

        if (acl == this) {
            return true;
        }

        if (acl.isInherited() != this.inherited) {
            return false;
        }

        Set actions = this.actionSets.keySet();

        if (actions.size() != acl.actionSets.keySet().size()) {
            return false;
        }

        for (Iterator i = actions.iterator(); i.hasNext();) {
            RepositoryAction action = (RepositoryAction) i.next();

            if (!acl.actionSets.containsKey(action)) {
                return false;
            }

            Set myPrincipals = (Set) this.actionSets.get(action);
            Set otherPrincipals = (Set) acl.actionSets.get(action);

            if (myPrincipals.size() != otherPrincipals.size()) {
                return false;
            }

            for (Iterator j = myPrincipals.iterator(); j.hasNext();) {
                Principal p = (Principal) j.next();

                if (!otherPrincipals.contains(p)) {
                    return false;
                }
            }
        }

        return true;
    }

    public int hashCode() {
        int hashCode = super.hashCode();

        Set actions = this.actionSets.keySet();

        for (Iterator i = actions.iterator(); i.hasNext();) {
            RepositoryAction action = (RepositoryAction) i.next();
            Set principals = (Set) this.actionSets.get(action);

            for (Iterator j = principals.iterator(); j.hasNext();) {
                Principal p = (Principal) j.next();
                hashCode += p.hashCode() + action.hashCode();
            }
        }

        return hashCode;
    }

    public Object clone() {
        AclImpl clone = new AclImpl();
        clone.setInherited(this.inherited);

        for (Iterator iter = this.actionSets.entrySet().iterator(); iter.hasNext();) {
            Map.Entry entry = (Map.Entry) iter.next();
            for (Iterator iterator = ((Set)entry.getValue()).iterator(); iterator.hasNext();) {
                Principal p = (Principal) iterator.next();
                clone.addEntry((RepositoryAction) entry.getKey(), p);
            }
        }
        clone.setDirty(this.dirty);
        
        return clone;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();

        sb.append("[ACL: ");
        sb.append("[inherited: ").append(this.inherited).append("] ");
        sb.append("access: ");
        for (Iterator i = this.actionSets.keySet().iterator(); i.hasNext();) {
            RepositoryAction action = (RepositoryAction) i.next();
            Set principalSet = (Set)this.actionSets.get(action);

            sb.append(" [");
            sb.append(action);
            sb.append(":");
            for (Iterator j = principalSet.iterator(); j.hasNext();) {
                Principal p = (Principal) j.next();

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

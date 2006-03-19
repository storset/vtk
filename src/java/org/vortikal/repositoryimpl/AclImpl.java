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
import org.vortikal.security.Principal;
import org.vortikal.security.PrincipalManager;


public class AclImpl implements Acl {

    // XXX: These are duplicated from resource, check consistency!
    private boolean inherited;
    private Principal owner;
    
    private PrincipalManager principalManager;
    
    /**
     * map: [action --> List(ACLPrincipal)]
     */
    private Map actionLists = new HashMap();

    public AclImpl(PrincipalManager principalManager) {
        this.principalManager = principalManager;
    }
    
    
    public Map getPrivilegeMap() {
        return actionLists;
    }

    public List getPrincipalList(String action) {
        return (List) actionLists.get(action);
    }

    public void addEntry(String action, String name, boolean isGroup) {

        List actionEntry = (List) this.actionLists.get(action);
        if (actionEntry == null) {
            actionEntry = new ArrayList();
            this.actionLists.put(action, actionEntry);
        }

        actionEntry.add(new ACLPrincipal(name, isGroup));

    }
    
    public void removeEntry(String username, String action) {
        List actionEntry = (List) this.actionLists.get(action);
        
        if (actionEntry == null) return;
        
        ACLPrincipal a = null;
        for (Iterator iter = actionEntry.iterator(); iter.hasNext();) {
            ACLPrincipal p = (ACLPrincipal) iter.next();
            if (p.getUrl().equals(username)) {
                a = p;
                break;
            }
        }
        if (a != null) actionEntry.remove(a);
    }

    public Principal[] listPrivilegedUsers(String action) {
        List principals = (List)this.actionLists.get(action);
   
        List userList = new ArrayList();
        for (Iterator iter = principals.iterator(); iter.hasNext();) {
            ACLPrincipal p = (ACLPrincipal) iter.next();

            // Don't include user = "dav:authenticated" or groups
            if (p.getType() != ACLPrincipal.TYPE_AUTHENTICATED && !p.isGroup()) {
                Principal principal = principalManager.getPrincipal(p.getUrl());
                userList.add(principal);
            }
            
        }
        return (Principal[]) userList.toArray(new Principal[userList.size()]);
    }

    public String[] listPrivilegedGroups(String action) {
        List principals = (List)this.actionLists.get(action);
        
        List groupList = new ArrayList();
        for (Iterator iter = principals.iterator(); iter.hasNext();) {
            ACLPrincipal p = (ACLPrincipal) iter.next();

            if (p.isGroup()) {
                groupList.add(p.getUrl());
            }
            
        }
        return (String[]) groupList.toArray(new String[groupList.size()]);
    }

    public boolean isInherited() {
        return this.inherited;
    }

    public void setInherited(boolean inherited) {
        this.inherited = inherited;
    }

    public boolean hasPrivilege(String principalName, String action) {
        List actionEntry = (List) this.actionLists.get(action);
        
        if (actionEntry == null) return false;
        
        for (Iterator iter = actionEntry.iterator(); iter.hasNext();) {
            ACLPrincipal p = (ACLPrincipal) iter.next();
            if (p.getUrl().equals(principalName)) {
                return true;
            }
        }
        return false;
    }

    public String[] getPrivilegeSet(Principal principal) {
        Set actions = new HashSet();
        
        // XXX: Unfinished: root and the likes?
        for (Iterator iter = this.actionLists.entrySet().iterator(); iter.hasNext();) {
            Map.Entry entry = (Map.Entry) iter.next();
            String action = (String)entry.getKey();
            List actionEntries = (List)entry.getValue();

            boolean finished = false;
            for (Iterator iterator = actionEntries.iterator(); !finished && iterator.hasNext();) {
                ACLPrincipal p = (ACLPrincipal) iterator.next();

                switch (p.getType()) {

                case ACLPrincipal.TYPE_URL:
                
                    if (principal != null && p.getUrl().equals(principal.getQualifiedName())) {
                        actions.add(action);
                        finished = true;
                    } else if (principal != null && p.isGroup() && principalManager.isMember(principal, p.getUrl())) {
                        actions.add(action);
                        finished = true;
                    }
                    break;
                case ACLPrincipal.TYPE_ALL:
                    actions.add(action);
                    finished = true;
                    break;
                case ACLPrincipal.TYPE_AUTHENTICATED:
                    if (principal != null) {
                        actions.add(action);
                        finished = true;
                    }
                    break;
                case ACLPrincipal.TYPE_OWNER:
                    if (this.owner.equals(principal)) {
                        actions.add(action);
                        finished = true;
                    }
                }
            }
        }
        
        return (String[])actions.toArray(new String[actions.size()]);
    }


    
    public boolean equals(Object o) {
        if (!(o instanceof AclImpl)) {
            return false;
        }

        AclImpl acl = (AclImpl) o;

        Set actions = actionLists.keySet();

        if (actions.size() != acl.actionLists.keySet().size()) {
            return false;
        }

        for (Iterator i = actions.iterator(); i.hasNext();) {
            String action = (String) i.next();

            if (!acl.actionLists.containsKey(action)) {
                return false;
            }

            List myPrincipals = (List) actionLists.get(action);
            List otherPrincipals = (List) acl.actionLists.get(action);

            if (myPrincipals.size() != otherPrincipals.size()) {
                return false;
            }

            for (Iterator j = myPrincipals.iterator(); j.hasNext();) {
                ACLPrincipal p = (ACLPrincipal) j.next();

                if (!otherPrincipals.contains(p)) {
                    return false;
                }
            }
        }

        return true;
    }

    public int hashCode() {
        int hashCode = super.hashCode();

        Set actions = actionLists.keySet();

        for (Iterator i = actions.iterator(); i.hasNext();) {
            String action = (String) i.next();
            List principals = (List) actionLists.get(action);

            for (Iterator j = principals.iterator(); j.hasNext();) {
                ACLPrincipal p = (ACLPrincipal) j.next();

                hashCode += p.hashCode();
            }
        }

        return hashCode;
    }

    public Object clone() throws CloneNotSupportedException {
        AclImpl clone = new AclImpl(principalManager);
        clone.setInherited(this.inherited);
        clone.setOwner(this.owner);
        // XXX: This is not exactly recommended
        clone.actionLists = this.actionLists;
        return clone;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();

        sb.append("[ACL:");

        for (Iterator i = actionLists.keySet().iterator(); i.hasNext();) {
            String action = (String) i.next();
            List principalList = getPrincipalList(action);

            sb.append(" [");
            sb.append(action);
            sb.append(":");
            for (Iterator j = principalList.iterator(); j.hasNext();) {
                ACLPrincipal p = (ACLPrincipal) j.next();

                sb.append(" ");
                sb.append(p.getUrl());

                if (p.isGroup()) {
                    sb.append("(g)");
                }

                if (j.hasNext()) {
                    sb.append(",");
                }
            }
            sb.append("]");
        }
        sb.append("]");
        return sb.toString();
    }


    /**
     * @param owner The owner to set.
     */
    public void setOwner(Principal owner) {
        this.owner = owner;
    }


    public Principal getOwner() {
        return this.owner;
    }

}

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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.vortikal.repository.Ace;
import org.vortikal.repository.AclException;
import org.vortikal.repository.AuthorizationException;
import org.vortikal.repository.IllegalOperationException;
import org.vortikal.repository.Privilege;
import org.vortikal.repository.PrivilegeDefinition;
import org.vortikal.security.AuthenticationException;
import org.vortikal.security.InvalidPrincipalException;
import org.vortikal.security.Principal;
import org.vortikal.security.PrincipalManager;
import org.vortikal.security.roles.RoleManager;


public class ACL implements Cloneable {

    private static Log logger = LogFactory.getLog(ACL.class);

    /**
     * map: [action --> List(ACLPrincipal)]
     */
    private Map actionLists = new HashMap();

    public ACL(Map actionLists) {

        validateActionMap(actionLists);
        this.actionLists = actionLists;
    }

    public Map getActionMap() {
        return actionLists;
    }

    public void setActionMap(Map actionLists) {
        validateActionMap(actionLists);
        this.actionLists = actionLists;
    }

    public void authorize(Principal principal, String action, Resource resource,
                          PrincipalManager principalManager, RoleManager roleManager)
        throws AuthenticationException, AuthorizationException, IOException {
        /*
         * Special treatment for uio:read-processed needed:
         * dav:read also grants uio:read-processed
         */
        if (action.equals(Resource.CUSTOM_PRIVILEGE_READ_PROCESSED)) {
            try {
                authorize(principal, PrivilegeDefinition.READ, resource, principalManager, roleManager);

                return;
            } catch (AuthenticationException e) {
                /* Handle below */
            } catch (AuthorizationException e) {
                /* Handle below */
            }
        }

        List principalList = getPrincipalList(action);

        /*
         * A user is granted access if one of these conditions are met:
         *
         *
         * 1) (dav:all, action) is present in the resource's ACL
         *
         * The rest requires that the user is authenticated:
         *
         * 2) user is authenticated and dav:authenticated is present
         *
         * 3a) user has role ROOT
         *
         * 3b) action = 'read' and user has role READ_EVERYTHING
         *
         * 4a) dav:owner evaluates to user and action is dav:read,
         *     dav:write or dav:write-acl (COMMENTED OUT)
         *
         * The rest is meaningless if principalList == null:
         *
         * 4b) dav:owner evaluates to user and (dav:owner, action) is
         *     present in the resource's ACL
         *
         * 5) (user, action) is present in the resource's ACL
         *
         * 6) (g, action) is present in the resource's ACL, where g
         *    is a group identifier and the user is a member of that group
         * */

        // Condition 1:
        if (userMatch(principalList, "dav:all")) {
            return;
        }

        // If not condition 1 - needs to be authenticated
        if (principal == null) {
            throw new AuthenticationException();
        }

        // Condition 2:
        if (userMatch(principalList, "dav:authenticated")) {
            return;
        }
        
        // Condition 3a:
        if (roleManager.hasRole(principal.getQualifiedName(), RoleManager.ROOT)) {
            return;
        }

        // Condition 3b:
        if (PrivilegeDefinition.READ.equals(action) &&
                roleManager.hasRole(principal.getQualifiedName(),
                    RoleManager.READ_EVERYTHING)) {
            return;
        }

        // Condition 4a:
        //         if (resource.getOwner().equals(principal.getQualifiedName())) {
        //             if (action.equals(PrivilegeDefinition.READ) || 
        //                 action.equals(PrivilegeDefinition.WRITE) ||
        //                 action.equals(PrivilegeDefinition.WRITE_ACL)) {
        //                 return;
        //             }
        //         }
        // Dont't need to test the remaining conditions if (principalList == null)
        if (principalList == null) {
            throw new AuthorizationException();
        }

        if (resource.getOwner().equals(principal.getQualifiedName())) {
            if (userMatch(principalList, "dav:owner")) {
                return;
            }
        }

        // Condition 5:
        if (userMatch(principalList, principal.getQualifiedName())) {
            return;
        }

        // Condition 6:
        if (groupMatch(principalList, principal, principalManager)) {
            return;
        }

        throw new AuthorizationException();
    }

    /**
     * Generates a list of Ace objects (for data exchange).
     *
     * @return an <code>Ace[]</code>
     */
    public Ace[] toAceList(Resource resource) {
        Set actions = actionLists.keySet();

        HashMap userMap = new HashMap();

        /*
         * Add ((dav:owner (dav:read dav:write dav:write-acl))
         */

        //         HashSet owner = new HashSet();
        //         owner.add(PrivilegeDefinition.READ);
        //         owner.add(PrivilegeDefinition.WRITE);
        //         owner.add(PrivilegeDefinition.WRITE_ACL);
        //         userMap.put(new ACLPrincipal("dav:owner"), owner);
        for (Iterator i = actions.iterator(); i.hasNext();) {
            String action = (String) i.next();

            List principalList = getPrincipalList(action);

            for (Iterator j = principalList.iterator(); j.hasNext();) {
                ACLPrincipal p = (ACLPrincipal) j.next();

                if (!userMap.containsKey(p)) {
                    HashSet actionSet = new HashSet();

                    userMap.put(p, actionSet);
                }

                HashSet actionSet = (HashSet) userMap.get(p);

                actionSet.add(action);
            }
        }

        Ace[] acl = new Ace[userMap.size()];
        int aclIndex = 0;

        /* Create the ACE's  */
        for (Iterator i = userMap.keySet().iterator(); i.hasNext();) {
            ACLPrincipal p = (ACLPrincipal) i.next();

            /* Create the principal */
            org.vortikal.repository.ACLPrincipal principal = new org.vortikal.repository.ACLPrincipal();

            if (p.getUrl().equals("dav:all")) {
                principal.setType(org.vortikal.repository.ACLPrincipal.TYPE_ALL);
            } else if (p.getUrl().equals("dav:owner")) {
                principal.setType(org.vortikal.repository.ACLPrincipal.TYPE_OWNER);
            } else if (p.getUrl().equals("dav:authenticated")) {
                principal.setType(org.vortikal.repository.ACLPrincipal.TYPE_AUTHENTICATED);
            } else {
                principal.setType(org.vortikal.repository.ACLPrincipal.TYPE_URL);
                principal.setIsUser(!p.isGroup());
                principal.setURL(p.getUrl());
            }

            /* Create the ACE */
            Ace element = new Ace();

            element.setPrincipal(principal);

            ArrayList privs = new ArrayList();

            for (Iterator j = ((Set) userMap.get(p)).iterator(); j.hasNext();) {
                String action = (String) j.next();
                Privilege priv = new Privilege();

                priv.setName(action);

                // FIXME: Hack coming up:
                if (action.equals(Resource.CUSTOM_PRIVILEGE_READ_PROCESSED)) {
                    priv.setNamespace(Resource.CUSTOM_NAMESPACE);
                } else {
                    priv.setNamespace(PrivilegeDefinition.STANDARD_NAMESPACE);
                }

                privs.add(priv);
            }

            Privilege[] privileges = (Privilege[]) privs.toArray(new Privilege[] {
                        
                    });

            element.setPrivileges(privileges);
            element.setGranted(true);

            if (resource.isInheritedACL()) {
                element.setInheritedFrom(resource.getParentURI());
            }

            acl[aclIndex++] = element;
        }

        return acl;
    }

    /**
     * Decides whether a given principal exists in a principal list.
     *
     * @param principalList a <code>List</code> value
     * @param username a <code>String</code> value
     * @return a <code>boolean</code>
     */
    public boolean userMatch(List principalList, String username) {
        if (principalList != null) {
            return principalList.contains(new ACLPrincipal(username));
        }

        return false;
    }

    public boolean groupMatch(List principalList, Principal principal, PrincipalManager principalManager)
        throws IOException {
        for (Iterator i = principalList.iterator(); i.hasNext();) {
            ACLPrincipal p = (ACLPrincipal) i.next();

            if (p.isGroup()) {
                if (principalManager.isMember(principal, p.getUrl())) {
                    return true;
                }
            }
        }

        return false;
    }

    public List getPrincipalList(String action) {
        return (List) actionLists.get(action);
    }

    /**
     * Checks the validity of an ACL.
     *
     * @param aceList an <code>Ace[]</code> value
     * @exception AclException if an error occurs
     * @exception IllegalOperationException if an error occurs
     * @exception IOException if an error occurs
     */
    public static void validateACL(Ace[] aceList, PrincipalManager principalManager)
        throws AclException, IllegalOperationException, IOException {
        /*
         * Enforce ((dav:owner (dav:read dav:write dav:write-acl))
         */
        if (!containsUserPrivilege(aceList, PrivilegeDefinition.WRITE,
                    "dav:owner")) {
            throw new IllegalOperationException(
                "Owner must be granted write privilege in ACL.");
        }

        if (!containsUserPrivilege(aceList, PrivilegeDefinition.READ,
                    "dav:owner")) {
            throw new IllegalOperationException(
                "Owner must be granted read privilege in ACL.");
        }

        if (!containsUserPrivilege(aceList, PrivilegeDefinition.WRITE_ACL,
                    "dav:owner")) {
            throw new IllegalOperationException(
                "Owner must be granted write-acl privilege in ACL.");
        }

        boolean inheritance = aceList[0].getInheritedFrom() != null;

        /*
         * Walk trough the ACL, for every ACE, enforce that:
         * 1) Privileges are never denied (only granted)
         * 2) Either every ACE is inherited or none
         * 3) Every principal is valid
         * 4) Every privilege has a supported namespace and name
         */
        for (int i = 0; i < aceList.length; i++) {
            Ace ace = aceList[i];

            if (!ace.isGranted()) {
                throw new AclException(AclException.GRANT_ONLY,
                    "Privileges may only be granted, not denied.");
            }

            if ((ace.getInheritedFrom() != null) != inheritance) {
                throw new IllegalOperationException(
                    "Either every ACE must be inherited from a resource, " +
                    "or none.");
            }

            org.vortikal.repository.ACLPrincipal principal = ace.getPrincipal();

            if (principal.getType() == org.vortikal.repository.ACLPrincipal.TYPE_URL) {
                boolean validPrincipal = false;

                if (principal.isUser()) {
                    Principal p = null;
                    try {
                        p = principalManager.getPrincipal(principal.getURL());
                    } catch (InvalidPrincipalException e) {
                        throw new AclException("Invalid principal '" 
                                + principal.getURL() + "' in ACL");
                    }
                    validPrincipal = principalManager.validatePrincipal(p);
                } else {
                    validPrincipal = principalManager.validateGroup(principal.getURL());
                }

                if (!validPrincipal) {
                    throw new AclException(AclException.RECOGNIZED_PRINCIPAL,
                        "Unknown principal: " + principal.getURL());
                }
            } else {
                if ((principal.getType() != org.vortikal.repository.ACLPrincipal.TYPE_ALL) &&
                        (principal.getType() != org.vortikal.repository.ACLPrincipal.TYPE_OWNER) &&
                        (principal.getType() != org.vortikal.repository.ACLPrincipal.TYPE_AUTHENTICATED)) {
                    throw new AclException(AclException.RECOGNIZED_PRINCIPAL,
                        "Allowed principal types are " +
                        "either TYPE_ALL, TYPE_OWNER " + "OR  TYPE_URL.");
                }
            }

            Privilege[] privileges = ace.getPrivileges();

            for (int j = 0; j < privileges.length; j++) {
                Privilege privilege = privileges[j];

                if (privilege.getNamespace().equals(PrivilegeDefinition.STANDARD_NAMESPACE)) {
                    if (!(privilege.getName().equals(PrivilegeDefinition.WRITE) ||
                            privilege.getName().equals(PrivilegeDefinition.READ) ||
                            privilege.getName().equals(PrivilegeDefinition.WRITE_ACL))) {
                        throw new AclException(AclException.NOT_SUPPORTED_PRIVILEGE,
                            "Unsupported privilege name: " +
                            privilege.getName());
                    }
                } else if (privilege.getNamespace().equals(Resource.CUSTOM_NAMESPACE)) {
                    if (!(privilege.getName().equals(Resource.CUSTOM_PRIVILEGE_READ_PROCESSED))) {
                        throw new AclException(AclException.NOT_SUPPORTED_PRIVILEGE,
                            "Unsupported privilege name: " +
                            privilege.getName());
                    }
                } else {
                    throw new AclException(AclException.NOT_SUPPORTED_PRIVILEGE,
                        "Unsupported privilege namespace: " +
                        privilege.getNamespace());
                }
            }
        }
    }

    public boolean equals(Object o) {
        if (!(o instanceof ACL)) {
            return false;
        }

        ACL acl = (ACL) o;

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

    protected Privilege[] getCurrentUserPrivileges(
        Principal principal, Resource resource, PrincipalManager principalManager,
        RoleManager roleManager) throws IOException {

        ArrayList privs = new ArrayList();

        String[] testedPrivileges = new String[] {
                PrivilegeDefinition.WRITE, PrivilegeDefinition.READ,
                PrivilegeDefinition.WRITE_ACL,
                Resource.CUSTOM_PRIVILEGE_READ_PROCESSED
            };

        for (int i = 0; i < testedPrivileges.length; i++) {
            String action = testedPrivileges[i];

            /* Try to authorize against every privilege, and if it
             * works, add the privilege to the list: */
            try {
                this.authorize(principal, action, resource, principalManager, roleManager);

                Privilege priv = new Privilege();

                priv.setName(action);
                privs.add(priv);
            } catch (AuthenticationException e) {
                // ignore
            } catch (AuthorizationException e) {
                // ignore
            }
        }

        return (Privilege[]) privs.toArray(new Privilege[] {});
    }

    protected void validateActionMap(Map actionLists) {
        for (Iterator i = actionLists.keySet().iterator(); i.hasNext();) {
            Object key = i.next();

            if (!(key instanceof String)) {
                throw new RuntimeException("Action is not a string.");
            }

            Object val = actionLists.get(key);

            if (!(val instanceof List)) {
                throw new RuntimeException("Value not a list");
            }

            for (Iterator j = ((List) val).iterator(); j.hasNext();) {
                Object principal = j.next();

                if (!(principal instanceof ACLPrincipal)) {
                    throw new RuntimeException(
                        "Items in list must be of class ACLPrincipal.");
                }
            }
        }
    }

    /*
     * Build an ACL object from the Ace[] array (assumes valid
     * input, except that principal names of URL-type principals are
     * trimmed of whitespace, since the principal manager may be
     * tolerant and pass these trough validation).
     */
    public static ACL buildACL(Ace[] aceList, PrincipalManager principalManager)
        throws AclException {
        HashMap privilegeMap = new HashMap();

        for (int i = 0; i < aceList.length; i++) {
            Ace ace = aceList[i];
            org.vortikal.repository.ACLPrincipal principal = ace.getPrincipal();
            String principalName = null;

            if (principal.getType() == org.vortikal.repository.ACLPrincipal.TYPE_URL) {
                //TODO: document behaviour where name = name@defaultdomain
                if (principal.isUser())
                    principalName = principalManager.getPrincipal(
                            principal.getURL().trim()).getQualifiedName();
                else 
                    principalName = principal.getURL().trim();
            } else if (principal.getType() == org.vortikal.repository.ACLPrincipal.TYPE_ALL) {
                principalName = "dav:all";
            } else if (principal.getType() == org.vortikal.repository.ACLPrincipal.TYPE_OWNER) {
                principalName = "dav:owner";
            } else if (principal.getType() == org.vortikal.repository.ACLPrincipal.TYPE_AUTHENTICATED) {
                principalName = "dav:authenticated";
            } else {
                throw new AclException(AclException.RECOGNIZED_PRINCIPAL,
                    "Unknown principal: " + principal.getURL());
            }

            Privilege[] privileges = ace.getPrivileges();

            for (int j = 0; j < privileges.length; j++) {
                Privilege privilege = privileges[j];

                /*
                 * We don't store the required ACE
                 * ((dav:owner (dav:read dav:write dav:write-acl))
                 */

                //                 if (principalName.equals("dav:owner") &&
                //                     (privilege.getName().equals(PrivilegeDefinition.READ) ||
                //                      privilege.getName().equals(PrivilegeDefinition.WRITE) ||
                //                      privilege.getName().equals(PrivilegeDefinition.WRITE_ACL))) {
                //                     continue;
                //                 }
                // Add an entry for (privilege, principal)
                if (!privilegeMap.containsKey(privilege.getName())) {
                    privilegeMap.put(privilege.getName(), new ArrayList());
                }

                List principals = (List) privilegeMap.get(privilege.getName());

                ACLPrincipal p = new ACLPrincipal(principalName,
                        !principal.isUser());

                if (!principals.contains(p)) {
                    principals.add(p);
                }
            }
        }

        //ACL acl = new ACL(privilegeMap, principalManager);
        ACL acl = new ACL(privilegeMap);

        return acl;
    }

    /**
     * Checks if an ACL grants a given privilege to a given principal.
     *
     */
    protected static boolean containsUserPrivilege(Ace[] aceList,
        String privilegeName, String principalURL) {
        for (int i = 0; i < aceList.length; i++) {
            Ace ace = aceList[i];

            Privilege[] privileges = ace.getPrivileges();

            for (int j = 0; j < privileges.length; j++) {
                Privilege priv = privileges[j];

                org.vortikal.repository.ACLPrincipal principal = ace.getPrincipal();

                if ((principal.getType() == org.vortikal.repository.ACLPrincipal.TYPE_URL) &&
                        principal.getURL().equals(principalURL) &&
                        priv.getName().equals(privilegeName)) {
                    return true;
                } else if (principal.getType() == org.vortikal.repository.ACLPrincipal.TYPE_OWNER) {
                    return true;
                } else if (principal.getType() == org.vortikal.repository.ACLPrincipal.TYPE_ALL) {
                    if (priv.getName().equals(privilegeName)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
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

    public void dump() {
        dump(logger);
    }

    public void dump(Log logger) {
        if (logger.isDebugEnabled()) {
            logger.debug(toString());
        }
    }

    public void dump(java.io.PrintStream out) {
        out.println(toString());
    }
}

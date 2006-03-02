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
package org.vortikal.repositoryimpl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.vortikal.repository.Ace;
import org.vortikal.repository.AclException;
import org.vortikal.repository.AuthorizationException;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Privilege;
import org.vortikal.repository.PrivilegeDefinition;
import org.vortikal.repositoryimpl.dao.DataAccessor;
import org.vortikal.security.AuthenticationException;
import org.vortikal.security.Principal;
import org.vortikal.security.PrincipalManager;
import org.vortikal.security.roles.RoleManager;
import org.vortikal.util.repository.URIUtil;

public class PermissionsManager {

    private RoleManager roleManager;
    private DataAccessor dao;
    private PrincipalManager principalManager;

    public void authorize(Resource resource, Principal principal, String action)
            throws AuthenticationException, AuthorizationException, IOException {

        ACL acl = resource.getACL();

        /*
         * Special treatment for uio:read-processed needed: dav:read also grants
         * uio:read-processed
         */
        if (action.equals(PrivilegeDefinition.CUSTOM_PRIVILEGE_READ_PROCESSED)) {
            try {
                authorize(resource, principal, PrivilegeDefinition.READ);

                return;
            } catch (AuthenticationException e) {
                /* Handle below */
            } catch (AuthorizationException e) {
                /* Handle below */
            }
        }

        List principalList = acl.getPrincipalList(action);

        /*
         * A user is granted access if one of these conditions are met:
         * 
         * 
         * 1) (dav:all, action) is present in the resource's ACL. NOTE: Now
         * limits this to read operations
         * 
         * The rest requires that the user is authenticated:
         * 
         * 2) user is authenticated and dav:authenticated is present
         * 
         * 3a) user has role ROOT
         * 
         * 3b) action = 'read' and user has role READ_EVERYTHING
         * 
         * 4a) dav:owner evaluates to user and action is dav:read, dav:write or
         * dav:write-acl (COMMENTED OUT)
         * 
         * The rest is meaningless if principalList == null:
         * 
         * 4b) dav:owner evaluates to user and (dav:owner, action) is present in
         * the resource's ACL
         * 
         * 5) (user, action) is present in the resource's ACL
         * 
         * 6) (g, action) is present in the resource's ACL, where g is a group
         * identifier and the user is a member of that group
         */

        // Condition 1:
        if (userMatch(principalList, "dav:all")
                && (PrivilegeDefinition.READ.equals(action) 
                        || PrivilegeDefinition.CUSTOM_PRIVILEGE_READ_PROCESSED
                        .equals(action))) {
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
        if (PrivilegeDefinition.READ.equals(action)
                && roleManager.hasRole(principal.getQualifiedName(),
                        RoleManager.READ_EVERYTHING)) {
            return;
        }

        // Condition 4a:
        // if (resource.getOwner().equals(principal.getQualifiedName())) {
        // if (action.equals(PrivilegeDefinition.READ) ||
        // action.equals(PrivilegeDefinition.WRITE) ||
        // action.equals(PrivilegeDefinition.WRITE_ACL)) {
        // return;
        // }
        // }
        // Dont't need to test the remaining conditions if (principalList ==
        // null)
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
        if (groupMatch(principalList, principal)) {
            return;
        }

        throw new AuthorizationException();
    }

    /**
     * Decides whether a given principal exists in a principal list.
     * 
     * @param principalList
     *            a <code>List</code> value
     * @param username
     *            a <code>String</code> value
     * @return a <code>boolean</code>
     */
    private boolean userMatch(List principalList, String username) {
        if (principalList != null) {
            return principalList.contains(new ACLPrincipal(username));
        }

        return false;
    }

    private boolean groupMatch(List principalList, Principal principal) {

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

    /*
     * Build an ACL object from the Ace[] array (assumes valid
     * input, except that principal names of URL-type principals are
     * trimmed of whitespace, since the principal manager may be
     * tolerant and pass these trough validation).
     */
    public ACL buildACL(Ace[] aceList)
        throws AclException {

        ACL acl = new ACL();
        Map privilegeMap = acl.getActionMap();

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

        return acl;
    }
    
    public void addPermissionsToDTO(Resource resource, 
            org.vortikal.repository.Resource dto) throws IOException {
        try {
            ACL originalACL = (ACL) resource.acl.clone();

            dto.setACL(addRolesToACL(resource, originalACL));

            if ("/".equals(resource.getURI())) {
                dto.setParentACL(new Ace[0]);
            } else {
                Resource parent = 
                    this.dao.load(URIUtil.getParentURI(resource.getURI()));
                ACL parentACL = (ACL) parent.getACL().clone();

                dto.setParentACL(addRolesToACL(resource, parentACL));
                dto.setParentOwner(principalManager.getPrincipal(parent.getOwner()));
            }
        } catch (CloneNotSupportedException e) {
        }
        
    }

    /**
     * Generates a list of Ace objects (for data exchange).
     *
     * @return an <code>Ace[]</code>
     */
    public Ace[] toAceList(ACL acl, String inheritedFrom) {

        Set actions = acl.getActionMap().keySet();

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

            List principalList = acl.getPrincipalList(action);

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

        Ace[] aces = new Ace[userMap.size()];
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
                if (action.equals(PrivilegeDefinition.CUSTOM_PRIVILEGE_READ_PROCESSED)) {
                    priv.setNamespace(Namespace.CUSTOM_NAMESPACE);
                } else {
                    priv.setNamespace(Namespace.STANDARD_NAMESPACE);
                }

                privs.add(priv);
            }

            Privilege[] privileges = (Privilege[]) privs.toArray(new Privilege[] {
                        
                    });

            element.setPrivileges(privileges);
            element.setGranted(true);

            element.setInheritedFrom(inheritedFrom);

            aces[aclIndex++] = element;
        }

        return aces;
    }


    /** To be removed...?
     * 
     * @deprecated
     * @param principal
     * @param resource
     * @param principalManager
     * @param roleManager
     * @return
     * @throws IOException
     */
    protected Privilege[] getCurrentUserPrivileges(
        Principal principal, Resource resource, PrincipalManager principalManager,
        RoleManager roleManager) throws IOException {

        ArrayList privs = new ArrayList();

        String[] testedPrivileges = new String[] {
                PrivilegeDefinition.WRITE, PrivilegeDefinition.READ,
                PrivilegeDefinition.WRITE_ACL,
                PrivilegeDefinition.CUSTOM_PRIVILEGE_READ_PROCESSED
            };

        for (int i = 0; i < testedPrivileges.length; i++) {
            String action = testedPrivileges[i];

            /* Try to authorize against every privilege, and if it
             * works, add the privilege to the list: */
            try {
                authorize(resource, principal, action);

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



    
    /**
     * Adds root and read everything roles to ACL
     * 
     * @param originalACL
     *            an <code>Ace[]</code> value
     * @return an <code>Ace[]</code>
     */
    private Ace[] addRolesToACL(Resource resource, ACL originalACL) {

        String inheritedFrom = null;
        if (resource.isInheritedACL()) {
            inheritedFrom = URIUtil.getParentURI(resource.getURI());
        }

        List acl = new ArrayList(Arrays.asList(toAceList(originalACL, inheritedFrom)));
        List rootPrincipals = roleManager.listPrincipals(RoleManager.ROOT);

        for (Iterator i = rootPrincipals.iterator(); i.hasNext();) {
            String root = (String) i.next();
            org.vortikal.repository.ACLPrincipal aclPrincipal = org.vortikal.repository.ACLPrincipal.getInstance(org.vortikal.repository.ACLPrincipal.TYPE_URL,
                    root, true);
            Ace ace = new Ace();

            ace.setPrincipal(aclPrincipal);
            ace.setPrivileges(getRootPrivileges());
            acl.add(ace);
        }

        List readPrincipals = roleManager.listPrincipals(RoleManager.READ_EVERYTHING);

        for (Iterator i = readPrincipals.iterator(); i.hasNext();) {
            String read = (String) i.next();
            org.vortikal.repository.ACLPrincipal aclPrincipal = org.vortikal.repository.ACLPrincipal.getInstance(org.vortikal.repository.ACLPrincipal.TYPE_URL,
                    read, true);
            Ace ace = new Ace();

            ace.setPrincipal(aclPrincipal);
            ace.setPrivileges(getReadPrivileges());
            acl.add(ace);
        }

        return (Ace[]) acl.toArray(new Ace[0]);
    }

    
    private Privilege[] getRootPrivileges() {
        Privilege read = new Privilege();

        read.setName(PrivilegeDefinition.READ);

        Privilege write = new Privilege();

        write.setName(PrivilegeDefinition.WRITE);

        Privilege writeACL = new Privilege();

        writeACL.setName(PrivilegeDefinition.WRITE_ACL);

        Privilege[] rootPrivs = new Privilege[3];

        rootPrivs[0] = read;
        rootPrivs[1] = write;
        rootPrivs[2] = writeACL;

        return rootPrivs;
    }

    private Privilege[] getReadPrivileges() {
        Privilege read = new Privilege();

        read.setName(PrivilegeDefinition.READ);

        Privilege[] readPrivs = new Privilege[1];

        readPrivs[0] = read;

        return readPrivs;
    }


    /**
     * @param dao The dao to set.
     */
    public void setDao(DataAccessor dao) {
        this.dao = dao;
    }


    /**
     * @param principalManager The principalManager to set.
     */
    public void setPrincipalManager(PrincipalManager principalManager) {
        this.principalManager = principalManager;
    }


    /**
     * @param roleManager The roleManager to set.
     */
    public void setRoleManager(RoleManager roleManager) {
        this.roleManager = roleManager;
    }


}
